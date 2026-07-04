package com.mcelma.rope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class RopeActionManager {
    private static final double LOOK_DOT_THRESHOLD = 0.55D;
    private static final double SELF_ESCAPE_MAX_MOVEMENT_SQUARED = 0.25D;

    private final RopeManager ropeManager;
    private final Map<UUID, PendingAction> actions = new HashMap<>();
    private final Map<UUID, Integer> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> selfEscapeCooldowns = new HashMap<>();

    public RopeActionManager(RopeManager ropeManager) {
        this.ropeManager = ropeManager;
    }

    public void tick(MinecraftServer server) {
        tickCooldowns();
        if (actions.isEmpty()) {
            return;
        }

        Iterator<PendingAction> iterator = actions.values().iterator();
        while (iterator.hasNext()) {
            PendingAction action = iterator.next();
            ServerPlayerEntity actor = server.getPlayerManager().getPlayer(action.actorUuid);
            if (actor == null || !isUsable(actor) || !isStillValid(server, actor, action)) {
                if (actor != null) {
                    actor.sendMessage(Text.literal("Rope action canceled: keep the right hand, distance, and aim."), true);
                }
                iterator.remove();
                applyActionCooldown(action);
                continue;
            }

            action.elapsedTicks++;
            if (action.elapsedTicks == 1 || action.elapsedTicks % 20 == 0) {
                sendProgress(actor, action);
            }
            if (action.elapsedTicks >= action.durationTicks) {
                complete(server, actor, action);
                iterator.remove();
                applyActionCooldown(action);
            }
        }
    }

    public ActionResult startBindPlayer(ServerPlayerEntity actor, ServerPlayerEntity target) {
        if (actor.getUuid().equals(target.getUuid())) {
            return fail(actor, "Cannot tie yourself.");
        }
        if (cannotStart(actor)) {
            return ActionResult.FAIL;
        }
        if (!withinStartDistance(actor, target.getEyePos())) {
            return fail(actor, "That player is too far away.");
        }
        if (RopeConfig.isProtectedPlayer(actor) || RopeConfig.isProtectedPlayer(target)) {
            return fail(actor, "That player is protected from rope binding.");
        }
        if (isInSpawnProtection(actor) || isInSpawnProtection(target)) {
            return fail(actor, "Rope binding is disabled near spawn.");
        }
        if (ropeManager.hasLink(actor.getUuid())) {
            return fail(actor, "You are already tied and cannot bind another player.");
        }
        if (ropeManager.hasLink(target.getUuid())) {
            return fail(actor, "That player is already tied.");
        }

        PendingAction action = PendingAction.player(
                ActionType.BIND_PLAYER,
                actor,
                target,
                null,
                RopeConfig.bindDurationTicks(),
                RequiredHand.LEAD,
                false);
        actions.put(actor.getUuid(), action);
        actor.sendMessage(Text.literal("Tying " + target.getName().getString() + ". Keep looking."), true);
        return ActionResult.SUCCESS;
    }

    public ActionResult startAnchorPlayer(ServerPlayerEntity actor, World world, BlockPos blockPos, RequiredHand hand) {
        if (!RopeConfig.isAnchorBlock(world, blockPos)) {
            return ActionResult.PASS;
        }
        if (cannotStart(actor)) {
            return ActionResult.FAIL;
        }

        Vec3d anchor = Vec3d.ofCenter(blockPos);
        if (!withinStartDistance(actor, anchor)) {
            return fail(actor, "That anchor is too far away.");
        }

        Optional<RopeLink> link = ropeManager.findForPlayer(actor.getUuid());
        if (link.isEmpty()) {
            return fail(actor, "Tie a player first, then use this anchor.");
        }
        RopeLink current = link.get();
        if (!actor.getUuid().equals(current.controllerUuid())) {
            return fail(actor, "Only the player leading this rope can tie it to an anchor.");
        }
        RopeEndpoint carried = current.otherEndpoint(actor.getUuid());
        if (carried == null || carried.type() != RopeEndpoint.Type.PLAYER) {
            return fail(actor, "No carried player found.");
        }

        PendingAction action = PendingAction.anchor(
                ActionType.ANCHOR_PLAYER,
                actor,
                current.id(),
                world.getRegistryKey(),
                blockPos,
                RopeConfig.bindDurationTicks(),
                hand,
                false);
        actions.put(actor.getUuid(), action);
        actor.sendMessage(Text.literal("Tying rope to anchor. Keep looking."), true);
        return ActionResult.SUCCESS;
    }

    public ActionResult startReleasePlayer(ServerPlayerEntity actor, ServerPlayerEntity target) {
        Optional<RopeLink> link = ropeManager.findForPlayer(target.getUuid());
        if (link.isEmpty()) {
            return ActionResult.PASS;
        }
        if (cannotStart(actor)) {
            return ActionResult.FAIL;
        }
        if (!withinStartDistance(actor, target.getEyePos())) {
            return fail(actor, "That player is too far away.");
        }
        if (link.get().includesPlayer(actor.getUuid()) && !actor.getUuid().equals(link.get().controllerUuid())) {
            return startSelfEscape(actor, link.get());
        }

        ReleasePlan releasePlan = releasePlan(actor, link.get());
        if (!releasePlan.allowed()) {
            return fail(actor, releasePlan.message());
        }

        PendingAction action = PendingAction.player(
                ActionType.RELEASE_PLAYER,
                actor,
                target,
                link.get().id(),
                releasePlan.durationTicks(),
                RequiredHand.EMPTY,
                releasePlan.thirdParty());
        actions.put(actor.getUuid(), action);
        actor.sendMessage(Text.literal(releasePlan.startMessage(target.getName().getString())), true);
        return ActionResult.SUCCESS;
    }

    public ActionResult startReleaseAnchor(ServerPlayerEntity actor, BlockPos blockPos) {
        Vec3d anchor = Vec3d.ofCenter(blockPos);
        Optional<RopeLink> link = ropeManager.findForAnchor(actor.getEntityWorld().getRegistryKey(), anchor);
        if (link.isEmpty()) {
            return ActionResult.PASS;
        }
        if (cannotStart(actor)) {
            return ActionResult.FAIL;
        }
        if (!withinStartDistance(actor, anchor)) {
            return fail(actor, "That anchor is too far away.");
        }
        if (link.get().includesPlayer(actor.getUuid()) && !actor.getUuid().equals(link.get().controllerUuid())) {
            return startSelfEscape(actor, link.get());
        }

        ReleasePlan releasePlan = releasePlan(actor, link.get());
        if (!releasePlan.allowed()) {
            return fail(actor, releasePlan.message());
        }

        PendingAction action = PendingAction.anchor(
                ActionType.RELEASE_ANCHOR,
                actor,
                link.get().id(),
                actor.getEntityWorld().getRegistryKey(),
                blockPos,
                releasePlan.durationTicks(),
                RequiredHand.EMPTY,
                releasePlan.thirdParty());
        actions.put(actor.getUuid(), action);
        actor.sendMessage(Text.literal(releasePlan.startMessage("the anchor rope")), true);
        return ActionResult.SUCCESS;
    }

    public ActionResult startSelfEscape(ServerPlayerEntity actor) {
        Optional<RopeLink> link = ropeManager.findForPlayer(actor.getUuid());
        if (link.isEmpty() || actor.getUuid().equals(link.get().controllerUuid())) {
            return ActionResult.PASS;
        }
        if (cannotStart(actor)) {
            return ActionResult.FAIL;
        }

        return startSelfEscape(actor, link.get());
    }

    public boolean cancelSelfEscape(UUID playerUuid) {
        PendingAction action = actions.get(playerUuid);
        if (action == null || action.type != ActionType.SELF_ESCAPE) {
            return false;
        }

        actions.remove(playerUuid);
        applySelfEscapeCooldown(playerUuid);
        return true;
    }

    public boolean hasAction(UUID playerUuid) {
        return actions.containsKey(playerUuid);
    }

    private boolean cannotStart(ServerPlayerEntity actor) {
        if (actions.containsKey(actor.getUuid())) {
            actor.sendMessage(Text.literal("Keep looking to finish the current rope action."), true);
            return true;
        }
        if (cooldowns.getOrDefault(actor.getUuid(), 0) > 0) {
            actor.sendMessage(Text.literal("Wait a moment before another rope action."), true);
            return true;
        }
        return false;
    }

    private ActionResult startSelfEscape(ServerPlayerEntity actor, RopeLink link) {
        if (!RopeConfig.enableSelfEscape()) {
            return fail(actor, "You cannot escape this rope by yourself.");
        }
        if (selfEscapeCooldowns.getOrDefault(actor.getUuid(), 0) > 0) {
            return fail(actor, "You need more time before another escape attempt.");
        }
        if (nearController(actor, link)) {
            applySelfEscapeCooldown(actor.getUuid());
            return fail(actor, "The rope holder is too close to escape.");
        }
        if (RopeConfig.selfEscapeCancelWhenTaut() && link.isTaut()) {
            applySelfEscapeCooldown(actor.getUuid());
            return fail(actor, "The rope is too tight to escape.");
        }

        PendingAction action = PendingAction.selfEscape(actor, link.id(), RopeConfig.selfEscapeDurationTicks());
        actions.put(actor.getUuid(), action);
        actor.sendMessage(Text.literal("Trying to loosen the rope. Stay still."), true);
        return ActionResult.SUCCESS;
    }

    private boolean isStillValid(MinecraftServer server, ServerPlayerEntity actor, PendingAction action) {
        if (!matchesRequiredHand(actor, action.requiredHand)) {
            return false;
        }

        return switch (action.type) {
            case BIND_PLAYER -> validateBind(server, actor, action);
            case ANCHOR_PLAYER -> validateAnchor(actor, action);
            case RELEASE_PLAYER -> validateReleasePlayer(server, actor, action);
            case RELEASE_ANCHOR -> validateReleaseAnchor(actor, action);
            case SELF_ESCAPE -> validateSelfEscape(actor, action);
        };
    }

    private boolean validateBind(MinecraftServer server, ServerPlayerEntity actor, PendingAction action) {
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(action.targetUuid);
        return target != null
                && isUsable(target)
                && sameWorld(actor, target)
                && !RopeConfig.isProtectedPlayer(actor)
                && !RopeConfig.isProtectedPlayer(target)
                && !isInSpawnProtection(actor)
                && !isInSpawnProtection(target)
                && !ropeManager.hasLink(actor.getUuid())
                && !ropeManager.hasLink(target.getUuid())
                && canMaintain(actor, target.getEyePos());
    }

    private boolean validateAnchor(ServerPlayerEntity actor, PendingAction action) {
        if (!actor.getEntityWorld().getRegistryKey().equals(action.worldKey)
                || !RopeConfig.isAnchorBlock(actor.getEntityWorld(), action.anchorPos)
                || !canMaintain(actor, action.anchor())) {
            return false;
        }

        Optional<RopeLink> link = ropeManager.findForPlayer(actor.getUuid());
        if (link.isEmpty() || !link.get().id().equals(action.linkId)) {
            return false;
        }

        RopeEndpoint carried = link.get().otherEndpoint(actor.getUuid());
        return actor.getUuid().equals(link.get().controllerUuid())
                && carried != null
                && carried.type() == RopeEndpoint.Type.PLAYER;
    }

    private boolean validateReleasePlayer(MinecraftServer server, ServerPlayerEntity actor, PendingAction action) {
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(action.targetUuid);
        if (target == null || !isUsable(target) || !sameWorld(actor, target) || !canMaintain(actor, target.getEyePos())) {
            return false;
        }

        Optional<RopeLink> link = ropeManager.findForPlayer(target.getUuid());
        return link.isPresent()
                && link.get().id().equals(action.linkId)
                && releasePlan(actor, link.get()).allowed();
    }

    private boolean validateReleaseAnchor(ServerPlayerEntity actor, PendingAction action) {
        if (!actor.getEntityWorld().getRegistryKey().equals(action.worldKey)
                || !canMaintain(actor, action.anchor())) {
            return false;
        }

        Optional<RopeLink> link = ropeManager.findForAnchor(action.worldKey, action.anchor());
        return link.isPresent()
                && link.get().id().equals(action.linkId)
                && releasePlan(actor, link.get()).allowed();
    }

    private boolean validateSelfEscape(ServerPlayerEntity actor, PendingAction action) {
        Optional<RopeLink> link = ropeManager.findForPlayer(actor.getUuid());
        if (link.isEmpty() || !link.get().id().equals(action.linkId)) {
            return false;
        }
        if (actor.getEyePos().squaredDistanceTo(action.startPosition) > SELF_ESCAPE_MAX_MOVEMENT_SQUARED) {
            return false;
        }
        if (nearController(actor, link.get())) {
            return false;
        }
        return !RopeConfig.selfEscapeCancelWhenTaut() || !link.get().isTaut();
    }

    private void complete(MinecraftServer server, ServerPlayerEntity actor, PendingAction action) {
        switch (action.type) {
            case BIND_PLAYER -> completeBind(server, actor, action);
            case ANCHOR_PLAYER -> completeAnchor(actor, action);
            case RELEASE_PLAYER, RELEASE_ANCHOR -> completeRelease(actor, action);
            case SELF_ESCAPE -> completeSelfEscape(actor, action);
        }
    }

    private void completeBind(MinecraftServer server, ServerPlayerEntity actor, PendingAction action) {
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(action.targetUuid);
        if (target == null) {
            actor.sendMessage(Text.literal("Rope action canceled."), true);
            return;
        }

        RopeManager.AddResult result = ropeManager.addPlayerLink(
                actor,
                target,
                RopeConfig.defaultPlayerRopeLength(),
                true);
        if (result != RopeManager.AddResult.ADDED) {
            actor.sendMessage(messageFor(result), true);
            return;
        }

        consumeLead(actor);
        feedback(actor, Feedback.SUCCESS);
        feedback(target, Feedback.NOTICE);
        log("Lead bind completed: {} tied {}", actor.getName().getString(), target.getName().getString());
        actor.sendMessage(Text.literal("Rope secured: " + target.getName().getString() + "."), true);
        target.sendMessage(Text.literal("You have been tied by " + actor.getName().getString() + "."), true);
    }

    private void completeAnchor(ServerPlayerEntity actor, PendingAction action) {
        RopeManager.AddResult result = ropeManager.anchorCarriedPlayer(actor, action.anchor(), action.linkId);
        if (result != RopeManager.AddResult.ADDED) {
            actor.sendMessage(messageFor(result), true);
            return;
        }

        feedback(actor, Feedback.SUCCESS);
        log("Anchor bind completed by {}", actor.getName().getString());
        actor.sendMessage(Text.literal("Tied the rope to the anchor."), true);
    }

    private void completeRelease(ServerPlayerEntity actor, PendingAction action) {
        Optional<RopeLink> removed = ropeManager.removeById(action.linkId);
        if (removed.isEmpty()) {
            actor.sendMessage(Text.literal("Rope action canceled."), true);
            return;
        }

        refundLead(actor, removed.get());
        feedback(actor, Feedback.SUCCESS);
        notifyReleasedEndpoint(actor.getEntityWorld().getServer(), actor, removed.get(), action.thirdPartyRelease);
        log("{} released rope {}", actor.getName().getString(), removed.get().id());
        String message = action.thirdPartyRelease
                ? "Released the rope after a careful rescue."
                : "Released the rope.";
        actor.sendMessage(Text.literal(message), true);
    }

    private void completeSelfEscape(ServerPlayerEntity actor, PendingAction action) {
        Optional<RopeLink> link = ropeManager.findForPlayer(actor.getUuid());
        if (link.isEmpty() || !link.get().id().equals(action.linkId)) {
            actor.sendMessage(Text.literal("Escape attempt canceled."), true);
            return;
        }

        applySelfEscapeCooldown(actor.getUuid());
        int denominator = RopeConfig.selfEscapeSuccessDenominator();
        if (actor.getRandom().nextInt(denominator) != 0) {
            feedback(actor, Feedback.FAIL);
            log("Self escape failed for {} on rope {}", actor.getName().getString(), action.linkId);
            actor.sendMessage(Text.literal("You failed to loosen the rope."), true);
            return;
        }

        Optional<RopeLink> removed = ropeManager.removeById(action.linkId);
        if (removed.isEmpty()) {
            actor.sendMessage(Text.literal("Escape attempt canceled."), true);
            return;
        }

        feedback(actor, Feedback.SUCCESS);
        log("Self escape succeeded for {} on rope {}", actor.getName().getString(), action.linkId);
        actor.sendMessage(Text.literal("You slipped free from the rope."), true);
    }

    private ReleasePlan releasePlan(ServerPlayerEntity actor, RopeLink link) {
        boolean controller = actor.getUuid().equals(link.controllerUuid());
        if (controller) {
            if (ropeManager.hasLink(actor.getUuid()) && !link.includesPlayer(actor.getUuid())) {
                return ReleasePlan.denied("You are tied and cannot release another rope.");
            }
            return ReleasePlan.allowed(RopeConfig.controllerReleaseDurationTicks(), false);
        }

        if (!RopeConfig.enableThirdPartyRelease()) {
            return ReleasePlan.denied("Only the player who tied this rope can release it.");
        }
        if (ropeManager.hasLink(actor.getUuid())) {
            return ReleasePlan.denied("You are tied and cannot release another rope.");
        }

        return ReleasePlan.allowed(RopeConfig.thirdPartyReleaseDurationTicks(), true);
    }

    private boolean withinStartDistance(ServerPlayerEntity actor, Vec3d target) {
        return actor.getEyePos().squaredDistanceTo(target) <= square(RopeConfig.maxStartDistance());
    }

    private static boolean isInSpawnProtection(ServerPlayerEntity player) {
        double radius = RopeConfig.spawnProtectionRadius();
        if (radius <= 0.0D) {
            return false;
        }

        ServerWorld world = player.getEntityWorld();
        Vec3d spawn = Vec3d.ofCenter(world.getSpawnPoint().getPos());
        return player.getEntityPos().squaredDistanceTo(spawn) <= square(radius);
    }

    private boolean canMaintain(ServerPlayerEntity actor, Vec3d target) {
        return actor.getEyePos().squaredDistanceTo(target) <= square(RopeConfig.maxMaintainDistance())
                && isLookingAt(actor, target);
    }

    private boolean isLookingAt(ServerPlayerEntity actor, Vec3d target) {
        Vec3d offset = target.subtract(actor.getEyePos());
        if (offset.lengthSquared() < 0.01D) {
            return true;
        }
        return actor.getRotationVec(1.0F).normalize().dotProduct(offset.normalize()) >= LOOK_DOT_THRESHOLD;
    }

    private void sendProgress(ServerPlayerEntity actor, PendingAction action) {
        int remainingTicks = Math.max(0, action.durationTicks - action.elapsedTicks);
        int remainingSeconds = Math.max(1, (int) Math.ceil(remainingTicks / 20.0D));
        actor.sendMessage(Text.literal(action.progressVerb() + "... " + remainingSeconds + "s"), true);
    }

    private void tickCooldowns() {
        Iterator<Map.Entry<UUID, Integer>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }

        Iterator<Map.Entry<UUID, Integer>> selfIterator = selfEscapeCooldowns.entrySet().iterator();
        while (selfIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = selfIterator.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                selfIterator.remove();
            } else {
                entry.setValue(next);
            }
        }
    }

    private void applyCooldown(UUID playerUuid) {
        int cooldown = RopeConfig.interactionCooldownTicks();
        if (cooldown > 0) {
            cooldowns.put(playerUuid, cooldown);
        }
    }

    private void applyActionCooldown(PendingAction action) {
        applyCooldown(action.actorUuid);
        if (action.type == ActionType.SELF_ESCAPE) {
            applySelfEscapeCooldown(action.actorUuid);
        }
    }

    private void applySelfEscapeCooldown(UUID playerUuid) {
        int cooldown = RopeConfig.selfEscapeCooldownTicks();
        if (cooldown > 0) {
            selfEscapeCooldowns.put(playerUuid, cooldown);
        }
    }

    private boolean nearController(ServerPlayerEntity actor, RopeLink link) {
        ServerPlayerEntity controller = actor.getEntityWorld().getServer()
                .getPlayerManager()
                .getPlayer(link.controllerUuid());
        if (controller == null || !sameWorld(actor, controller)) {
            return false;
        }

        double guardRadius = RopeConfig.selfEscapeGuardRadius();
        return guardRadius > 0.0D && actor.getEyePos().squaredDistanceTo(controller.getEyePos()) <= square(guardRadius);
    }

    private static boolean matchesRequiredHand(ServerPlayerEntity player, RequiredHand requiredHand) {
        return switch (requiredHand) {
            case LEAD -> isLeadInMainHand(player);
            case EMPTY -> player.getStackInHand(Hand.MAIN_HAND).isEmpty();
        };
    }

    private static void consumeLead(ServerPlayerEntity player) {
        if (!player.isCreative()) {
            player.getStackInHand(Hand.MAIN_HAND).decrement(1);
        }
    }

    private static void refundLead(ServerPlayerEntity player, RopeLink link) {
        if (!link.refundLeadOnManualRelease() || player.isCreative()) {
            return;
        }

        ItemStack lead = new ItemStack(Items.LEAD);
        if (!player.giveItemStack(lead) && !lead.isEmpty()) {
            player.dropItem(lead, false);
        }
    }

    private static boolean isLeadInMainHand(ServerPlayerEntity player) {
        return player.getStackInHand(Hand.MAIN_HAND).isOf(Items.LEAD);
    }

    private static boolean isUsable(ServerPlayerEntity player) {
        return !player.isRemoved() && player.isAlive() && !player.isSpectator();
    }

    private static boolean sameWorld(ServerPlayerEntity first, ServerPlayerEntity second) {
        return first.getEntityWorld().getRegistryKey().equals(second.getEntityWorld().getRegistryKey());
    }

    private static double square(double value) {
        return value * value;
    }

    private static ActionResult fail(ServerPlayerEntity actor, String message) {
        actor.sendMessage(Text.literal(message), true);
        return ActionResult.FAIL;
    }

    private static void feedback(ServerPlayerEntity player, Feedback feedback) {
        if (!RopeConfig.enableActionFeedbackEffects()) {
            return;
        }

        ServerWorld world = player.getEntityWorld();
        Vec3d position = player.getEyePos().add(0.0D, 0.15D, 0.0D);
        switch (feedback) {
            case SUCCESS -> world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    position.x,
                    position.y,
                    position.z,
                    3,
                    0.15D,
                    0.1D,
                    0.15D,
                    0.01D);
            case FAIL -> world.spawnParticles(
                    ParticleTypes.SMOKE,
                    position.x,
                    position.y,
                    position.z,
                    2,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.005D);
            case NOTICE -> world.spawnParticles(
                    ParticleTypes.CRIT,
                    position.x,
                    position.y,
                    position.z,
                    2,
                    0.08D,
                    0.08D,
                    0.08D,
                    0.005D);
        }
    }

    private static void notifyReleasedEndpoint(
            MinecraftServer server,
            ServerPlayerEntity actor,
            RopeLink link,
            boolean thirdPartyRelease) {
        RopeEndpoint target = link.otherEndpoint(actor.getUuid());
        if (target == null || target.type() != RopeEndpoint.Type.PLAYER) {
            target = link.first().type() == RopeEndpoint.Type.PLAYER ? link.first() : link.second();
        }
        if (target == null || target.type() != RopeEndpoint.Type.PLAYER) {
            return;
        }

        RopeEndpoint endpoint = target;
        endpoint.resolvePlayer(server).ifPresent(player -> {
            if (player.getUuid().equals(actor.getUuid())) {
                return;
            }
            String message = thirdPartyRelease
                    ? actor.getName().getString() + " rescued you from the rope."
                    : "The rope was released.";
            player.sendMessage(Text.literal(message), true);
        });
    }

    private static void log(String message, Object... args) {
        if (RopeConfig.logRopeEvents()) {
            McElmaRopeMod.LOGGER.info(message, args);
        }
    }

    private static Text messageFor(RopeManager.AddResult result) {
        return switch (result) {
            case FULL -> Text.literal("Rope limit reached.");
            case SAME_PLAYER -> Text.literal("Cannot tie yourself.");
            case DIFFERENT_WORLD -> Text.literal("Players must be in the same world.");
            case FIRST_ALREADY_LINKED -> Text.literal("You are already tied.");
            case SECOND_ALREADY_LINKED -> Text.literal("That player is already tied.");
            case NO_LINK -> Text.literal("No rope link found.");
            case NOT_CONTROLLER -> Text.literal("Only the player leading this rope can do that.");
            case NO_CARRIED_PLAYER -> Text.literal("No carried player found.");
            case PROTECTED_PLAYER -> Text.literal("That player is protected from rope binding.");
            case ADDED -> Text.literal("Rope added.");
        };
    }

    private enum Feedback {
        SUCCESS,
        FAIL,
        NOTICE
    }

    public enum RequiredHand {
        LEAD,
        EMPTY
    }

    private enum ActionType {
        BIND_PLAYER,
        ANCHOR_PLAYER,
        RELEASE_PLAYER,
        RELEASE_ANCHOR,
        SELF_ESCAPE
    }

    private record ReleasePlan(boolean allowed, int durationTicks, boolean thirdParty, String message) {
        static ReleasePlan allowed(int durationTicks, boolean thirdParty) {
            return new ReleasePlan(true, durationTicks, thirdParty, "");
        }

        static ReleasePlan denied(String message) {
            return new ReleasePlan(false, 0, false, message);
        }

        String startMessage(String targetName) {
            String prefix = thirdParty ? "Rescuing " : "Releasing ";
            return prefix + targetName + ". Keep looking.";
        }
    }

    private static final class PendingAction {
        private final ActionType type;
        private final UUID actorUuid;
        private final UUID targetUuid;
        private final UUID linkId;
        private final RegistryKey<World> worldKey;
        private final BlockPos anchorPos;
        private final Vec3d startPosition;
        private final int durationTicks;
        private final RequiredHand requiredHand;
        private final boolean thirdPartyRelease;
        private int elapsedTicks;

        private PendingAction(
                ActionType type,
                UUID actorUuid,
                UUID targetUuid,
                UUID linkId,
                RegistryKey<World> worldKey,
                BlockPos anchorPos,
                Vec3d startPosition,
                int durationTicks,
                RequiredHand requiredHand,
                boolean thirdPartyRelease) {
            this.type = type;
            this.actorUuid = actorUuid;
            this.targetUuid = targetUuid;
            this.linkId = linkId;
            this.worldKey = worldKey;
            this.anchorPos = anchorPos;
            this.startPosition = startPosition;
            this.durationTicks = durationTicks;
            this.requiredHand = requiredHand;
            this.thirdPartyRelease = thirdPartyRelease;
        }

        static PendingAction player(
                ActionType type,
                ServerPlayerEntity actor,
                ServerPlayerEntity target,
                UUID linkId,
                int durationTicks,
                RequiredHand requiredHand,
                boolean thirdPartyRelease) {
            return new PendingAction(
                    type,
                    actor.getUuid(),
                    target.getUuid(),
                    linkId,
                    null,
                    null,
                    actor.getEyePos(),
                    durationTicks,
                    requiredHand,
                    thirdPartyRelease);
        }

        static PendingAction anchor(
                ActionType type,
                ServerPlayerEntity actor,
                UUID linkId,
                RegistryKey<World> worldKey,
                BlockPos anchorPos,
                int durationTicks,
                RequiredHand requiredHand,
                boolean thirdPartyRelease) {
            return new PendingAction(
                    type,
                    actor.getUuid(),
                    null,
                    linkId,
                    worldKey,
                    anchorPos,
                    actor.getEyePos(),
                    durationTicks,
                    requiredHand,
                    thirdPartyRelease);
        }

        static PendingAction selfEscape(ServerPlayerEntity actor, UUID linkId, int durationTicks) {
            return new PendingAction(
                    ActionType.SELF_ESCAPE,
                    actor.getUuid(),
                    null,
                    linkId,
                    null,
                    null,
                    actor.getEyePos(),
                    durationTicks,
                    RequiredHand.EMPTY,
                    false);
        }

        Vec3d anchor() {
            return Vec3d.ofCenter(anchorPos);
        }

        String progressVerb() {
            return switch (type) {
                case BIND_PLAYER -> "Tying";
                case ANCHOR_PLAYER -> "Tying anchor";
                case RELEASE_PLAYER, RELEASE_ANCHOR -> thirdPartyRelease ? "Rescuing" : "Releasing";
                case SELF_ESCAPE -> "Escaping";
            };
        }
    }
}
