package com.mcelma.rope;

import java.util.Optional;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class RopePhysics {
    private RopePhysics() {
    }

    public static boolean tick(MinecraftServer server, RopeLink link) {
        EndpointState first = resolve(server, link.first());
        EndpointState second = resolve(server, link.second());

        if (!first.valid() || !second.valid()) {
            return false;
        }

        if (first.player() != null && second.player() != null) {
            if (!sameWorld(first.player(), second.player())) {
                return false;
            }
            return solvePlayerToPlayer(first.player(), second.player(), link);
        }

        if (first.player() != null) {
            if (!link.second().isInWorld(first.player())) {
                return false;
            }
            solvePlayerToAnchor(first.player(), second.position(), link);
            return true;
        }

        if (second.player() != null) {
            if (!link.first().isInWorld(second.player())) {
                return false;
            }
            solvePlayerToAnchor(second.player(), first.position(), link);
            return true;
        }

        return false;
    }

    private static EndpointState resolve(MinecraftServer server, RopeEndpoint endpoint) {
        if (endpoint.type() == RopeEndpoint.Type.ANCHOR) {
            return new EndpointState(null, endpoint.position(server), true);
        }

        Optional<ServerPlayerEntity> player = endpoint.resolvePlayer(server);
        if (player.isEmpty() || !isUsable(player.get())) {
            return EndpointState.invalid();
        }

        return new EndpointState(player.get(), player.get().getEntityPos(), true);
    }

    private static boolean isUsable(ServerPlayerEntity player) {
        return !player.isRemoved() && player.isAlive() && !player.isSpectator();
    }

    private static boolean sameWorld(ServerPlayerEntity first, ServerPlayerEntity second) {
        return first.getEntityWorld().getRegistryKey().equals(second.getEntityWorld().getRegistryKey());
    }

    private static void solvePlayerToAnchor(ServerPlayerEntity player, Vec3d anchor, RopeLink link) {
        applyRopeConstraint(player, anchor, link);
    }

    private static boolean solvePlayerToPlayer(ServerPlayerEntity first, ServerPlayerEntity second, RopeLink link) {
        ServerPlayerEntity fixed = null;
        ServerPlayerEntity dynamic = null;
        if (first.getUuid().equals(link.controllerUuid())) {
            fixed = first;
            dynamic = second;
        } else if (second.getUuid().equals(link.controllerUuid())) {
            fixed = second;
            dynamic = first;
        }
        if (fixed == null || dynamic == null) {
            return false;
        }

        applyRopeConstraint(dynamic, fixed.getEntityPos(), link);
        return true;
    }

    private static void applyRopeConstraint(ServerPlayerEntity dynamic, Vec3d fixedPosition, RopeLink link) {
        Vec3d dynamicPos = dynamic.getEntityPos();
        Vec3d offset = dynamicPos.subtract(fixedPosition);
        double distance = offset.length();
        double limit = link.length();

        if (distance <= limit + RopeConfig.tautTolerance() || distance == 0.0D) {
            link.setTaut(false);
            return;
        }

        Vec3d outward = offset.multiply(1.0D / distance);
        Vec3d nextVelocity = constrainedVelocity(dynamic.getVelocity(), outward, distance, limit);
        setSyncedVelocity(dynamic, nextVelocity);
        dynamic.fallDistance = 0.0D;
        link.setTaut(true);
    }

    private static Vec3d constrainedVelocity(Vec3d velocity, Vec3d outward, double distance, double limit) {
        double stretch = Math.max(0.0D, distance - limit);
        double outwardSpeed = velocity.dotProduct(outward);
        Vec3d tangential = velocity.subtract(outward.multiply(outwardSpeed)).multiply(RopeConfig.ropeSwingDamping());
        double maxPullSpeed = maxPullSpeed(distance, limit);
        double correctionSpeed = Math.min(maxPullSpeed, stretch * RopeConfig.ropeCorrectionRate());
        double radialSpeed = Math.min(outwardSpeed, 0.0D);
        if (radialSpeed > -correctionSpeed) {
            radialSpeed = -correctionSpeed;
        }

        return tangential.add(outward.multiply(radialSpeed));
    }

    private static double maxPullSpeed(double distance, double limit) {
        if (distance > limit * RopeConfig.ropeEmergencyStretchMultiplier()) {
            return RopeConfig.ropeEmergencyMaxPullSpeed();
        }
        return RopeConfig.ropeMaxPullSpeed();
    }

    private static void setSyncedVelocity(ServerPlayerEntity player, Vec3d velocity) {
        if (player.getVelocity().squaredDistanceTo(velocity) < 1.0E-8D) {
            return;
        }

        player.setVelocity(velocity);
        player.velocityModified = true;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
    }

    private record EndpointState(ServerPlayerEntity player, Vec3d position, boolean valid) {
        static EndpointState invalid() {
            return new EndpointState(null, Vec3d.ZERO, false);
        }
    }
}
