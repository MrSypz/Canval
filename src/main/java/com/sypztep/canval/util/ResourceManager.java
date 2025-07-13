package com.sypztep.canval.util;

import com.sypztep.canval.util.resource.*;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class ResourceManager {
    private static final Map<ResourceLocation, ByteBuffer> fontCache = new HashMap<>();
    private static final String ASSETS_PATH = "/assets/";

    public static FontResource createFont(ResourceLocation id, String path) {
        return createFont(id, path, id.getFileName(), 16.0f);
    }

    public static FontResource createFont(ResourceLocation id, String path, String displayName, float defaultSize) {
        try {
            String resourcePath = ASSETS_PATH + "fonts/" + path;

            try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Font resource not found: " + resourcePath);
                }

                System.out.println("Loading font: " + path);
                byte[] fontData = inputStream.readAllBytes();

                ByteBuffer fontBuffer = MemoryUtil.memAlloc(fontData.length);
                fontBuffer.put(fontData).flip();

                STBTTFontinfo fontInfo = STBTTFontinfo.create();
                if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
                    MemoryUtil.memFree(fontBuffer);
                    throw new RuntimeException("Failed to initialize font: " + path);
                }

                fontCache.put(id, fontBuffer);
                return new FontResource(id, fontBuffer, fontInfo, displayName, defaultSize);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + path, e);
        }
    }

    public static TextureResource createTexture(ResourceLocation id, String path) {
        return createTexture(id, path, id.getFileName());
    }

    public static TextureResource createTexture(ResourceLocation id, String path, String displayName) {
        try {
            String resourcePath = ASSETS_PATH + "textures/" + path;

            try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Texture resource not found: " + resourcePath);
                }

                System.out.println("Loading texture: " + path);

                // TODO: Actual texture loading with STBI
                int textureId = 1; // Generated texture ID
                int width = 256;
                int height = 256;

                return new TextureResource(id, textureId, width, height, displayName, TextureResource.TextureFormat.RGBA);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }

    public static SoundResource createSound(ResourceLocation id, String path) {
        return createSound(id, path, id.getFileName());
    }

    public static SoundResource createSound(ResourceLocation id, String path, String displayName) {
        try {
            String resourcePath = ASSETS_PATH + "sounds/" + path;

            try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Sound resource not found: " + resourcePath);
                }

                System.out.println("Loading sound: " + path);
                byte[] soundData = inputStream.readAllBytes();

                ByteBuffer audioBuffer = MemoryUtil.memAlloc(soundData.length);
                audioBuffer.put(soundData).flip();

                int sampleRate = 44100;
                int channels = 2;
                float duration = 5.0f;

                return new SoundResource(id, audioBuffer, sampleRate, channels, displayName, duration);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sound: " + path, e);
        }
    }

    public static void cleanup() {
        fontCache.values().forEach(MemoryUtil::memFree);
        fontCache.clear();
    }
}