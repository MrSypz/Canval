package com.sypztep.canval.graphic.gl;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * High-level OpenGL state manager that automatically handles state changes
 * and reduces redundant OpenGL calls.
 */
public class GlStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlStateManager.class);

    // State tracking
    private static boolean blendEnabled = false;
    private static int blendSrcFactor = -1;
    private static int blendDstFactor = -1;
    private static boolean textureEnabled = false;
    private static int boundTexture = -1;
    private static float[] currentColor = {1.0f, 1.0f, 1.0f, 1.0f};
    private static boolean projectionSet = false;
    private static boolean matrixDirty = true;

    // Matrix handling
    private static final FloatBuffer matrixBuffer = memAllocFloat(16);
    private static final Matrix4f lastAppliedMatrix = new Matrix4f();

    // Viewport
    private static int viewportWidth = 0;
    private static int viewportHeight = 0;

    /**
     * Initialize the state manager for 2D rendering
     */
    public static void init2D(int windowWidth, int windowHeight) {
        viewportWidth = windowWidth;
        viewportHeight = windowHeight;

        // Set up 2D projection matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
        projectionSet = true;

        // Default 2D settings
        enableBlend();
        setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        disableDepthTest();

        LOGGER.info("GlStateManager initialized for 2D rendering: {}x{}", windowWidth, windowHeight);
    }

    /**
     * Begin a new frame - automatically sets up 2D projection if needed
     */
    public static void beginFrame() {
        if (!projectionSet) {
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, viewportWidth, viewportHeight, 0, -1, 1);
            projectionSet = true;
        }
        matrixDirty = true;
    }

    /**
     * End the current frame
     */
    public static void endFrame() {
        matrixDirty = true; // Reset some states for next frame
    }

    /**
     * Apply matrix transformation - only when needed
     */
    public static void applyMatrix(Matrix4f matrix) {
        glMatrixMode(GL_MODELVIEW);

        if (matrixDirty || !matrix.equals(lastAppliedMatrix)) {
            matrix.get(matrixBuffer);
            glLoadMatrixf(matrixBuffer);
            lastAppliedMatrix.set(matrix);
            matrixDirty = false;
        }
    }

    /**
     * Mark matrix as dirty - will be reapplied next draw call
     */
    public static void markMatrixDirty() {
        matrixDirty = true;
    }

    /**
     * Enable blending with automatic state tracking
     */
    public static void enableBlend() {
        if (!blendEnabled) {
            glEnable(GL_BLEND);
            blendEnabled = true;
        }
    }

    /**
     * Disable blending with automatic state tracking
     */
    public static void disableBlend() {
        if (blendEnabled) {
            glDisable(GL_BLEND);
            blendEnabled = false;
        }
    }

    /**
     * Set blend function - only if different from current
     */
    public static void setBlendFunc(int srcFactor, int dstFactor) {
        if (blendSrcFactor != srcFactor || blendDstFactor != dstFactor) {
            glBlendFunc(srcFactor, dstFactor);
            blendSrcFactor = srcFactor;
            blendDstFactor = dstFactor;
        }
    }

    /**
     * Enable 2D texturing with automatic state tracking
     */
    public static void enableTexture2D() {
        if (!textureEnabled) {
            glEnable(GL_TEXTURE_2D);
            textureEnabled = true;
        }
    }

    /**
     * Disable 2D texturing with automatic state tracking
     */
    public static void disableTexture2D() {
        if (textureEnabled) {
            glDisable(GL_TEXTURE_2D);
            textureEnabled = false;
        }
    }

    /**
     * Bind texture - only if different from current
     */
    public static void bindTexture(int textureId) {
        if (boundTexture != textureId) {
            glBindTexture(GL_TEXTURE_2D, textureId);
            boundTexture = textureId;
        }
    }

    /**
     * Set color - only if different from current
     */
    public static void setColor(float r, float g, float b, float a) {
        if (currentColor[0] != r || currentColor[1] != g ||
                currentColor[2] != b || currentColor[3] != a) {
            glColor4f(r, g, b, a);
            currentColor[0] = r;
            currentColor[1] = g;
            currentColor[2] = b;
            currentColor[3] = a;
        }
    }

    /**
     * Set white color (common for textured rendering)
     */
    public static void setColorWhite() {
        setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Disable depth testing (common for 2D)
     */
    public static void disableDepthTest() {
        glDisable(GL_DEPTH_TEST);
    }

    /**
     * Enable depth testing
     */
    public static void enableDepthTest() {
        glEnable(GL_DEPTH_TEST);
    }

    /**
     * Prepare for texture rendering
     */
    public static void prepareTextureRender() {
        enableTexture2D();
        enableBlend();
        setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        setColorWhite();
    }

    /**
     * Prepare for colored shape rendering (no texture)
     */
    public static void prepareColoredRender() {
        disableTexture2D();
        enableBlend();
        setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Prepare for text rendering
     */
    public static void prepareTextRender() {
        enableTexture2D();
        enableBlend();
        setBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Reset all state tracking (call when context changes)
     */
    public static void resetStateTracking() {
        blendEnabled = false;
        blendSrcFactor = -1;
        blendDstFactor = -1;
        textureEnabled = false;
        boundTexture = -1;
        currentColor[0] = currentColor[1] = currentColor[2] = currentColor[3] = 1.0f;
        projectionSet = false;
        matrixDirty = true;
        lastAppliedMatrix.identity();
    }

    /**
     * Get current viewport width
     */
    public static int getViewportWidth() {
        return viewportWidth;
    }

    /**
     * Get current viewport height
     */
    public static int getViewportHeight() {
        return viewportHeight;
    }

    /**
     * Update viewport size
     */
    public static void updateViewport(int width, int height) {
        if (viewportWidth != width || viewportHeight != height) {
            viewportWidth = width;
            viewportHeight = height;
            projectionSet = false; // Force projection matrix update

            glViewport(0, 0, width, height);
            LOGGER.debug("Viewport updated: {}x{}", width, height);
        }
    }

    /**
     * Cleanup resources
     */
    public static void cleanup() {
        memFree(matrixBuffer);
        LOGGER.info("GlStateManager cleaned up");
    }
}