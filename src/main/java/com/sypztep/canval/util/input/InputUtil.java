package com.sypztep.canval.util.input;

import org.lwjgl.glfw.GLFW;

/**
 * Utility class for handling input types and key codes, similar to Minecraft's InputUtil
 */
public final class InputUtil {
    public static final Key UNKNOWN_KEY = Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_UNKNOWN);

    /**
     * Input device types
     */
    public enum Type {
        KEYSYM("key.keyboard", (code, translationKey) -> {
            String name = GLFW.glfwGetKeyName(code, 0);
            return name != null ? name : translationKey;
        }),
        SCANCODE("scancode.keyboard", (code, translationKey) -> {
            String name = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, code);
            return name != null ? name : translationKey;
        }),
        MOUSE("key.mouse", (code, translationKey) -> translationKey);

        private final String name;
        private final KeyNameProvider keyNameProvider;

        Type(String name, KeyNameProvider keyNameProvider) {
            this.name = name;
            this.keyNameProvider = keyNameProvider;
        }

        public Key createFromCode(int code) {
            return new Key(this, code);
        }

        public String getName() {
            return name;
        }

        String getKeyName(int code, String translationKey) {
            return keyNameProvider.provide(code, translationKey);
        }

        @FunctionalInterface
        interface KeyNameProvider {
            String provide(int code, String translationKey);
        }
    }

    /**
     * Represents a key with its type and code
     */
    public static final class Key {
        private final Type category;
        private final int code;
        private final String translationKey;

        Key(Type category, int code) {
            this.category = category;
            this.code = code;
            this.translationKey = category.getName() + "." + code;
        }

        public Type getCategory() {
            return category;
        }

        public int getCode() {
            return code;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public String getLocalizedText() {
            if (this.equals(UNKNOWN_KEY)) {
                return "Unknown";
            }

            return switch (category) {
                case KEYSYM -> getKeySymName();
                case SCANCODE -> getScancodeName();
                case MOUSE -> getMouseButtonName();
            };
        }

        private String getKeySymName() {
            return switch (code) {
                case GLFW.GLFW_KEY_ESCAPE -> "Escape";
                case GLFW.GLFW_KEY_ENTER -> "Enter";
                case GLFW.GLFW_KEY_TAB -> "Tab";
                case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
                case GLFW.GLFW_KEY_DELETE -> "Delete";
                case GLFW.GLFW_KEY_RIGHT -> "Right Arrow";
                case GLFW.GLFW_KEY_LEFT -> "Left Arrow";
                case GLFW.GLFW_KEY_DOWN -> "Down Arrow";
                case GLFW.GLFW_KEY_UP -> "Up Arrow";
                case GLFW.GLFW_KEY_PAGE_UP -> "Page Up";
                case GLFW.GLFW_KEY_PAGE_DOWN -> "Page Down";
                case GLFW.GLFW_KEY_HOME -> "Home";
                case GLFW.GLFW_KEY_END -> "End";
                case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock";
                case GLFW.GLFW_KEY_SCROLL_LOCK -> "Scroll Lock";
                case GLFW.GLFW_KEY_NUM_LOCK -> "Num Lock";
                case GLFW.GLFW_KEY_PRINT_SCREEN -> "Print Screen";
                case GLFW.GLFW_KEY_PAUSE -> "Pause";
                case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
                case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Control";
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Control";
                case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
                case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
                case GLFW.GLFW_KEY_LEFT_SUPER -> "Left Super";
                case GLFW.GLFW_KEY_RIGHT_SUPER -> "Right Super";
                case GLFW.GLFW_KEY_SPACE -> "Space";
                default -> {
                    String name = GLFW.glfwGetKeyName(code, 0);
                    if (name != null) {
                        yield name.toUpperCase();
                    }
                    // Function keys
                    if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) {
                        yield "F" + (code - GLFW.GLFW_KEY_F1 + 1);
                    }
                    // Number pad
                    if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) {
                        yield "Keypad " + (code - GLFW.GLFW_KEY_KP_0);
                    }
                    yield "Key " + code;
                }
            };
        }

        private String getScancodeName() {
            String name = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, code);
            return name != null ? name.toUpperCase() : "Scancode " + code;
        }

        private String getMouseButtonName() {
            return switch (code) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "Left Click";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "Right Click";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Middle Click";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "Mouse 4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "Mouse 5";
                case GLFW.GLFW_MOUSE_BUTTON_6 -> "Mouse 6";
                case GLFW.GLFW_MOUSE_BUTTON_7 -> "Mouse 7";
                case GLFW.GLFW_MOUSE_BUTTON_8 -> "Mouse 8";
                default -> "Mouse " + (code + 1);
            };
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Key key = (Key) obj;
            return code == key.code && category == key.category;
        }

        @Override
        public int hashCode() {
            return category.hashCode() * 31 + code;
        }

        @Override
        public String toString() {
            return translationKey;
        }
    }

    /**
     * Check if a key is currently pressed
     */
    public static boolean isKeyPressed(long window, int keyCode) {
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }

    /**
     * Check if a mouse button is currently pressed
     */
    public static boolean isMousePressed(long window, int button) {
        return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }

    /**
     * Convert GLFW key action to boolean
     */
    public static boolean actionToPressed(int action) {
        return action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT;
    }

    public static Key fromKeyCode(int keyCode) {
        return Type.KEYSYM.createFromCode(keyCode);
    }

    public static Key fromMouseButton(int button) {
        return Type.MOUSE.createFromCode(button);
    }

    public static Key fromScancode(int scancode) {
        return Type.SCANCODE.createFromCode(scancode);
    }
}