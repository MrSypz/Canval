package com.sypztep.canval.init;

import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.ResourceManager;
import com.sypztep.canval.util.identifier.Registries;
import com.sypztep.canval.util.identifier.Registry;
import com.sypztep.canval.util.identifier.RegistryEntry;
import com.sypztep.canval.util.resource.FontResource;

import java.util.ArrayList;

public final class Fonts {
    public Fonts() {
    }

    public static final ArrayList<RegistryEntry<FontResource>> ENTRIES = new ArrayList<>();

    public static final RegistryEntry<FontResource> DEFAULT_FONT = register("thai", ResourceManager.createFont(ResourceLocation.of("NotoSansThai-Regular.ttf"),
            "Thai Font", 16.0f));

    private static RegistryEntry<FontResource> register(String id, FontResource font) {
        RegistryEntry<FontResource> entry = Registry.registerReference(Registries.FONT, ResourceLocation.of(id), font);
        ENTRIES.add(entry);
        return entry;
    }

    public static void init() {
        System.out.println("Fonts initialized: " + ENTRIES.size() + " fonts registered");
    }
}