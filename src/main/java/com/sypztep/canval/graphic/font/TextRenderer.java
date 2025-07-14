package com.sypztep.canval.graphic.font;

import com.sypztep.canval.graphic.gl.GlStateManager;
import com.sypztep.canval.util.math.MatrixStack;
import com.sypztep.canval.util.resource.FontResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * High-level text renderer with automatic typewriter effects and text management.
 * No need to manage OpenGL state or font atlases manually.
 */
public class TextRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextRenderer.class);

    private final Map<String, Float> textWidthCache = new HashMap<>();
    private final Map<String, TypewriterState> typewriterStates = new HashMap<>();

    /**
     * Typewriter effect state
     */
    public static class TypewriterState {
        private final String fullText;
        private final float speed; // characters per second
        private float currentTime = 0;
        private int visibleChars = 0;
        private boolean completed = false;

        public TypewriterState(String text, float charactersPerSecond) {
            this.fullText = text;
            this.speed = charactersPerSecond;
        }

        public void update(float deltaTime) {
            if (completed) return;

            currentTime += deltaTime;
            int newVisibleChars = (int)(currentTime * speed);

            if (newVisibleChars > fullText.length()) {
                visibleChars = fullText.length();
                completed = true;
            } else {
                visibleChars = newVisibleChars;
            }
        }

        public String getVisibleText() {
            if (visibleChars >= fullText.length()) {
                return fullText;
            }
            return fullText.substring(0, visibleChars);
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted() {
            completed = true;
            visibleChars = fullText.length();
        }

        public void reset() {
            currentTime = 0;
            visibleChars = 0;
            completed = false;
        }

        public float getProgress() {
            return (float)visibleChars / fullText.length();
        }
    }

    /**
     * Text alignment options
     */
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    /**
     * Draw text with default settings (white color)
     */
    public void drawText(MatrixStack matrices, String text, float x, float y, float fontSize, FontResource font) {
        drawText(matrices, text, x, y, fontSize, font, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Draw text with custom color
     */
    public void drawText(MatrixStack matrices, String text, float x, float y, float fontSize, FontResource font,
                         float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

        GlStateManager.applyMatrix(matrices.peek().getPositionMatrix());
        GlStateManager.prepareTextRender();
        GlStateManager.setColor(r, g, b, a);

        FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);
        GlStateManager.bindTexture(atlas.getAtlasTextureId());

        float currentX = x;
        float currentY = y + atlas.getAscent();

        glBegin(GL_QUADS);
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

                // Quad vertices
                glTexCoord2f(charInfo.u1(), charInfo.v1()); glVertex2f(x1, y1); // Top-left
                glTexCoord2f(charInfo.u2(), charInfo.v1()); glVertex2f(x2, y1); // Top-right
                glTexCoord2f(charInfo.u2(), charInfo.v2()); glVertex2f(x2, y2); // Bottom-right
                glTexCoord2f(charInfo.u1(), charInfo.v2()); glVertex2f(x1, y2); // Bottom-left
            }

            currentX += charInfo.advance();
        }
        glEnd();
    }

    /**
     * Draw centered text
     */
    public void drawCenteredText(MatrixStack matrices, String text, float fontSize, FontResource font) {
        drawCenteredText(matrices, text, fontSize, font, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Draw centered text with custom color
     */
    public void drawCenteredText(MatrixStack matrices, String text, float fontSize, FontResource font,
                                 float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

        float textWidth = getTextWidth(text, fontSize, font);
        float x = (GlStateManager.getViewportWidth() - textWidth) / 2.0f;
        float y = (GlStateManager.getViewportHeight() - fontSize) / 2.0f;

        drawText(matrices, text, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Draw aligned text
     */
    public void drawAlignedText(MatrixStack matrices, String text, float x, float y, float width, float fontSize,
                                FontResource font, Alignment alignment, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

        float textWidth = getTextWidth(text, fontSize, font);
        float drawX = x;

        switch (alignment) {
            case CENTER:
                drawX = x + (width - textWidth) / 2.0f;
                break;
            case RIGHT:
                drawX = x + width - textWidth;
                break;
            case LEFT:
            default:
                break; // Already set to x
        }

        drawText(matrices, text, drawX, y, fontSize, font, r, g, b, a);
    }

    /**
     * Start a typewriter effect for a text ID
     */
    public void startTypewriter(String textId, String text, float charactersPerSecond) {
        typewriterStates.put(textId, new TypewriterState(text, charactersPerSecond));
        LOGGER.debug("Started typewriter effect for '{}': {} chars at {} chars/sec", textId, text.length(), charactersPerSecond);
    }

    /**
     * Update typewriter effects (call this each frame)
     */
    public void updateTypewriters(float deltaTime) {
        for (TypewriterState state : typewriterStates.values()) {
            state.update(deltaTime);
        }
    }

    /**
     * Draw text with typewriter effect
     */
    public void drawTypewriterText(MatrixStack matrices, String textId, float x, float y, float fontSize,
                                   FontResource font, float r, float g, float b, float a) {
        TypewriterState state = typewriterStates.get(textId);
        if (state == null) {
            LOGGER.warn("No typewriter state found for text ID: {}", textId);
            return;
        }

        String visibleText = state.getVisibleText();
        drawText(matrices, visibleText, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Draw typewriter text with default white color
     */
    public void drawTypewriterText(MatrixStack matrices, String textId, float x, float y, float fontSize, FontResource font) {
        drawTypewriterText(matrices, textId, x, y, fontSize, font, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Check if typewriter effect is completed
     */
    public boolean isTypewriterCompleted(String textId) {
        TypewriterState state = typewriterStates.get(textId);
        return state != null && state.isCompleted();
    }

    /**
     * Complete typewriter effect immediately
     */
    public void completeTypewriter(String textId) {
        TypewriterState state = typewriterStates.get(textId);
        if (state != null) {
            state.setCompleted();
        }
    }

    /**
     * Reset typewriter effect
     */
    public void resetTypewriter(String textId) {
        TypewriterState state = typewriterStates.get(textId);
        if (state != null) {
            state.reset();
        }
    }

    /**
     * Get typewriter progress (0.0 to 1.0)
     */
    public float getTypewriterProgress(String textId) {
        TypewriterState state = typewriterStates.get(textId);
        return state != null ? state.getProgress() : 0.0f;
    }

    /**
     * Remove typewriter state
     */
    public void removeTypewriter(String textId) {
        typewriterStates.remove(textId);
    }

    /**
     * Clear all typewriter states
     */
    public void clearTypewriters() {
        typewriterStates.clear();
    }

    /**
     * Draw text with shadow effect
     */
    public void drawTextWithShadow(MatrixStack matrices, String text, float x, float y, float fontSize,
                                   FontResource font, float r, float g, float b, float a) {
        // Draw shadow (offset and darker)
        drawText(matrices, text, x + 1, y + 1, fontSize, font, 0.0f, 0.0f, 0.0f, a * 0.5f);
        // Draw main text
        drawText(matrices, text, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Draw text with outline effect
     */
    public void drawTextWithOutline(MatrixStack matrices, String text, float x, float y, float fontSize,
                                    FontResource font, float r, float g, float b, float a,
                                    float outlineR, float outlineG, float outlineB, float outlineA) {
        // Draw outline in 8 directions
        float offset = 1.0f;
        drawText(matrices, text, x - offset, y - offset, fontSize, font, outlineR, outlineG, outlineB, outlineA);
        drawText(matrices, text, x, y - offset, fontSize, font, outlineR, outlineG, outlineB, outlineA);
        drawText(matrices, text, x + offset, y - offset, fontSize, font, outlineR, outlineG, outlineB, outlineA);
        drawText(matrices, text, x - offset, y, fontSize, font, outlineR, outlineG, outlineB, outlineA);
        drawText(matrices, text, x + offset, y, fontSize, font, outlineR, outlineG, outlineB, outlineA);
        drawText(matrices, text, x - offset, y + offset, fontSize, font, outlineR, outlineG, outlineB, outlineA);
        drawText(matrices, text, x, y + offset, fontSize, font, outlineR, outlineG, outlineB, outlineA);
        drawText(matrices, text, x + offset, y + offset, fontSize, font, outlineR, outlineG, outlineB, outlineA);

        // Draw main text
        drawText(matrices, text, x, y, fontSize, font, r, g, b, a);
    }

    /**
     * Draw multi-line text with line height control
     */
    public void drawMultilineText(MatrixStack matrices, String text, float x, float y, float fontSize,
                                  FontResource font, float lineSpacing, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

        String[] lines = text.split("\n");
        FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);
        float lineHeight = atlas.getLineHeight() * lineSpacing;

        for (int i = 0; i < lines.length; i++) {
            float lineY = y + (i * lineHeight);
            drawText(matrices, lines[i], x, lineY, fontSize, font, r, g, b, a);
        }
    }

    /**
     * Draw text that fits within a width (wrapping)
     */
    public void drawWrappedText(MatrixStack matrices, String text, float x, float y, float maxWidth,
                                float fontSize, FontResource font, float lineSpacing,
                                float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);
        float lineHeight = atlas.getLineHeight() * lineSpacing;
        float currentY = y;

        for (String word : words) {
            String testLine = !currentLine.isEmpty() ? currentLine + " " + word : word;
            float testWidth = getTextWidth(testLine, fontSize, font);

            if (testWidth > maxWidth && !currentLine.isEmpty()) {
                // Draw current line and start new one
                drawText(matrices, currentLine.toString(), x, currentY, fontSize, font, r, g, b, a);
                currentLine = new StringBuilder(word);
                currentY += lineHeight;
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        // Draw the last line
        if (!currentLine.isEmpty()) {
            drawText(matrices, currentLine.toString(), x, currentY, fontSize, font, r, g, b, a);
        }
    }

    /**
     * Get text width (cached for performance)
     */
    public float getTextWidth(String text, float fontSize, FontResource font) {
        if (text == null || text.isEmpty()) return 0;

        String cacheKey = text + "_" + fontSize + "_" + font.id();
        return textWidthCache.computeIfAbsent(cacheKey, k -> {
            FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);
            float width = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') break; // Stop at newline for single line width

                CharacterInfo charInfo = atlas.getCharacter(c);
                width += charInfo.advance();
            }

            return width;
        });
    }

    /**
     * Get text height for given font and size
     */
    public float getTextHeight(float fontSize, FontResource font) {
        FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);
        return atlas.getLineHeight();
    }

    /**
     * Get text height for multi-line text
     */
    public float getMultilineTextHeight(String text, float fontSize, FontResource font, float lineSpacing) {
        if (text == null || text.isEmpty()) return 0;

        int lineCount = text.split("\n").length;
        FontAtlas atlas = FontAtlasManager.getInstance().getAtlas(font, fontSize);
        return lineCount * atlas.getLineHeight() * lineSpacing;
    }

    /**
     * Clear text width cache (call if memory is a concern)
     */
    public void clearCache() {
        textWidthCache.clear();
        LOGGER.debug("Text width cache cleared");
    }

    /**
     * Get cache size for debugging
     */
    public int getCacheSize() {
        return textWidthCache.size();
    }
}