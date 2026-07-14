package com.mcelma.rope;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class RopeCommands {
    private RopeCommands() {
    }

    public static void register(RopeManager ropeManager) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(root(ropeManager));
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> root(RopeManager ropeManager) {
        return literal("mcelmarope")
                .requires(source -> source.hasPermissionLevel(RopeConfig.commandPermissionLevel()))
                .then(literal("anchor")
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("x", DoubleArgumentType.doubleArg())
                                        .then(argument("y", DoubleArgumentType.doubleArg())
                                                .then(argument("z", DoubleArgumentType.doubleArg())
                                                        .then(argument("length", DoubleArgumentType.doubleArg(1.0D))
                                                                .executes(context -> anchor(ropeManager, context.getSource(),
                                                                        EntityArgumentType.getPlayer(context, "player"),
                                                                        DoubleArgumentType.getDouble(context, "x"),
                                                                        DoubleArgumentType.getDouble(context, "y"),
                                                                        DoubleArgumentType.getDouble(context, "z"),
                                                                        DoubleArgumentType.getDouble(context, "length")))))))))
                .then(literal("bind")
                        .then(argument("playerA", EntityArgumentType.player())
                                .then(argument("playerB", EntityArgumentType.player())
                                        .then(argument("length", DoubleArgumentType.doubleArg(1.0D))
                                                .executes(context -> bind(ropeManager, context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "playerA"),
                                                        EntityArgumentType.getPlayer(context, "playerB"),
                                                        DoubleArgumentType.getDouble(context, "length")))))))
                .then(literal("clear")
                        .then(literal("all")
                                .executes(context -> clearAll(ropeManager, context.getSource())))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> clearPlayer(ropeManager, context.getSource(),
                                        EntityArgumentType.getPlayer(context, "player")))))
                .then(literal("clearall")
                        .executes(context -> clearAll(ropeManager, context.getSource())))
                .then(literal("status")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> status(ropeManager, context.getSource(),
                                        EntityArgumentType.getPlayer(context, "player")))))
                .then(literal("inspect")
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> inspect(ropeManager, context.getSource(),
                                        EntityArgumentType.getPlayer(context, "player")))))
                .then(literal("list")
                        .executes(context -> list(ropeManager, context.getSource())))
                .then(literal("config")
                        .executes(context -> config(context.getSource())))
                .then(literal("reload")
                        .executes(context -> reloadConfig(context.getSource())));
    }

    private static int anchor(
            RopeManager ropeManager,
            ServerCommandSource source,
            ServerPlayerEntity player,
            double x,
            double y,
            double z,
            double length) {
        RopeManager.AddResult result = ropeManager.addAnchor(player, new Vec3d(x, y, z), length);
        if (result != RopeManager.AddResult.ADDED) {
            return reportAddFailure(source, result);
        }

        double clampedLength = RopeConfig.clampLength(length);
        source.sendFeedback(() -> Text.literal("Added anchor rope for " + player.getName().getString()
                + " with length " + clampedLength + "."), true);
        return 1;
    }

    private static int bind(
            RopeManager ropeManager,
            ServerCommandSource source,
            ServerPlayerEntity first,
            ServerPlayerEntity second,
            double length) {
        RopeManager.AddResult result = ropeManager.addPlayerLink(first, second, length);
        if (result != RopeManager.AddResult.ADDED) {
            return reportAddFailure(source, result);
        }

        double clampedLength = RopeConfig.clampLength(length);
        source.sendFeedback(() -> Text.literal("Bound " + first.getName().getString() + " and "
                + second.getName().getString() + " with length " + clampedLength + "."), true);
        return 1;
    }

    private static int clearAll(RopeManager ropeManager, ServerCommandSource source) {
        int removed = ropeManager.clearAll() + RopeAnchoredOfflinePersistence.clearAll(source.getServer());
        source.sendFeedback(() -> Text.literal("Cleared " + removed + " rope link(s)."), true);
        return removed;
    }

    private static int clearPlayer(RopeManager ropeManager, ServerCommandSource source, ServerPlayerEntity player) {
        int removed = ropeManager.removeForPlayer(player.getUuid())
                + RopeAnchoredOfflinePersistence.clearForPlayer(source.getServer(), player.getUuid());
        source.sendFeedback(() -> Text.literal("Cleared " + removed + " rope link(s) for "
                + player.getName().getString() + "."), true);
        return removed;
    }

    private static int status(RopeManager ropeManager, ServerCommandSource source, ServerPlayerEntity player) {
        MinecraftServer server = source.getServer();
        var endpointLink = ropeManager.findForPlayer(player.getUuid());
        var controlledLink = ropeManager.findControlledBy(player.getUuid());

        if (endpointLink.isEmpty() && controlledLink.isEmpty()) {
            source.sendFeedback(() -> Text.literal(player.getName().getString() + " has no rope state."), false);
            return 0;
        }

        endpointLink.ifPresent(link -> source.sendFeedback(() -> Text.literal(
                player.getName().getString() + " is tied in " + describeLink(link, server)), false));
        controlledLink
                .filter(link -> endpointLink.isEmpty() || !link.id().equals(endpointLink.get().id()))
                .ifPresent(link -> source.sendFeedback(() -> Text.literal(
                        player.getName().getString() + " controls " + describeLink(link, server)), false));
        return 1;
    }

    private static int inspect(RopeManager ropeManager, ServerCommandSource source, ServerPlayerEntity player) {
        MinecraftServer server = source.getServer();
        var link = ropeManager.findForPlayer(player.getUuid()).or(() -> ropeManager.findControlledBy(player.getUuid()));
        if (link.isEmpty()) {
            source.sendFeedback(() -> Text.literal(player.getName().getString() + " has no rope state."), false);
            return 0;
        }

        RopeLink current = link.get();
        source.sendFeedback(() -> Text.literal("Inspecting " + player.getName().getString() + ": "
                + describeLink(current, server)), false);
        source.sendFeedback(() -> Text.literal("age=" + formatSeconds(current.ageTicks()) + "s"
                + ", taut=" + current.isTaut()
                + ", refundLead=" + current.refundLeadOnManualRelease()
                + ", maxHeld=" + formatOptionalTicks(RopeConfig.maxHeldDurationTicks())), false);
        return 1;
    }

    private static int list(RopeManager ropeManager, ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        var links = ropeManager.links();
        source.sendFeedback(() -> Text.literal("Active rope links: " + links.size()), false);

        int shown = Math.min(10, links.size());
        for (int index = 0; index < shown; index++) {
            RopeLink link = links.get(index);
            int number = index + 1;
            source.sendFeedback(() -> Text.literal(number + ". " + describeLink(link, server)), false);
        }
        if (links.size() > shown) {
            int remaining = links.size() - shown;
            source.sendFeedback(() -> Text.literal("... and " + remaining + " more."), false);
        }
        return links.size();
    }

    private static int reloadConfig(ServerCommandSource source) {
        RopeConfig.load();
        return config(source, "Reloaded MC-ELMA Rope config");
    }

    private static int config(ServerCommandSource source) {
        return config(source, "MC-ELMA Rope config");
    }

    private static int config(ServerCommandSource source, String prefix) {
        source.sendFeedback(() -> Text.literal(prefix + ": "
                + RopeConfig.anchorBlockIdCount() + " anchor id(s), "
                + RopeConfig.anchorBlockTagCount() + " anchor tag(s), bind "
                + formatSeconds(RopeConfig.bindDurationTicks()) + "s, controller release "
                + formatSeconds(RopeConfig.controllerReleaseDurationTicks()) + "s, third-party release "
                + formatSeconds(RopeConfig.thirdPartyReleaseDurationTicks()) + "s, self escape "
                + formatSeconds(RopeConfig.selfEscapeDurationTicks()) + "s at 1/"
                + RopeConfig.selfEscapeSuccessDenominator() + ", holder damage drop 1/"
                + RopeConfig.holderDamageDropDenominator() + ", rope pull "
                + RopeConfig.ropeMaxPullSpeed() + "b/t, physics="
                + RopeConfig.ropePhysicsPreset() + "."), true);
        source.sendFeedback(() -> Text.literal(prefix + ": escape guard radius="
                + RopeConfig.selfEscapeGuardRadius()
                + ", guard progress x" + RopeConfig.selfEscapeGuardProgressMultiplier()
                + ", taut cancel=" + RopeConfig.selfEscapeCancelWhenTaut()
                + ", taut progress x" + RopeConfig.selfEscapeTautProgressMultiplier()
                + ", holder damage allow/deny="
                + RopeConfig.holderDamageDropAllowedDamageTypeCount()
                + "/" + RopeConfig.holderDamageDropDeniedDamageTypeCount() + "."), false);
        source.sendFeedback(() -> Text.literal(prefix + ": disconnect refund="
                + RopeConfig.refundLeadToControllerOnTargetDisconnect()
                + ", disconnect penalty=" + RopeConfig.enableDisconnectPenalty()
                + ", penalty duration=" + formatOptionalTicks(RopeConfig.disconnectPenaltyDurationTicks())
                + ", fatigue/slowness="
                + RopeConfig.disconnectPenaltyMiningFatigueLevel()
                + "/" + RopeConfig.disconnectPenaltySlownessLevel()
                + ", persist penalties=" + RopeConfig.persistDisconnectPenalties()
                + ", persist anchored disconnects="
                + RopeConfig.persistAnchoredRopesOnDisconnect() + "."), false);
        source.sendFeedback(() -> Text.literal(prefix + ": permission=" + RopeConfig.commandPermissionLevel()
                + ", maxLinks=" + RopeConfig.maxActiveLinks()
                + ", maxHeld=" + formatOptionalTicks(RopeConfig.maxHeldDurationTicks())
                + ", spawnProtection=" + RopeConfig.spawnProtectionRadius()
                + ", visual=" + RopeConfig.ropeVisualEnabled()
                + "/" + RopeConfig.ropeVisualStyle()
                + "/" + RopeConfig.ropeVisualWidthPreset()
                + "/" + RopeConfig.ropeVisualSegments()
                + ", particles=" + RopeConfig.enableActionFeedbackEffects()
                + ", sounds=" + RopeConfig.enableActionFeedbackSounds()
                + ", logs=" + RopeConfig.logRopeEvents()
                + ", persistRopes=" + RopeConfig.persistRopes() + "."), false);
        return 1;
    }

    private static String describeLink(RopeLink link, MinecraftServer server) {
        return "link " + link.id().toString().substring(0, 8)
                + " [" + describeEndpoint(link.first(), server)
                + " <-> " + describeEndpoint(link.second(), server)
                + "], controller=" + describePlayer(link.controllerUuid(), server)
                + ", length=" + link.length();
    }

    private static String describeEndpoint(RopeEndpoint endpoint, MinecraftServer server) {
        if (endpoint.type() == RopeEndpoint.Type.PLAYER) {
            return describePlayer(endpoint.playerUuid(), server);
        }

        Vec3d position = endpoint.anchorPosition();
        return "anchor@" + endpoint.worldKey().getValue()
                + " " + formatCoordinate(position.x)
                + " " + formatCoordinate(position.y)
                + " " + formatCoordinate(position.z);
    }

    private static String describePlayer(java.util.UUID playerUuid, MinecraftServer server) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        return player == null ? playerUuid.toString().substring(0, 8) : player.getName().getString();
    }

    private static String formatCoordinate(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String formatSeconds(int ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    private static String formatOptionalTicks(int ticks) {
        return ticks <= 0 ? "off" : formatSeconds(ticks) + "s";
    }

    private static int reportAddFailure(ServerCommandSource source, RopeManager.AddResult result) {
        Text message = switch (result) {
            case FULL -> Text.literal("Rope limit reached.");
            case SAME_PLAYER -> Text.literal("Cannot bind a player to themselves.");
            case DIFFERENT_WORLD -> Text.literal("Players must be in the same world.");
            case FIRST_ALREADY_LINKED -> Text.literal("The first player is already tied.");
            case SECOND_ALREADY_LINKED -> Text.literal("The second player is already tied.");
            case NO_LINK -> Text.literal("No rope link found.");
            case NOT_CONTROLLER -> Text.literal("Only the player leading this rope can do that.");
            case NO_CARRIED_PLAYER -> Text.literal("No carried player found.");
            case PROTECTED_PLAYER -> Text.literal("That player is protected from rope binding.");
            case ADDED -> Text.literal("Rope added.");
        };
        source.sendError(message);
        return 0;
    }
}
