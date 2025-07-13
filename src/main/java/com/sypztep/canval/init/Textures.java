package com.sypztep.canval.init;

import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.ResourceManager;
import com.sypztep.canval.util.identifier.Registries;
import com.sypztep.canval.util.identifier.Registry;
import com.sypztep.canval.util.identifier.RegistryEntry;
import com.sypztep.canval.util.resource.TextureResource;

import java.util.ArrayList;

public final class Textures {
    public Textures() {
    }

    public static final ArrayList<RegistryEntry<TextureResource>> ENTRIES = new ArrayList<>();

    public static final RegistryEntry<TextureResource> EXAMPLE_PNG = register("example",
            ResourceManager.createTexture(ResourceLocation.of("example.png"), "Example PNG Texture"));

    public static final RegistryEntry<TextureResource> TEST_JPG = register("test",
            ResourceManager.createTexture(ResourceLocation.of("test.jpg"), "Test JPEG Texture"));


    private static RegistryEntry<TextureResource> register(String id, TextureResource texture) {
        RegistryEntry<TextureResource> entry = Registry.registerReference(Registries.TEXTURE, ResourceLocation.of(id), texture);
        ENTRIES.add(entry);
        return entry;
    }

    public static void init() {
        System.out.println("Textures initialized: " + ENTRIES.size() + " textures registered");
    }
}