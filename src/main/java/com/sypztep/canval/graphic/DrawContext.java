package com.sypztep.canval.graphic;

import com.sypztep.canval.graphic.font.CharacterInfo;
import com.sypztep.canval.graphic.font.FontAtlas;
import com.sypztep.canval.graphic.font.FontAtlasManager;
import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.identifier.Registries;
import com.sypztep.canval.util.math.MatrixStack;
import com.sypztep.canval.util.resource.FontResource;
import com.sypztep.canval.util.resource.TextureResource;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public class DrawContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrawContext.class);

    private final int windowWidth;
    private final int windowHeight;
    private final MatrixStack matrices;
    private boolean projectionSet = false;

    private final FloatBuffer matrixBuffer = memAllocFloat(16);
    private final Matrix4f lastAppliedMatrix = new Matrix4f();
    private boolean matrixDirty = true;

    private static class TextVertex {
        float x, y, u, v;
        TextVertex(float x, float y, float u, float v) {
            this.x = x; this.y = y; this.u = u; this.v = v;
        }
    }

    private final List<TextVertex> textVertices = new ArrayList<>();
    private FontAtlas currentTextAtlas;

    private static class TextureVertex {
        float x, y, z, u, v;
        TextureVertex(float x, float y, float z, float u, float v) {
            this.x = x; this.y = y; this.z = z; this.u = u; this.v = v;
        }
    }

    private final List<TextureVertex> textureVertices = new ArrayList<>();
    private int currentTextureId = -1;

    public DrawContext(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.matrices = new MatrixStack();
        LOGGER.debug("DrawContext created: {}x{}", windowWidth, windowHeight);
    }

    public MatrixStack getMatrix() {
        return matrices;
    }

    public void beginFrame() {
        if (!projectionSet) {
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
            projectionSet = true;
        }
        matrixDirty = true;
    }

    public void endFrame() {
        flushTextures();
        projectionSet = false;
        matrixDirty = true;
    }

    private void applyMatrixTransform() {
        glMatrixMode(GL_MODELVIEW);
        Matrix4f currentMatrix = matrices.peek().getPositionMatrix();

        if (matrixDirty || !currentMatrix.equals(lastAppliedMatrix)) {
            currentMatrix.get(matrixBuffer);
            glLoadMatrixf(matrixBuffer);
            lastAppliedMatrix.set(currentMatrix);
            matrixDirty = false;
        }
    }

    public void markMatrixDirty() {
        matrixDirty = true;
    }

    /**
     * Draws a textured rectangle from a region in a 256x256 texture.
     * The Z coordinate of the rectangle is 0.
     * The width and height of the region are the same as the dimensions of the rectangle.
     */
    public void drawTexture(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        // Use 256x256 as reference texture size (Minecraft default)
        this.drawTexture(texture, x, y, 0, (float)u, (float)v, width, height, 256, 256);
    }

    /**
     * Draws a textured rectangle from a region in a texture.
     * The width and height of the region are the same as the dimensions of the rectangle.
     */
    public void drawTexture(ResourceLocation texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, x + width, y, y + height, z, width, height, u, v, textureWidth, textureHeight);
    }

    /**
     * Draws a textured rectangle from a region in a texture.
     */
    public void drawTexture(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, x + width, y, y + height, 0, regionWidth, regionHeight, u, v, textureWidth, textureHeight);
    }

    /**
     * Draws a textured rectangle from a region in a texture.
     * The width and height of the region are the same as the dimensions of the rectangle.
     */
    public void drawTexture(ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    /**
     * Core texture drawing method - all other texture methods delegate to this
     * This is where the textureWidth/textureHeight parameters are actually used
     */
    void drawTexture(ResourceLocation texture, int x1, int x2, int y1, int y2, int z, int regionWidth, int regionHeight, float u, float v, int textureWidth, int textureHeight) {
        // FIXED: Use the provided textureWidth/textureHeight parameters for UV calculation
        this.drawTexturedQuad(
                texture,
                x1, x2, y1, y2, z,
                (u + 0.0F) / (float)textureWidth,                    // u1
                (u + (float)regionWidth) / (float)textureWidth,      // u2
                (v + 0.0F) / (float)textureHeight,                   // v1
                (v + (float)regionHeight) / (float)textureHeight     // v2
        );
    }

    /**
     * Convenience method to draw entire texture at specified size
     * FIXED: Use actual texture dimensions when drawing the full texture
     */
    public void drawTexture(ResourceLocation texture, int x, int y, int width, int height) {
        TextureResource textureResource;
        try {
            textureResource = Registries.TEXTURE.get(texture);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Texture not found: {}", texture);
            return;
        }

        drawTexture(texture, x, y, width, height, 0, 0,
                textureResource.width(), textureResource.height(),
                textureResource.width(), textureResource.height());
    }

    /**
     * Convenience method to draw texture at original size
     * FIXED: Use actual texture dimensions
     */
    public void drawTexture(ResourceLocation texture, int x, int y) {
        TextureResource textureResource;
        try {
            textureResource = Registries.TEXTURE.get(texture);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Texture not found: {}", texture);
            return;
        }

        // Draw at original size using actual dimensions
        drawTexture(texture, x, y, textureResource.width(), textureResource.height());
    }

    /**
     * NEW: Method to draw texture region with explicit texture size reference
     * This is useful for sprite sheets and GUI textures
     */
    public void drawTextureRegion(ResourceLocation texture, int x, int y, int u, int v,
                                  int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, y, regionWidth, regionHeight, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    /**
     * NEW: Method to draw scaled texture region
     * Draws a region from texture but scales it to different display size
     */
    public void drawScaledTextureRegion(ResourceLocation texture, int x, int y, int displayWidth, int displayHeight,
                                        int u, int v, int regionWidth, int regionHeight,
                                        int textureWidth, int textureHeight) {
        this.drawTexture(texture, x, y, displayWidth, displayHeight, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    /**
     * Draws a textured quad with normalized UV coordinates
     */
    private void drawTexturedQuad(ResourceLocation texture, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2) {
        TextureResource textureResource;
        try {
            textureResource = Registries.TEXTURE.get(texture);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Texture not found: {}", texture);
            return;
        }

        if (!textureResource.isBound()) {
            LOGGER.warn("Texture not bound to OpenGL: {}", texture);
            return;
        }

        int textureId = textureResource.glTextureId();
        if (currentTextureId != textureId) {
            flushTextures(); // Flush previous batch
            currentTextureId = textureId;
        }

        // Add vertices to batch (creating a quad from two triangles)
        float zf = (float)z;

        // Triangle 1: top-left, top-right, bottom-left
        textureVertices.add(new TextureVertex(x1, y1, zf, u1, v1)); // Top-left
        textureVertices.add(new TextureVertex(x2, y1, zf, u2, v1)); // Top-right
        textureVertices.add(new TextureVertex(x1, y2, zf, u1, v2)); // Bottom-left

        // Triangle 2: top-right, bottom-right, bottom-left
        textureVertices.add(new TextureVertex(x2, y1, zf, u2, v1)); // Top-right
        textureVertices.add(new TextureVertex(x2, y2, zf, u2, v2)); // Bottom-right
        textureVertices.add(new TextureVertex(x1, y2, zf, u1, v2)); // Bottom-left
    }

    /**
     * Flush batched texture rendering
     */
    private void flushTextures() {
        if (textureVertices.isEmpty() || currentTextureId == -1) {
            return;
        }

        applyMatrixTransform();

        // Enable texturing and blending
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Bind texture
        glBindTexture(GL_TEXTURE_2D, currentTextureId);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // White tint

        // Render all vertices as triangles
        glBegin(GL_TRIANGLES);
        for (TextureVertex vertex : textureVertices) {
            glTexCoord2f(vertex.u, vertex.v);
            glVertex3f(vertex.x, vertex.y, vertex.z);
        }
        glEnd();

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);

        // Clear batch
        textureVertices.clear();
        currentTextureId = -1;
    }

    public void drawText(String text, float x, float y, float fontSize, FontResource font) {
        drawText(text, x, y, fontSize, font, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void drawText(String text, float x, float y, float fontSize, FontResource font, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

        flushTextures();

        applyMatrixTransform();

        FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);

        textVertices.clear();
        currentTextAtlas = atlas;

        float currentX = x;
        float currentY = y + atlas.getAscent();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                currentX = x;
                currentY += atlas.getLineHeight();
                continue;
            }

            CharacterInfo charInfo = atlas.getCharacter(c);

            if (charInfo.width() > 0 && charInfo.height() > 0) {
                float x1 = currentX + charInfo.xOffset();
                float y1 = currentY + charInfo.yOffset();
                float x2 = x1 + charInfo.width();
                float y2 = y1 + charInfo.height();

                textVertices.add(new TextVertex(x1, y1, charInfo.u1(), charInfo.v1())); // Top-left
                textVertices.add(new TextVertex(x2, y1, charInfo.u2(), charInfo.v1())); // Top-right
                textVertices.add(new TextVertex(x2, y2, charInfo.u2(), charInfo.v2())); // Bottom-right
                textVertices.add(new TextVertex(x1, y2, charInfo.u1(), charInfo.v2())); // Bottom-left
            }

            currentX += charInfo.advance();
        }

        renderBatchedText(r, g, b, a);
    }

    public void drawOneByOneText(String text, int visibleCharCount, float x, float y, float fontSize, FontResource font, float r, float g, float b, float a) {
        if (text == null || text.isEmpty() || visibleCharCount <= 0) return;

        // Flush any pending texture batches before drawing text
        flushTextures();

        applyMatrixTransform();

        FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);

        textVertices.clear();
        currentTextAtlas = atlas;

        float currentX = x;
        float currentY = y + atlas.getAscent();
        int charCount = 0;

        for (int i = 0; i < text.length() && charCount < visibleCharCount; i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                currentX = x;
                currentY += atlas.getLineHeight();
                continue;
            }

            CharacterInfo charInfo = atlas.getCharacter(c);

            if (charInfo.width() > 0 && charInfo.height() > 0) {
                float x1 = currentX + charInfo.xOffset();
                float y1 = currentY + charInfo.yOffset();
                float x2 = x1 + charInfo.width();
                float y2 = y1 + charInfo.height();

                textVertices.add(new TextVertex(x1, y1, charInfo.u1(), charInfo.v1()));
                textVertices.add(new TextVertex(x2, y1, charInfo.u2(), charInfo.v1()));
                textVertices.add(new TextVertex(x2, y2, charInfo.u2(), charInfo.v2()));
                textVertices.add(new TextVertex(x1, y2, charInfo.u1(), charInfo.v2()));
            }

            currentX += charInfo.advance();
            charCount++;
        }

        renderBatchedText(r, g, b, a);
    }

    private void renderBatchedText(float r, float g, float b, float a) {
        if (textVertices.isEmpty()) return;

        // ONE texture bind for entire string
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, currentTextAtlas.getAtlasTextureId());
        glColor4f(r, g, b, a);

        // ONE draw call for entire string
        glBegin(GL_QUADS);
        for (TextVertex vertex : textVertices) {
            glTexCoord2f(vertex.u, vertex.v);
            glVertex2f(vertex.x, vertex.y);
        }
        glEnd();

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }

    public void drawCenteredText(String text, float fontSize, FontResource font) {
        drawCenteredText(text, fontSize, font, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void drawCenteredText(String text, float fontSize, FontResource font, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

        float textWidth = getTextWidth(text, fontSize, font);
        float x = (windowWidth - textWidth) / 2.0f;
        float y = (windowHeight - fontSize) / 2.0f;

        drawText(text, x, y, fontSize, font, r, g, b, a);
    }

    private final Map<String, Float> textWidthCache = new HashMap<>();

    public float getTextWidth(String text, float fontSize, FontResource font) {
        if (text == null || text.isEmpty()) return 0;

        String cacheKey = text + "_" + fontSize + "_" + font.id();
        return textWidthCache.computeIfAbsent(cacheKey, k -> {
            FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);
            float width = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') break;

                CharacterInfo charInfo = atlas.getCharacter(c);
                width += charInfo.advance();
            }

            return width;
        });
    }

    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        flushTextures();

        applyMatrixTransform();

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(r, g, b, a);

        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();

        glDisable(GL_BLEND);
    }

    public void cleanup() {
        LOGGER.info("Cleaning up DrawContext...");

        // Flush any remaining batches
        flushTextures();

        FontAtlasManager.getInstance().cleanup();
        textWidthCache.clear();

        memFree(matrixBuffer);

        LOGGER.debug("DrawContext cleaned up successfully");
    }
}