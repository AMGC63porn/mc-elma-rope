package com.mcelma.rope.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mcelma.rope.RopeConfig;
import com.mcelma.rope.RopeVisualPayload;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class RopeVisualRenderer {
    private static final int CORE_R = 132;
    private static final int CORE_G = 92;
    private static final int CORE_B = 52;
    private static final int FIBER_R = 164;
    private static final int FIBER_G = 132;
    private static final int FIBER_B = 82;
    private static final int TWIST_R = 96;
    private static final int TWIST_G = 67;
    private static final int TWIST_B = 38;
    private static final int SHADOW_R = 62;
    private static final int SHADOW_G = 38;
    private static final int SHADOW_B = 20;
    private static final int HIGHLIGHT_R = 194;
    private static final int HIGHLIGHT_G = 151;
    private static final int HIGHLIGHT_B = 94;
    private static final int COLOR_A = 255;
    private static final double SMOOTHING = 0.45D;
    private static final Map<UUID, SmoothedLink> SMOOTHED_LINKS = new HashMap<>();

    private RopeVisualRenderer() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!RopeConfig.ropeVisualEnabled() || client.world == null || RopeVisualState.links().isEmpty()) {
            SMOOTHED_LINKS.clear();
            return;
        }

        Camera camera = context.gameRenderer().getCamera();
        Vec3d cameraPos = camera.getPos();
        float tickDelta = camera.getLastTickProgress();
        MatrixStack matrices = context.matrices();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLines());

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        MatrixStack.Entry entry = matrices.peek();
        Set<UUID> renderedIds = new HashSet<>();

        for (RopeVisualPayload.VisualLink link : RopeVisualState.links()) {
            Vec3d first = resolveEndpoint(client, link.first(), tickDelta);
            Vec3d second = resolveEndpoint(client, link.second(), tickDelta);
            if (first == null || second == null) {
                continue;
            }
            SmoothedLink smoothed = smooth(link.id(), first, second);
            drawRope(entry, consumer, cameraPos, smoothed.first(), smoothed.second(), link);
            renderedIds.add(link.id());
        }
        SMOOTHED_LINKS.keySet().removeIf(id -> !renderedIds.contains(id));

        matrices.pop();
    }

    private static Vec3d resolveEndpoint(
            MinecraftClient client,
            RopeVisualPayload.VisualEndpoint endpoint,
            float tickDelta) {
        if (!endpoint.player()) {
            return endpoint.anchorPos();
        }

        Entity entity = client.world.getEntityById(endpoint.entityId());
        if (entity == null) {
            return null;
        }
        return entity.getLeashPos(tickDelta);
    }

    private static SmoothedLink smooth(UUID id, Vec3d first, Vec3d second) {
        SmoothedLink previous = SMOOTHED_LINKS.get(id);
        if (previous == null) {
            SmoothedLink current = new SmoothedLink(first, second, 0.0D);
            SMOOTHED_LINKS.put(id, current);
            return current;
        }

        Vec3d nextFirst = lerp(previous.first(), first, SMOOTHING);
        Vec3d nextSecond = lerp(previous.second(), second, SMOOTHING);
        double endpointMotion = nextFirst.distanceTo(previous.first()) + nextSecond.distanceTo(previous.second());
        SmoothedLink current = new SmoothedLink(nextFirst, nextSecond, endpointMotion);
        SMOOTHED_LINKS.put(id, current);
        return current;
    }

    private static void drawRope(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Vec3d cameraPos,
            Vec3d start,
            Vec3d end,
            RopeVisualPayload.VisualLink link) {
        double sag = sag(start, end, link);
        Vec3d side = sideVector(start, end, cameraPos);
        double width = widthOffset();

        if ("layered_lines".equals(RopeConfig.ropeVisualStyle())) {
            drawLayeredRope(entry, consumer, start, end, sag, side, width);
            return;
        }

        drawVanillaLikeRope(entry, consumer, start, end, sag, side, width);
    }

    private static void drawLayeredRope(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Vec3d start,
            Vec3d end,
            double sag,
            Vec3d side,
            double width) {
        drawRopeLayer(entry, consumer, start, end, sag, side.multiply(width), SHADOW_R, SHADOW_G, SHADOW_B);
        drawRopeLayer(entry, consumer, start, end, sag, Vec3d.ZERO, CORE_R, CORE_G, CORE_B);
        if (!RopeConfig.ropeVisualWidthPreset().equals("thin")) {
            drawRopeLayer(entry, consumer, start, end, sag, side.multiply(-width * 0.55D),
                    HIGHLIGHT_R, HIGHLIGHT_G, HIGHLIGHT_B);
        }
    }

    private static void drawVanillaLikeRope(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Vec3d start,
            Vec3d end,
            double sag,
            Vec3d side,
            double width) {
        int segments = Math.max(12, RopeConfig.ropeVisualSegments() * 2);
        Vec3d previousCenter = ropePoint(start, end, 0.0D, sag);
        for (int segment = 1; segment <= segments; segment++) {
            double t = (double) segment / segments;
            Vec3d center = ropePoint(start, end, t, sag);
            boolean twist = segment % 2 == 0;
            drawSegment(entry, consumer, previousCenter, center,
                    twist ? FIBER_R : CORE_R,
                    twist ? FIBER_G : CORE_G,
                    twist ? FIBER_B : CORE_B);

            double previousT = (double) (segment - 1) / segments;
            double insetStart = lerp(previousT, t, 0.22D);
            double insetEnd = lerp(previousT, t, 0.78D);
            Vec3d fiberStart = ropePoint(start, end, insetStart, sag)
                    .add(side.multiply(twist ? width : -width));
            Vec3d fiberEnd = ropePoint(start, end, insetEnd, sag)
                    .add(side.multiply(twist ? -width : width));
            drawSegment(entry, consumer, fiberStart, fiberEnd,
                    twist ? HIGHLIGHT_R : TWIST_R,
                    twist ? HIGHLIGHT_G : TWIST_G,
                    twist ? HIGHLIGHT_B : TWIST_B);
            previousCenter = center;
        }
    }

    private static void drawRopeLayer(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Vec3d start,
            Vec3d end,
            double sag,
            Vec3d offset,
            int red,
            int green,
            int blue) {
        Vec3d previous = ropePoint(start, end, 0.0D, sag).add(offset);
        int segments = RopeConfig.ropeVisualSegments();
        for (int segment = 1; segment <= segments; segment++) {
            double t = (double) segment / segments;
            Vec3d next = ropePoint(start, end, t, sag).add(offset);
            drawSegment(entry, consumer, previous, next, red, green, blue);
            previous = next;
        }
    }

    private static Vec3d ropePoint(Vec3d start, Vec3d end, double t, double sag) {
        double x = lerp(start.x, end.x, t);
        double y = lerp(start.y, end.y, t) - Math.sin(Math.PI * t) * sag;
        double z = lerp(start.z, end.z, t);
        return new Vec3d(x, y, z);
    }

    private static double sag(Vec3d start, Vec3d end, RopeVisualPayload.VisualLink link) {
        double distance = start.distanceTo(end);
        double slack = Math.max(0.0D, link.length() - distance);
        double baseSag = Math.min(0.65D, distance * RopeConfig.ropeVisualSag() + slack * 0.035D);
        if (link.taut()) {
            baseSag *= 0.35D;
        }
        SmoothedLink smoothed = SMOOTHED_LINKS.get(link.id());
        if (smoothed != null) {
            baseSag += Math.min(0.18D, smoothed.endpointMotion() * 0.25D);
        }
        return baseSag;
    }

    private static Vec3d sideVector(Vec3d start, Vec3d end, Vec3d cameraPos) {
        Vec3d direction = end.subtract(start);
        Vec3d midpoint = start.add(end).multiply(0.5D);
        Vec3d toCamera = cameraPos.subtract(midpoint);
        Vec3d side = direction.crossProduct(toCamera);
        if (side.lengthSquared() < 1.0E-6D) {
            side = new Vec3d(0.0D, 1.0D, 0.0D).crossProduct(direction);
        }
        if (side.lengthSquared() < 1.0E-6D) {
            return new Vec3d(1.0D, 0.0D, 0.0D);
        }
        return side.normalize();
    }

    private static double widthOffset() {
        return switch (RopeConfig.ropeVisualWidthPreset()) {
            case "thin" -> 0.006D;
            case "thick" -> 0.018D;
            default -> 0.012D;
        };
    }

    private static void drawSegment(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Vec3d start,
            Vec3d end,
            int red,
            int green,
            int blue) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1.0E-6D) {
            return;
        }
        Vec3d normal = direction.normalize();
        vertex(entry, consumer, start, normal, red, green, blue);
        vertex(entry, consumer, end, normal, red, green, blue);
    }

    private static void vertex(
            MatrixStack.Entry entry,
            VertexConsumer consumer,
            Vec3d position,
            Vec3d normal,
            int red,
            int green,
            int blue) {
        consumer.vertex(entry, (float) position.x, (float) position.y, (float) position.z)
                .color(red, green, blue, COLOR_A)
                .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static Vec3d lerp(Vec3d start, Vec3d end, double t) {
        return new Vec3d(lerp(start.x, end.x, t), lerp(start.y, end.y, t), lerp(start.z, end.z, t));
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private record SmoothedLink(Vec3d first, Vec3d second, double endpointMotion) {
    }
}
