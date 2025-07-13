package com.sypztep.canval.util;

import com.sypztep.canval.util.resource.*;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public final class ResourceManager {
    private static final Map<ResourceLocation, ByteBuffer> fontCache = new HashMap<>();
    private static final String ASSETS_PATH = "/assets/";

    public static FontResource createFont(ResourceLocation location) {
        return createFont(location, location.getFileName(), 16.0f);
    }

    public static FontResource createFont(ResourceLocation location, String displayName, float defaultSize) {
        try {
            String resourcePath = ASSETS_PATH + "fonts/" + location.path();

            try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Font resource not found: " + resourcePath);
                }

                System.out.println("Loading font: " + location.path());
                byte[] fontData = inputStream.readAllBytes();

                ByteBuffer fontBuffer = MemoryUtil.memAlloc(fontData.length);
                fontBuffer.put(fontData).flip();

                STBTTFontinfo fontInfo = STBTTFontinfo.create();
                if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
                    MemoryUtil.memFree(fontBuffer);
                    throw new RuntimeException("Failed to initialize font: " + location.path());
                }

                fontCache.put(location, fontBuffer);
                return new FontResource(location, fontBuffer, fontInfo, displayName, defaultSize);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + location.path(), e);
        }
    }

    public static TextureResource createTexture(ResourceLocation location) {
        return createTexture(location, location.getFileName());
    }

    public static TextureResource createTexture(ResourceLocation location, String displayName) {
        try {
            String resourcePath = ASSETS_PATH + "textures/" + location.path();

            try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Texture resource not found: " + resourcePath);
                }

                System.out.println("Loading texture: " + location.path());

                // Read image data
                byte[] imageData = inputStream.readAllBytes();
                ByteBuffer imageBuffer = MemoryUtil.memAlloc(imageData.length);
                imageBuffer.put(imageData).flip();

                // Load image with STBI
                IntBuffer width = MemoryUtil.memAllocInt(1);
                IntBuffer height = MemoryUtil.memAllocInt(1);
                IntBuffer channels = MemoryUtil.memAllocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 0);

                if (image == null) {
                    MemoryUtil.memFree(imageBuffer);
                    MemoryUtil.memFree(width);
                    MemoryUtil.memFree(height);
                    MemoryUtil.memFree(channels);
                    throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
                }

                int w = width.get(0);
                int h = height.get(0);
                int c = channels.get(0);

                // Determine format
                TextureResource.TextureFormat format;
                int glFormat;
                if (c == 3) {
                    format = TextureResource.TextureFormat.RGB;
                    glFormat = GL12.GL_RGB;
                } else if (c == 4) {
                    format = TextureResource.TextureFormat.RGBA;
                    glFormat = GL12.GL_RGBA;
                } else if (c == 1) {
                    format = TextureResource.TextureFormat.ALPHA;
                    glFormat = GL12.GL_RED;
                } else {
                    STBImage.stbi_image_free(image);
                    MemoryUtil.memFree(imageBuffer);
                    MemoryUtil.memFree(width);
                    MemoryUtil.memFree(height);
                    MemoryUtil.memFree(channels);
                    throw new RuntimeException("Unsupported image format: " + c + " channels");
                }

                // Create OpenGL texture
                int textureId = GL12.glGenTextures();
                GL12.glBindTexture(GL12.GL_TEXTURE_2D, textureId);

                // Set texture parameters
                GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_LINEAR);
                GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_LINEAR);
                GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
                GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);

                // Upload texture data
                GL12.glTexImage2D(GL12.GL_TEXTURE_2D, 0, glFormat, w, h, 0, glFormat, GL12.GL_UNSIGNED_BYTE, image);

                // Generate mipmaps
                GL30.glGenerateMipmap(GL12.GL_TEXTURE_2D);

                // Cleanup
                STBImage.stbi_image_free(image);
                MemoryUtil.memFree(imageBuffer);
                MemoryUtil.memFree(width);
                MemoryUtil.memFree(height);
                MemoryUtil.memFree(channels);

                return new TextureResource(location, textureId, w, h, displayName, format);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: " + location.path(), e);
        }
    }

    public static SoundResource createSound(ResourceLocation location) {
        return createSound(location, location.getFileName());
    }

    public static SoundResource createSound(ResourceLocation location, String displayName) {
        try {
            String resourcePath = ASSETS_PATH + "sounds/" + location.path();

            try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Sound resource not found: " + resourcePath);
                }

                System.out.println("Loading sound: " + location.path());
                byte[] soundData = inputStream.readAllBytes();

                ByteBuffer audioBuffer = MemoryUtil.memAlloc(soundData.length);
                audioBuffer.put(soundData).flip();

                int sampleRate = 44100;
                int channels = 2;
                float duration = 5.0f;

                return new SoundResource(location, audioBuffer, sampleRate, channels, displayName, duration);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sound: " + location.path(), e);
        }
    }

    public static void cleanup() {
        fontCache.values().forEach(MemoryUtil::memFree);
        fontCache.clear();
    }
}
