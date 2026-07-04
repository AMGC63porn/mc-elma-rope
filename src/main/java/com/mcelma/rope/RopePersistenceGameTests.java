package com.mcelma.rope;

import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class RopePersistenceGameTests {
    @GameTest(maxTicks = 20)
    public void saveLoadRestoresOnlinePlayerRope(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        preparePersistenceTest(server, context);
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        RopeManager manager = new RopeManager();
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addPlayerLink(controller, target, RopeConfig.defaultPlayerRopeLength(), true),
                Text.literal("Player rope was not added."));
        RopeLink original = manager.links().get(0);
        original.tickAge();

        RopePersistence.save(server, manager);
        RopeManager restoredManager = new RopeManager();
        RopePersistence.loadPending(server, restoredManager);

        context.assertEquals(1, restoredManager.activeCount(), Text.literal("Persisted player rope did not restore."));
        context.assertEquals(0, RopePersistence.pendingLinkCountForTests(),
                Text.literal("Restored player rope stayed pending."));
        RopeLink restored = restoredManager.links().get(0);
        context.assertEquals(original.id(), restored.id(), Text.literal("Persisted rope id changed."));
        context.assertEquals(1, restored.ageTicks(), Text.literal("Persisted rope age did not restore."));
        context.assertTrue(restored.refundLeadOnManualRelease(), Text.literal("Lead refund flag did not restore."));
        context.assertTrue(restored.hasOnlyPlayerEndpoints(), Text.literal("Player rope endpoints did not restore."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void saveLoadRestoresAnchoredRope(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        preparePersistenceTest(server, context);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        Vec3d anchor = Vec3d.ofCenter(context.getAbsolutePos(new net.minecraft.util.math.BlockPos(1, 1, 1)));
        RopeManager manager = new RopeManager();
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addAnchor(player, anchor, 999.0D),
                Text.literal("Anchor rope was not added."));

        RopePersistence.save(server, manager);
        RopeManager restoredManager = new RopeManager();
        RopePersistence.loadPending(server, restoredManager);

        context.assertEquals(1, restoredManager.activeCount(), Text.literal("Persisted anchor rope did not restore."));
        RopeLink restored = restoredManager.links().get(0);
        context.assertFalse(restored.hasOnlyPlayerEndpoints(), Text.literal("Anchor endpoint did not restore."));
        context.assertEquals(48.0D, restored.length(), Text.literal("Persisted anchor rope length was not clamped."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void missingPlayerEndpointStaysPendingUntilAvailable(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        preparePersistenceTest(server, context);
        ServerPlayerEntity controller = context.createMockCreativeServerPlayerInWorld();
        UUID missingPlayer = UUID.randomUUID();
        RopePersistence.writeStateJsonForTests(server, """
                {
                  "schemaVersion": 1,
                  "links": [
                    {
                      "id": "%s",
                      "controllerUuid": "%s",
                      "first": {
                        "type": "player",
                        "playerUuid": "%s"
                      },
                      "second": {
                        "type": "player",
                        "playerUuid": "%s"
                      },
                      "length": 12.0,
                      "refundLeadOnManualRelease": true,
                      "ageTicks": 7
                    }
                  ]
                }
                """.formatted(UUID.randomUUID(), controller.getUuid(), controller.getUuid(), missingPlayer));

        RopeManager restoredManager = new RopeManager();
        RopePersistence.loadPending(server, restoredManager);

        context.assertEquals(0, restoredManager.activeCount(),
                Text.literal("Rope with missing player endpoint should not restore."));
        context.assertEquals(1, RopePersistence.pendingLinkCountForTests(),
                Text.literal("Rope with missing player endpoint should remain pending."));
        context.complete();
    }

    private static void preparePersistenceTest(MinecraftServer server, TestContext context) {
        RopeConfig.resetForTests();
        RopeConfig.loadJsonForTests("{\"persistRopes\": true}");
        RopePersistence.clearForTests(server);
        context.addFinalTask(() -> {
            RopePersistence.clearForTests(server);
            RopeConfig.resetForTests();
        });
    }
}
