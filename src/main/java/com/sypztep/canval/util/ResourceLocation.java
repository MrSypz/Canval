package com.sypztep.canval.util;


public record ResourceLocation(String path) {

    public ResourceLocation {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be null or blank");
        }
    }

    public static ResourceLocation of(String path) {
        return new ResourceLocation(path);
    }

    public String getExtension() {
        int lastDot = path.lastIndexOf('.');
        return lastDot != -1 ? path.substring(lastDot + 1).toLowerCase() : "";
    }

    public String getFileName() {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
    }

    @Override
    public String toString() {
        return path;
    }
}
