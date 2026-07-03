package com.mcelma.rope;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class RopeInteractionHandler {
    private RopeInteractionHandler() {
    }

    public static void register(RopeActionManager actionManager) {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity actor) || !(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            if (actor.getStackInHand(hand).isEmpty()) {
                return actionManager.startReleasePlayer(actor, target);
            }
            if (actor.getStackInHand(hand).isOf(Items.LEAD)) {
                return actionManager.startBindPlayer(actor, target);
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity actor)) {
                return ActionResult.PASS;
            }

            if (actor.getStackInHand(hand).isEmpty()) {
                ActionResult release = actionManager.startReleaseAnchor(actor, hitResult.getBlockPos());
                if (release != ActionResult.PASS) {
                    return release;
                }
                return actionManager.startAnchorPlayer(
                        actor,
                        world,
                        hitResult.getBlockPos(),
                        RopeActionManager.RequiredHand.EMPTY);
            }
            if (actor.getStackInHand(hand).isOf(Items.LEAD)) {
                return actionManager.startAnchorPlayer(
                        actor,
                        world,
                        hitResult.getBlockPos(),
                        RopeActionManager.RequiredHand.LEAD);
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity actor) || !actor.getStackInHand(hand).isEmpty()) {
                return ActionResult.PASS;
            }

            return actionManager.startSelfEscape(actor);
        });
    }
}
