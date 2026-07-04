package com.mcelma.rope;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class RopePhysicsGameTests {
    @GameTest(maxTicks = 20)
    public void tautPlayerRopeCorrectsOnlyTiedPlayerVelocity(TestContext context) {
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        controller.refreshPositionAndAngles(context.getAbsolute(new Vec3d(1.0D, 2.0D, 1.0D)), 0.0F, 0.0F);
        target.refreshPositionAndAngles(context.getAbsolute(new Vec3d(9.0D, 2.0D, 1.0D)), 0.0F, 0.0F);
        controller.setVelocity(Vec3d.ZERO);
        target.setVelocity(Vec3d.ZERO);

        RopeLink link = new RopeLink(
                controller.getUuid(),
                RopeEndpoint.player(controller),
                RopeEndpoint.player(target),
                4.0D,
                true);

        context.assertTrue(RopePhysics.tick(context.getWorld().getServer(), link), Text.literal("Physics tick failed."));
        context.assertTrue(link.isTaut(), Text.literal("Stretched rope was not marked taut."));
        context.assertEquals(Vec3d.ZERO, controller.getVelocity(), Text.literal("Controller velocity was changed."));
        context.assertTrue(target.getVelocity().x < 0.0D, Text.literal("Tied player was not pulled inward."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void loosePlayerRopeKeepsVelocityAndTautStateClear(TestContext context) {
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        Vec3d targetVelocity = new Vec3d(0.05D, 0.0D, 0.02D);
        controller.refreshPositionAndAngles(context.getAbsolute(new Vec3d(1.0D, 2.0D, 1.0D)), 0.0F, 0.0F);
        target.refreshPositionAndAngles(context.getAbsolute(new Vec3d(2.0D, 2.0D, 1.0D)), 0.0F, 0.0F);
        target.setVelocity(targetVelocity);

        RopeLink link = new RopeLink(
                controller.getUuid(),
                RopeEndpoint.player(controller),
                RopeEndpoint.player(target),
                4.0D,
                true);
        link.setTaut(true);

        context.assertTrue(RopePhysics.tick(context.getWorld().getServer(), link), Text.literal("Physics tick failed."));
        context.assertFalse(link.isTaut(), Text.literal("Loose rope stayed taut."));
        context.assertEquals(targetVelocity, target.getVelocity(), Text.literal("Loose rope changed target velocity."));
        context.complete();
    }
}
