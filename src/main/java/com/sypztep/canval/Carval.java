package com.sypztep.canval;

import com.sypztep.canval.init.Fonts;
import com.sypztep.canval.util.Initializer;


public final class Carval implements Initializer {
    @Override
    public void initialize() {
        Fonts.init();
    }
}