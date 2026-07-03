package com.mcelma.rope;

import java.util.UUID;

public final class RopeLink {
    private final UUID id;
    private final UUID controllerUuid;
    private final RopeEndpoint first;
    private final RopeEndpoint second;
    private final double length;
    private final boolean refundLeadOnManualRelease;
    private boolean taut;

    public RopeLink(UUID controllerUuid, RopeEndpoint first, RopeEndpoint second, double length) {
        this(controllerUuid, first, second, length, false);
    }

    public RopeLink(
            UUID controllerUuid,
            RopeEndpoint first,
            RopeEndpoint second,
            double length,
            boolean refundLeadOnManualRelease) {
        this.id = UUID.randomUUID();
        this.controllerUuid = controllerUuid;
        this.first = first;
        this.second = second;
        this.length = RopeConfig.clampLength(length);
        this.refundLeadOnManualRelease = refundLeadOnManualRelease;
    }

    public UUID id() {
        return id;
    }

    public RopeEndpoint first() {
        return first;
    }

    public UUID controllerUuid() {
        return controllerUuid;
    }

    public RopeEndpoint second() {
        return second;
    }

    public double length() {
        return length;
    }

    public boolean refundLeadOnManualRelease() {
        return refundLeadOnManualRelease;
    }

    public boolean isTaut() {
        return taut;
    }

    public void setTaut(boolean taut) {
        this.taut = taut;
    }

    public boolean includesPlayer(UUID playerUuid) {
        return matches(first, playerUuid) || matches(second, playerUuid);
    }

    public boolean hasOnlyPlayerEndpoints() {
        return first.type() == RopeEndpoint.Type.PLAYER && second.type() == RopeEndpoint.Type.PLAYER;
    }

    public RopeEndpoint otherEndpoint(UUID playerUuid) {
        if (matches(first, playerUuid)) {
            return second;
        }
        if (matches(second, playerUuid)) {
            return first;
        }
        return null;
    }

    private static boolean matches(RopeEndpoint endpoint, UUID playerUuid) {
        return endpoint.type() == RopeEndpoint.Type.PLAYER && endpoint.playerUuid().equals(playerUuid);
    }
}
