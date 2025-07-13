package com.sypztep.canval;

import com.sypztep.canval.graphic.DrawContext;
import com.sypztep.canval.init.Fonts;
import com.sypztep.canval.util.ResourceManager;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class CarvalEngine {
    private long window;
    private final Carval carval = new Carval();
    private DrawContext drawContext;

    // Threading for resource loading
    private final ExecutorService resourceLoader = Executors.newCachedThreadPool();
    private CompletableFuture<Void> preloadFuture;
    private volatile boolean resourcesLoaded = false;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        // Start preloading resources in background BEFORE any OpenGL setup
        startPreloading();

        // Initialize OpenGL context
        init();

        // Wait for resources to finish loading
        waitForPreloading();

        // Initialize things that need OpenGL context
        postLoadInit();

        // Main loop
        loop();

        // Cleanup
        cleanup();
    }

    /**
     * Start loading resources in background threads BEFORE OpenGL initialization
     */
    private void startPreloading() {
        System.out.println("Starting resource preloading...");

        preloadFuture = CompletableFuture.runAsync(() -> {
            try {
                carval.initialize();
                System.out.println("Resource preloading completed!");
                resourcesLoaded = true;
            } catch (Exception e) {
                System.err.println("Error during preloading: " + e.getMessage());
                e.printStackTrace();
            }
        }, resourceLoader);
    }

    /**
     * Wait for preloading to complete
     */
    private void waitForPreloading() {
        try {
            System.out.println("Waiting for resource preloading to complete...");
            preloadFuture.get(); // Block until preloading is done
            System.out.println("All resources loaded!");
        } catch (Exception e) {
            System.err.println("Error waiting for preloading: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize things that require OpenGL context (after preloading)
     */
    private void postLoadInit() {
        drawContext = new DrawContext(CanvalConfig.getDefaultWindowWidth(), CanvalConfig.getDefaultWindowHeight());
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(CanvalConfig.getDefaultWindowWidth(), CanvalConfig.getDefaultWindowHeight(), CanvalConfig.getDefaultWindowTitle(), NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);
        });

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

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
    }

    private void loop() {
        GL.createCapabilities();

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (resourcesLoaded) {
                drawContext.drawCenteredText("สวัสดี World", 48.0f, Fonts.DEFAULT_FONT.value().fontInfo());
                drawContext.drawRect(10, 10, 50, 30, 0.0f, 1.0f, 0.0f);
            } else {
                drawContext.drawCenteredText("Loading...", 24.0f, null);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        ResourceManager.cleanup();
        resourceLoader.shutdown();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}