package com.mcelma.rope;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RopeAnchoredOfflinePersistence {
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<OfflineAnchoredLink> pendingAnchoredLinks = new ArrayList<>();

    private RopeAnchoredOfflinePersistence() {
    }

    public static void load(MinecraftServer server) {
        pendingAnchoredLinks.clear();
        if (!RopeConfig.persistAnchoredRopesOnDisconnect()) {
            return;
        }

        Path path = statePath(server);
        if (Files.notExists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            OfflineAnchoredState state = GSON.fromJson(reader, OfflineAnchoredState.class);
            if (state == null || state.links == null || state.links.isEmpty()) {
                return;
            }
            pendingAnchoredLinks.addAll(state.links.stream().filter(OfflineAnchoredLink::isValid).toList());
            McElmaRopeMod.LOGGER.info("Loaded {} offline anchored rope record(s).",
                    pendingAnchoredLinks.size());
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to load offline anchored rope state at {}.", path, exception);
            pendingAnchoredLinks.clear();
        }
    }

    public static void save(MinecraftServer server) {
        if (!RopeConfig.persistAnchoredRopesOnDisconnect()) {
            return;
        }

        Path path = statePath(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(OfflineAnchoredState.from(pendingAnchoredLinks), writer);
            }
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to save offline anchored rope state at {}.", path, exception);
        }
    }

    public static boolean storeDisconnectedAnchoredTarget(
            MinecraftServer server,
            ServerPlayerEntity disconnected,
            RopeLink link) {
        if (!RopeConfig.persistAnchoredRopesOnDisconnect() || link.hasOnlyPlayerEndpoints()) {
            return false;
        }

        RopeEndpoint anchor = link.otherEndpoint(disconnected.getUuid());
        if (anchor == null || anchor.type() != RopeEndpoint.Type.ANCHOR) {
            return false;
        }

        pendingAnchoredLinks.removeIf(record ->
                disconnected.getUuidAsString().equals(record.targetUuid) || link.id().toString().equals(record.id));
        pendingAnchoredLinks.add(OfflineAnchoredLink.from(disconnected, link, anchor));
        save(server);

        if (RopeConfig.logRopeEvents()) {
            McElmaRopeMod.LOGGER.info("Stored offline anchored rope {} for {}.",
                    link.id(),
                    disconnected.getName().getString());
        }
        return true;
    }

    public static int restoreForPlayer(MinecraftServer server, RopeManager ropeManager, ServerPlayerEntity player) {
        if (!RopeConfig.persistAnchoredRopesOnDisconnect() || pendingAnchoredLinks.isEmpty()) {
            return 0;
        }

        int restored = 0;
        boolean changed = false;
        Iterator<OfflineAnchoredLink> iterator = pendingAnchoredLinks.iterator();
        while (iterator.hasNext()) {
            OfflineAnchoredLink record = iterator.next();
            if (!player.getUuidAsString().equals(record.targetUuid)) {
                continue;
            }

            Optional<RopeLink> link = record.toLink(server, player);
            if (link.isEmpty()) {
                iterator.remove();
                changed = true;
                continue;
            }

            RopeManager.AddResult result = ropeManager.restoreLink(server, link.get());
            if (result == RopeManager.AddResult.ADDED) {
                iterator.remove();
                restored++;
                changed = true;
                player.sendMessage(Text.literal("You are still tied to the anchor."), true);
            } else if (result != RopeManager.AddResult.FULL) {
                iterator.remove();
                changed = true;
                McElmaRopeMod.LOGGER.warn("Dropped offline anchored rope {} during restore: {}.",
                        record.id,
                        result);
            }
        }

        if (changed) {
            save(server);
        }
        return restored;
    }

    public static int removeForAnchor(MinecraftServer server, RegistryKey<World> worldKey, Vec3d anchor) {
        if (pendingAnchoredLinks.isEmpty()) {
            return 0;
        }

        int removed = 0;
        Iterator<OfflineAnchoredLink> iterator = pendingAnchoredLinks.iterator();
        while (iterator.hasNext()) {
            OfflineAnchoredLink record = iterator.next();
            if (!record.matchesAnchor(worldKey, anchor)) {
                continue;
            }
            iterator.remove();
            removed++;
        }

        if (removed > 0) {
            save(server);
            if (RopeConfig.logRopeEvents()) {
                McElmaRopeMod.LOGGER.info("Removed {} offline anchored rope record(s) for broken anchor {} in {}.",
                        removed,
                        anchor,
                        worldKey.getValue());
            }
        }
        return removed;
    }

    public static int clearAll(MinecraftServer server) {
        int count = pendingAnchoredLinks.size();
        if (count == 0) {
            return 0;
        }
        pendingAnchoredLinks.clear();
        save(server);
        return count;
    }

    public static int clearForPlayer(MinecraftServer server, UUID playerUuid) {
        String uuid = playerUuid.toString();
        int before = pendingAnchoredLinks.size();
        pendingAnchoredLinks.removeIf(record -> uuid.equals(record.targetUuid) || uuid.equals(record.controllerUuid));
        int removed = before - pendingAnchoredLinks.size();
        if (removed > 0) {
            save(server);
        }
        return removed;
    }

    static void clearForTests(MinecraftServer server) {
        pendingAnchoredLinks.clear();
        try {
            Files.deleteIfExists(statePath(server));
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to clear offline anchored rope test state.", exception);
        }
    }

    static int pendingCountForTests() {
        return pendingAnchoredLinks.size();
    }

    private static Path statePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("mc_elma_rope_anchored_offline.json");
    }

    private static final class OfflineAnchoredState {
        int schemaVersion = SCHEMA_VERSION;
        List<OfflineAnchoredLink> links = List.of();

        static OfflineAnchoredState from(List<OfflineAnchoredLink> links) {
            OfflineAnchoredState state = new OfflineAnchoredState();
            state.links = List.copyOf(links);
            return state;
        }
    }

    private static final class OfflineAnchoredLink {
        String id;
        String controllerUuid;
        String targetUuid;
        String world;
        double x;
        double y;
        double z;
        double length;
        boolean refundLeadOnManualRelease;
        int ageTicks;

        static OfflineAnchoredLink from(ServerPlayerEntity target, RopeLink link, RopeEndpoint anchor) {
            OfflineAnchoredLink record = new OfflineAnchoredLink();
            record.id = link.id().toString();
            record.controllerUuid = link.controllerUuid().toString();
            record.targetUuid = target.getUuidAsString();
            record.world = anchor.worldKey().getValue().toString();
            record.x = anchor.anchorPosition().x;
            record.y = anchor.anchorPosition().y;
            record.z = anchor.anchorPosition().z;
            record.length = link.length();
            record.refundLeadOnManualRelease = link.refundLeadOnManualRelease();
            record.ageTicks = link.ageTicks();
            return record;
        }

        Optional<RopeLink> toLink(MinecraftServer server, ServerPlayerEntity player) {
            Optional<UUID> parsedId = parseUuid(id);
            Optional<UUID> parsedController = parseUuid(controllerUuid);
            Optional<UUID> parsedTarget = parseUuid(targetUuid);
            if (parsedId.isEmpty()
                    || parsedController.isEmpty()
                    || parsedTarget.isEmpty()
                    || !player.getUuid().equals(parsedTarget.get())) {
                return Optional.empty();
            }

            Identifier worldId = Identifier.tryParse(world);
            if (worldId == null) {
                return Optional.empty();
            }

            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
            ServerWorld anchorWorld = server.getWorld(worldKey);
            if (anchorWorld == null) {
                return Optional.empty();
            }
            if (!player.getEntityWorld().getRegistryKey().equals(worldKey)) {
                McElmaRopeMod.LOGGER.warn("Dropped offline anchored rope {} because {} rejoined in {} instead of {}.",
                        id,
                        player.getName().getString(),
                        player.getEntityWorld().getRegistryKey().getValue(),
                        worldKey.getValue());
                return Optional.empty();
            }

            Vec3d anchor = new Vec3d(x, y, z);
            BlockPos anchorPos = BlockPos.ofFloored(anchor.x, anchor.y, anchor.z);
            if (!RopeConfig.isAnchorBlock(anchorWorld, anchorPos) || RopeConfig.isProtectedPlayer(player)) {
                return Optional.empty();
            }

            return Optional.of(RopeLink.restored(
                    parsedId.get(),
                    parsedController.get(),
                    RopeEndpoint.player(player),
                    RopeEndpoint.anchor(worldKey, anchor),
                    length,
                    refundLeadOnManualRelease,
                    ageTicks));
        }

        boolean matchesAnchor(RegistryKey<World> worldKey, Vec3d anchor) {
            Identifier worldId = Identifier.tryParse(world);
            return worldId != null
                    && worldKey.getValue().equals(worldId)
                    && new Vec3d(x, y, z).squaredDistanceTo(anchor) < 1.0E-6D;
        }

        boolean isValid() {
            return isUuid(id)
                    && isUuid(controllerUuid)
                    && isUuid(targetUuid)
                    && Identifier.tryParse(world) != null
                    && Double.isFinite(x)
                    && Double.isFinite(y)
                    && Double.isFinite(z)
                    && Double.isFinite(length);
        }
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static boolean isUuid(String value) {
        return parseUuid(value).isPresent();
    }
}
