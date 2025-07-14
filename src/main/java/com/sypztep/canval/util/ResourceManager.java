package com.sypztep.canval.util;

import com.sypztep.canval.util.resource.*;
import org.lwjgl.opengl.GL12;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public final class ResourceManager {
    private static final Map<ResourceLocation, ByteBuffer> fontCache = new HashMap<>();
    private static final String ASSETS_PATH = "/assets/";
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);

    /**
     * Load texture - Your working direct approach
     */
    public static TextureResource loadTexture(ResourceLocation location, String displayName) {
        String resourcePath = ASSETS_PATH + "textures/" + location.path();

        LOGGER.info("Loading texture: {}", resourcePath);

        try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Texture resource not found: " + resourcePath);
            }

            byte[] imageData = inputStream.readAllBytes();
            if (imageData.length == 0) {
                throw new RuntimeException("Empty image file: " + resourcePath);
            }

            ByteBuffer imageBuffer = MemoryUtil.memAlloc(imageData.length);
            imageBuffer.put(imageData);
            imageBuffer.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                // Validate image format
                if (!STBImage.stbi_info_from_memory(imageBuffer, width, height, channels)) {
                    String error = STBImage.stbi_failure_reason();
                    MemoryUtil.memFree(imageBuffer);
                    throw new RuntimeException("Invalid image format for " + location.path() + ": " + error);
                }

                imageBuffer.rewind();

                // Force RGBA (4 channels) for consistency
                ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);

                if (image == null) {
                    String error = STBImage.stbi_failure_reason();
                    MemoryUtil.memFree(imageBuffer);
                    throw new RuntimeException("Failed to load image " + location.path() + ": " + error);
                }

                int w = width.get(0);
                int h = height.get(0);

                LOGGER.info("Successfully loaded image {}: {}x{} format RGBA", location.path(), w, h);

                // Create our own copy
                int imageSize = w * h * 4; // Always RGBA
                ByteBuffer ownedImageData = MemoryUtil.memAlloc(imageSize);
                ownedImageData.put(0, image, 0, imageSize);
                ownedImageData.rewind();

                // Free STB resources
                STBImage.stbi_image_free(image);
                MemoryUtil.memFree(imageBuffer);

                return new TextureResource(location, ownedImageData, w, h, displayName,
                        TextureResource.TextureFormat.RGBA);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: " + location.path(), e);
        }
    }

    /**
     * Bind texture to OpenGL - Your working approach
     */
    public static TextureResource bindTexture(TextureResource texture) {
        if (texture.isBound()) {
            return texture;
        }

        LOGGER.info("Binding texture to OpenGL: {}", texture.id());

        try {
            int textureId = GL12.glGenTextures();
            GL12.glBindTexture(GL12.GL_TEXTURE_2D, textureId);

            // Set texture parameters
            GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_FILTER, GL12.GL_NEAREST);
            GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAG_FILTER, GL12.GL_NEAREST);
            GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL12.glTexParameteri(GL12.GL_TEXTURE_2D, GL12.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

            // Upload texture data (always RGBA now)
            GL12.glTexImage2D(GL12.GL_TEXTURE_2D, 0, GL12.GL_RGBA8, texture.width(), texture.height(),
                    0, GL12.GL_RGBA, GL12.GL_UNSIGNED_BYTE, texture.imageData());

            int error = GL12.glGetError();
            if (error != GL12.GL_NO_ERROR) {
                GL12.glDeleteTextures(textureId);
                throw new RuntimeException("OpenGL error: " + error);
            }

            LOGGER.info("Successfully bound texture {} to OpenGL ID {}", texture.id(), textureId);
            return texture.withGLTexture(textureId);

        } catch (Exception e) {
            LOGGER.error("Failed to bind texture {}", texture.id(), e);
            throw new RuntimeException("Failed to bind texture: " + texture.id(), e);
        }
    }

    public static FontResource createFont(ResourceLocation location, String displayName, float defaultSize) {
        try {
            String resourcePath = ASSETS_PATH + "fonts/" + location.path();

            try (InputStream inputStream = ResourceManager.class.getResourceAsStream(resourcePath)) {
                if (inputStream == null)
                    throw new RuntimeException("Font resource not found: " + resourcePath);

                LOGGER.info("Loading font resource: {}", resourcePath);
                byte[] fontData = inputStream.readAllBytes();

                ByteBuffer fontBuffer = MemoryUtil.memAlloc(fontData.length);
                fontBuffer.put(fontData).flip();

                STBTTFontinfo fontInfo = STBTTFontinfo.create();
                if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
                    MemoryUtil.memFree(fontBuffer);
                    throw new RuntimeException("Failed to initialize font: " + location.path());
                }

                fontCache.put(location, fontBuffer);
                return new FontResource(location, fontBuffer, fontInfo, displayName, defaultSize);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + location.path(), e);
        }
    }

    public static void cleanup() {
        LOGGER.info("Cleaning up ResourceManager...");
        fontCache.values().forEach(MemoryUtil::memFree);
        fontCache.clear();
    }
}