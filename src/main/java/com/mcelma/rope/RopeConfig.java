package com.mcelma.rope;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class RopeConfig {
    private static final Logger LOGGER = McElmaRopeMod.LOGGER;
    private static final double MIN_LENGTH = 1.0D;
    private static final double FALLBACK_MAX_LENGTH = 48.0D;
    private static final double FALLBACK_PLAYER_ROPE_LENGTH = 12.0D;
    private static final double FALLBACK_ANCHOR_ROPE_LENGTH = 12.0D;
    private static final double FALLBACK_MAX_START_DISTANCE = 6.0D;
    private static final double FALLBACK_MAX_MAINTAIN_DISTANCE = 8.0D;
    private static final int FALLBACK_BIND_DURATION_TICKS = 60;
    private static final int FALLBACK_CONTROLLER_RELEASE_DURATION_TICKS = 60;
    private static final int FALLBACK_THIRD_PARTY_RELEASE_DURATION_TICKS = 240;
    private static final int FALLBACK_INTERACTION_COOLDOWN_TICKS = 10;
    private static final int FALLBACK_SELF_ESCAPE_DURATION_TICKS = 900;
    private static final int FALLBACK_SELF_ESCAPE_COOLDOWN_TICKS = 1200;
    private static final int FALLBACK_SELF_ESCAPE_SUCCESS_DENOMINATOR = 300;
    private static final double FALLBACK_SELF_ESCAPE_GUARD_RADIUS = 12.0D;
    private static final double FALLBACK_SELF_ESCAPE_GUARD_PROGRESS_MULTIPLIER = 0.0D;
    private static final double FALLBACK_SELF_ESCAPE_TAUT_PROGRESS_MULTIPLIER = 0.15D;
    private static final int FALLBACK_HOLDER_DAMAGE_DROP_DENOMINATOR = 100;
    private static final int FALLBACK_DISCONNECT_PENALTY_DURATION_TICKS = 2400;
    private static final double FALLBACK_ROPE_CORRECTION_RATE = 0.35D;
    private static final double FALLBACK_ROPE_MAX_PULL_SPEED = 0.45D;
    private static final double FALLBACK_ROPE_EMERGENCY_STRETCH_MULTIPLIER = 2.5D;
    private static final double FALLBACK_ROPE_EMERGENCY_MAX_PULL_SPEED = 0.8D;
    private static final double FALLBACK_ROPE_SWING_DAMPING = 0.985D;
    private static final String FALLBACK_ROPE_PHYSICS_PRESET = "custom";
    private static final int FALLBACK_ROPE_VISUAL_SEGMENTS = 20;
    private static final double FALLBACK_ROPE_VISUAL_SAG = 0.045D;
    private static final String FALLBACK_ROPE_VISUAL_WIDTH_PRESET = "balanced";
    private static final double TAUT_TOLERANCE = 0.03D;
    private static final int COMMAND_PERMISSION_LEVEL = 2;
    private static final int MAX_ACTIVE_LINKS = 256;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> DEFAULT_ANCHOR_BLOCKS = List.of(
            "#minecraft:fences",
            "#minecraft:fence_gates",
            "#minecraft:walls",
            "minecraft:iron_bars",
            "minecraft:chain",
            "minecraft:lightning_rod",
            "minecraft:end_rod",
            "minecraft:bell");

    private static ConfigData data = ConfigData.defaults();
    private static Set<Identifier> anchorBlocks = parseAnchorBlockIds(data.anchorBlockIds);
    private static Set<TagKey<Block>> anchorBlockTags = parseAnchorBlockTags(data.anchorBlockIds);

    private RopeConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("mc_elma_rope.json");
        if (Files.notExists(path)) {
            saveDefault(path);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            data = loaded == null ? ConfigData.defaults() : loaded.normalized();
        } catch (IOException | RuntimeException ignored) {
            LOGGER.warn("Failed to load MC-ELMA Rope config at {}. Using in-memory defaults.", path);
            data = ConfigData.defaults();
        }

        anchorBlocks = parseAnchorBlockIds(data.anchorBlockIds);
        anchorBlockTags = parseAnchorBlockTags(data.anchorBlockIds);
    }

    public static double clampLength(double length) {
        return Math.max(MIN_LENGTH, Math.min(maxRopeLength(), length));
    }

    public static double defaultPlayerRopeLength() {
        return clampLength(data.defaultPlayerRopeLength);
    }

    public static double anchorRopeLength() {
        return clampLength(data.anchorRopeLength);
    }

    public static double maxRopeLength() {
        return Math.max(MIN_LENGTH, data.maxRopeLength);
    }

    public static double tautTolerance() {
        return TAUT_TOLERANCE;
    }

    public static int commandPermissionLevel() {
        return Math.max(0, data.commandPermissionLevel);
    }

    public static int maxActiveLinks() {
        return Math.max(1, data.maxActiveLinks);
    }

    public static int bindDurationTicks() {
        return Math.max(1, data.bindDurationTicks);
    }

    public static int controllerReleaseDurationTicks() {
        return Math.max(1, data.controllerReleaseDurationTicks);
    }

    public static int thirdPartyReleaseDurationTicks() {
        return Math.max(1, data.thirdPartyReleaseDurationTicks);
    }

    public static boolean enableThirdPartyRelease() {
        return data.enableThirdPartyRelease;
    }

    public static boolean allowThirdPartyRelease() {
        return data.enableThirdPartyRelease;
    }

    public static double maxStartDistance() {
        return Math.max(MIN_LENGTH, data.maxStartDistance);
    }

    public static double maxMaintainDistance() {
        return Math.max(MIN_LENGTH, data.maxMaintainDistance);
    }

    public static int interactionCooldownTicks() {
        return Math.max(0, data.interactionCooldownTicks);
    }

    public static boolean enableSelfEscape() {
        return data.enableSelfEscape;
    }

    public static boolean allowSelfEscape() {
        return data.enableSelfEscape;
    }

    public static int selfEscapeDurationTicks() {
        return Math.max(1, data.selfEscapeDurationTicks);
    }

    public static int selfEscapeCooldownTicks() {
        return Math.max(0, data.selfEscapeCooldownTicks);
    }

    public static int selfEscapeSuccessDenominator() {
        return Math.max(1, data.selfEscapeSuccessDenominator);
    }

    public static double selfEscapeGuardRadius() {
        return Math.max(0.0D, data.selfEscapeGuardRadius);
    }

    public static boolean selfEscapeCancelWhenTaut() {
        return data.selfEscapeCancelWhenTaut;
    }

    public static double selfEscapeGuardProgressMultiplier() {
        return clamp(data.selfEscapeGuardProgressMultiplier, 0.0D, 1.0D);
    }

    public static double selfEscapeTautProgressMultiplier() {
        return clamp(data.selfEscapeTautProgressMultiplier, 0.0D, 1.0D);
    }

    public static boolean enableHolderDamageDrop() {
        return data.enableHolderDamageDrop;
    }

    public static boolean allowHolderDamageDrop() {
        return data.enableHolderDamageDrop;
    }

    public static int holderDamageDropDenominator() {
        return Math.max(1, data.holderDamageDropDenominator);
    }

    public static boolean isHolderDamageTypeAllowed(String damageTypeId, String damageName) {
        String normalizedId = normalizeDamageId(damageTypeId);
        String normalizedName = normalizeDamageId(damageName);
        if (matchesDamageEntry(data.holderDamageDropDeniedDamageTypeIds, normalizedId, normalizedName)) {
            return false;
        }
        return data.holderDamageDropAllowedDamageTypeIds.isEmpty()
                || matchesDamageEntry(data.holderDamageDropAllowedDamageTypeIds, normalizedId, normalizedName);
    }

    public static int holderDamageDropAllowedDamageTypeCount() {
        return data.holderDamageDropAllowedDamageTypeIds.size();
    }

    public static int holderDamageDropDeniedDamageTypeCount() {
        return data.holderDamageDropDeniedDamageTypeIds.size();
    }

    public static double ropeCorrectionRate() {
        return physicsPreset().correctionRate(data.ropeCorrectionRate);
    }

    public static double ropeMaxPullSpeed() {
        return physicsPreset().maxPullSpeed(data.ropeMaxPullSpeed);
    }

    public static double ropeEmergencyStretchMultiplier() {
        return physicsPreset().emergencyStretchMultiplier(data.ropeEmergencyStretchMultiplier);
    }

    public static double ropeEmergencyMaxPullSpeed() {
        return physicsPreset().emergencyMaxPullSpeed(data.ropeEmergencyMaxPullSpeed);
    }

    public static double ropeSwingDamping() {
        return physicsPreset().swingDamping(data.ropeSwingDamping);
    }

    public static String ropePhysicsPreset() {
        return data.ropePhysicsPreset;
    }

    public static boolean ropeVisualEnabled() {
        return data.ropeVisualEnabled;
    }

    public static int ropeVisualSegments() {
        return clamp(data.ropeVisualSegments, 4, 64);
    }

    public static double ropeVisualSag() {
        return clamp(data.ropeVisualSag, 0.0D, 0.25D);
    }

    public static String ropeVisualWidthPreset() {
        return data.ropeVisualWidthPreset;
    }

    public static boolean enableActionFeedbackEffects() {
        return data.enableActionFeedbackEffects;
    }

    public static boolean enableActionFeedbackSounds() {
        return data.enableActionFeedbackSounds;
    }

    public static boolean logRopeEvents() {
        return data.logRopeEvents;
    }

    public static int maxHeldDurationTicks() {
        return Math.max(0, data.maxHeldDurationTicks);
    }

    public static double spawnProtectionRadius() {
        return Math.max(0.0D, data.spawnProtectionRadius);
    }

    public static boolean persistRopes() {
        return data.persistRopes;
    }

    public static boolean refundLeadToControllerOnTargetDisconnect() {
        return data.refundLeadToControllerOnTargetDisconnect;
    }

    public static boolean enableDisconnectPenalty() {
        return data.enableDisconnectPenalty;
    }

    public static boolean persistDisconnectPenalties() {
        return data.persistDisconnectPenalties;
    }

    public static boolean disconnectPenaltyOnlyLeadCreatedRopes() {
        return data.disconnectPenaltyOnlyLeadCreatedRopes;
    }

    public static int disconnectPenaltyDurationTicks() {
        return Math.max(0, data.disconnectPenaltyDurationTicks);
    }

    public static int disconnectPenaltyMiningFatigueLevel() {
        return Math.max(0, data.disconnectPenaltyMiningFatigueLevel);
    }

    public static int disconnectPenaltySlownessLevel() {
        return Math.max(0, data.disconnectPenaltySlownessLevel);
    }

    public static boolean disconnectPenaltyShowParticles() {
        return data.disconnectPenaltyShowParticles;
    }

    public static boolean disconnectPenaltyShowIcon() {
        return data.disconnectPenaltyShowIcon;
    }

    public static boolean isProtectedPlayer(ServerPlayerEntity player) {
        String uuid = player.getUuidAsString();
        String name = player.getName().getString();
        return data.protectedPlayerIds.stream().anyMatch(entry -> entry.equalsIgnoreCase(uuid))
                || data.protectedPlayerNames.stream().anyMatch(entry -> entry.equalsIgnoreCase(name));
    }

    public static int anchorBlockIdCount() {
        return anchorBlocks.size();
    }

    public static int anchorBlockTagCount() {
        return anchorBlockTags.size();
    }

    public static boolean isAnchorBlock(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        return anchorBlocks.contains(id) || anchorBlockTags.stream().anyMatch(state::isIn);
    }

    private static void saveDefault(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(ConfigData.defaults(), writer);
            }
        } catch (IOException ignored) {
            LOGGER.warn("Failed to write default MC-ELMA Rope config at {}. Using in-memory defaults.", path);
        }
    }

    private static Set<Identifier> parseAnchorBlockIds(List<String> ids) {
        return ids.stream()
                .filter(id -> id != null)
                .filter(id -> !id.startsWith("#"))
                .map(Identifier::tryParse)
                .filter(id -> id != null)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<TagKey<Block>> parseAnchorBlockTags(List<String> ids) {
        return ids.stream()
                .filter(id -> id != null)
                .filter(id -> id.startsWith("#"))
                .map(id -> Identifier.tryParse(id.substring(1)))
                .filter(id -> id != null)
                .map(id -> TagKey.of(RegistryKeys.BLOCK, id))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static final class ConfigData {
        double maxRopeLength = FALLBACK_MAX_LENGTH;
        double defaultPlayerRopeLength = FALLBACK_PLAYER_ROPE_LENGTH;
        double anchorRopeLength = FALLBACK_ANCHOR_ROPE_LENGTH;
        int bindDurationTicks = FALLBACK_BIND_DURATION_TICKS;
        int controllerReleaseDurationTicks = FALLBACK_CONTROLLER_RELEASE_DURATION_TICKS;
        int thirdPartyReleaseDurationTicks = FALLBACK_THIRD_PARTY_RELEASE_DURATION_TICKS;
        boolean enableThirdPartyRelease = true;
        double maxStartDistance = FALLBACK_MAX_START_DISTANCE;
        double maxMaintainDistance = FALLBACK_MAX_MAINTAIN_DISTANCE;
        int interactionCooldownTicks = FALLBACK_INTERACTION_COOLDOWN_TICKS;
        boolean enableSelfEscape = true;
        int selfEscapeDurationTicks = FALLBACK_SELF_ESCAPE_DURATION_TICKS;
        int selfEscapeCooldownTicks = FALLBACK_SELF_ESCAPE_COOLDOWN_TICKS;
        int selfEscapeSuccessDenominator = FALLBACK_SELF_ESCAPE_SUCCESS_DENOMINATOR;
        double selfEscapeGuardRadius = FALLBACK_SELF_ESCAPE_GUARD_RADIUS;
        boolean selfEscapeCancelWhenTaut = true;
        double selfEscapeGuardProgressMultiplier = FALLBACK_SELF_ESCAPE_GUARD_PROGRESS_MULTIPLIER;
        double selfEscapeTautProgressMultiplier = FALLBACK_SELF_ESCAPE_TAUT_PROGRESS_MULTIPLIER;
        boolean enableHolderDamageDrop = true;
        int holderDamageDropDenominator = FALLBACK_HOLDER_DAMAGE_DROP_DENOMINATOR;
        List<String> holderDamageDropAllowedDamageTypeIds = List.of();
        List<String> holderDamageDropDeniedDamageTypeIds = List.of();
        double ropeCorrectionRate = FALLBACK_ROPE_CORRECTION_RATE;
        double ropeMaxPullSpeed = FALLBACK_ROPE_MAX_PULL_SPEED;
        double ropeEmergencyStretchMultiplier = FALLBACK_ROPE_EMERGENCY_STRETCH_MULTIPLIER;
        double ropeEmergencyMaxPullSpeed = FALLBACK_ROPE_EMERGENCY_MAX_PULL_SPEED;
        double ropeSwingDamping = FALLBACK_ROPE_SWING_DAMPING;
        String ropePhysicsPreset = FALLBACK_ROPE_PHYSICS_PRESET;
        List<String> anchorBlockIds = DEFAULT_ANCHOR_BLOCKS;
        int commandPermissionLevel = COMMAND_PERMISSION_LEVEL;
        int maxActiveLinks = MAX_ACTIVE_LINKS;
        boolean ropeVisualEnabled = true;
        int ropeVisualSegments = FALLBACK_ROPE_VISUAL_SEGMENTS;
        double ropeVisualSag = FALLBACK_ROPE_VISUAL_SAG;
        String ropeVisualWidthPreset = FALLBACK_ROPE_VISUAL_WIDTH_PRESET;
        boolean enableActionFeedbackEffects = true;
        boolean enableActionFeedbackSounds = true;
        boolean logRopeEvents = true;
        int maxHeldDurationTicks = 0;
        double spawnProtectionRadius = 0.0D;
        boolean persistRopes = false;
        boolean refundLeadToControllerOnTargetDisconnect = true;
        boolean enableDisconnectPenalty = true;
        boolean persistDisconnectPenalties = true;
        boolean disconnectPenaltyOnlyLeadCreatedRopes = true;
        int disconnectPenaltyDurationTicks = FALLBACK_DISCONNECT_PENALTY_DURATION_TICKS;
        int disconnectPenaltyMiningFatigueLevel = 1;
        int disconnectPenaltySlownessLevel = 1;
        boolean disconnectPenaltyShowParticles = true;
        boolean disconnectPenaltyShowIcon = true;
        List<String> protectedPlayerNames = List.of();
        List<String> protectedPlayerIds = List.of();

        static ConfigData defaults() {
            return new ConfigData();
        }

        ConfigData normalized() {
            maxRopeLength = sanitize(maxRopeLength, FALLBACK_MAX_LENGTH);
            defaultPlayerRopeLength = sanitize(defaultPlayerRopeLength, FALLBACK_PLAYER_ROPE_LENGTH);
            anchorRopeLength = sanitize(anchorRopeLength, FALLBACK_ANCHOR_ROPE_LENGTH);
            bindDurationTicks = sanitizeTicks(bindDurationTicks, FALLBACK_BIND_DURATION_TICKS);
            controllerReleaseDurationTicks = sanitizeTicks(
                    controllerReleaseDurationTicks,
                    FALLBACK_CONTROLLER_RELEASE_DURATION_TICKS);
            thirdPartyReleaseDurationTicks = sanitizeTicks(
                    thirdPartyReleaseDurationTicks,
                    FALLBACK_THIRD_PARTY_RELEASE_DURATION_TICKS);
            maxStartDistance = sanitize(maxStartDistance, FALLBACK_MAX_START_DISTANCE);
            maxMaintainDistance = sanitize(maxMaintainDistance, FALLBACK_MAX_MAINTAIN_DISTANCE);
            interactionCooldownTicks = Math.max(0, interactionCooldownTicks);
            selfEscapeDurationTicks = sanitizeTicks(selfEscapeDurationTicks, FALLBACK_SELF_ESCAPE_DURATION_TICKS);
            selfEscapeCooldownTicks = Math.max(0, selfEscapeCooldownTicks);
            selfEscapeSuccessDenominator = sanitizeTicks(
                    selfEscapeSuccessDenominator,
                    FALLBACK_SELF_ESCAPE_SUCCESS_DENOMINATOR);
            selfEscapeGuardRadius = sanitizeNonNegative(selfEscapeGuardRadius, FALLBACK_SELF_ESCAPE_GUARD_RADIUS);
            selfEscapeGuardProgressMultiplier = sanitizeRange(
                    selfEscapeGuardProgressMultiplier,
                    0.0D,
                    1.0D,
                    FALLBACK_SELF_ESCAPE_GUARD_PROGRESS_MULTIPLIER);
            selfEscapeTautProgressMultiplier = sanitizeRange(
                    selfEscapeTautProgressMultiplier,
                    0.0D,
                    1.0D,
                    FALLBACK_SELF_ESCAPE_TAUT_PROGRESS_MULTIPLIER);
            holderDamageDropDenominator = sanitizeTicks(
                    holderDamageDropDenominator,
                    FALLBACK_HOLDER_DAMAGE_DROP_DENOMINATOR);
            holderDamageDropAllowedDamageTypeIds = normalizeDamageList(holderDamageDropAllowedDamageTypeIds);
            holderDamageDropDeniedDamageTypeIds = normalizeDamageList(holderDamageDropDeniedDamageTypeIds);
            ropeCorrectionRate = sanitizeNonNegative(ropeCorrectionRate, FALLBACK_ROPE_CORRECTION_RATE);
            ropeMaxPullSpeed = sanitizeNonNegative(ropeMaxPullSpeed, FALLBACK_ROPE_MAX_PULL_SPEED);
            ropeEmergencyStretchMultiplier = sanitize(
                    ropeEmergencyStretchMultiplier,
                    FALLBACK_ROPE_EMERGENCY_STRETCH_MULTIPLIER);
            ropeEmergencyMaxPullSpeed = sanitizeNonNegative(
                    ropeEmergencyMaxPullSpeed,
                    FALLBACK_ROPE_EMERGENCY_MAX_PULL_SPEED);
            ropeSwingDamping = sanitizeRange(ropeSwingDamping, 0.0D, 1.0D, FALLBACK_ROPE_SWING_DAMPING);
            ropePhysicsPreset = normalizePhysicsPreset(ropePhysicsPreset);
            commandPermissionLevel = Math.max(0, commandPermissionLevel);
            maxActiveLinks = Math.max(1, maxActiveLinks);
            ropeVisualSegments = clamp(ropeVisualSegments, 4, 64);
            ropeVisualSag = sanitizeRange(ropeVisualSag, 0.0D, 0.25D, FALLBACK_ROPE_VISUAL_SAG);
            ropeVisualWidthPreset = normalizeWidthPreset(ropeVisualWidthPreset);
            maxHeldDurationTicks = Math.max(0, maxHeldDurationTicks);
            spawnProtectionRadius = sanitizeNonNegative(spawnProtectionRadius, 0.0D);
            disconnectPenaltyDurationTicks = Math.max(0, disconnectPenaltyDurationTicks);
            disconnectPenaltyMiningFatigueLevel = Math.max(0, disconnectPenaltyMiningFatigueLevel);
            disconnectPenaltySlownessLevel = Math.max(0, disconnectPenaltySlownessLevel);
            if (anchorBlockIds == null || anchorBlockIds.isEmpty()) {
                anchorBlockIds = DEFAULT_ANCHOR_BLOCKS;
            }
            if (protectedPlayerNames == null) {
                protectedPlayerNames = List.of();
            }
            if (protectedPlayerIds == null) {
                protectedPlayerIds = List.of();
            }
            return this;
        }

        private static double sanitize(double value, double fallback) {
            if (!Double.isFinite(value) || value < MIN_LENGTH) {
                return fallback;
            }
            return value;
        }

        private static int sanitizeTicks(int value, int fallback) {
            return value < 1 ? fallback : value;
        }

        private static double sanitizeNonNegative(double value, double fallback) {
            if (!Double.isFinite(value) || value < 0.0D) {
                return fallback;
            }
            return value;
        }

        private static double sanitizeRange(double value, double min, double max, double fallback) {
            if (!Double.isFinite(value)) {
                return fallback;
            }
            return clamp(value, min, max);
        }

        private static String normalizeWidthPreset(String value) {
            if (value == null) {
                return FALLBACK_ROPE_VISUAL_WIDTH_PRESET;
            }
            String normalized = value.toLowerCase(java.util.Locale.ROOT);
            return switch (normalized) {
                case "thin", "balanced", "thick" -> normalized;
                default -> FALLBACK_ROPE_VISUAL_WIDTH_PRESET;
            };
        }

        private static String normalizePhysicsPreset(String value) {
            if (value == null) {
                return FALLBACK_ROPE_PHYSICS_PRESET;
            }
            String normalized = value.toLowerCase(java.util.Locale.ROOT);
            return switch (normalized) {
                case "custom", "soft", "balanced", "strict" -> normalized;
                default -> FALLBACK_ROPE_PHYSICS_PRESET;
            };
        }

        private static List<String> normalizeDamageList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            return values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(RopeConfig::normalizeDamageId)
                    .distinct()
                    .toList();
        }
    }

    private static boolean matchesDamageEntry(List<String> entries, String normalizedId, String normalizedName) {
        return entries.stream().anyMatch(entry -> entry.equals(normalizedId)
                || entry.equals(normalizedName)
                || normalizedId.endsWith(":" + entry)
                || normalizedName.endsWith(":" + entry));
    }

    private static String normalizeDamageId(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static PhysicsPreset physicsPreset() {
        return switch (data.ropePhysicsPreset) {
            case "soft" -> new PhysicsPreset(0.22D, 0.32D, 2.8D, 0.62D, 0.992D);
            case "balanced" -> new PhysicsPreset(
                    FALLBACK_ROPE_CORRECTION_RATE,
                    FALLBACK_ROPE_MAX_PULL_SPEED,
                    FALLBACK_ROPE_EMERGENCY_STRETCH_MULTIPLIER,
                    FALLBACK_ROPE_EMERGENCY_MAX_PULL_SPEED,
                    FALLBACK_ROPE_SWING_DAMPING);
            case "strict" -> new PhysicsPreset(0.52D, 0.58D, 2.0D, 1.0D, 0.972D);
            default -> PhysicsPreset.custom();
        };
    }

    private record PhysicsPreset(
            double correctionRate,
            double maxPullSpeed,
            double emergencyStretchMultiplier,
            double emergencyMaxPullSpeed,
            double swingDamping) {
        static PhysicsPreset custom() {
            return new PhysicsPreset(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }

        double correctionRate(double custom) {
            return Double.isNaN(correctionRate) ? Math.max(0.0D, custom) : correctionRate;
        }

        double maxPullSpeed(double custom) {
            return Double.isNaN(maxPullSpeed) ? Math.max(0.0D, custom) : maxPullSpeed;
        }

        double emergencyStretchMultiplier(double custom) {
            return Double.isNaN(emergencyStretchMultiplier) ? Math.max(1.0D, custom) : emergencyStretchMultiplier;
        }

        double emergencyMaxPullSpeed(double custom) {
            return Double.isNaN(emergencyMaxPullSpeed) ? Math.max(0.0D, custom) : emergencyMaxPullSpeed;
        }

        double swingDamping(double custom) {
            return Double.isNaN(swingDamping) ? clamp(custom, 0.0D, 1.0D) : swingDamping;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
