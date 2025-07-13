package com.sypztep.canval.graphic.font;

import com.sypztep.canval.util.resource.FontResource;
import org.lwjgl.opengl.GL20;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class FontAtlas {
    private static final Logger LOGGER = LoggerFactory.getLogger(FontAtlas.class);

    private final FontResource font;
    private final float scale;
    private final Map<Character, CharacterInfo> characterCache = new HashMap<>();

    // Single texture atlas
    private int atlasTextureId = 0;
    private int atlasWidth = 512;  // Start with 512x512, can expand
    private int atlasHeight = 512;
    private int currentX = 0;
    private int currentY = 0;
    private int rowHeight = 0;

    // Atlas pixel data
    private ByteBuffer atlasData;

    // Font metrics (cached)
    private final int ascent;
    private final int descent;
    private final int lineGap;

    public FontAtlas(FontResource font, float fontSize) {
        this.font = font;
        this.scale = stbtt_ScaleForPixelHeight(font.fontInfo(), fontSize);

        // Cache font metrics
        try (MemoryStack stack = stackPush()) {
            IntBuffer ascentBuffer = stack.mallocInt(1);
            IntBuffer descentBuffer = stack.mallocInt(1);
            IntBuffer lineGapBuffer = stack.mallocInt(1);

            stbtt_GetFontVMetrics(font.fontInfo(), ascentBuffer, descentBuffer, lineGapBuffer);

            this.ascent = ascentBuffer.get(0);
            this.descent = descentBuffer.get(0);
            this.lineGap = lineGapBuffer.get(0);
        }

        // Initialize atlas
        createAtlasTexture();

        LOGGER.debug("Created optimized FontAtlas for {} at size {}", font.displayName(), fontSize);
    }

    private void createAtlasTexture() {
        // Create atlas texture
        atlasTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);

        // Allocate atlas pixel data (RGBA)
        atlasData = MemoryUtil.memAlloc(atlasWidth * atlasHeight * 4);

        // Clear to transparent
        BufferAtlasUpdate(atlasWidth, atlasHeight, atlasData);

        // Upload empty texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasData);

        LOGGER.debug("Created atlas texture {} ({}x{})", atlasTextureId, atlasWidth, atlasHeight);
    }

    public CharacterInfo getCharacter(char c) {
        return characterCache.computeIfAbsent(c, this::addCharacterToAtlas);
    }

    private CharacterInfo addCharacterToAtlas(char c) {
        STBTTFontinfo fontInfo = font.fontInfo();

        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer xOffset = stack.mallocInt(1);
            IntBuffer yOffset = stack.mallocInt(1);

            // Get character bitmap
            ByteBuffer bitmap = stbtt_GetCodepointBitmap(fontInfo, 0, scale, c, width, height, xOffset, yOffset);

            if (bitmap == null) {
                LOGGER.warn("Failed to create bitmap for character: {}", c);
                return new CharacterInfo(0, 0, 0, 0, 0, 0, 0, 0, 0);
            }

            int w = width.get(0);
            int h = height.get(0);
            int xOff = xOffset.get(0);
            int yOff = yOffset.get(0);

            // Get advance width
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer leftSideBearing = stack.mallocInt(1);
            stbtt_GetCodepointHMetrics(fontInfo, c, advanceWidth, leftSideBearing);
            float advance = advanceWidth.get(0) * scale;

            if (w == 0 || h == 0) {
                // Character has no visual representation (space, etc.)
                stbtt_FreeBitmap(bitmap);
                return new CharacterInfo(0, 0, 0, 0, w, h, xOff, yOff, advance);
            }

            // Check if character fits in current row
            if (currentX + w > atlasWidth) {
                // Move to next row
                currentX = 0;
                currentY += rowHeight;
                rowHeight = 0;

                // Check if we need to expand atlas
                if (currentY + h > atlasHeight) {
                    expandAtlas();
                }
            }

            // Calculate UV coordinates
            float u1 = (float) currentX / atlasWidth;
            float v1 = (float) currentY / atlasHeight;
            float u2 = (float) (currentX + w) / atlasWidth;
            float v2 = (float) (currentY + h) / atlasHeight;

            // Copy character bitmap to atlas
            copyBitmapToAtlas(bitmap, currentX, currentY, w, h);

            // Update position tracking
            currentX += w;
            rowHeight = Math.max(rowHeight, h);

            // Free bitmap
            stbtt_FreeBitmap(bitmap);

//            LOGGER.debug("Added character '{}' to atlas at ({}, {}) size {}x{}", c, currentX - w, currentY, w, h);

            return new CharacterInfo(u1, v1, u2, v2, w, h, xOff, yOff, advance);
        }
    }

    private void copyBitmapToAtlas(ByteBuffer bitmap, int atlasX, int atlasY, int charWidth, int charHeight) {
        for (int y = 0; y < charHeight; y++) {
            for (int x = 0; x < charWidth; x++) {
                // Get alpha from bitmap
                byte alpha = bitmap.get(y * charWidth + x);

                // Calculate position in atlas
                int atlasPos = ((atlasY + y) * atlasWidth + (atlasX + x)) * 4;

                // Set RGBA (white with bitmap alpha)
                atlasData.put(atlasPos, (byte) 255);     // R
                atlasData.put(atlasPos + 1, (byte) 255); // G
                atlasData.put(atlasPos + 2, (byte) 255); // B
                atlasData.put(atlasPos + 3, alpha);      // A
            }
        }
        // Update texture region
        glBindTexture(GL_TEXTURE_2D, atlasTextureId);

        // Create temporary buffer for the region
        ByteBuffer regionData = MemoryUtil.memAlloc(charWidth * charHeight * 4);
        for (int y = 0; y < charHeight; y++) {
            for (int x = 0; x < charWidth; x++) {
                int atlasPos = ((atlasY + y) * atlasWidth + (atlasX + x)) * 4;
                regionData.put(atlasData.get(atlasPos));     // R
                regionData.put(atlasData.get(atlasPos + 1)); // G
                regionData.put(atlasData.get(atlasPos + 2)); // B
                regionData.put(atlasData.get(atlasPos + 3)); // A
            }
        }
        regionData.flip();

        // Upload only the changed region
        glTexSubImage2D(GL_TEXTURE_2D, 0, atlasX, atlasY, charWidth, charHeight, GL_RGBA, GL_UNSIGNED_BYTE, regionData);

        MemoryUtil.memFree(regionData);
    }

    private void expandAtlas() {
        int newWidth = atlasWidth * 2;
        int newHeight = atlasHeight * 2;

        LOGGER.debug("Expanding atlas from {}x{} to {}x{}", atlasWidth, atlasHeight, newWidth, newHeight);

        // Create new atlas data
        ByteBuffer newAtlasData = MemoryUtil.memAlloc(newWidth * newHeight * 4);
        // Clear to transparent
        BufferAtlasUpdate(newWidth, newHeight, newAtlasData);
        // Copy old data to new atlas
        for (int y = 0; y < atlasHeight; y++) {
            for (int x = 0; x < atlasWidth; x++) {
                int oldPos = (y * atlasWidth + x) * 4;
                int newPos = (y * newWidth + x) * 4;

                newAtlasData.put(newPos, atlasData.get(oldPos));               // R
                newAtlasData.put(newPos + 1, atlasData.get(oldPos + 1)); // G
                newAtlasData.put(newPos + 2, atlasData.get(oldPos + 2)); // B
                newAtlasData.put(newPos + 3, atlasData.get(oldPos + 3)); // A
            }
        }

        glBindTexture(GL_TEXTURE_2D, atlasTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, newWidth, newHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, newAtlasData);

        // Free old data and update references
        MemoryUtil.memFree(atlasData);
        atlasData = newAtlasData;
        atlasWidth = newWidth;
        atlasHeight = newHeight;

        recalculateUVCoordinates();
    }

    private void BufferAtlasUpdate(int newWidth, int newHeight, ByteBuffer newAtlasData) {
        for (int i = 0; i < newWidth * newHeight * 4; i += 4) {
            newAtlasData.put(i, (byte) 255);     // R
            newAtlasData.put(i + 1, (byte) 255); // G
            newAtlasData.put(i + 2, (byte) 255); // B
            newAtlasData.put(i + 3, (byte) 0);   // A
        }
    }

    private void recalculateUVCoordinates() {
        Map<Character, CharacterInfo> oldCache = new HashMap<>(characterCache);
        characterCache.clear();
        currentX = 0;
        currentY = 0;
        rowHeight = 0;

        for (Character c : oldCache.keySet())
            characterCache.put(c, addCharacterToAtlas(c));

    }

    public int getAtlasTextureId() {
        return atlasTextureId;
    }

    public float getAscent() {
        return ascent * scale;
    }

    public float getDescent() {
        return descent * scale;
    }

    public float getLineGap() {
        return lineGap * scale;
    }

    public float getLineHeight() {
        return (ascent - descent + lineGap) * scale;
    }

    public void cleanup() {
        LOGGER.debug("Cleaning up FontAtlas for {} (cached {} characters)", font.displayName(), characterCache.size());

        if (atlasTextureId > 0) {
            glDeleteTextures(atlasTextureId);
            atlasTextureId = 0;
        }

        if (atlasData != null) {
            MemoryUtil.memFree(atlasData);
            atlasData = null;
        }

        characterCache.clear();
    }
}