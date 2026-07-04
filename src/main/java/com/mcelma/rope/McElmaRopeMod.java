package com.mcelma.rope;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ropeManager.tick(server);
            actionManager.tick(server);
            RopeVisualSync.tick(server, ropeManager);
        });
    }
}
