package com.mcelma.rope.client;

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
    private static final int SEGMENTS = 16;
    private static final int COLOR_R = 116;
    private static final int COLOR_G = 80;
    private static final int COLOR_B = 46;
    private static final int COLOR_A = 255;

    private RopeVisualRenderer() {
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || RopeVisualState.links().isEmpty()) {
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

        for (RopeVisualPayload.VisualLink link : RopeVisualState.links()) {
            Vec3d first = resolveEndpoint(client, link.first(), tickDelta);
            Vec3d second = resolveEndpoint(client, link.second(), tickDelta);
            if (first == null || second == null) {
                continue;
            }
            drawRope(entry, consumer, first, second);
        }

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

    private static void drawRope(MatrixStack.Entry entry, VertexConsumer consumer, Vec3d start, Vec3d end) {
        Vec3d previous = ropePoint(start, end, 0.0D, sag(start, end));
        for (int segment = 1; segment <= SEGMENTS; segment++) {
            double t = (double) segment / SEGMENTS;
            Vec3d next = ropePoint(start, end, t, sag(start, end));
            drawSegment(entry, consumer, previous, next);
            previous = next;
        }
    }

    private static Vec3d ropePoint(Vec3d start, Vec3d end, double t, double sag) {
        double x = lerp(start.x, end.x, t);
        double y = lerp(start.y, end.y, t) - Math.sin(Math.PI * t) * sag;
        double z = lerp(start.z, end.z, t);
        return new Vec3d(x, y, z);
    }

    private static double sag(Vec3d start, Vec3d end) {
        double distance = start.distanceTo(end);
        return Math.min(0.45D, distance * 0.035D);
    }

    private static void drawSegment(MatrixStack.Entry entry, VertexConsumer consumer, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1.0E-6D) {
            return;
        }
        Vec3d normal = direction.normalize();
        vertex(entry, consumer, start, normal);
        vertex(entry, consumer, end, normal);
    }

    private static void vertex(MatrixStack.Entry entry, VertexConsumer consumer, Vec3d position, Vec3d normal) {
        consumer.vertex(entry, (float) position.x, (float) position.y, (float) position.z)
                .color(COLOR_R, COLOR_G, COLOR_B, COLOR_A)
                .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }
}
