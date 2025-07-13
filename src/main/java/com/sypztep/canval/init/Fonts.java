package com.sypztep.canval.init;

import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.ResourceManager;
import com.sypztep.canval.util.identifier.Registries;
import com.sypztep.canval.util.identifier.Registry;
import com.sypztep.canval.util.identifier.RegistryEntry;
import com.sypztep.canval.util.resource.FontResource;

public final class Fonts {
    public static final RegistryEntry<FontResource> DEFAULT_FONT = register("default",
            ResourceManager.createFont(
                    ResourceLocation.of("canval", "default_font"),
                    "NotoSansThai-Regular.ttf",
                    "Default Font",
                    16.0f
            )
    );

    private static RegistryEntry<FontResource> register(String id, FontResource font) {
        return Registry.registerReference(Registries.FONT, ResourceLocation.of("canval", id), font);
    }

    public static void init() {
    }
}
