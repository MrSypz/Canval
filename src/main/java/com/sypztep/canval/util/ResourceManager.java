package com.sypztep.canval.util;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
    private static final Map<ResourceLocation, ByteBuffer> fontCache = new HashMap<>();
    private static final Map<ResourceLocation, STBTTFontinfo> fontInfoCache = new HashMap<>();
    private static final String ASSETS_PATH = "/assets/fonts/";

    public static ByteBuffer loadFont(ResourceLocation location) {
        if (fontCache.containsKey(location)) return fontCache.get(location);

        try {
            String resourcePath = ASSETS_PATH + location.getPath();
            InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath);

            if (inputStream == null) throw new RuntimeException("Font resource not found: " + resourcePath);

            byte[] fontData = inputStream.readAllBytes();
            inputStream.close();

            ByteBuffer fontBuffer = MemoryUtil.memAlloc(fontData.length);
            fontBuffer.put(fontData);
            fontBuffer.flip();

            fontCache.put(location, fontBuffer);
            return fontBuffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + location.getPath(), e);
        }
    }

    public static STBTTFontinfo getFontInfo(ResourceLocation location) {
        if (fontInfoCache.containsKey(location)) return fontInfoCache.get(location);


        ByteBuffer fontBuffer = loadFont(location);
        STBTTFontinfo fontInfo = STBTTFontinfo.create();

        if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) throw new RuntimeException("Failed to initialize font: " + location.getPath());


        fontInfoCache.put(location, fontInfo);
        return fontInfo;
    }

    public static void cleanup() {
        fontCache.values().forEach(MemoryUtil::memFree);
        fontCache.clear();
        fontInfoCache.clear();
    }
}