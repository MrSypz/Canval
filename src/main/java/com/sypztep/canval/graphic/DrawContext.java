package com.sypztep.canval.graphic;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class DrawContext {
    private int windowWidth;
    private int windowHeight;

    public DrawContext(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public void drawText(String text, float x, float y, float fontSize, STBTTFontinfo font) {
        if (text == null || text.isEmpty()) return;

        // Set up orthographic projection
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Calculate scale based on font size
        float scale = stbtt_ScaleForPixelHeight(font, fontSize);

        try (MemoryStack stack = stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            IntBuffer descent = stack.mallocInt(1);
            IntBuffer lineGap = stack.mallocInt(1);

            stbtt_GetFontVMetrics(font, ascent, descent, lineGap);

            float currentX = x;
            float currentY = y + ascent.get(0) * scale;

            // Render each character
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '\n') {
                    currentX = x;
                    currentY += (ascent.get(0) - descent.get(0) + lineGap.get(0)) * scale;
                    continue;
                }

                renderCharacter(font, c, currentX, currentY, scale, stack);

                // Advance cursor
                IntBuffer advanceWidth = stack.mallocInt(1);
                IntBuffer leftSideBearing = stack.mallocInt(1);
                stbtt_GetCodepointHMetrics(font, c, advanceWidth, leftSideBearing);
                currentX += advanceWidth.get(0) * scale;
            }
        }

        // Restore matrices
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private void renderCharacter(STBTTFontinfo font, char c, float x, float y, float scale, MemoryStack stack) {
        IntBuffer width = stack.mallocInt(1);
        IntBuffer height = stack.mallocInt(1);
        IntBuffer xOffset = stack.mallocInt(1);
        IntBuffer yOffset = stack.mallocInt(1);

        ByteBuffer bitmap = stbtt_GetCodepointBitmap(font, 0, scale, c, width, height, xOffset, yOffset);

        if (bitmap != null) {
            int w = width.get(0);
            int h = height.get(0);
            int xOff = xOffset.get(0);
            int yOff = yOffset.get(0);

            if (w > 0 && h > 0) {
                // Create texture
                int texture = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texture);

                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

                // Convert single channel to RGBA
                ByteBuffer rgbaBuffer = MemoryUtil.memAlloc(w * h * 4);
                for (int i = 0; i < w * h; i++) {
                    byte alpha = bitmap.get(i);
                    rgbaBuffer.put((byte) 255); // R
                    rgbaBuffer.put((byte) 255); // G
                    rgbaBuffer.put((byte) 255); // B
                    rgbaBuffer.put(alpha);      // A
                }
                rgbaBuffer.flip();

                // Upload as RGBA texture
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgbaBuffer);

                // Free the RGBA buffer
                MemoryUtil.memFree(rgbaBuffer);

                // Save current state
                glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT);

                // Enable texture and blending
                glEnable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                // Set color to white
                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

                float x1 = x + xOff;
                float y1 = y + yOff;
                float x2 = x1 + w;
                float y2 = y1 + h;

                glBegin(GL_QUADS);
                glTexCoord2f(0, 0); glVertex2f(x1, y1);
                glTexCoord2f(1, 0); glVertex2f(x2, y1);
                glTexCoord2f(1, 1); glVertex2f(x2, y2);
                glTexCoord2f(0, 1); glVertex2f(x1, y2);
                glEnd();

                // Restore state
                glPopAttrib();
                glDeleteTextures(texture);
            }

            // Free the bitmap
            stbtt_FreeBitmap(bitmap);
        }
    }

    public void drawCenteredText(String text, float fontSize, STBTTFontinfo font) {
        if (text == null || text.isEmpty()) return;

        // Calculate text dimensions
        float textWidth = getTextWidth(text, fontSize, font);
        float textHeight = fontSize;

        // Center the text
        float x = (windowWidth - textWidth) / 2.0f;
        float y = (windowHeight - textHeight) / 2.0f;

        drawText(text, x, y, fontSize, font);
    }

    private float getTextWidth(String text, float fontSize, STBTTFontinfo font) {
        float scale = stbtt_ScaleForPixelHeight(font, fontSize);
        float width = 0;

        try (MemoryStack stack = stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer leftSideBearing = stack.mallocInt(1);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') break; // Only measure first line for centering

                stbtt_GetCodepointHMetrics(font, c, advanceWidth, leftSideBearing);
                width += advanceWidth.get(0) * scale;
            }
        }

        return width;
    }
    /**
     * Draw a solid colored rectangle
     * @param x X position
     * @param y Y position
     * @param width Width of rectangle
     * @param height Height of rectangle
     * @param r Red component (0.0 to 1.0)
     * @param g Green component (0.0 to 1.0)
     * @param b Blue component (0.0 to 1.0)
     * @param a Alpha component (0.0 to 1.0)
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        // Set up orthographic projection
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        // Save current state
        glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT);

        // Disable texture and enable blending for transparency
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Set color
        glColor4f(r, g, b, a);

        // Draw rectangle
        glBegin(GL_QUADS);
        glVertex2f(x, y);                    // Top-left
        glVertex2f(x + width, y);            // Top-right
        glVertex2f(x + width, y + height);   // Bottom-right
        glVertex2f(x, y + height);           // Bottom-left
        glEnd();

        // Restore state
        glPopAttrib();

        // Restore matrices
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    /**
     * Draw a solid colored rectangle with RGB (fully opaque)
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b) {
        drawRect(x, y, width, height, r, g, b, 1.0f);
    }
}