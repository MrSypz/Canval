package com.sypztep.canval;

import com.sypztep.canval.init.Fonts;
import com.sypztep.canval.util.identifier.RegistryEntry;
import com.sypztep.canval.util.resource.FontResource;

public final class CanvalConfig {
    private static RegistryEntry<FontResource> defaultFont = null; // Set after Fonts.init()

    private static int defaultWindowWidth = 1080;
    private static int defaultWindowHeight = 720;
    private static String defaultWindowTitle = "Canval Engine";

    public static FontResource getDefaultFont() {
        return defaultFont != null ? defaultFont.value() : Fonts.DEFAULT_FONT.value();
    }

    public static void setDefaultFont(RegistryEntry<FontResource> font) {
        defaultFont = font;
    }

    public static int getDefaultWindowWidth() { return defaultWindowWidth; }
    public static void setDefaultWindowWidth(int width) { defaultWindowWidth = width; }

    public static int getDefaultWindowHeight() { return defaultWindowHeight; }
    public static void setDefaultWindowHeight(int height) { defaultWindowHeight = height; }

    public static String getDefaultWindowTitle() { return defaultWindowTitle; }
    public static void setDefaultWindowTitle(String title) { defaultWindowTitle = title; }
}