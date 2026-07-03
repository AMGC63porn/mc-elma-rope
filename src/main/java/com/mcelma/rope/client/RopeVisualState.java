package com.mcelma.rope.client;

import java.util.List;

import com.mcelma.rope.RopeVisualPayload;

public final class RopeVisualState {
    private static volatile List<RopeVisualPayload.VisualLink> links = List.of();

    private RopeVisualState() {
    }

    public static void setLinks(List<RopeVisualPayload.VisualLink> nextLinks) {
        links = List.copyOf(nextLinks);
    }

    public static List<RopeVisualPayload.VisualLink> links() {
        return links;
    }

    public static void clear() {
        links = List.of();
    }
}
