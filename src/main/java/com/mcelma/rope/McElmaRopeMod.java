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
        RopeCommands.register(ropeManager);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> RopePersistence.loadPending(server, ropeManager));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RopePersistence.save(server, ropeManager));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                RopePersistence.restoreAvailable(server, ropeManager));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            int removed = ropeManager.removeForPlayer(
                    server,
                    handler.player.getUuid(),
                    "The rope was released because a player disconnected.");
            if (removed > 0 && RopeConfig.logRopeEvents()) {
                LOGGER.info("Cleaned {} rope link(s) after {} disconnected.",
                        removed,
                        handler.player.getName().getString());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ropeManager.tick(server);
            actionManager.tick(server);
            RopeVisualSync.tick(server, ropeManager);
        });
    }
}
