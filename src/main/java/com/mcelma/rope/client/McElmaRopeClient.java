package com.mcelma.rope.client;

import com.mcelma.rope.RopeVisualPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public final class McElmaRopeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RopeVisualPayload.ID, (payload, context) ->
                context.client().execute(() -> RopeVisualState.setLinks(payload.links())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RopeVisualState.clear());
        WorldRenderEvents.AFTER_ENTITIES.register(RopeVisualRenderer::render);
    }
}
