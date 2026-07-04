package com.mcelma.rope;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameMode;

public final class RopeDisconnectPolicy {
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<PenaltyRecord> pendingPenalties = new ArrayList<>();

    private RopeDisconnectPolicy() {
    }

    public static void load(MinecraftServer server) {
        pendingPenalties.clear();
        if (!RopeConfig.enableDisconnectPenalty() || !RopeConfig.persistDisconnectPenalties()) {
            return;
        }

        Path path = statePath(server);
        if (Files.notExists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            PenaltyState state = GSON.fromJson(reader, PenaltyState.class);
            if (state == null || state.penalties == null || state.penalties.isEmpty()) {
                return;
            }
            pendingPenalties.addAll(state.penalties.stream().filter(PenaltyRecord::isValid).toList());
            McElmaRopeMod.LOGGER.info("Loaded {} pending rope disconnect penalty record(s).",
                    pendingPenalties.size());
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to load MC-ELMA Rope disconnect penalties at {}.", path, exception);
            pendingPenalties.clear();
        }
    }

    public static void save(MinecraftServer server) {
        if (!RopeConfig.persistDisconnectPenalties()) {
            return;
        }

        Path path = statePath(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(PenaltyState.from(pendingPenalties), writer);
            }
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to save MC-ELMA Rope disconnect penalties at {}.", path, exception);
        }
    }

    public static void handleDisconnect(MinecraftServer server, ServerPlayerEntity disconnected, RopeManager ropeManager) {
        List<RopeLink> removed = ropeManager.removeLinksForPlayer(
                server,
                disconnected.getUuid(),
                "The rope was released because a player disconnected.");
        if (removed.isEmpty()) {
            return;
        }

        for (RopeLink link : removed) {
            if (!isDisconnectingTarget(disconnected.getUuid(), link)) {
                continue;
            }
            if (RopeConfig.refundLeadToControllerOnTargetDisconnect()) {
                refundController(server, link);
            }
            if (shouldPunish(link)) {
                queuePenalty(server, disconnected, link);
            }
        }

        if (RopeConfig.logRopeEvents()) {
            McElmaRopeMod.LOGGER.info("Cleaned {} rope link(s) after {} disconnected.",
                    removed.size(),
                    disconnected.getName().getString());
        }
    }

    public static void handleJoin(MinecraftServer server, ServerPlayerEntity player) {
        if (!RopeConfig.enableDisconnectPenalty() || pendingPenalties.isEmpty()) {
            return;
        }

        Iterator<PenaltyRecord> iterator = pendingPenalties.iterator();
        while (iterator.hasNext()) {
            PenaltyRecord record = iterator.next();
            if (!player.getUuidAsString().equals(record.playerUuid)) {
                continue;
            }

            iterator.remove();
            applyPenalty(player);
            if (RopeConfig.persistDisconnectPenalties()) {
                save(server);
            }
            return;
        }
    }

    static void clearPendingPenaltiesForTests() {
        pendingPenalties.clear();
    }

    static int pendingPenaltyCountForTests() {
        return pendingPenalties.size();
    }

    private static boolean isDisconnectingTarget(UUID disconnectedUuid, RopeLink link) {
        return !link.controllerUuid().equals(disconnectedUuid)
                && link.includesPlayer(disconnectedUuid)
                && (link.hasOnlyPlayerEndpoints() || link.otherEndpoint(disconnectedUuid) != null);
    }

    private static boolean shouldPunish(RopeLink link) {
        return RopeConfig.enableDisconnectPenalty()
                && (!RopeConfig.disconnectPenaltyOnlyLeadCreatedRopes() || link.refundLeadOnManualRelease());
    }

    private static void refundController(MinecraftServer server, RopeLink link) {
        if (!link.refundLeadOnManualRelease()) {
            return;
        }

        ServerPlayerEntity controller = server.getPlayerManager().getPlayer(link.controllerUuid());
        if (controller == null || isCreativeMode(controller)) {
            return;
        }

        ItemStack lead = new ItemStack(Items.LEAD);
        if (!controller.giveItemStack(lead) && !lead.isEmpty()) {
            controller.dropItem(lead, false);
        }
        controller.sendMessage(Text.literal("A tied player disconnected. Your lead was returned."), true);
    }

    private static void queuePenalty(MinecraftServer server, ServerPlayerEntity player, RopeLink link) {
        pendingPenalties.removeIf(record -> player.getUuidAsString().equals(record.playerUuid));
        pendingPenalties.add(PenaltyRecord.from(server, player, link));
        player.sendMessage(Text.literal("Disconnecting while tied will apply a penalty when you return."), false);
        if (RopeConfig.logRopeEvents()) {
            McElmaRopeMod.LOGGER.info("Queued disconnect penalty for {} from rope {}.",
                    player.getName().getString(),
                    link.id());
        }
        if (RopeConfig.persistDisconnectPenalties()) {
            save(server);
        }
    }

    private static void applyPenalty(ServerPlayerEntity player) {
        int duration = RopeConfig.disconnectPenaltyDurationTicks();
        if (duration <= 0) {
            return;
        }

        int fatigueLevel = RopeConfig.disconnectPenaltyMiningFatigueLevel();
        int slownessLevel = RopeConfig.disconnectPenaltySlownessLevel();
        boolean particles = RopeConfig.disconnectPenaltyShowParticles();
        boolean icon = RopeConfig.disconnectPenaltyShowIcon();

        if (fatigueLevel > 0) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.MINING_FATIGUE,
                    duration,
                    fatigueLevel - 1,
                    false,
                    particles,
                    icon));
        }
        if (slownessLevel > 0) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    duration,
                    slownessLevel - 1,
                    false,
                    particles,
                    icon));
        }

        player.sendMessage(Text.literal("Penalty applied for disconnecting while tied."), true);
        if (RopeConfig.logRopeEvents()) {
            McElmaRopeMod.LOGGER.info("Applied disconnect penalty to {} for {} ticks.",
                    player.getName().getString(),
                    duration);
        }
    }

    private static Path statePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("mc_elma_rope_disconnect_penalties.json");
    }

    private static boolean isCreativeMode(ServerPlayerEntity player) {
        return player.interactionManager.getGameMode() == GameMode.CREATIVE;
    }

    private static final class PenaltyState {
        int schemaVersion = SCHEMA_VERSION;
        List<PenaltyRecord> penalties = List.of();

        static PenaltyState from(List<PenaltyRecord> records) {
            PenaltyState state = new PenaltyState();
            state.penalties = List.copyOf(records);
            return state;
        }
    }

    private static final class PenaltyRecord {
        String playerUuid;
        String controllerUuid;
        String linkId;
        int createdAtTick;

        static PenaltyRecord from(MinecraftServer server, ServerPlayerEntity player, RopeLink link) {
            PenaltyRecord record = new PenaltyRecord();
            record.playerUuid = player.getUuidAsString();
            record.controllerUuid = link.controllerUuid().toString();
            record.linkId = link.id().toString();
            record.createdAtTick = server.getTicks();
            return record;
        }

        boolean isValid() {
            return isUuid(playerUuid) && isUuid(controllerUuid) && isUuid(linkId);
        }

        private static boolean isUuid(String value) {
            try {
                if (value == null) {
                    return false;
                }
                UUID.fromString(value);
                return true;
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
    }
}
