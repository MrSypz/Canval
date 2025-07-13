package com.sypztep.canval;

import com.sypztep.canval.util.Initializer;
import com.sypztep.canval.util.ResourceLocation;
import com.sypztep.canval.util.ResourceManager;
import org.lwjgl.stb.STBTTFontinfo;

public class Carval implements Initializer {
    private static STBTTFontinfo thaiFont;

    @Override
    public void initialize() {
        ResourceLocation fontLocation = ResourceLocation.id("NotoSansThai-Regular.ttf");
        thaiFont = ResourceManager.getFontInfo(fontLocation);

        System.out.println("Thai font loaded successfully!");
    }

    public static STBTTFontinfo getThaiFont() {
        return thaiFont;
    }
}
