package com.sypztep.canval.util.identifier;

import com.sypztep.canval.util.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Registry<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Registry.class);
    private final Map<ResourceLocation, T> entries = new HashMap<>();
    private final Map<ResourceLocation, RegistryEntry<T>> references = new HashMap<>();
    private final Class<T> type;
    private final String registryName;

    public Registry(String registryName, Class<T> type) {
        this.registryName = registryName;
        this.type = type;
    }

    // Standard registration
    public static <T> T register(Registry<T> registry, ResourceLocation id, T entry) {
        if (registry.entries.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate registration: " + id);
        }
        registry.entries.put(id, entry);
        LOGGER.info("Registered {} : {}" , registry.type.getSimpleName(), id);
        return entry;
    }
    //TODO : Make it dynamic instead of manual update
    // NEW: Update existing registration
    public static <T> T update(Registry<T> registry, ResourceLocation id, T entry) {
        registry.entries.put(id, entry);
        LOGGER.info("Updated {} : {}" , registry.type.getSimpleName(), id);
        return entry;
    }

    public static <T> T register(Registry<T> registry, String id, T entry) {
        return register(registry, ResourceLocation.of(id), entry);
    }

    public static <T> T update(Registry<T> registry, String id, T entry) {
        return update(registry, ResourceLocation.of(id), entry);
    }

    // Reference registration
    public static <T> RegistryEntry<T> registerReference(Registry<T> registry, ResourceLocation id, T entry) {
        register(registry, id, entry);
        RegistryEntry<T> reference = new RegistryEntry<>(registry, id, entry);
        registry.references.put(id, reference);
        return reference;
    }

    public static <T> RegistryEntry<T> registerReference(Registry<T> registry, String id, T entry) {
        return registerReference(registry, ResourceLocation.of(id), entry);
    }

    // NEW: Update reference
    public static <T> RegistryEntry<T> updateReference(Registry<T> registry, ResourceLocation id, T entry) {
        update(registry, id, entry);
        RegistryEntry<T> reference = new RegistryEntry<>(registry, id, entry);
        registry.references.put(id, reference); // This will overwrite
        return reference;
    }

    public static <T> RegistryEntry<T> updateReference(Registry<T> registry, String id, T entry) {
        return updateReference(registry, ResourceLocation.of(id), entry);
    }

    public T get(ResourceLocation id) {
        T entry = entries.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown registry entry: " + id);
        }
        return entry;
    }

    public T get(String id) {
        return get(ResourceLocation.of(id));
    }

    public RegistryEntry<T> getReference(ResourceLocation id) {
        RegistryEntry<T> reference = references.get(id);
        if (reference == null) {
            throw new IllegalArgumentException("Unknown registry reference: " + id);
        }
        return reference;
    }

    public RegistryEntry<T> getReference(String id) {
        return getReference(ResourceLocation.of(id));
    }

    public boolean contains(ResourceLocation id) {
        return entries.containsKey(id);
    }

    public Set<ResourceLocation> getIds() {
        return entries.keySet();
    }

    public String getRegistryName() {
        return registryName;
    }

    public void clear() {
        entries.clear();
        references.clear();
    }
}