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
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RopePersistence {
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<PersistedLink> pendingLinks = new ArrayList<>();

    private RopePersistence() {
    }

    public static void loadPending(MinecraftServer server, RopeManager ropeManager) {
        pendingLinks.clear();
        if (!RopeConfig.persistRopes()) {
            return;
        }

        Path path = statePath(server);
        if (Files.notExists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            PersistedState state = GSON.fromJson(reader, PersistedState.class);
            if (state == null || state.links == null || state.links.isEmpty()) {
                return;
            }

            pendingLinks.addAll(state.links);
            McElmaRopeMod.LOGGER.info("Loaded {} persisted rope record(s).", pendingLinks.size());
            restoreAvailable(server, ropeManager);
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to load persisted MC-ELMA Rope state at {}.", path, exception);
            pendingLinks.clear();
        }
    }

    public static void restoreAvailable(MinecraftServer server, RopeManager ropeManager) {
        if (!RopeConfig.persistRopes() || pendingLinks.isEmpty()) {
            return;
        }

        Iterator<PersistedLink> iterator = pendingLinks.iterator();
        while (iterator.hasNext()) {
            PersistedLink persisted = iterator.next();
            Optional<RopeLink> link = persisted.toLink(server);
            if (link.isEmpty()) {
                continue;
            }

            RopeManager.AddResult result = ropeManager.restoreLink(server, link.get());
            if (result == RopeManager.AddResult.ADDED) {
                iterator.remove();
                notifyRestored(server, link.get());
            } else if (result != RopeManager.AddResult.FULL) {
                iterator.remove();
                McElmaRopeMod.LOGGER.warn("Dropped persisted rope {} during restore: {}.", persisted.id, result);
            }
        }
    }

    public static void save(MinecraftServer server, RopeManager ropeManager) {
        if (!RopeConfig.persistRopes()) {
            pendingLinks.clear();
            return;
        }

        Path path = statePath(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(PersistedState.from(ropeManager.links()), writer);
            }
            McElmaRopeMod.LOGGER.info("Saved {} rope link(s) to {}.", ropeManager.activeCount(), path);
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to save persisted MC-ELMA Rope state at {}.", path, exception);
        }
    }

    static void clearForTests(MinecraftServer server) {
        pendingLinks.clear();
        try {
            Files.deleteIfExists(statePath(server));
        } catch (IOException | RuntimeException exception) {
            McElmaRopeMod.LOGGER.warn("Failed to clear persisted MC-ELMA Rope test state.", exception);
        }
    }

    static int pendingLinkCountForTests() {
        return pendingLinks.size();
    }

    static void writeStateJsonForTests(MinecraftServer server, String json) {
        Path path = statePath(server);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, json);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Failed to write persisted rope test state.", exception);
        }
    }

    private static void notifyRestored(MinecraftServer server, RopeLink link) {
        notifyEndpoint(server, link.first());
        notifyEndpoint(server, link.second());
    }

    private static void notifyEndpoint(MinecraftServer server, RopeEndpoint endpoint) {
        if (endpoint.type() == RopeEndpoint.Type.PLAYER) {
            endpoint.resolvePlayer(server).ifPresent(player -> player.sendMessage(
                    net.minecraft.text.Text.literal("Rope state restored after restart."),
                    true));
        }
    }

    private static Path statePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("mc_elma_rope_state.json");
    }

    private static final class PersistedState {
        int schemaVersion = SCHEMA_VERSION;
        List<PersistedLink> links = List.of();

        static PersistedState from(List<RopeLink> activeLinks) {
            PersistedState state = new PersistedState();
            state.links = activeLinks.stream().map(PersistedLink::from).toList();
            return state;
        }
    }

    private static final class PersistedLink {
        String id;
        String controllerUuid;
        PersistedEndpoint first;
        PersistedEndpoint second;
        double length;
        boolean refundLeadOnManualRelease;
        int ageTicks;

        static PersistedLink from(RopeLink link) {
            PersistedLink persisted = new PersistedLink();
            persisted.id = link.id().toString();
            persisted.controllerUuid = link.controllerUuid().toString();
            persisted.first = PersistedEndpoint.from(link.first());
            persisted.second = PersistedEndpoint.from(link.second());
            persisted.length = link.length();
            persisted.refundLeadOnManualRelease = link.refundLeadOnManualRelease();
            persisted.ageTicks = link.ageTicks();
            return persisted;
        }

        Optional<RopeLink> toLink(MinecraftServer server) {
            Optional<UUID> parsedId = parseUuid(id);
            Optional<UUID> parsedController = parseUuid(controllerUuid);
            if (parsedId.isEmpty() || parsedController.isEmpty() || first == null || second == null) {
                return Optional.empty();
            }

            Optional<RopeEndpoint> firstEndpoint = first.toEndpoint(server);
            Optional<RopeEndpoint> secondEndpoint = second.toEndpoint(server);
            if (firstEndpoint.isEmpty() || secondEndpoint.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(RopeLink.restored(
                    parsedId.get(),
                    parsedController.get(),
                    firstEndpoint.get(),
                    secondEndpoint.get(),
                    length,
                    refundLeadOnManualRelease,
                    ageTicks));
        }
    }

    private static final class PersistedEndpoint {
        String type;
        String playerUuid;
        String world;
        double x;
        double y;
        double z;

        static PersistedEndpoint from(RopeEndpoint endpoint) {
            PersistedEndpoint persisted = new PersistedEndpoint();
            persisted.type = endpoint.type().name().toLowerCase(java.util.Locale.ROOT);
            if (endpoint.type() == RopeEndpoint.Type.PLAYER) {
                persisted.playerUuid = endpoint.playerUuid().toString();
            } else {
                persisted.world = endpoint.worldKey().getValue().toString();
                persisted.x = endpoint.anchorPosition().x;
                persisted.y = endpoint.anchorPosition().y;
                persisted.z = endpoint.anchorPosition().z;
            }
            return persisted;
        }

        Optional<RopeEndpoint> toEndpoint(MinecraftServer server) {
            if ("player".equals(type)) {
                Optional<UUID> uuid = parseUuid(playerUuid);
                if (uuid.isEmpty()) {
                    return Optional.empty();
                }

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid.get());
                return player == null ? Optional.empty() : Optional.of(RopeEndpoint.player(player));
            }
            if ("anchor".equals(type)) {
                Identifier worldId = Identifier.tryParse(world);
                if (worldId == null) {
                    return Optional.empty();
                }

                RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldId);
                if (server.getWorld(worldKey) == null) {
                    return Optional.empty();
                }
                return Optional.of(RopeEndpoint.anchor(worldKey, new Vec3d(x, y, z)));
            }
            return Optional.empty();
        }
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
