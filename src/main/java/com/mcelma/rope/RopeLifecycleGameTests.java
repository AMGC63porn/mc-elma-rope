package com.mcelma.rope;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class RopeLifecycleGameTests {
    @GameTest(maxTicks = 20)
    public void controllerReleaseRemovesPlayerRope(TestContext context) {
        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity rescuer = context.createMockCreativeServerPlayerInWorld();

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));
        context.assertEquals(
                RopeManager.ReleaseStatus.NOT_CONTROLLER,
                manager.releasePlayerForController(rescuer.getUuid(), target.getUuid()).status(),
                Text.literal("Non-controller should not directly release the rope."));
        context.assertEquals(1, manager.activeCount(), Text.literal("Failed release changed active rope count."));
        context.assertEquals(
                RopeManager.ReleaseStatus.RELEASED,
                manager.releasePlayerForController(controller.getUuid(), target.getUuid()).status(),
                Text.literal("Controller release failed."));
        context.assertEquals(0, manager.activeCount(), Text.literal("Controller release did not remove the rope."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void anchorReleaseRequiresController(TestContext context) {
        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 1, 1)));

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.anchorCarriedPlayer(controller, anchor),
                Text.literal("Controller could not anchor the carried player."));
        context.assertEquals(
                RopeManager.ReleaseStatus.NOT_CONTROLLER,
                manager.releaseAnchorForController(target, anchor).status(),
                Text.literal("Anchored target should not release controller-owned rope."));
        context.assertEquals(1, manager.activeCount(), Text.literal("Failed anchor release changed active rope count."));
        context.assertEquals(
                RopeManager.ReleaseStatus.RELEASED,
                manager.releaseAnchorForController(controller, anchor).status(),
                Text.literal("Controller could not release anchored rope."));
        context.assertEquals(0, manager.activeCount(), Text.literal("Anchor release did not remove the rope."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void tickClearsDeadAndSpectatorEndpoints(TestContext context) {
        RopeManager deadManager = new RopeManager();
        ServerPlayerEntity deadController = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity deadTarget = context.createMockCreativeServerPlayerInWorld();
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                deadManager.addPlayerLink(deadController, deadTarget, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Dead-endpoint test rope was not added."));
        deadTarget.discard();
        deadManager.tick(context.getWorld().getServer());
        context.assertEquals(0, deadManager.activeCount(), Text.literal("Dead endpoint rope was not cleared."));

        RopeManager spectatorManager = new RopeManager();
        ServerPlayerEntity spectatorController = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity spectatorTarget = context.createMockCreativeServerPlayerInWorld();
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                spectatorManager.addPlayerLink(
                        spectatorController,
                        spectatorTarget,
                        RopeConfig.defaultPlayerRopeLength(),
                        true),
                Text.literal("Spectator-endpoint test rope was not added."));
        makeSpectator(spectatorTarget);
        spectatorManager.tick(context.getWorld().getServer());
        context.assertEquals(0, spectatorManager.activeCount(), Text.literal("Spectator endpoint rope was not cleared."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void maxHeldDurationExpiresOldRopes(TestContext context) {
        RopeConfig.resetForTests();
        context.addFinalTask(RopeConfig::resetForTests);
        RopeConfig.loadJsonForTests("""
                {
                  "maxHeldDurationTicks": 1
                }
                """);

        RopeManager manager = new RopeManager();
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));

        manager.tick(context.getWorld().getServer());

        context.assertEquals(0, manager.activeCount(), Text.literal("Old rope was not expired."));
        RopeConfig.resetForTests();
        context.complete();
    }

    private static void makeSpectator(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SPECTATOR);
        player.interactionManager.changeGameMode(GameMode.SPECTATOR);
    }
}
