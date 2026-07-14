package com.mcelma.rope.test;

import java.util.UUID;

import com.mcelma.rope.RopeConfig;
import com.mcelma.rope.RopeEndpoint;
import com.mcelma.rope.RopeLink;
import com.mcelma.rope.RopeManager;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class McElmaRopeGameTests {
    @GameTest(maxTicks = 20)
    public void configDefaultsAndAnchorTags(TestContext context) {
        context.assertEquals(48.0D, RopeConfig.maxRopeLength(), Text.literal("Default max rope length changed."));
        context.assertEquals(60, RopeConfig.bindDurationTicks(), Text.literal("Default bind duration changed."));
        context.assertEquals(240, RopeConfig.thirdPartyReleaseDurationTicks(), Text.literal("Third-party rescue duration changed."));
        context.assertEquals(2400, RopeConfig.disconnectPenaltyDurationTicks(), Text.literal("Disconnect penalty duration changed."));
        context.assertEquals("vanilla_like", RopeConfig.ropeVisualStyle(), Text.literal("Default visual style changed."));
        context.assertTrue(RopeConfig.persistAnchoredRopesOnDisconnect(),
                Text.literal("Default anchored disconnect persistence changed."));

        BlockPos fencePos = new BlockPos(1, 1, 1);
        context.setBlockState(fencePos, Blocks.OAK_FENCE);
        context.assertTrue(RopeConfig.isAnchorBlock(context.getWorld(), context.getAbsolutePos(fencePos)),
                Text.literal("Default fence tag anchor support failed."));

        BlockPos chainPos = new BlockPos(2, 1, 1);
        context.setBlockState(chainPos, Blocks.IRON_CHAIN);
        context.assertTrue(RopeConfig.isAnchorBlock(context.getWorld(), context.getAbsolutePos(chainPos)),
                Text.literal("Default chain anchor support failed."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void managerRejectsSelfLinkAndClearsAnchor(TestContext context) {
        RopeManager manager = new RopeManager();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();

        context.assertEquals(
                RopeManager.AddResult.SAME_PLAYER,
                manager.addPlayerLink(player, player, RopeConfig.defaultPlayerRopeLength()),
                Text.literal("Self-links must be rejected."));
        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.addAnchor(player, Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 1, 1))), 999.0D),
                Text.literal("Admin anchor add failed."));
        context.assertEquals(1, manager.activeCount(), Text.literal("Anchor rope was not tracked."));
        context.assertEquals(48.0D, manager.links().get(0).length(), Text.literal("Anchor rope length was not clamped."));
        context.assertEquals(1, manager.removeForPlayer(player.getUuid()), Text.literal("Player rope cleanup failed."));
        context.assertEquals(0, manager.activeCount(), Text.literal("Rope manager did not clear the link."));
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void restoreRejectsAlreadyLinkedPlayer(TestContext context) {
        RopeManager manager = new RopeManager();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        RopeEndpoint playerEndpoint = RopeEndpoint.player(player);
        RopeEndpoint firstAnchor = RopeEndpoint.anchor(player.getEntityWorld().getRegistryKey(), Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 1, 1))));
        RopeEndpoint secondAnchor = RopeEndpoint.anchor(player.getEntityWorld().getRegistryKey(), Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(2, 1, 1))));

        RopeLink first = RopeLink.restored(
                UUID.randomUUID(),
                player.getUuid(),
                playerEndpoint,
                firstAnchor,
                12.0D,
                true,
                -20);
        RopeLink second = RopeLink.restored(
                UUID.randomUUID(),
                player.getUuid(),
                playerEndpoint,
                secondAnchor,
                12.0D,
                true,
                0);

        context.assertEquals(
                RopeManager.AddResult.ADDED,
                manager.restoreLink(context.getWorld().getServer(), first),
                Text.literal("First persisted rope did not restore."));
        context.assertEquals(0, first.ageTicks(), Text.literal("Restored rope age was not sanitized."));
        context.assertEquals(
                RopeManager.AddResult.SECOND_ALREADY_LINKED,
                manager.restoreLink(context.getWorld().getServer(), second),
                Text.literal("Duplicate persisted rope should not restore."));
        context.assertEquals(1, manager.activeCount(), Text.literal("Duplicate restore changed active link count."));
        context.complete();
    }
}
