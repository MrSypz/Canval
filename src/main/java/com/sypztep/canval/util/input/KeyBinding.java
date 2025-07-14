package com.sypztep.canval.util.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a key binding, similar to Minecraft's KeyBinding system
 */
public class KeyBinding implements Comparable<KeyBinding> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBinding.class);

    public static final String ENGINE_CATEGORY = "key.categories.engine";

    private final String translationKey;
    private final InputUtil.Key defaultKey;
    private final String category;
    private InputUtil.Key boundKey;
    private boolean pressed;
    private int timesPressed;

    /**
     * Create a new key binding
     * @param translationKey The translation key for this binding
     * @param type The input type (KEYSYM, SCANCODE, MOUSE)
     * @param code The key/button code
     * @param category The category for organization
     */
    public KeyBinding(String translationKey, InputUtil.Type type, int code, String category) {
        this.translationKey = translationKey;
        this.defaultKey = type.createFromCode(code);
        this.boundKey = this.defaultKey;
        this.category = category;
    }

    /**
     * Create a key binding with KEYSYM type (most common)
     */
    public KeyBinding(String translationKey, int keyCode, String category) {
        this(translationKey, InputUtil.Type.KEYSYM, keyCode, category);
    }

    /**
     * Called when the bound key is pressed
     */
    public void onKeyPressed() {
        this.timesPressed++;
        LOGGER.debug("Key binding '{}' pressed (times: {})", translationKey, timesPressed);
    }

    /**
     * Called when the bound key state changes
     */
    public void setPressed(boolean pressed) {
        if (this.pressed != pressed) {
            this.pressed = pressed;
            LOGGER.debug("Key binding '{}' state changed to: {}", translationKey, pressed);
        }
    }

    /**
     * Check if the key is currently being held down
     * @return true if the key is currently pressed
     */
    public boolean isPressed() {
        return this.pressed;
    }

    /**
     * Check if the key was pressed and consume the press
     * This method "consumes" one press count each time it's called
     * @return true if the key was pressed since last check
     */
    public boolean wasPressed() {
        if (this.timesPressed == 0) {
            return false;
        } else {
            this.timesPressed--;
            return true;
        }
    }

    /**
     * Reset the key binding state
     */
    public void reset() {
        this.timesPressed = 0;
        this.setPressed(false);
    }

    /**
     * Get the translation key
     */
    public String getTranslationKey() {
        return translationKey;
    }

    /**
     * Get the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get the default key
     */
    public InputUtil.Key getDefaultKey() {
        return defaultKey;
    }

    /**
     * Get the currently bound key
     */
    public InputUtil.Key getBoundKey() {
        return boundKey;
    }

    /**
     * Set the bound key
     */
    public void setBoundKey(InputUtil.Key boundKey) {
        this.boundKey = boundKey;
        LOGGER.info("Key binding '{}' rebound to: {}", translationKey, boundKey.getLocalizedText());
    }

    /**
     * Check if this binding matches a specific key
     */
    public boolean matchesKey(int keyCode, int scanCode) {
        return keyCode == InputUtil.UNKNOWN_KEY.getCode()
                ? this.boundKey.getCategory() == InputUtil.Type.SCANCODE && this.boundKey.getCode() == scanCode
                : this.boundKey.getCategory() == InputUtil.Type.KEYSYM && this.boundKey.getCode() == keyCode;
    }

    /**
     * Check if this binding matches a mouse button
     */
    public boolean matchesMouse(int button) {
        return this.boundKey.getCategory() == InputUtil.Type.MOUSE && this.boundKey.getCode() == button;
    }

    /**
     * Check if the binding is set to its default key
     */
    public boolean isDefault() {
        return this.boundKey.equals(this.defaultKey);
    }

    /**
     * Check if the binding is unbound
     */
    public boolean isUnbound() {
        return this.boundKey.equals(InputUtil.UNKNOWN_KEY);
    }

    /**
     * Get the localized text for the bound key
     */
    public String getBoundKeyLocalizedText() {
        return this.boundKey.getLocalizedText();
    }

    /**
     * Reset to default key
     */
    public void resetToDefault() {
        setBoundKey(this.defaultKey);
    }

    /**
     * Compare for sorting
     */
    @Override
    public int compareTo(KeyBinding other) {
        int categoryCompare = this.category.compareTo(other.category);
        if (categoryCompare != 0) {
            return categoryCompare;
        }
        return this.translationKey.compareTo(other.translationKey);
    }

    /**
     * Check equality with another KeyBinding based on bound key
     */
    public boolean equals(KeyBinding other) {
        return other != null && this.boundKey.equals(other.boundKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        KeyBinding that = (KeyBinding) obj;
        return translationKey.equals(that.translationKey);
    }

    @Override
    public int hashCode() {
        return translationKey.hashCode();
    }

    @Override
    public String toString() {
        return "KeyBinding{" +
                "key='" + translationKey + '\'' +
                ", bound=" + boundKey.getLocalizedText() +
                ", category='" + category + '\'' +
                '}';
    }
}