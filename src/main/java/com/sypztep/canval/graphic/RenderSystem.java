package com.sypztep.canval.graphic;

import com.sypztep.canval.graphic.gl.GlStateManager;
import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.identifier.Registries;
import com.sypztep.canval.util.math.MatrixStack;
import com.sypztep.canval.util.resource.TextureResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * High-level rendering system that handles batching and automatic state management.
 * Users don't need to worry about OpenGL state - just call draw methods.
 */
public class RenderSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderSystem.class);

    private final MatrixStack matrices = new MatrixStack();

    // Batched rendering
    private final List<TexturedQuad> textureQuads = new ArrayList<>();
    private final List<ColoredQuad> coloredQuads = new ArrayList<>();
    private int currentTextureId = -1;

    // Rendering data structures
    private static class TexturedQuad {
        float x1, y1, x2, y2, z;
        float u1, v1, u2, v2;
        int textureId;

        TexturedQuad(float x1, float y1, float x2, float y2, float z,
                     float u1, float v1, float u2, float v2, int textureId) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.z = z;
            this.u1 = u1; this.v1 = v1; this.u2 = u2; this.v2 = v2;
            this.textureId = textureId;
        }
    }

    private static class ColoredQuad {
        float x1, y1, x2, y2;
        float r, g, b, a;

        ColoredQuad(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.r = r; this.g = g; this.b = b; this.a = a;
        }
    }

    public RenderSystem() {
        LOGGER.debug("RenderSystem created");
    }

    /**
     * Get the matrix stack for transformations
     */
    public MatrixStack getMatrixStack() {
        return matrices;
    }

    /**
     * Begin a rendering frame
     */
    public void beginFrame() {
        GlStateManager.beginFrame();
        GlStateManager.markMatrixDirty();
    }

    /**
     * End a rendering frame - flushes all batches
     */
    public void endFrame() {
        flushAllBatches();
        GlStateManager.endFrame();
    }

    /**
     * Draw a texture at specified position and size
     */
    public void drawTexture(ResourceLocation texture, float x, float y, float width, float height) {
        TextureResource textureResource = getTextureResource(texture);
        if (textureResource == null) return;

        drawTextureRegion(texture, x, y, width, height,
                0, 0, textureResource.width(), textureResource.height(),
                textureResource.width(), textureResource.height());
    }

    /**
     * Draw a texture region
     */
    public void drawTextureRegion(ResourceLocation texture, float x, float y, float displayWidth, float displayHeight,
                                  float u, float v, float regionWidth, float regionHeight,
                                  float textureWidth, float textureHeight) {
        TextureResource textureResource = getTextureResource(texture);
        if (textureResource == null) return;

        // Calculate UV coordinates
        float u1 = u / textureWidth;
        float v1 = v / textureHeight;
        float u2 = (u + regionWidth) / textureWidth;
        float v2 = (v + regionHeight) / textureHeight;

        // Add to batch
        addTexturedQuad(x, y, x + displayWidth, y + displayHeight, 0,
                u1, v1, u2, v2, textureResource.glTextureId());
    }

    /**
     * Draw a colored rectangle
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        addColoredQuad(x, y, x + width, y + height, r, g, b, a);
    }

    /**
     * Draw an outline rectangle
     */
    public void drawRectOutline(float x, float y, float width, float height, float lineWidth,
                                float r, float g, float b, float a) {
        // Top
        drawRect(x, y, width, lineWidth, r, g, b, a);
        // Bottom
        drawRect(x, y + height - lineWidth, width, lineWidth, r, g, b, a);
        // Left
        drawRect(x, y + lineWidth, lineWidth, height - 2 * lineWidth, r, g, b, a);
        // Right
        drawRect(x + width - lineWidth, y + lineWidth, lineWidth, height - 2 * lineWidth, r, g, b, a);
    }

    /**
     * Convenience methods for common colors
     */
    public void drawRect(float x, float y, float width, float height, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        drawRect(x, y, width, height, r, g, b, a);
    }

    public void drawWhiteRect(float x, float y, float width, float height) {
        drawRect(x, y, width, height, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void drawBlackRect(float x, float y, float width, float height) {
        drawRect(x, y, width, height, 0.0f, 0.0f, 0.0f, 1.0f);
    }

    /**
     * Add a textured quad to the batch
     */
    private void addTexturedQuad(float x1, float y1, float x2, float y2, float z,
                                 float u1, float v1, float u2, float v2, int textureId) {
        // If texture changes, flush current batch
        if (currentTextureId != -1 && currentTextureId != textureId) {
            flushTextureBatch();
        }

        currentTextureId = textureId;
        textureQuads.add(new TexturedQuad(x1, y1, x2, y2, z, u1, v1, u2, v2, textureId));
    }

    /**
     * Add a colored quad to the batch
     */
    private void addColoredQuad(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        // Flush textures first if any
        if (!textureQuads.isEmpty()) {
            flushTextureBatch();
        }

        coloredQuads.add(new ColoredQuad(x1, y1, x2, y2, r, g, b, a));
    }

    /**
     * Flush all batches
     */
    public void flushAllBatches() {
        flushTextureBatch();
        flushColoredBatch();
    }

    /**
     * Flush texture batch
     */
    private void flushTextureBatch() {
        if (textureQuads.isEmpty()) return;

        GlStateManager.applyMatrix(matrices.peek().getPositionMatrix());
        GlStateManager.prepareTextureRender();
        GlStateManager.bindTexture(currentTextureId);

        glBegin(GL_QUADS);
        for (TexturedQuad quad : textureQuads) {
            // Bottom-left
            glTexCoord2f(quad.u1, quad.v2);
            glVertex3f(quad.x1, quad.y2, quad.z);

            // Bottom-right
            glTexCoord2f(quad.u2, quad.v2);
            glVertex3f(quad.x2, quad.y2, quad.z);

            // Top-right
            glTexCoord2f(quad.u2, quad.v1);
            glVertex3f(quad.x2, quad.y1, quad.z);

            // Top-left
            glTexCoord2f(quad.u1, quad.v1);
            glVertex3f(quad.x1, quad.y1, quad.z);
        }
        glEnd();

        textureQuads.clear();
        currentTextureId = -1;
    }

    /**
     * Flush colored batch
     */
    private void flushColoredBatch() {
        if (coloredQuads.isEmpty()) return;

        GlStateManager.applyMatrix(matrices.peek().getPositionMatrix());
        GlStateManager.prepareColoredRender();

        glBegin(GL_QUADS);
        for (ColoredQuad quad : coloredQuads) {
            GlStateManager.setColor(quad.r, quad.g, quad.b, quad.a);

            glVertex2f(quad.x1, quad.y1); // Top-left
            glVertex2f(quad.x2, quad.y1); // Top-right
            glVertex2f(quad.x2, quad.y2); // Bottom-right
            glVertex2f(quad.x1, quad.y2); // Bottom-left
        }
        glEnd();

        coloredQuads.clear();
    }

    /**
     * Helper to get texture resource safely
     */
    private TextureResource getTextureResource(ResourceLocation texture) {
        try {
            TextureResource textureResource = Registries.TEXTURE.get(texture);
            if (!textureResource.isBound()) {
                LOGGER.warn("Texture not bound to OpenGL: {}", texture);
                return null;
            }
            return textureResource;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Texture not found: {}", texture);
            return null;
        }
    }

    /**
     * Force flush - useful for ensuring draw order
     */
    public void flush() {
        flushAllBatches();
        GlStateManager.markMatrixDirty();
    }

    /**
     * Convenience methods for matrix operations
     */
    public void pushMatrix() {
        matrices.push();
    }

    public void popMatrix() {
        matrices.pop();
        GlStateManager.markMatrixDirty();
    }

    public void translate(float x, float y, float z) {
        matrices.translate(x, y, z);
        GlStateManager.markMatrixDirty();
    }

    public void translate(float x, float y) {
        translate(x, y, 0);
    }

    public void scale(float x, float y, float z) {
        matrices.scale(x, y, z);
        GlStateManager.markMatrixDirty();
    }

    public void scale(float factor) {
        scale(factor, factor, factor);
    }

    public void rotateZ(float angle) {
        matrices.rotateZ(angle);
        GlStateManager.markMatrixDirty();
    }

    /**
     * Get screen dimensions
     */
    public int getScreenWidth() {
        return GlStateManager.getViewportWidth();
    }

    public int getScreenHeight() {
        return GlStateManager.getViewportHeight();
    }
}