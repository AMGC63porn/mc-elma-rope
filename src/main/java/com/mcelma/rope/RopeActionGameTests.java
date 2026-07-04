package com.mcelma.rope;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class RopeActionGameTests {
    @GameTest(maxTicks = 20)
    public void timedBindConsumesLeadAndCreatesRope(TestContext context) {
        RopeManager manager = new RopeManager();
        RopeActionManager actions = new RopeActionManager(manager);
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        placeFacingSouth(context, controller, new Vec3d(1.0D, 2.0D, 1.0D));
        placeFacingSouth(context, target, new Vec3d(1.0D, 2.0D, 4.0D));
        makeSurvival(controller);
        controller.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.LEAD, 2));

        context.assertEquals(
                ActionResult.SUCCESS,
                actions.startBindPlayer(controller, target),
                Text.literal("Lead bind action did not start."));
        tickActions(actions, context.getWorld().getServer(), RopeConfig.bindDurationTicks());

        context.assertEquals(1, manager.activeCount(), Text.literal("Timed bind did not create a rope."));
        context.assertEquals(1, controller.getStackInHand(Hand.MAIN_HAND).getCount(),
                Text.literal("Timed bind did not consume exactly one lead."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void controllerTimedReleaseClearsRope(TestContext context) {
        RopeManager manager = new RopeManager();
        RopeActionManager actions = new RopeActionManager(manager);
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        placeFacingSouth(context, controller, new Vec3d(2.0D, 2.0D, 1.0D));
        placeFacingSouth(context, target, new Vec3d(2.0D, 2.0D, 4.0D));
        controller.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));

        context.assertEquals(
                ActionResult.SUCCESS,
                actions.startReleasePlayer(controller, target),
                Text.literal("Controller release action did not start."));
        tickActions(actions, context.getWorld().getServer(), RopeConfig.controllerReleaseDurationTicks());

        context.assertEquals(0, manager.activeCount(), Text.literal("Controller timed release did not clear rope."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void thirdPartyRescueUsesSlowDuration(TestContext context) {
        RopeManager manager = new RopeManager();
        RopeActionManager actions = new RopeActionManager(manager);
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity rescuer = context.createMockCreativeServerPlayerInWorld();
        placeFacingSouth(context, controller, new Vec3d(3.0D, 2.0D, 1.0D));
        placeFacingSouth(context, target, new Vec3d(3.0D, 2.0D, 4.0D));
        placeFacingSouth(context, rescuer, new Vec3d(4.0D, 2.0D, 1.0D));
        rescuer.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));

        context.assertEquals(
                ActionResult.SUCCESS,
                actions.startReleasePlayer(rescuer, target),
                Text.literal("Third-party rescue did not start."));
        tickActions(actions, context.getWorld().getServer(), RopeConfig.controllerReleaseDurationTicks());
        context.assertEquals(1, manager.activeCount(), Text.literal("Rescue finished at controller duration."));
        tickActions(
                actions,
                context.getWorld().getServer(),
                RopeConfig.thirdPartyReleaseDurationTicks() - RopeConfig.controllerReleaseDurationTicks());

        context.assertEquals(0, manager.activeCount(), Text.literal("Third-party rescue did not clear rope."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void tiedPlayerCannotRescueAnotherRope(TestContext context) {
        RopeManager manager = new RopeManager();
        RopeActionManager actions = new RopeActionManager(manager);
        ServerPlayerEntity firstController = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity tiedActor = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity secondController = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity secondTarget = context.createMockCreativeServerPlayerInWorld();
        placeFacingSouth(context, firstController, new Vec3d(5.0D, 2.0D, 1.0D));
        placeFacingSouth(context, tiedActor, new Vec3d(5.0D, 2.0D, 2.0D));
        placeFacingSouth(context, secondController, new Vec3d(6.0D, 2.0D, 1.0D));
        placeFacingSouth(context, secondTarget, new Vec3d(5.0D, 2.0D, 4.0D));
        tiedActor.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(firstController, tiedActor, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("First player rope was not added."));
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(secondController, secondTarget, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Second player rope was not added."));

        context.assertEquals(
                ActionResult.FAIL,
                actions.startReleasePlayer(tiedActor, secondTarget),
                Text.literal("Tied player should not be able to rescue another rope."));
        context.assertFalse(actions.hasAction(tiedActor.getUuid()), Text.literal("Denied rescue still created an action."));
        context.assertEquals(2, manager.activeCount(), Text.literal("Denied rescue changed active rope count."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void timedAnchorActionConvertsCarriedPlayerToAnchor(TestContext context) {
        RopeManager manager = new RopeManager();
        RopeActionManager actions = new RopeActionManager(manager);
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        BlockPos anchorPos = new BlockPos(8, 1, 4);
        context.setBlockState(anchorPos, Blocks.OAK_FENCE);
        placeFacingSouth(context, controller, new Vec3d(8.0D, 2.0D, 1.0D));
        placeFacingSouth(context, target, new Vec3d(8.0D, 2.0D, 2.0D));
        controller.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.LEAD));
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));

        context.assertEquals(
                ActionResult.SUCCESS,
                actions.startAnchorPlayer(controller, context.getWorld(), context.getAbsolutePos(anchorPos),
                        RopeActionManager.RequiredHand.LEAD),
                Text.literal("Anchor action did not start."));
        tickActions(actions, context.getWorld().getServer(), RopeConfig.bindDurationTicks());

        context.assertEquals(1, manager.activeCount(), Text.literal("Anchor action changed active rope count."));
        context.assertFalse(manager.links().get(0).hasOnlyPlayerEndpoints(),
                Text.literal("Anchor action did not convert player rope to anchored rope."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void tautSelfEscapeFailsWithoutStartingAction(TestContext context) {
        RopeManager manager = new RopeManager();
        RopeActionManager actions = new RopeActionManager(manager);
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        target.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));
        manager.links().get(0).setTaut(true);

        context.assertEquals(
                ActionResult.FAIL,
                actions.startSelfEscape(target),
                Text.literal("Taut self-escape should fail by default."));
        context.assertFalse(actions.hasAction(target.getUuid()), Text.literal("Failed self-escape still created an action."));
        context.assertEquals(1, manager.activeCount(), Text.literal("Failed self-escape changed active rope count."));
        context.complete();
    }

    private static void tickActions(RopeActionManager actions, MinecraftServer server, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            actions.tick(server);
        }
    }

    private static void placeFacingSouth(TestContext context, ServerPlayerEntity player, Vec3d relativePos) {
        player.refreshPositionAndAngles(context.getAbsolute(relativePos), 0.0F, 0.0F);
    }

    private static void makeSurvival(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SURVIVAL);
        player.interactionManager.changeGameMode(GameMode.SURVIVAL);
        player.getAbilities().creativeMode = false;
        player.sendAbilitiesUpdate();
    }
}
