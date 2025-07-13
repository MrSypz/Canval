package com.sypztep.canval.util.resource;

import com.sypztep.canval.util.ResourceLocation;

import java.nio.ByteBuffer;

public record SoundResource(ResourceLocation id, ByteBuffer audioData, int sampleRate, int channels, String displayName,
                            float duration) {
}
