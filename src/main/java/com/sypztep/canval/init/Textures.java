package com.sypztep.canval.init;

import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.ResourceManager;
import com.sypztep.canval.util.identifier.Registries;
import com.sypztep.canval.util.identifier.Registry;
import com.sypztep.canval.util.identifier.RegistryEntry;
import com.sypztep.canval.util.resource.TextureResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public final class Textures {
    private static final Logger LOGGER = LoggerFactory.getLogger(Textures.class);
    public static final ArrayList<RegistryEntry<TextureResource>> ENTRIES = new ArrayList<>();

    public static final RegistryEntry<TextureResource> TEST_RGB = register("rgb", ResourceManager.loadTexture(ResourceLocation.of("rgb.jpg"), "Test JPEG Texture"));

    private static RegistryEntry<TextureResource> register(String id, TextureResource texture) {
        RegistryEntry<TextureResource> entry = Registry.registerReference(Registries.TEXTURE, ResourceLocation.of(id), texture);
        ENTRIES.add(entry);
        return entry;
    }

    public static void loadTextures() {
        LOGGER.info("Loading texture data...");
        try {
            LOGGER.info("Texture data loaded: {} textures", ENTRIES.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load texture data", e);
            throw e;
        }
    }

    public static void bindTextures() {
        if (ENTRIES.isEmpty()) {
            LOGGER.info("No textures to bind");
            return;
        }

        LOGGER.info("Binding {} textures to OpenGL...", ENTRIES.size());

        try {
            for (RegistryEntry<TextureResource> entry : ENTRIES) {
                TextureResource boundTexture = ResourceManager.bindTexture(entry.value());
                Registry.updateReference(Registries.TEXTURE, entry.id(), boundTexture);
            }

            LOGGER.info("All textures bound to OpenGL: {} textures", ENTRIES.size());
        } catch (Exception e) {
            LOGGER.error("Failed to bind textures", e);
            throw e;
        }
    }
}