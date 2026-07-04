package com.mcelma.rope;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;

public final class RopeDisconnectGameTests {
    @GameTest(maxTicks = 20)
    public void targetDisconnectQueuesPenaltyAndJoinAppliesEffects(TestContext context) {
        RopeDisconnectPolicy.clearPendingPenaltiesForTests();
        context.addFinalTask(RopeDisconnectPolicy::clearPendingPenaltiesForTests);

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), target, manager);

        context.assertEquals(0, manager.activeCount(), Text.literal("Target disconnect did not clear the rope."));
        context.assertEquals(1, RopeDisconnectPolicy.pendingPenaltyCountForTests(),
                Text.literal("Target disconnect did not queue a reconnect penalty."));
        context.assertFalse(target.hasStatusEffect(StatusEffects.MINING_FATIGUE),
                Text.literal("Disconnect penalty was applied before reconnect."));

        RopeDisconnectPolicy.handleJoin(context.getWorld().getServer(), target);

        context.assertEquals(0, RopeDisconnectPolicy.pendingPenaltyCountForTests(),
                Text.literal("Reconnect did not consume the pending penalty."));
        context.assertTrue(target.hasStatusEffect(StatusEffects.MINING_FATIGUE),
                Text.literal("Reconnect did not apply Mining Fatigue."));
        context.assertTrue(target.hasStatusEffect(StatusEffects.SLOWNESS),
                Text.literal("Reconnect did not apply Slowness."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void controllerDisconnectDoesNotPunishTarget(TestContext context) {
        RopeDisconnectPolicy.clearPendingPenaltiesForTests();
        context.addFinalTask(RopeDisconnectPolicy::clearPendingPenaltiesForTests);

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), controller, manager);

        context.assertEquals(0, manager.activeCount(), Text.literal("Controller disconnect did not clear the rope."));
        context.assertEquals(0, RopeDisconnectPolicy.pendingPenaltyCountForTests(),
                Text.literal("Controller disconnect should not punish the tied target."));
        context.complete();
    }
}
