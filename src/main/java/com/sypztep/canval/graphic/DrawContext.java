package com.sypztep.canval.graphic;

import com.sypztep.canval.graphic.font.TextRenderer;
import com.sypztep.canval.graphic.gl.GlStateManager;
import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.math.MatrixStack;
import com.sypztep.canval.util.resource.FontResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level drawing context that provides easy-to-use drawing methods.
 * This is the main API that users interact with for 2D rendering.
 * No need to manage OpenGL state manually!
 */
public class DrawContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrawContext.class);

    private final RenderSystem renderSystem;
    private final TextRenderer textRenderer;
    private long lastFrameTime = System.nanoTime();
    private float deltaTime = 0.0f;

    public DrawContext(int windowWidth, int windowHeight) {
        // Initialize GL state manager for 2D rendering
        GlStateManager.init2D(windowWidth, windowHeight);

        this.renderSystem = new RenderSystem();
        this.textRenderer = new TextRenderer();

        LOGGER.debug("DrawContext created: {}x{}", windowWidth, windowHeight);
    }

    /**
     * Get the matrix stack for transformations
     */
    public MatrixStack getMatrix() {
        return renderSystem.getMatrixStack();
    }

    /**
     * Begin a new frame
     */
    public void beginFrame() {
        // Calculate delta time
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = currentTime;

        renderSystem.beginFrame();
        textRenderer.updateTypewriters(deltaTime);
    }

    /**
     * End the current frame
     */
    public void endFrame() {
        renderSystem.endFrame();
    }

    /**
     * Get delta time in seconds
     */
    public float getDeltaTime() {
        return deltaTime;
    }

    // =================== TEXTURE DRAWING ===================

    /**
     * Draw a texture at specified position with original size
     */
    public void drawTexture(ResourceLocation texture, float x, float y) {
        renderSystem.drawTexture(texture, x, y, 0, 0); // Will use original size
    }

    /**
     * Draw a texture at specified position and size
     */
    public void drawTexture(ResourceLocation texture, float x, float y, float width, float height) {
        renderSystem.drawTexture(texture, x, y, width, height);
    }

    /**
     * Draw a region from a texture (sprite sheet support)
     */
    public void drawTextureRegion(ResourceLocation texture, float x, float y, float width, float height,
                                  float u, float v, float regionWidth, float regionHeight) {
        renderSystem.drawTextureRegion(texture, x, y, width, height, u, v, regionWidth, regionHeight, 256, 256);
    }

    /**
     * Draw a texture region with explicit texture dimensions
     */
    public void drawTextureRegion(ResourceLocation texture, float x, float y, float width, float height,
                                  float u, float v, float regionWidth, float regionHeight,
                                  float textureWidth, float textureHeight) {
        renderSystem.drawTextureRegion(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    // =================== SHAPE DRAWING ===================

    /**
     * Draw a colored rectangle
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        renderSystem.drawRect(x, y, width, height, r, g, b, a);
    }

    /**
     * Draw a rectangle with ARGB color (0xAARRGGBB format)
     */
    public void drawRect(float x, float y, float width, float height, int color) {
        renderSystem.drawRect(x, y, width, height, color);
    }

    /**
     * Draw a white rectangle
     */
    public void drawWhiteRect(float x, float y, float width, float height) {
        renderSystem.drawWhiteRect(x, y, width, height);
    }

    /**
     * Draw a black rectangle
     */
    public void drawBlackRect(float x, float y, float width, float height) {
        renderSystem.drawBlackRect(x, y, width, height);
    }

    /**
     * Draw a rectangle outline
     */
    public void drawRectOutline(float x, float y, float width, float height, float lineWidth,
                                float r, float g, float b, float a) {
        renderSystem.drawRectOutline(x, y, width, height, lineWidth, r, g, b, a);
    }

    /**
     * Draw a rectangle outline with ARGB color
     */
    public void drawRectOutline(float x, float y, float width, float height, float lineWidth, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        drawRectOutline(x, y, width, height, lineWidth, r, g, b, a);
    }

    // =================== TEXT DRAWING ===================

    /**
     * Draw text with default white color
     */
    public void drawText(String text, float x, float y, float fontSize, FontResource font) {
        textRenderer.drawText(getMatrix(), text, x, y, fontSize, font);
    }

    /**
     * Draw text with custom color
     */
    public void drawText(String text, float x, float y, float fontSize, FontResource font,
                         float r, float g, float b, float a) {
        textRenderer.drawText(getMatrix(), text, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Draw text with ARGB color
     */
    public void drawText(String text, float x, float y, float fontSize, FontResource font, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        drawText(text, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Draw centered text
     */
    public void drawCenteredText(String text, float fontSize, FontResource font) {
        textRenderer.drawCenteredText(getMatrix(), text, fontSize, font);
    }

    /**
     * Draw centered text with custom color
     */
    public void drawCenteredText(String text, float fontSize, FontResource font, float r, float g, float b, float a) {
        textRenderer.drawCenteredText(getMatrix(), text, fontSize, font, r, g, b, a);
    }

    /**
     * Draw text with shadow
     */
    public void drawTextWithShadow(String text, float x, float y, float fontSize, FontResource font) {
        textRenderer.drawTextWithShadow(getMatrix(), text, x, y, fontSize, font, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Draw text with shadow and custom color
     */
    public void drawTextWithShadow(String text, float x, float y, float fontSize, FontResource font,
                                   float r, float g, float b, float a) {
        textRenderer.drawTextWithShadow(getMatrix(), text, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Draw text with outline
     */
    public void drawTextWithOutline(String text, float x, float y, float fontSize, FontResource font,
                                    float r, float g, float b, float a) {
        textRenderer.drawTextWithOutline(getMatrix(), text, x, y, fontSize, font, r, g, b, a, 0.0f, 0.0f, 0.0f, 1.0f);
    }

    /**
     * Draw multi-line text
     */
    public void drawMultilineText(String text, float x, float y, float fontSize, FontResource font) {
        textRenderer.drawMultilineText(getMatrix(), text, x, y, fontSize, font, 1.2f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Draw multi-line text with custom line spacing and color
     */
    public void drawMultilineText(String text, float x, float y, float fontSize, FontResource font,
                                  float lineSpacing, float r, float g, float b, float a) {
        textRenderer.drawMultilineText(getMatrix(), text, x, y, fontSize, font, lineSpacing, r, g, b, a);
    }

    /**
     * Draw wrapped text (automatically breaks lines to fit width)
     */
    public void drawWrappedText(String text, float x, float y, float maxWidth, float fontSize, FontResource font) {
        textRenderer.drawWrappedText(getMatrix(), text, x, y, maxWidth, fontSize, font, 1.2f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    // =================== TYPEWRITER EFFECTS ===================

    /**
     * Start a typewriter effect
     */
    public void startTypewriter(String textId, String text, float charactersPerSecond) {
        textRenderer.startTypewriter(textId, text, charactersPerSecond);
    }

    /**
     * Start a typewriter effect with default speed (30 chars/sec)
     */
    public void startTypewriter(String textId, String text) {
        startTypewriter(textId, text, 30.0f);
    }

    /**
     * Draw typewriter text
     */
    public void drawTypewriterText(String textId, float x, float y, float fontSize, FontResource font) {
        textRenderer.drawTypewriterText(getMatrix(), textId, x, y, fontSize, font);
    }

    /**
     * Draw typewriter text with custom color
     */
    public void drawTypewriterText(String textId, float x, float y, float fontSize, FontResource font,
                                   float r, float g, float b, float a) {
        textRenderer.drawTypewriterText(getMatrix(), textId, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Check if typewriter is completed
     */
    public boolean isTypewriterCompleted(String textId) {
        return textRenderer.isTypewriterCompleted(textId);
    }

    /**
     * Complete typewriter immediately
     */
    public void completeTypewriter(String textId) {
        textRenderer.completeTypewriter(textId);
    }

    /**
     * Reset typewriter
     */
    public void resetTypewriter(String textId) {
        textRenderer.resetTypewriter(textId);
    }

    /**
     * Get typewriter progress (0.0 to 1.0)
     */
    public float getTypewriterProgress(String textId) {
        return textRenderer.getTypewriterProgress(textId);
    }

    // =================== TEXT MEASUREMENT ===================

    /**
     * Get text width
     */
    public float getTextWidth(String text, float fontSize, FontResource font) {
        return textRenderer.getTextWidth(text, fontSize, font);
    }

    /**
     * Get text height
     */
    public float getTextHeight(float fontSize, FontResource font) {
        return textRenderer.getTextHeight(fontSize, font);
    }

    // =================== TRANSFORMATIONS ===================

    /**
     * Push matrix (save current transform)
     */
    public void push() {
        renderSystem.pushMatrix();
    }

    /**
     * Pop matrix (restore previous transform)
     */
    public void pop() {
        renderSystem.popMatrix();
    }

    /**
     * Translate
     */
    public void translate(float x, float y) {
        renderSystem.translate(x, y);
    }

    /**
     * Scale
     */
    public void scale(float factor) {
        renderSystem.scale(factor);
    }

    /**
     * Scale with different X and Y factors
     */
    public void scale(float x, float y) {
        renderSystem.scale(x, y, 1.0f);
    }

    /**
     * Rotate around Z axis (2D rotation)
     */
    public void rotate(float angle) {
        renderSystem.rotateZ(angle);
    }

    // =================== UTILITY METHODS ===================

    /**
     * Get screen width
     */
    public int getScreenWidth() {
        return renderSystem.getScreenWidth();
    }

    /**
     * Get screen height
     */
    public int getScreenHeight() {
        return renderSystem.getScreenHeight();
    }

    /**
     * Force flush all rendering batches
     */
    public void flush() {
        renderSystem.flush();
    }

    /**
     * Update viewport size (call when window is resized)
     */
    public void updateViewport(int width, int height) {
        GlStateManager.updateViewport(width, height);
    }

    //TODO: Hardcode for Opacity
    public void drawWithOpacity(float opacity, Runnable drawCode) {
        push();
        drawCode.run();
        pop();
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        LOGGER.info("Cleaning up DrawContext...");

        textRenderer.clearCache();
        textRenderer.clearTypewriters();
        GlStateManager.cleanup();

        LOGGER.debug("DrawContext cleaned up successfully");
    }
}