package com.sypztep.canval.graphic;

import com.sypztep.canval.graphic.font.CharacterInfo;
import com.sypztep.canval.graphic.font.FontAtlas;
import com.sypztep.canval.graphic.font.FontAtlasManager;
import com.sypztep.canval.util.math.MatrixStack;
import com.sypztep.canval.util.resource.FontResource;
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

    // Matrix optimization
    private final FloatBuffer matrixBuffer = memAllocFloat(16);
    private final Matrix4f lastAppliedMatrix = new Matrix4f();
    private boolean matrixDirty = true;

    // Batched rendering for text
    private static class TextVertex {
        float x, y, u, v;
        TextVertex(float x, float y, float u, float v) {
            this.x = x; this.y = y; this.u = u; this.v = v;
        }
    }

    private final List<TextVertex> textVertices = new ArrayList<>();
    private FontAtlas currentTextAtlas;

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

    public void drawText(String text, float x, float y, float fontSize, FontResource font) {
        drawText(text, x, y, fontSize, font, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void drawText(String text, float x, float y, float fontSize, FontResource font, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;

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
        FontAtlasManager.getInstance().cleanup();
        textWidthCache.clear();

        if (matrixBuffer != null) memFree(matrixBuffer);


        LOGGER.debug("DrawContext cleaned up successfully");
    }
}