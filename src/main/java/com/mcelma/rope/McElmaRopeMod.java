package com.mcelma.rope;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McElmaRopeMod implements ModInitializer {
    public static final String MOD_ID = "mc_elma_rope";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final RopeManager ropeManager = new RopeManager();
    private final RopeActionManager actionManager = new RopeActionManager(ropeManager);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(RopeVisualPayload.ID, RopeVisualPayload.CODEC);
        RopeConfig.load();
        RopeInteractionHandler.register(actionManager);
        RopeDamageHandler.register(ropeManager, actionManager);
        RopeAnchorBlockBreakHandler.register(ropeManager);
        RopeCommands.register(ropeManager);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RopePersistence.loadPending(server, ropeManager);
            RopeAnchoredOfflinePersistence.load(server);
            RopeDisconnectPolicy.load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            RopeAnchoredOfflinePersistence.captureActiveAnchoredRopesForShutdown(server, ropeManager);
            RopePersistence.save(server, ropeManager);
            RopeAnchoredOfflinePersistence.save(server);
            RopeDisconnectPolicy.save(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            RopePersistence.restoreAvailable(server, ropeManager);
            RopeAnchoredOfflinePersistence.restoreForPlayer(server, ropeManager, handler.player);
            RopeDisconnectPolicy.handleJoin(server, handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RopeDisconnectPolicy.handleDisconnect(server, handler.player, ropeManager));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ropeManager.tick(server);
            actionManager.tick(server);
            RopeVisualSync.tick(server, ropeManager);
        });
    }
}
