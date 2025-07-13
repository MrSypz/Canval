package com.sypztep.canval.util.resource;

import com.sypztep.canval.util.ResourceLocation;

public record TextureResource(
        ResourceLocation id,
        int textureId,
        int width,
        int height,
        String displayName,
        TextureFormat format
) {
    public enum TextureFormat {
        RGB, RGBA, ALPHA
    }

    public TextureResource {
        if (textureId <= 0)
            throw new IllegalArgumentException("Invalid texture ID");

        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Invalid texture dimensions");

    }
}