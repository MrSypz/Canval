package com.sypztep.canval.graphic.font;

public record CharacterInfo(
        float u1, float v1,
        float u2, float v2,
        int width,
        int height,
        int xOffset,
        int yOffset,
        float advance
) {
}