package com.sypztep.canval.util;


public class ResourceLocation {
    private final String path;

    private ResourceLocation(String path) {
        this.path = path;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ResourceLocation that = (ResourceLocation) obj;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}