package com.mcelma.rope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public final class RopeVisualSync {
    private static final int SYNC_INTERVAL_TICKS = 2;
    private static int ticksUntilSync;

    private RopeVisualSync() {
    }

    public static void tick(MinecraftServer server, RopeManager ropeManager) {
        ticksUntilSync--;
        if (ticksUntilSync > 0) {
            return;
        }
        ticksUntilSync = SYNC_INTERVAL_TICKS;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!ServerPlayNetworking.canSend(player, RopeVisualPayload.ID)) {
                continue;
            }

            List<RopeVisualPayload.VisualLink> links = linksForWorld(
                    server,
                    ropeManager,
                    player.getEntityWorld().getRegistryKey());
            ServerPlayNetworking.send(player, new RopeVisualPayload(links));
        }
    }

    private static List<RopeVisualPayload.VisualLink> linksForWorld(
            MinecraftServer server,
            RopeManager ropeManager,
            RegistryKey<World> worldKey) {
        List<RopeVisualPayload.VisualLink> visualLinks = new ArrayList<>();
        for (RopeLink link : ropeManager.links()) {
            if (!endpointInWorld(server, link.first(), worldKey) || !endpointInWorld(server, link.second(), worldKey)) {
                continue;
            }

            Optional<RopeVisualPayload.VisualEndpoint> first = visualEndpoint(server, link.first());
            Optional<RopeVisualPayload.VisualEndpoint> second = visualEndpoint(server, link.second());
            if (first.isPresent() && second.isPresent()) {
                visualLinks.add(new RopeVisualPayload.VisualLink(link.id(), first.get(), second.get()));
            }
        }
        return visualLinks;
    }

    private static boolean endpointInWorld(
            MinecraftServer server,
            RopeEndpoint endpoint,
            RegistryKey<World> worldKey) {
        if (endpoint.type() == RopeEndpoint.Type.ANCHOR) {
            return endpoint.worldKey().equals(worldKey);
        }

        return endpoint.resolvePlayer(server)
                .map(player -> player.getEntityWorld().getRegistryKey().equals(worldKey))
                .orElse(false);
    }

    private static Optional<RopeVisualPayload.VisualEndpoint> visualEndpoint(
            MinecraftServer server,
            RopeEndpoint endpoint) {
        if (endpoint.type() == RopeEndpoint.Type.ANCHOR) {
            return Optional.of(RopeVisualPayload.VisualEndpoint.anchor(endpoint.anchorPosition()));
        }

        return endpoint.resolvePlayer(server)
                .map(player -> RopeVisualPayload.VisualEndpoint.player(player.getId()));
    }
}
