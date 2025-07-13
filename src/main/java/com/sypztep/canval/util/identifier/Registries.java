package com.sypztep.canval.util.identifier;

import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.resource.FontResource;
import com.sypztep.canval.util.resource.SoundResource;
import com.sypztep.canval.util.resource.TextureResource;

public final class Registries {
    public static final Registry<FontResource> FONT = new Registry<>("font", FontResource.class);
    public static final Registry<TextureResource> TEXTURE = new Registry<>("texture", TextureResource.class);
    public static final Registry<SoundResource> SOUND = new Registry<>("sound", SoundResource.class);

    public static void cleanup() {
        FONT.clear();
        TEXTURE.clear();
        SOUND.clear();
    }
}