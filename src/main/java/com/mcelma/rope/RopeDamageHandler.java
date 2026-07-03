package com.mcelma.rope;

import java.util.Optional;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class RopeDamageHandler {
    private RopeDamageHandler() {
    }

    public static void register(RopeManager ropeManager, RopeActionManager actionManager) {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (blocked || damageTaken <= 0.0F || !(entity instanceof ServerPlayerEntity player)) {
                return;
            }

            if (actionManager.cancelSelfEscape(player.getUuid())) {
                player.sendMessage(Text.literal("Escape attempt interrupted."), true);
            }
            maybeDropHeldRope(ropeManager, player);
        });
    }

    private static void maybeDropHeldRope(RopeManager ropeManager, ServerPlayerEntity holder) {
        if (!RopeConfig.enableHolderDamageDrop()) {
            return;
        }

        int denominator = RopeConfig.holderDamageDropDenominator();
        if (holder.getRandom().nextInt(denominator) != 0) {
            return;
        }

        Optional<RopeLink> removed = ropeManager.removeControlledPlayerLink(holder.getUuid());
        if (removed.isEmpty()) {
            return;
        }

        RopeLink link = removed.get();
        if (link.refundLeadOnManualRelease()) {
            holder.dropItem(new ItemStack(Items.LEAD), false);
        }

        holder.sendMessage(Text.literal("The rope slipped from your hand."), true);
        notifyOtherEndpoint(holder.getEntityWorld().getServer(), holder, link);
    }

    private static void notifyOtherEndpoint(MinecraftServer server, ServerPlayerEntity holder, RopeLink link) {
        RopeEndpoint other = link.otherEndpoint(holder.getUuid());
        if (other == null || other.type() != RopeEndpoint.Type.PLAYER) {
            return;
        }

        other.resolvePlayer(server)
                .ifPresent(player -> player.sendMessage(Text.literal("The rope slipped free."), true));
    }
}
