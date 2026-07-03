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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class RopeConfig {
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
    private static final int FALLBACK_HOLDER_DAMAGE_DROP_DENOMINATOR = 100;
    private static final double FALLBACK_ROPE_CORRECTION_RATE = 0.35D;
    private static final double FALLBACK_ROPE_MAX_PULL_SPEED = 0.45D;
    private static final double FALLBACK_ROPE_EMERGENCY_STRETCH_MULTIPLIER = 2.5D;
    private static final double FALLBACK_ROPE_EMERGENCY_MAX_PULL_SPEED = 0.8D;
    private static final double FALLBACK_ROPE_SWING_DAMPING = 0.985D;
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
        return COMMAND_PERMISSION_LEVEL;
    }

    public static int maxActiveLinks() {
        return MAX_ACTIVE_LINKS;
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

    public static boolean enableHolderDamageDrop() {
        return data.enableHolderDamageDrop;
    }

    public static int holderDamageDropDenominator() {
        return Math.max(1, data.holderDamageDropDenominator);
    }

    public static double ropeCorrectionRate() {
        return Math.max(0.0D, data.ropeCorrectionRate);
    }

    public static double ropeMaxPullSpeed() {
        return Math.max(0.0D, data.ropeMaxPullSpeed);
    }

    public static double ropeEmergencyStretchMultiplier() {
        return Math.max(1.0D, data.ropeEmergencyStretchMultiplier);
    }

    public static double ropeEmergencyMaxPullSpeed() {
        return Math.max(0.0D, data.ropeEmergencyMaxPullSpeed);
    }

    public static double ropeSwingDamping() {
        return clamp(data.ropeSwingDamping, 0.0D, 1.0D);
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
            // The mod can still run with in-memory defaults.
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
        boolean enableHolderDamageDrop = true;
        int holderDamageDropDenominator = FALLBACK_HOLDER_DAMAGE_DROP_DENOMINATOR;
        double ropeCorrectionRate = FALLBACK_ROPE_CORRECTION_RATE;
        double ropeMaxPullSpeed = FALLBACK_ROPE_MAX_PULL_SPEED;
        double ropeEmergencyStretchMultiplier = FALLBACK_ROPE_EMERGENCY_STRETCH_MULTIPLIER;
        double ropeEmergencyMaxPullSpeed = FALLBACK_ROPE_EMERGENCY_MAX_PULL_SPEED;
        double ropeSwingDamping = FALLBACK_ROPE_SWING_DAMPING;
        List<String> anchorBlockIds = DEFAULT_ANCHOR_BLOCKS;

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
            holderDamageDropDenominator = sanitizeTicks(
                    holderDamageDropDenominator,
                    FALLBACK_HOLDER_DAMAGE_DROP_DENOMINATOR);
            ropeCorrectionRate = sanitizeNonNegative(ropeCorrectionRate, FALLBACK_ROPE_CORRECTION_RATE);
            ropeMaxPullSpeed = sanitizeNonNegative(ropeMaxPullSpeed, FALLBACK_ROPE_MAX_PULL_SPEED);
            ropeEmergencyStretchMultiplier = sanitize(
                    ropeEmergencyStretchMultiplier,
                    FALLBACK_ROPE_EMERGENCY_STRETCH_MULTIPLIER);
            ropeEmergencyMaxPullSpeed = sanitizeNonNegative(
                    ropeEmergencyMaxPullSpeed,
                    FALLBACK_ROPE_EMERGENCY_MAX_PULL_SPEED);
            ropeSwingDamping = sanitizeRange(ropeSwingDamping, 0.0D, 1.0D, FALLBACK_ROPE_SWING_DAMPING);
            if (anchorBlockIds == null || anchorBlockIds.isEmpty()) {
                anchorBlockIds = DEFAULT_ANCHOR_BLOCKS;
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
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
