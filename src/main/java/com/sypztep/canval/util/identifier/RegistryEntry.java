package com.sypztep.canval.util.identifier;

import com.sypztep.canval.util.ResourceLocation;

public record RegistryEntry<T>(Registry<T> registry, ResourceLocation id, T value) {

    @Override
    public String toString() {
        return id.toString();
    }
}