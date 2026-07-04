package com.mcelma.rope;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public record RopeVisualPayload(List<VisualLink> links) implements CustomPayload {
    public static final CustomPayload.Id<RopeVisualPayload> ID =
            new CustomPayload.Id<>(Identifier.of(McElmaRopeMod.MOD_ID, "rope_visuals"));
    public static final PacketCodec<RegistryByteBuf, RopeVisualPayload> CODEC =
            PacketCodec.ofStatic(RopeVisualPayload::write, RopeVisualPayload::read);

    private static final int MAX_VISUAL_LINKS = 512;

    public RopeVisualPayload {
        links = List.copyOf(links);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, RopeVisualPayload payload) {
        int count = Math.min(payload.links.size(), MAX_VISUAL_LINKS);
        buf.writeVarInt(count);
        for (int index = 0; index < count; index++) {
            VisualLink link = payload.links.get(index);
            buf.writeUuid(link.id());
            buf.writeDouble(link.length());
            buf.writeBoolean(link.taut());
            writeEndpoint(buf, link.first());
            writeEndpoint(buf, link.second());
        }
    }

    private static RopeVisualPayload read(RegistryByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_VISUAL_LINKS);
        List<VisualLink> links = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            links.add(new VisualLink(buf.readUuid(), buf.readDouble(), buf.readBoolean(), readEndpoint(buf), readEndpoint(buf)));
        }
        return new RopeVisualPayload(links);
    }

    private static void writeEndpoint(RegistryByteBuf buf, VisualEndpoint endpoint) {
        buf.writeBoolean(endpoint.player());
        if (endpoint.player()) {
            buf.writeVarInt(endpoint.entityId());
        } else {
            buf.writeVec3d(endpoint.anchorPos());
        }
    }

    private static VisualEndpoint readEndpoint(RegistryByteBuf buf) {
        if (buf.readBoolean()) {
            return VisualEndpoint.player(buf.readVarInt());
        }
        return VisualEndpoint.anchor(buf.readVec3d());
    }

    public record VisualLink(UUID id, double length, boolean taut, VisualEndpoint first, VisualEndpoint second) {
    }

    public record VisualEndpoint(boolean player, int entityId, Vec3d anchorPos) {
        public static VisualEndpoint player(int entityId) {
            return new VisualEndpoint(true, entityId, Vec3d.ZERO);
        }

        public static VisualEndpoint anchor(Vec3d anchorPos) {
            return new VisualEndpoint(false, -1, anchorPos);
        }
    }
}
