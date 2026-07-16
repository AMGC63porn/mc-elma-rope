package com.mcelma.rope;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

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

    @GameTest(maxTicks = 20)
    public void targetDisconnectRefundsControllerLead(TestContext context) {
        RopeDisconnectPolicy.clearPendingPenaltiesForTests();
        context.addFinalTask(RopeDisconnectPolicy::clearPendingPenaltiesForTests);

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        makeSurvival(controller);

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), target, manager);

        context.assertEquals(0, manager.activeCount(), Text.literal("Target disconnect did not clear the rope."));
        context.assertEquals(1, countLeads(controller),
                Text.literal("Target disconnect did not refund one lead to the controller."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void commandCreatedRopeDoesNotQueueDisconnectPenalty(TestContext context) {
        RopeDisconnectPolicy.clearPendingPenaltiesForTests();
        context.addFinalTask(RopeDisconnectPolicy::clearPendingPenaltiesForTests);

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), false),
                Text.literal("Command-created player rope was not added."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), target, manager);

        context.assertEquals(0, manager.activeCount(), Text.literal("Target disconnect did not clear the rope."));
        context.assertEquals(0, RopeDisconnectPolicy.pendingPenaltyCountForTests(),
                Text.literal("Command-created rope should not queue a reconnect penalty by default."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void anchoredTargetDisconnectPersistsWithoutLeadRefundAndRestores(TestContext context) {
        RopeConfig.resetForTests();
        RopeDisconnectPolicy.clearPendingPenaltiesForTests();
        RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        context.addFinalTask(() -> {
            RopeConfig.resetForTests();
            RopeDisconnectPolicy.clearPendingPenaltiesForTests();
            RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        });

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        makeSurvival(controller);
        BlockPos anchorPos = new BlockPos(1, 1, 1);
        context.setBlockState(anchorPos, Blocks.OAK_FENCE);
        Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(anchorPos));

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.anchorCarriedPlayer(controller, anchor),
                Text.literal("Player rope did not convert to anchored rope."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), target, manager);

        context.assertEquals(0, manager.activeCount(), Text.literal("Anchored disconnect kept active rope online."));
        context.assertEquals(1, RopeAnchoredOfflinePersistence.pendingCountForTests(),
                Text.literal("Anchored disconnect did not store an offline anchor record."));
        context.assertEquals(0, countLeads(controller), Text.literal("Anchored disconnect refunded a lead."));
        context.assertEquals(1, RopeDisconnectPolicy.pendingPenaltyCountForTests(),
                Text.literal("Anchored disconnect did not queue the reconnect penalty."));

        context.assertEquals(
                1,
                RopeAnchoredOfflinePersistence.restoreForPlayer(context.getWorld().getServer(), manager, target),
                Text.literal("Anchored offline rope did not restore on join."));
        context.assertEquals(1, manager.activeCount(), Text.literal("Restored anchored rope was not active."));
        context.assertEquals(0, RopeAnchoredOfflinePersistence.pendingCountForTests(),
                Text.literal("Restored anchored rope left stale offline state."));
        context.assertTrue(manager.findForAnchor(context.getWorld().getRegistryKey(), anchor).isPresent(),
                Text.literal("Restored anchored rope did not target the original anchor."));

        RopeDisconnectPolicy.handleJoin(context.getWorld().getServer(), target);
        context.assertTrue(target.hasStatusEffect(StatusEffects.MINING_FATIGUE),
                Text.literal("Anchored reconnect penalty did not apply Mining Fatigue."));
        context.assertTrue(target.hasStatusEffect(StatusEffects.SLOWNESS),
                Text.literal("Anchored reconnect penalty did not apply Slowness."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void anchoredDisconnectRecordSurvivesDiskReload(TestContext context) {
        RopeConfig.resetForTests();
        RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        context.addFinalTask(() -> {
            RopeConfig.resetForTests();
            RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        });

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        BlockPos anchorPos = new BlockPos(1, 1, 1);
        context.setBlockState(anchorPos, Blocks.OAK_FENCE);
        Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(anchorPos));

        context.assertEquals(RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));
        context.assertEquals(RopeManager.AddResult.ADDED, manager.anchorCarriedPlayer(controller, anchor),
                Text.literal("Player rope did not convert to anchored rope."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), target, manager);
        RopeAnchoredOfflinePersistence.load(context.getWorld().getServer());

        context.assertEquals(1, RopeAnchoredOfflinePersistence.pendingCountForTests(),
                Text.literal("Saved anchored disconnect record did not reload from disk."));
        context.assertEquals(1,
                RopeAnchoredOfflinePersistence.restoreForPlayer(context.getWorld().getServer(), manager, target),
                Text.literal("Reloaded anchored disconnect record did not restore."));
        context.assertTrue(manager.findForAnchor(context.getWorld().getRegistryKey(), anchor).isPresent(),
                Text.literal("Reloaded record restored to the wrong anchor."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void activeAnchoredRopePersistsAcrossOrderlyServerShutdown(TestContext context) {
        RopeConfig.resetForTests();
        RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        context.addFinalTask(() -> {
            RopeConfig.resetForTests();
            RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        });

        RopeManager activeManager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        BlockPos anchorPos = new BlockPos(1, 1, 1);
        context.setBlockState(anchorPos, Blocks.OAK_FENCE);
        Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(anchorPos));

        context.assertEquals(RopeManager.AddResult.ADDED,
                activeManager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));
        context.assertEquals(RopeManager.AddResult.ADDED, activeManager.anchorCarriedPlayer(controller, anchor),
                Text.literal("Player rope did not convert to anchored rope."));

        context.assertEquals(1,
                RopeAnchoredOfflinePersistence.captureActiveAnchoredRopesForShutdown(
                        context.getWorld().getServer(), activeManager),
                Text.literal("Server shutdown did not capture the active anchored rope."));
        RopeAnchoredOfflinePersistence.load(context.getWorld().getServer());

        RopeManager restartedManager = new RopeManager();
        context.assertEquals(1,
                RopeAnchoredOfflinePersistence.restoreForPlayer(context.getWorld().getServer(), restartedManager, target),
                Text.literal("Shutdown-captured anchor rope did not restore after restart."));
        context.assertTrue(restartedManager.findForAnchor(context.getWorld().getRegistryKey(), anchor).isPresent(),
                Text.literal("Shutdown-captured rope restored to the wrong anchor."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void brokenAnchorClearsOfflineAnchoredRecord(TestContext context) {
        RopeConfig.resetForTests();
        RopeDisconnectPolicy.clearPendingPenaltiesForTests();
        RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        context.addFinalTask(() -> {
            RopeConfig.resetForTests();
            RopeDisconnectPolicy.clearPendingPenaltiesForTests();
            RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        });

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        BlockPos anchorPos = new BlockPos(1, 1, 1);
        context.setBlockState(anchorPos, Blocks.OAK_FENCE);
        Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(anchorPos));

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.anchorCarriedPlayer(controller, anchor),
                Text.literal("Player rope did not convert to anchored rope."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), target, manager);
        context.assertEquals(1, RopeAnchoredOfflinePersistence.pendingCountForTests(),
                Text.literal("Anchored disconnect did not store an offline anchor record."));

        context.assertEquals(
                1,
                RopeAnchoredOfflinePersistence.removeForAnchor(
                        context.getWorld().getServer(),
                        context.getWorld().getRegistryKey(),
                        anchor),
                Text.literal("Broken anchor did not remove the offline anchored record."));
        context.assertEquals(0, RopeAnchoredOfflinePersistence.pendingCountForTests(),
                Text.literal("Broken anchor left stale offline anchored state."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void invalidOfflineAnchorDoesNotRestore(TestContext context) {
        RopeConfig.resetForTests();
        RopeDisconnectPolicy.clearPendingPenaltiesForTests();
        RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        context.addFinalTask(() -> {
            RopeConfig.resetForTests();
            RopeDisconnectPolicy.clearPendingPenaltiesForTests();
            RopeAnchoredOfflinePersistence.clearForTests(context.getWorld().getServer());
        });

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        BlockPos anchorPos = new BlockPos(1, 1, 1);
        context.setBlockState(anchorPos, Blocks.OAK_FENCE);
        Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(anchorPos));

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Lead-created player rope was not added."));
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.anchorCarriedPlayer(controller, anchor),
                Text.literal("Player rope did not convert to anchored rope."));

        RopeDisconnectPolicy.handleDisconnect(context.getWorld().getServer(), target, manager);
        context.setBlockState(anchorPos, Blocks.AIR);

        context.assertEquals(
                0,
                RopeAnchoredOfflinePersistence.restoreForPlayer(context.getWorld().getServer(), manager, target),
                Text.literal("Invalid offline anchor restored unexpectedly."));
        context.assertEquals(0, manager.activeCount(), Text.literal("Invalid anchor restore created an active rope."));
        context.assertEquals(0, RopeAnchoredOfflinePersistence.pendingCountForTests(),
                Text.literal("Invalid offline anchor record was not dropped."));
        context.complete();
    }

    private static int countLeads(ServerPlayerEntity player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.isOf(Items.LEAD)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void makeSurvival(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SURVIVAL);
        player.interactionManager.changeGameMode(GameMode.SURVIVAL);
        player.getAbilities().creativeMode = false;
        player.sendAbilitiesUpdate();
    }
}
