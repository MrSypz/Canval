package com.sypztep.canval.graphic.font;


import com.sypztep.canval.util.resource.FontResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FontAtlasManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FontAtlasManager.class);
    private static final FontAtlasManager INSTANCE = new FontAtlasManager();

    private final Map<String, FontAtlas> atlasCache = new HashMap<>();

    private FontAtlasManager() {}

    public static FontAtlasManager getInstance() {
        return INSTANCE;
    }

    public FontAtlas getAtlas(FontResource font, float fontSize) {
        String key = font.id() + "_" + fontSize;
        return atlasCache.computeIfAbsent(key, k -> {
            LOGGER.debug("Creating new FontAtlas: {}", key);
            return new FontAtlas(font, fontSize);
        });
    }

    public void cleanup() {
        LOGGER.info("Cleaning up FontAtlasManager ({} atlases)", atlasCache.size());
        atlasCache.values().forEach(FontAtlas::cleanup);
        atlasCache.clear();
    }
}
