package com.sypztep.canval.init;

import com.sypztep.canval.util.input.InputUtil;
import com.sypztep.canval.util.input.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class KeyBindings {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBindings.class);

    // Storage maps
    private static final Map<String, KeyBinding> KEYS_BY_ID = new HashMap<>();
    private static final Map<InputUtil.Key, KeyBinding> KEY_TO_BINDINGS = new HashMap<>();
    private static final Set<String> KEY_CATEGORIES = new HashSet<>();

    // Built-in engine key bindings only
    public static final KeyBinding QUIT = registerBuiltIn(new KeyBinding(
            "key.canval.quit",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_ESCAPE,
            KeyBinding.ENGINE_CATEGORY
    ));

    public static void init() {
        updateKeysByCode();
        LOGGER.info("KeyBindings initialized with {} bindings across {} categories",
                KEYS_BY_ID.size(), KEY_CATEGORIES.size());

        // Log all registered key bindings
        for (String category : KEY_CATEGORIES) {
            LOGGER.debug("Category '{}' key bindings:", category);
            KEYS_BY_ID.values().stream()
                    .filter(kb -> kb.getCategory().equals(category))
                    .forEach(kb -> LOGGER.debug("  {} -> {}", kb.getTranslationKey(), kb.getBoundKeyLocalizedText()));
        }
    }

    /**
     * Register a key binding (public method for external registration)
     */
    public static KeyBinding register(KeyBinding keyBinding) {
        String id = keyBinding.getTranslationKey();
        if (KEYS_BY_ID.containsKey(id)) {
            LOGGER.warn("Key binding already registered, replacing: {}", id);
            KeyBinding oldBinding = KEYS_BY_ID.get(id);
            KEY_TO_BINDINGS.remove(oldBinding.getBoundKey());
        }

        KEYS_BY_ID.put(id, keyBinding);
        KEY_CATEGORIES.add(keyBinding.getCategory());
        LOGGER.debug("Registered key binding: {} -> {}", id, keyBinding.getBoundKeyLocalizedText());

        return keyBinding;
    }

    /**
     * Register a key binding (private method for built-in registrations)
     */
    private static KeyBinding registerBuiltIn(KeyBinding keyBinding) {
        String id = keyBinding.getTranslationKey();
        if (KEYS_BY_ID.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate key binding registration: " + id);
        }

        KEYS_BY_ID.put(id, keyBinding);
        KEY_CATEGORIES.add(keyBinding.getCategory());
        LOGGER.debug("Registered key binding: {} -> {}", id, keyBinding.getBoundKeyLocalizedText());

        return keyBinding;
    }

    /**
     * Update the key-to-binding mappings
     */
    public static void updateKeysByCode() {
        KEY_TO_BINDINGS.clear();

        for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
            if (!keyBinding.isUnbound()) {
                KEY_TO_BINDINGS.put(keyBinding.getBoundKey(), keyBinding);
            }
        }

        LOGGER.debug("Updated key mappings: {} active bindings", KEY_TO_BINDINGS.size());
    }

    /**
     * Handle key events from GLFW
     */
    public static void onKey(int key, int scancode, int action, int mods) {
        // Handle key press/release
        InputUtil.Key inputKey = InputUtil.fromKeyCode(key);
        KeyBinding keyBinding = KEY_TO_BINDINGS.get(inputKey);

        if (keyBinding != null) {
            if (action == GLFW.GLFW_PRESS) {
                keyBinding.onKeyPressed();
            }
            keyBinding.setPressed(InputUtil.actionToPressed(action));
        }

        // Also check scancode bindings
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            InputUtil.Key scancodeKey = InputUtil.fromScancode(scancode);
            KeyBinding scancodeBinding = KEY_TO_BINDINGS.get(scancodeKey);
            if (scancodeBinding != null) {
                if (action == GLFW.GLFW_PRESS) {
                    scancodeBinding.onKeyPressed();
                }
                scancodeBinding.setPressed(InputUtil.actionToPressed(action));
            }
        }
    }

    /**
     * Handle mouse button events from GLFW
     */
    public static void onMouseButton(int button, int action, int mods) {
        InputUtil.Key mouseKey = InputUtil.fromMouseButton(button);
        KeyBinding keyBinding = KEY_TO_BINDINGS.get(mouseKey);

        if (keyBinding != null) {
            if (action == GLFW.GLFW_PRESS) {
                keyBinding.onKeyPressed();
            }
            keyBinding.setPressed(InputUtil.actionToPressed(action));
        }
    }

    /**
     * Update pressed states for all key bindings (call each frame)
     */
    public static void updatePressedStates(long window) {
        for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
            if (keyBinding.isUnbound()) continue;

            InputUtil.Key boundKey = keyBinding.getBoundKey();
            boolean isPressed = false;

            switch (boundKey.getCategory()) {
                case KEYSYM:
                    isPressed = InputUtil.isKeyPressed(window, boundKey.getCode());
                    break;
                case MOUSE:
                    isPressed = InputUtil.isMousePressed(window, boundKey.getCode());
                    break;
                case SCANCODE:
                    // Scancode checking is more complex and handled in key callback
                    continue;
            }

            keyBinding.setPressed(isPressed);
        }
    }

    /**
     * Reset all key binding states
     */
    public static void unpressAll() {
        for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
            keyBinding.reset();
        }
        LOGGER.debug("All key bindings reset");
    }

    public static KeyBinding getKeyBinding(String translationKey) {
        return KEYS_BY_ID.get(translationKey);
    }

    public static Collection<KeyBinding> getAllKeyBindings() {
        return Collections.unmodifiableCollection(KEYS_BY_ID.values());
    }

    public static List<KeyBinding> getKeyBindingsByCategory(String category) {
        return KEYS_BY_ID.values().stream()
                .filter(kb -> kb.getCategory().equals(category))
                .sorted()
                .toList();
    }

    public static Set<String> getCategories() {
        return Collections.unmodifiableSet(KEY_CATEGORIES);
    }

    public static boolean isKeyBound(InputUtil.Key key) {
        return KEY_TO_BINDINGS.containsKey(key);
    }

    public static KeyBinding getKeyBindingForKey(InputUtil.Key key) {
        return KEY_TO_BINDINGS.get(key);
    }

    public static boolean rebindKey(String translationKey, InputUtil.Key newKey) {
        KeyBinding keyBinding = KEYS_BY_ID.get(translationKey);
        if (keyBinding == null) {
            LOGGER.warn("Attempted to rebind unknown key binding: {}", translationKey);
            return false;
        }

        KeyBinding existingBinding = KEY_TO_BINDINGS.get(newKey);
        if (existingBinding != null && !existingBinding.equals(keyBinding)) {
            LOGGER.warn("Key {} is already bound to {}", newKey.getLocalizedText(), existingBinding.getTranslationKey());
            return false;
        }

        KEY_TO_BINDINGS.remove(keyBinding.getBoundKey());

        keyBinding.setBoundKey(newKey);

        updateKeysByCode();

        return true;
    }

    public static void resetToDefault(String translationKey) {
        KeyBinding keyBinding = KEYS_BY_ID.get(translationKey);
        if (keyBinding != null) {
            keyBinding.resetToDefault();
            updateKeysByCode();
        }
    }

    public static void resetAllToDefaults() {
        for (KeyBinding keyBinding : KEYS_BY_ID.values()) {
            keyBinding.resetToDefault();
        }
        updateKeysByCode();
        LOGGER.info("All key bindings reset to defaults");
    }

    public static void loadFromConfig() {
        // TODO: Implement loading from config file
        LOGGER.debug("Key binding configuration loading not yet implemented");
    }

    public static void saveToConfig() {
        // TODO: Implement saving to config file
        LOGGER.debug("Key binding configuration saving not yet implemented");
    }

    public static String getDiagnosticInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("KeyBindings Diagnostic Info:\n");
        sb.append("  Total bindings: ").append(KEYS_BY_ID.size()).append("\n");
        sb.append("  Active mappings: ").append(KEY_TO_BINDINGS.size()).append("\n");
        sb.append("  Categories: ").append(KEY_CATEGORIES.size()).append("\n");

        for (String category : KEY_CATEGORIES) {
            long count = KEYS_BY_ID.values().stream()
                    .filter(kb -> kb.getCategory().equals(category))
                    .count();
            sb.append("    ").append(category).append(": ").append(count).append(" bindings\n");
        }

        return sb.toString();
    }

    public static void cleanup() {
        KEYS_BY_ID.clear();
        KEY_TO_BINDINGS.clear();
        KEY_CATEGORIES.clear();
        LOGGER.info("KeyBindings system cleaned up");
    }
}