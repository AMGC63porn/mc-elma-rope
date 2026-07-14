package com.mcelma.rope;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class RopeAnchorBlockBreakHandler {
    private static final String BROKEN_ANCHOR_MESSAGE = "The rope was released because the anchor block was broken.";

    private RopeAnchorBlockBreakHandler() {
    }

    public static void register(RopeManager ropeManager) {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            int removed = ropeManager.removeForAnchor(
                    serverWorld.getServer(),
                    serverWorld.getRegistryKey(),
                    Vec3d.ofCenter(pos),
                    BROKEN_ANCHOR_MESSAGE);
            removed += RopeAnchoredOfflinePersistence.removeForAnchor(
                    serverWorld.getServer(),
                    serverWorld.getRegistryKey(),
                    Vec3d.ofCenter(pos));
            if (removed <= 0) {
                return;
            }

            player.sendMessage(Text.literal("Broken anchor released " + removed + " rope(s)."), true);
        });
    }
}
