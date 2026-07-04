package com.mcelma.rope;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class RopeConfigGameTests {
    @GameTest(maxTicks = 20)
    public void customConfigReloadsAnchorsAndSanitizesValues(TestContext context) {
        RopeConfig.resetForTests();
        context.addFinalTask(RopeConfig::resetForTests);

        RopeConfig.loadJsonForTests("""
                {
                  "maxRopeLength": -5.0,
                  "defaultPlayerRopeLength": 999.0,
                  "bindDurationTicks": 0,
                  "controllerReleaseDurationTicks": -2,
                  "thirdPartyReleaseDurationTicks": 0,
                  "selfEscapeSuccessDenominator": 0,
                  "ropeVisualSegments": 100,
                  "ropeVisualSag": 99.0,
                  "ropeVisualWidthPreset": "wide",
                  "ropePhysicsPreset": "nonsense",
                  "anchorBlockIds": [
                    "minecraft:diamond_block",
                    "#minecraft:fences",
                    "bad id"
                  ]
                }
                """);

        context.assertEquals(48.0D, RopeConfig.maxRopeLength(), Text.literal("Invalid max length did not fall back."));
        context.assertEquals(48.0D, RopeConfig.defaultPlayerRopeLength(),
                Text.literal("Default player rope length was not clamped."));
        context.assertEquals(60, RopeConfig.bindDurationTicks(), Text.literal("Invalid bind duration did not fall back."));
        context.assertEquals(60, RopeConfig.controllerReleaseDurationTicks(),
                Text.literal("Invalid release duration did not fall back."));
        context.assertEquals(240, RopeConfig.thirdPartyReleaseDurationTicks(),
                Text.literal("Invalid rescue duration did not fall back."));
        context.assertEquals(300, RopeConfig.selfEscapeSuccessDenominator(),
                Text.literal("Invalid escape denominator did not fall back."));
        context.assertEquals(64, RopeConfig.ropeVisualSegments(), Text.literal("Visual segment clamp failed."));
        context.assertEquals(0.25D, RopeConfig.ropeVisualSag(), Text.literal("Visual sag clamp failed."));
        context.assertEquals("balanced", RopeConfig.ropeVisualWidthPreset(), Text.literal("Width preset fallback failed."));
        context.assertEquals("custom", RopeConfig.ropePhysicsPreset(), Text.literal("Physics preset fallback failed."));
        context.assertEquals(1, RopeConfig.anchorBlockIdCount(), Text.literal("Anchor id reload count failed."));
        context.assertEquals(1, RopeConfig.anchorBlockTagCount(), Text.literal("Anchor tag reload count failed."));

        BlockPos diamondPos = new BlockPos(1, 1, 1);
        context.setBlockState(diamondPos, Blocks.DIAMOND_BLOCK);
        context.assertTrue(RopeConfig.isAnchorBlock(context.getWorld(), context.getAbsolutePos(diamondPos)),
                Text.literal("Custom anchor block id did not reload."));

        BlockPos fencePos = new BlockPos(2, 1, 1);
        context.setBlockState(fencePos, Blocks.OAK_FENCE);
        context.assertTrue(RopeConfig.isAnchorBlock(context.getWorld(), context.getAbsolutePos(fencePos)),
                Text.literal("Custom anchor tag did not reload."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void malformedConfigFallsBackToDefaults(TestContext context) {
        RopeConfig.resetForTests();
        context.addFinalTask(RopeConfig::resetForTests);

        RopeConfig.loadJsonForTests("{ this is not valid json");

        context.assertEquals(48.0D, RopeConfig.maxRopeLength(), Text.literal("Malformed config did not reset max length."));
        context.assertEquals(12.0D, RopeConfig.defaultPlayerRopeLength(),
                Text.literal("Malformed config did not reset player rope length."));
        context.assertEquals(3, RopeConfig.anchorBlockTagCount(),
                Text.literal("Malformed config did not reset default anchor tags."));
        BlockPos chainPos = new BlockPos(1, 1, 1);
        context.setBlockState(chainPos, Blocks.IRON_CHAIN);
        context.assertTrue(RopeConfig.isAnchorBlock(context.getWorld(), context.getAbsolutePos(chainPos)),
                Text.literal("Malformed config did not restore default chain anchor."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void protectedPlayerNamesReloadFromConfig(TestContext context) {
        RopeConfig.resetForTests();
        context.addFinalTask(RopeConfig::resetForTests);

        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.assertFalse(RopeConfig.isProtectedPlayer(player), Text.literal("Mock player started protected."));

        RopeConfig.loadJsonForTests("""
                {
                  "protectedPlayerNames": [
                    "test-mock-player"
                  ]
                }
                """);

        context.assertTrue(RopeConfig.isProtectedPlayer(player), Text.literal("Protected name did not reload."));
        context.complete();
    }
}
