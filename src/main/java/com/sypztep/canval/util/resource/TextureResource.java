package com.sypztep.canval.util.resource;

import com.sypztep.canval.util.ResourceLocation;

import java.nio.ByteBuffer;

public record TextureResource(
        ResourceLocation id,
        ByteBuffer imageData,  // Raw image data
        int width,
        int height,
        String displayName,
        TextureFormat format,
        int glTextureId        // OpenGL texture ID (0 = not bound yet)
) {
    public enum TextureFormat {
        RGB, RGBA, ALPHA
    }

    // Constructor for loading phase (no OpenGL)
    public TextureResource(ResourceLocation id, ByteBuffer imageData, int width, int height,
                           String displayName, TextureFormat format) {
        this(id, imageData, width, height, displayName, format, 0);
    }

    // Create new instance with OpenGL texture ID
    public TextureResource withGLTexture(int glTextureId) {
        return new TextureResource(id, imageData, width, height, displayName, format, glTextureId);
    }

    public boolean isBound() {
        return glTextureId > 0;
    }
}