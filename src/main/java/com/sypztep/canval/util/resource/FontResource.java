package com.sypztep.canval.util.resource;

import com.sypztep.canval.util.ResourceLocation;
import org.lwjgl.stb.STBTTFontinfo;

import java.nio.ByteBuffer;

public record FontResource(
        ResourceLocation id,
        ByteBuffer fontBuffer,
        STBTTFontinfo fontInfo,
        String displayName,
        float defaultSize
) {
    public FontResource {
        if (fontBuffer == null || fontInfo == null)
            throw new IllegalArgumentException("Font buffer and info cannot be null");

        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("Font name cannot be null or blank");

    }
}
