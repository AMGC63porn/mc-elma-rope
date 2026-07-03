package com.mcelma.rope;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RopeEndpoint {
    public enum Type {
        PLAYER,
        ANCHOR
    }

    private final Type type;
    private final UUID playerUuid;
    private final RegistryKey<World> worldKey;
    private final Vec3d anchorPosition;

    private RopeEndpoint(Type type, UUID playerUuid, RegistryKey<World> worldKey, Vec3d anchorPosition) {
        this.type = type;
        this.playerUuid = playerUuid;
        this.worldKey = worldKey;
        this.anchorPosition = anchorPosition;
    }

    public static RopeEndpoint player(ServerPlayerEntity player) {
        return new RopeEndpoint(Type.PLAYER, player.getUuid(), null, null);
    }

    public static RopeEndpoint anchor(RegistryKey<World> worldKey, Vec3d position) {
        return new RopeEndpoint(Type.ANCHOR, null, worldKey, position);
    }

    public Type type() {
        return type;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public RegistryKey<World> worldKey() {
        return worldKey;
    }

    public Vec3d anchorPosition() {
        return anchorPosition;
    }

    public Optional<ServerPlayerEntity> resolvePlayer(MinecraftServer server) {
        if (type != Type.PLAYER) {
            return Optional.empty();
        }
        return Optional.ofNullable(server.getPlayerManager().getPlayer(playerUuid));
    }

    public Vec3d position(MinecraftServer server) {
        if (type == Type.ANCHOR) {
            return anchorPosition;
        }
        return resolvePlayer(server).map(ServerPlayerEntity::getEntityPos).orElse(null);
    }

    public boolean matchesAnchor(RegistryKey<World> worldKey, Vec3d position) {
        return type == Type.ANCHOR
                && this.worldKey.equals(worldKey)
                && anchorPosition.squaredDistanceTo(position) < 1.0E-6D;
    }

    public boolean isInWorld(ServerPlayerEntity player) {
        if (type == Type.ANCHOR) {
            return player.getEntityWorld().getRegistryKey().equals(worldKey);
        }
        return resolvePlayer(player.getEntityWorld().getServer())
                .map(other -> other.getEntityWorld().getRegistryKey().equals(player.getEntityWorld().getRegistryKey()))
                .orElse(false);
    }
}
