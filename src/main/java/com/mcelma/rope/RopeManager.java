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
            if (!RopePhysics.tick(server, link)) {
                iterator.remove();
            }
        }
    }

    public AddResult addAnchor(ServerPlayerEntity player, Vec3d anchor, double length) {
        if (activeLinks.size() >= RopeConfig.maxActiveLinks()) {
            return AddResult.FULL;
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
        return ReleaseResult.released(current);
    }

    public int removeForPlayer(UUID playerUuid) {
        int before = activeLinks.size();
        activeLinks.removeIf(link -> link.includesPlayer(playerUuid));
        return before - activeLinks.size();
    }

    public int clearAll() {
        int count = activeLinks.size();
        activeLinks.clear();
        return count;
    }

    public Optional<RopeLink> removeById(UUID linkId) {
        Iterator<RopeLink> iterator = activeLinks.iterator();
        while (iterator.hasNext()) {
            RopeLink link = iterator.next();
            if (link.id().equals(linkId)) {
                iterator.remove();
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
                return Optional.of(link);
            }
        }
        return Optional.empty();
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

    public enum AddResult {
        ADDED,
        FULL,
        SAME_PLAYER,
        DIFFERENT_WORLD,
        FIRST_ALREADY_LINKED,
        SECOND_ALREADY_LINKED,
        NO_LINK,
        NOT_CONTROLLER,
        NO_CARRIED_PLAYER
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
