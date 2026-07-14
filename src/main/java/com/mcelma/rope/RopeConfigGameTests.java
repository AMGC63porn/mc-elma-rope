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
                  "ropeVisualStyle": "rainbow",
                  "ropePhysicsPreset": "nonsense",
                  "persistAnchoredRopesOnDisconnect": false,
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
        context.assertEquals("vanilla_like", RopeConfig.ropeVisualStyle(),
                Text.literal("Visual style fallback failed."));
        context.assertEquals("custom", RopeConfig.ropePhysicsPreset(), Text.literal("Physics preset fallback failed."));
        context.assertFalse(RopeConfig.persistAnchoredRopesOnDisconnect(),
                Text.literal("Anchored disconnect persistence did not reload."));
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
        RopeConfig.resetForTests();
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
        context.assertEquals("vanilla_like", RopeConfig.ropeVisualStyle(),
                Text.literal("Malformed config did not reset visual style."));
        context.assertTrue(RopeConfig.persistAnchoredRopesOnDisconnect(),
                Text.literal("Malformed config did not reset anchored disconnect persistence."));
        context.assertEquals(3, RopeConfig.anchorBlockTagCount(),
                Text.literal("Malformed config did not reset default anchor tags."));
        BlockPos chainPos = new BlockPos(1, 1, 1);
        context.setBlockState(chainPos, Blocks.IRON_CHAIN);
        context.assertTrue(RopeConfig.isAnchorBlock(context.getWorld(), context.getAbsolutePos(chainPos)),
                Text.literal("Malformed config did not restore default chain anchor."));
        RopeConfig.resetForTests();
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
        RopeConfig.resetForTests();
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void protectedPlayerIdsReloadFromConfig(TestContext context) {
        RopeConfig.resetForTests();
        context.addFinalTask(RopeConfig::resetForTests);

        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        context.assertFalse(RopeConfig.isProtectedPlayer(player), Text.literal("Mock player started protected."));

        RopeConfig.loadJsonForTests("""
                {
                  "protectedPlayerIds": [
                    "%s"
                  ]
                }
                """.formatted(player.getUuidAsString()));

        context.assertTrue(RopeConfig.isProtectedPlayer(player), Text.literal("Protected UUID did not reload."));
        RopeConfig.resetForTests();
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void physicsPresetsOverrideCustomValues(TestContext context) {
        RopeConfig.resetForTests();
        context.addFinalTask(RopeConfig::resetForTests);

        RopeConfig.loadJsonForTests("""
                {
                  "ropePhysicsPreset": "strict",
                  "ropeCorrectionRate": 0.01,
                  "ropeMaxPullSpeed": 0.01,
                  "ropeEmergencyStretchMultiplier": 9.0,
                  "ropeEmergencyMaxPullSpeed": 0.01,
                  "ropeSwingDamping": 0.5
                }
                """);

        context.assertEquals("strict", RopeConfig.ropePhysicsPreset(), Text.literal("Strict preset did not load."));
        context.assertEquals(0.52D, RopeConfig.ropeCorrectionRate(),
                Text.literal("Strict correction rate did not override custom value."));
        context.assertEquals(0.58D, RopeConfig.ropeMaxPullSpeed(),
                Text.literal("Strict max pull speed did not override custom value."));
        context.assertEquals(2.0D, RopeConfig.ropeEmergencyStretchMultiplier(),
                Text.literal("Strict emergency stretch did not override custom value."));
        context.assertEquals(1.0D, RopeConfig.ropeEmergencyMaxPullSpeed(),
                Text.literal("Strict emergency speed did not override custom value."));
        context.assertEquals(0.972D, RopeConfig.ropeSwingDamping(),
                Text.literal("Strict swing damping did not override custom value."));
        RopeConfig.resetForTests();
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void holderDamageTypeFiltersReloadFromConfig(TestContext context) {
        RopeConfig.resetForTests();
        context.addFinalTask(RopeConfig::resetForTests);

        RopeConfig.loadJsonForTests("""
                {
                  "holderDamageDropAllowedDamageTypeIds": [
                    "minecraft:player_attack"
                  ],
                  "holderDamageDropDeniedDamageTypeIds": [
                    "minecraft:fall"
                  ]
                }
                """);

        context.assertEquals(1, RopeConfig.holderDamageDropAllowedDamageTypeCount(),
                Text.literal("Allowed damage type count did not reload."));
        context.assertEquals(1, RopeConfig.holderDamageDropDeniedDamageTypeCount(),
                Text.literal("Denied damage type count did not reload."));
        context.assertTrue(
                RopeConfig.isHolderDamageTypeAllowed("minecraft:player_attack", "player_attack"),
                Text.literal("Allowed damage type was rejected."));
        context.assertFalse(
                RopeConfig.isHolderDamageTypeAllowed("minecraft:fall", "fall"),
                Text.literal("Denied damage type was accepted."));
        context.assertFalse(
                RopeConfig.isHolderDamageTypeAllowed("minecraft:lava", "lava"),
                Text.literal("Unlisted damage type was accepted while allow list was active."));
        RopeConfig.resetForTests();
        context.complete();
    }
}
