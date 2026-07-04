package com.mcelma.rope;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class RopePerformanceGameTests {
    @GameTest(maxTicks = 200)
    public void managerTicksOnlyActiveLinksAtPlannedScales(TestContext context) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (int index = 0; index < 256; index++) {
            players.add(context.createMockCreativeServerPlayerInWorld());
        }

        assertScale(context, players, 0);
        assertScale(context, players, 50);
        assertScale(context, players, 100);
        assertScale(context, players, 256);
        context.complete();
    }

    private static void assertScale(TestContext context, List<ServerPlayerEntity> players, int activeLinks) {
        RopeManager manager = new RopeManager();
        for (int index = 0; index < activeLinks; index++) {
            Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(index % 16, 1, 1 + index / 16)));
            context.assertEquals(
                    RopeManager.AddResult.ADDED,
                    manager.addAnchor(players.get(index), anchor, RopeConfig.anchorRopeLength()),
                    Text.literal("Failed to add active rope at performance scale " + activeLinks + "."));
        }

        context.assertEquals(activeLinks, manager.activeCount(),
                Text.literal("Unexpected active rope count before performance tick."));
        manager.tick(context.getWorld().getServer());
        context.assertEquals(activeLinks, manager.activeCount(),
                Text.literal("Unexpected active rope count after performance tick."));
        context.assertTrue(
                manager.links().stream().allMatch(link -> link.ageTicks() == 1),
                Text.literal("Not every active rope was ticked exactly once at scale " + activeLinks + "."));
    }
}
