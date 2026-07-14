package com.mcelma.rope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RopeManager {
    private final List<RopeLink> activeLinks = new ArrayList<>();

    public void tick(MinecraftServer server) {
        if (activeLinks.isEmpty()) {
            return;
        }

        Iterator<RopeLink> iterator = activeLinks.iterator();
        while (iterator.hasNext()) {
            RopeLink link = iterator.next();
            link.tickAge();
            if (shouldExpire(link, server)) {
                iterator.remove();
                log("Auto-cleared expired rope {}", link.id());
                notifyExpiration(server, link);
                continue;
            }
            if (!RopePhysics.tick(server, link)) {
                iterator.remove();
                log("Auto-cleared invalid rope {}", link.id());
                notifyEndpoint(server, link.first(), "The rope was released automatically.");
                notifyEndpoint(server, link.second(), "The rope was released automatically.");
            }
        }
    }

    public AddResult addAnchor(ServerPlayerEntity player, Vec3d anchor, double length) {
        if (activeLinks.size() >= RopeConfig.maxActiveLinks()) {
            return AddResult.FULL;
        }
        if (RopeConfig.isProtectedPlayer(player)) {
            return AddResult.PROTECTED_PLAYER;
        }

        removeForPlayer(player.getUuid());
        activeLinks.add(new RopeLink(
                player.getUuid(),
                RopeEndpoint.player(player),
                RopeEndpoint.anchor(player.getEntityWorld().getRegistryKey(), anchor),
                length));
        return AddResult.ADDED;
    }

    public AddResult addPlayerLink(ServerPlayerEntity first, ServerPlayerEntity second, double length) {
        return addPlayerLink(first, second, length, false);
    }

    public AddResult addPlayerLink(
            ServerPlayerEntity first,
            ServerPlayerEntity second,
            double length,
            boolean refundLeadOnManualRelease) {
        if (first.getUuid().equals(second.getUuid())) {
            return AddResult.SAME_PLAYER;
        }
        if (!first.getEntityWorld().getRegistryKey().equals(second.getEntityWorld().getRegistryKey())) {
            return AddResult.DIFFERENT_WORLD;
        }
        if (activeLinks.size() >= RopeConfig.maxActiveLinks()) {
            return AddResult.FULL;
        }
        if (RopeConfig.isProtectedPlayer(first) || RopeConfig.isProtectedPlayer(second)) {
            return AddResult.PROTECTED_PLAYER;
        }
        if (hasLink(first.getUuid())) {
            return AddResult.FIRST_ALREADY_LINKED;
        }
        if (hasLink(second.getUuid())) {
            return AddResult.SECOND_ALREADY_LINKED;
        }

        activeLinks.add(new RopeLink(
                first.getUuid(),
                RopeEndpoint.player(first),
                RopeEndpoint.player(second),
                length,
                refundLeadOnManualRelease));
        log("Added player rope {} -> {} length {}", first.getName().getString(), second.getName().getString(),
                RopeConfig.clampLength(length));
        return AddResult.ADDED;
    }

    public AddResult restoreLink(MinecraftServer server, RopeLink link) {
        if (activeLinks.size() >= RopeConfig.maxActiveLinks()) {
            return AddResult.FULL;
        }
        if (!canRestoreEndpoint(server, link.first()) || !canRestoreEndpoint(server, link.second())) {
            return AddResult.PROTECTED_PLAYER;
        }
        if (hasLinkedPlayer(link.first()) || hasLinkedPlayer(link.second())) {
            return AddResult.SECOND_ALREADY_LINKED;
        }

        activeLinks.add(link);
        log("Restored persisted rope {}", link.id());
        return AddResult.ADDED;
    }

    public AddResult anchorCarriedPlayer(ServerPlayerEntity carrier, Vec3d anchor) {
        Optional<RopeLink> link = findForPlayer(carrier.getUuid());
        if (link.isEmpty()) {
            return AddResult.NO_LINK;
        }

        return anchorCarriedPlayer(carrier, anchor, link.get());
    }

    public AddResult anchorCarriedPlayer(ServerPlayerEntity carrier, Vec3d anchor, UUID expectedLinkId) {
        Optional<RopeLink> link = findForPlayer(carrier.getUuid());
        if (link.isEmpty() || !link.get().id().equals(expectedLinkId)) {
            return AddResult.NO_LINK;
        }

        return anchorCarriedPlayer(carrier, anchor, link.get());
    }

    private AddResult anchorCarriedPlayer(ServerPlayerEntity carrier, Vec3d anchor, RopeLink current) {
        if (!carrier.getUuid().equals(current.controllerUuid())) {
            return AddResult.NOT_CONTROLLER;
        }

        RopeEndpoint carried = current.otherEndpoint(carrier.getUuid());
        if (carried == null || carried.type() != RopeEndpoint.Type.PLAYER) {
            return AddResult.NO_CARRIED_PLAYER;
        }

        activeLinks.remove(current);
        activeLinks.add(new RopeLink(
                carrier.getUuid(),
                carried,
                RopeEndpoint.anchor(carrier.getEntityWorld().getRegistryKey(), anchor),
                RopeConfig.anchorRopeLength(),
                current.refundLeadOnManualRelease()));
        log("Anchored rope controlled by {} at {}", carrier.getName().getString(), anchor);
        return AddResult.ADDED;
    }

    public ReleaseResult releasePlayerForController(UUID controllerUuid, UUID playerUuid) {
        Optional<RopeLink> link = findForPlayer(playerUuid);
        if (link.isEmpty()) {
            return ReleaseResult.noLink();
        }

        RopeLink current = link.get();
        if (!controllerUuid.equals(current.controllerUuid())) {
            return ReleaseResult.notController(current);
        }

        activeLinks.remove(current);
        log("Released player rope {}", current.id());
        return ReleaseResult.released(current);
    }

    public ReleaseResult releaseAnchorForController(ServerPlayerEntity controller, Vec3d anchor) {
        Optional<RopeLink> link = findForAnchor(controller.getEntityWorld().getRegistryKey(), anchor);
        if (link.isEmpty()) {
            return ReleaseResult.noLink();
        }

        RopeLink current = link.get();
        if (!controller.getUuid().equals(current.controllerUuid())) {
            return ReleaseResult.notController(current);
        }

        activeLinks.remove(current);
        log("Released anchor rope {}", current.id());
        return ReleaseResult.released(current);
    }

    public int removeForPlayer(UUID playerUuid) {
        return removeForPlayer(null, playerUuid, null);
    }

    public int removeForPlayer(MinecraftServer server, UUID playerUuid, String message) {
        return removeLinksForPlayer(server, playerUuid, message).size();
    }

    public List<RopeLink> removeLinksForPlayer(MinecraftServer server, UUID playerUuid, String message) {
        List<RopeLink> removed = new ArrayList<>();
        Iterator<RopeLink> iterator = activeLinks.iterator();
        while (iterator.hasNext()) {
            RopeLink link = iterator.next();
            if (!link.includesPlayer(playerUuid)) {
                continue;
            }

            iterator.remove();
            removed.add(link);
            if (server != null && message != null) {
                notifyEndpoint(server, link.first(), message);
                notifyEndpoint(server, link.second(), message);
            }
        }
        if (!removed.isEmpty()) {
            log("Removed {} rope link(s) for player {}", removed.size(), playerUuid);
        }
        return removed;
    }

    public int removeForAnchor(MinecraftServer server, RegistryKey<World> worldKey, Vec3d anchor, String message) {
        return removeLinksForAnchor(server, worldKey, anchor, message).size();
    }

    public List<RopeLink> removeLinksForAnchor(
            MinecraftServer server,
            RegistryKey<World> worldKey,
            Vec3d anchor,
            String message) {
        List<RopeLink> removed = new ArrayList<>();
        Iterator<RopeLink> iterator = activeLinks.iterator();
        while (iterator.hasNext()) {
            RopeLink link = iterator.next();
            if (!link.first().matchesAnchor(worldKey, anchor) && !link.second().matchesAnchor(worldKey, anchor)) {
                continue;
            }

            iterator.remove();
            removed.add(link);
            if (server != null && message != null) {
                notifyEndpoint(server, link.first(), message);
                notifyEndpoint(server, link.second(), message);
            }
        }
        if (!removed.isEmpty()) {
            log("Removed {} rope link(s) for broken anchor {} in {}", removed.size(), anchor, worldKey.getValue());
        }
        return removed;
    }

    public int clearAll() {
        int count = activeLinks.size();
        activeLinks.clear();
        if (count > 0) {
            log("Cleared all rope links: {}", count);
        }
        return count;
    }

    public Optional<RopeLink> removeById(UUID linkId) {
        Iterator<RopeLink> iterator = activeLinks.iterator();
        while (iterator.hasNext()) {
            RopeLink link = iterator.next();
            if (link.id().equals(linkId)) {
                iterator.remove();
                log("Removed rope by id {}", linkId);
                return Optional.of(link);
            }
        }
        return Optional.empty();
    }

    public Optional<RopeLink> removeControlledPlayerLink(UUID controllerUuid) {
        Iterator<RopeLink> iterator = activeLinks.iterator();
        while (iterator.hasNext()) {
            RopeLink link = iterator.next();
            if (link.controllerUuid().equals(controllerUuid) && link.hasOnlyPlayerEndpoints()) {
                iterator.remove();
                log("Dropped controlled player rope {}", link.id());
                return Optional.of(link);
            }
        }
        return Optional.empty();
    }

    private static boolean shouldExpire(RopeLink link, MinecraftServer server) {
        int maxHeldDurationTicks = RopeConfig.maxHeldDurationTicks();
        if (maxHeldDurationTicks <= 0 || link.ageTicks() < maxHeldDurationTicks) {
            return false;
        }
        return link.hasOnlyPlayerEndpoints() || hasOnlinePlayerEndpoint(server, link);
    }

    private static boolean hasOnlinePlayerEndpoint(MinecraftServer server, RopeLink link) {
        return link.first().type() == RopeEndpoint.Type.PLAYER && link.first().resolvePlayer(server).isPresent()
                || link.second().type() == RopeEndpoint.Type.PLAYER && link.second().resolvePlayer(server).isPresent();
    }

    private static void notifyExpiration(MinecraftServer server, RopeLink link) {
        notifyEndpoint(server, link.first(), "The rope was released automatically.");
        notifyEndpoint(server, link.second(), "The rope was released automatically.");
    }

    private static void notifyEndpoint(MinecraftServer server, RopeEndpoint endpoint, String message) {
        if (endpoint.type() == RopeEndpoint.Type.PLAYER) {
            endpoint.resolvePlayer(server).ifPresent(player -> player.sendMessage(
                    net.minecraft.text.Text.literal(message),
                    true));
        }
    }

    private static void log(String message, Object... args) {
        if (RopeConfig.logRopeEvents()) {
            McElmaRopeMod.LOGGER.info(message, args);
        }
    }

    public int activeCount() {
        return activeLinks.size();
    }

    public List<RopeLink> links() {
        return List.copyOf(activeLinks);
    }

    public boolean hasLink(UUID playerUuid) {
        return activeLinks.stream().anyMatch(link -> link.includesPlayer(playerUuid));
    }

    public Optional<RopeLink> findForPlayer(UUID playerUuid) {
        return activeLinks.stream().filter(link -> link.includesPlayer(playerUuid)).findFirst();
    }

    public Optional<RopeLink> findForAnchor(RegistryKey<World> worldKey, Vec3d anchor) {
        return activeLinks.stream()
                .filter(link -> link.first().matchesAnchor(worldKey, anchor)
                        || link.second().matchesAnchor(worldKey, anchor))
                .findFirst();
    }

    public Optional<RopeLink> findControlledBy(UUID controllerUuid) {
        return activeLinks.stream()
                .filter(link -> link.controllerUuid().equals(controllerUuid))
                .findFirst();
    }

    private boolean hasLinkedPlayer(RopeEndpoint endpoint) {
        return endpoint.type() == RopeEndpoint.Type.PLAYER && hasLink(endpoint.playerUuid());
    }

    private static boolean canRestoreEndpoint(MinecraftServer server, RopeEndpoint endpoint) {
        if (endpoint.type() != RopeEndpoint.Type.PLAYER) {
            return true;
        }
        return endpoint.resolvePlayer(server).map(player -> !RopeConfig.isProtectedPlayer(player)).orElse(false);
    }

    public enum AddResult {
        ADDED,
        FULL,
        SAME_PLAYER,
        DIFFERENT_WORLD,
        FIRST_ALREADY_LINKED,
        SECOND_ALREADY_LINKED,
        NO_LINK,
        NOT_CONTROLLER,
        NO_CARRIED_PLAYER,
        PROTECTED_PLAYER
    }

    public enum ReleaseStatus {
        RELEASED,
        NO_LINK,
        NOT_CONTROLLER
    }

    public record ReleaseResult(ReleaseStatus status, RopeLink link) {
        public static ReleaseResult released(RopeLink link) {
            return new ReleaseResult(ReleaseStatus.RELEASED, link);
        }

        public static ReleaseResult noLink() {
            return new ReleaseResult(ReleaseStatus.NO_LINK, null);
        }

        public static ReleaseResult notController(RopeLink link) {
            return new ReleaseResult(ReleaseStatus.NOT_CONTROLLER, link);
        }
    }
}
