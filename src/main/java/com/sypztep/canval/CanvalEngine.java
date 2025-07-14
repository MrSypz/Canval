package com.sypztep.canval;

import com.sypztep.canval.graphic.DrawContext;
import com.sypztep.canval.init.Fonts;
import com.sypztep.canval.util.ResourceManager;
import com.sypztep.canval.util.identifier.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class CanvalEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(CanvalConfig.getDefaultWindowTitle());

    private long window;
    private final Canval canval = new Canval();
    private final CanvalClient canvalClient = new CanvalClient();
    private DrawContext drawContext;

    public void run() {
        LOGGER.info("Hello LWJGL {}!", Version.getVersion());
        LOGGER.info("Starting Canval Engine...");

        // Phase 1: Load ALL resources first (no OpenGL needed)
        loadAllResources();

        // Phase 2: Initialize OpenGL context
        initializeOpenGL();

        // Phase 3: Initialize OpenGL-dependent components
        initializeOpenGLComponents();

        // Phase 4: Main game loop
        mainLoop();

        // Phase 5: Cleanup
        cleanup();
    }

    /**
     * Phase 1: Load all resources before OpenGL initialization
     * This runs without any OpenGL context
     */
    private void loadAllResources() {
        LOGGER.info("=== Phase 1: Loading Resources ===");

        try {
            LOGGER.info("Initializing resource systems...");

            // Load all fonts, textures, sounds, etc.
            long startTime = System.currentTimeMillis();
            canval.initialize();
            long loadTime = System.currentTimeMillis() - startTime;

            LOGGER.info("All resources loaded successfully in {} ms", loadTime);

            // Optional: Print resource statistics
            printResourceStatistics();

        } catch (Exception e) {
            LOGGER.error("Failed to load resources", e);
            throw new RuntimeException("Resource loading failed", e);
        }

        LOGGER.info("✓ Resource loading phase completed");
    }

    /**
     * Phase 2: Initialize OpenGL context and window
     */
    private void initializeOpenGL() {
        LOGGER.info("=== Phase 2: Initializing OpenGL ===");

        try {
            // Setup error callback
            GLFWErrorCallback.createPrint(System.err).set();

            // Initialize GLFW
            if (!glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }
            LOGGER.info("GLFW initialized");

            // Configure window
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

            // Create window
            window = glfwCreateWindow(
                    CanvalConfig.getDefaultWindowWidth(),
                    CanvalConfig.getDefaultWindowHeight(),
                    CanvalConfig.getDefaultWindowTitle(),
                    NULL, NULL
            );

            if (window == NULL) {
                throw new RuntimeException("Failed to create the GLFW window");
            }
            LOGGER.info("Window created: {}x{}",
                    CanvalConfig.getDefaultWindowWidth(),
                    CanvalConfig.getDefaultWindowHeight()
            );

            // Setup callbacks
            setupCallbacks();

            // Center window
            centerWindow();

            // Make context current and show window
            glfwMakeContextCurrent(window);
            glfwSwapInterval(1); // Enable v-sync
            glfwShowWindow(window);

            // Initialize OpenGL capabilities
            GL.createCapabilities();

            LOGGER.info("OpenGL context created: {}", glGetString(GL_VERSION));

        } catch (Exception e) {
            LOGGER.error("Failed to initialize OpenGL", e);
            throw new RuntimeException("OpenGL initialization failed", e);
        }

        LOGGER.info("✓ OpenGL initialization completed");
    }

    /**
     * Phase 3: Initialize components that need OpenGL context
     */
    private void initializeOpenGLComponents() {
        LOGGER.info("=== Phase 3: Initializing OpenGL Components ===");

        try {
            // Check OpenGL capabilities
            checkOpenGLCapabilities();
            // NOW bind resources to OpenGL
            canval.initializeOpenGL();

            // Create drawing context (needs OpenGL)
            drawContext = new DrawContext(
                    CanvalConfig.getDefaultWindowWidth(),
                    CanvalConfig.getDefaultWindowHeight()
            );
            LOGGER.info("DrawContext initialized");

            // Initialize any other OpenGL-dependent systems here
            // e.g., shader programs, render buffers, etc.

        } catch (Exception e) {
            LOGGER.error("Failed to initialize OpenGL components", e);
            throw new RuntimeException("OpenGL component initialization failed", e);
        }

        LOGGER.info("✓ OpenGL components initialized");
        LOGGER.info("=== Engine startup completed successfully ===");
    }

    /**
     * Phase 4: Main game loop
     */
    private void mainLoop() {
        LOGGER.info("Starting main loop");

        while (!glfwWindowShouldClose(window)) {
            // Clear framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            drawContext.beginFrame();
            render();
            drawContext.endFrame();

            // Swap buffers and poll events
            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        LOGGER.info("Main loop ended");
    }

    /**
     * Render game content
     */
    private void render() {
        try {
            canvalClient.render(drawContext);
        } catch (Exception e) {
            LOGGER.error("Rendering error", e);
        }
    }

    /**
     * Phase 5: Cleanup all resources
     */
    private void cleanup() {
        LOGGER.info("=== Cleanup Phase ===");

        try {
            drawContext.cleanup();
            ResourceManager.cleanup();
            Registries.cleanup();
            LOGGER.info("Resources cleaned up");

            if (window != NULL) {
                glfwFreeCallbacks(window);
                glfwDestroyWindow(window);
                LOGGER.info("Window destroyed");
            }

            glfwTerminate();
            GLFWErrorCallback callback = glfwSetErrorCallback(null);
            if (callback != null) callback.free();

            LOGGER.info("GLFW terminated");

        } catch (Exception e) {
            LOGGER.error("Error during cleanup", e);
        }

        LOGGER.info("✓ Cleanup completed");
    }

    private void setupCallbacks() {
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        //TODO: Add more callbacks as needed (resize, mouse, etc.)
    }

    private void centerWindow() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }
    }

    private void checkOpenGLCapabilities() {
        int maxTextureSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        LOGGER.info("OpenGL Info:");
        LOGGER.info("  Vendor: {}", glGetString(GL_VENDOR));
        LOGGER.info("  Renderer: {}", glGetString(GL_RENDERER));
        LOGGER.info("  Version: {}", glGetString(GL_VERSION));
        LOGGER.info("  Max Texture Size: {}x{}", maxTextureSize, maxTextureSize);

        if (maxTextureSize < 2048) {
            LOGGER.warn("GPU supports very small textures only: {}x{}", maxTextureSize, maxTextureSize);
        }
    }

    private void printResourceStatistics() {
        int fontCount = Fonts.ENTRIES.size();

        LOGGER.info("Resource Statistics:");
        LOGGER.info("  Fonts loaded: {}", fontCount);
    }
}