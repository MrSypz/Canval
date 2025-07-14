package com.sypztep.canval;

import com.sypztep.canval.graphic.DrawContext;
import com.sypztep.canval.util.ClientInitializer;

public class CanvalClient implements ClientInitializer {
    @Override
    public void render(DrawContext context) {
        context.drawCenteredText("Hello world",48,CanvalConfig.getDefaultFont());
    }
}
