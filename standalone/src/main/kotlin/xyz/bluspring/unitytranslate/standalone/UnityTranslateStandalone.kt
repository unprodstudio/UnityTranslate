package xyz.bluspring.unitytranslate.standalone

import com.google.gson.JsonParser
import com.mojang.blaze3d.opengl.GlBackend
import com.mojang.blaze3d.pipeline.MainTarget
import com.mojang.blaze3d.platform.*
import com.mojang.blaze3d.shaders.GpuDebugOptions
import com.mojang.blaze3d.systems.*
import net.minecraft.client.DeltaTracker
import net.minecraft.client.FramerateLimiter
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.util.Util
import net.minecraft.util.profiling.Profiler
import net.minecraft.util.thread.ReentrantBlockableEventLoop
import org.lwjgl.glfw.GLFW
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.api.v2.client.gui.screen.UTScreen
import xyz.bluspring.unitytranslate.client.renderer.BatchedGuiRenderer
import xyz.bluspring.unitytranslate.client.renderer.UnityTranslateGui
import xyz.bluspring.unitytranslate.client.renderer.ui.BatchedUIGraphics
import xyz.bluspring.unitytranslate.shared.HandledException
import xyz.bluspring.unitytranslate.shared.Metadata
import xyz.bluspring.unitytranslate.standalone.input.Keyboard
import xyz.bluspring.unitytranslate.standalone.input.Mouse
import xyz.bluspring.unitytranslate.standalone.resources.ShaderManager
import xyz.bluspring.unitytranslate.standalone.resources.StandalonePackResources
import xyz.bluspring.unitytranslate.standalone.ui.StandaloneScreen
import java.util.*

object UnityTranslateStandalone : ReentrantBlockableEventLoop<Runnable>("UnityTranslate", true), WindowEventHandler {
    val metadata = Metadata.parse(JsonParser.parseString(this::class.java.getResource("/metadata.json")!!.readText()).asJsonObject)
    val window: Window
    val windowSurface: GpuSurface
    val deltaTracker = DeltaTracker.Timer(20f, 0L) { it }
    val framebuffer: MainTarget
    val gameThread: Thread = Thread.currentThread()

    var frameTimeNs: Long = 0

    var screen: UTScreen = StandaloneScreen()

    private var running = true

    init {
        Util.setTimeSource(RenderSystem.initBackendSystem())
        Thread.currentThread().name = "UnityTranslate Render Thread"
        RenderSystem.initRenderThread()
        Util.startTimerHackThread()

        var window: Window? = null
        lateinit var device: GpuDevice

        val backends = listOf(GlBackend())

        for (backend in backends) {
            try {
                GLFW.glfwDefaultWindowHints()
                GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE) // transparent background :D
//                GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE) // Borderless window
//                GLFW.glfwWindowHint(GLFW.GLFW_MOUSE_PASSTHROUGH, GLFW.GLFW_TRUE) // Allow clicking the mouse through the window

                window = Window(this, DisplayData(843, 600, OptionalInt.empty(), OptionalInt.empty(), false), null, false, "UnityTranslate", MonitorManager(), backend)
                device = window.backend().createDevice(window.handle(),
                    ShaderManager::getShader,
                    GpuDebugOptions(0, false, false, false)
                ) {}
                GLFW.glfwShowWindow(window.handle())

                val deviceInfo = device.deviceInfo
                val maxSize = deviceInfo.limits.maxTextureSize
                GLFW.glfwSetWindowSizeLimits(window.handle(), -1, -1, maxSize, maxSize)

                RenderSystem.initRenderer(device)
                UnityTranslate.logger.info("Using graphics backend ${deviceInfo.backendName}, using drivers: ${deviceInfo.driverInfo}")
                UnityTranslate.logger.info("Using graphics device: ${deviceInfo.name} (${deviceInfo.vendorName})")
                UnityTranslate.logger.info("Using graphics device extensions: ${deviceInfo.underlyingExtensions.joinToString(", ")}")

                break
            } catch (e: BackendCreationException) {
                UnityTranslate.logger.error("Failed to create backend ${backend.name}", e)
                window?.close()
                window = null
            }
        }

        if (window == null) {
            throw IllegalStateException("No supported graphics backend was found.")
        }

        this.window = window
        this.windowSurface = device.createSurface(this.window.handle())
        this.framebuffer = MainTarget(843, 600)

        try {
            this.window.setIcon(StandalonePackResources, IconSet.RELEASE)
        } catch (e: Throwable) {
            UnityTranslate.logger.error("Couldn't set icon", e)
        }

        InputConstants.setupMouseCallbacks(this.window,
            { handle, x, y ->
                this.execute { Mouse.onMove(handle, x, y) }
            },
            { handle, button, action, modifiers ->
                this.execute { Mouse.onPress(handle, MouseButtonInfo(button, modifiers), action) }
            },
            { handle, scrollX, scrollY ->
                this.execute { Mouse.onScroll(handle, scrollX, scrollY) }
            },
            { handle, count, namesPtr ->
                // TODO: do we want this?
            })

        InputConstants.setupKeyboardCallbacks(this.window, { handle, key, scanCode, action, modifiers ->
            this.execute { Keyboard.keyPress(handle, action, KeyEvent(key, scanCode, modifiers)) }
        }, { handle, codepoint ->
            this.execute { Keyboard.charTyped(handle, CharacterEvent(codepoint)) }
        }, { handle, preeditSize, preeditPtr, blockCount, blockSizesPtr, focusedBlock, caret ->
            // TODO: do we need this?
        }, { handle ->
            // TODO: do we need this?
        })
    }

    @JvmStatic
    fun init() {
        try {
            UnityTranslate.logger.info("Started UnityTranslate Standalone v${metadata.version} (build hash: ${metadata.buildHash})")
            UnityTranslate.init()

            while (this.running) {
                try {
                    RenderSystem.pollEvents()
                    this.runTick()
                } catch (e: OutOfMemoryError) {
                    System.gc()
                    UnityTranslate.logger.error("Ran out of memory!", e)

                    throw e
                }
            }

            UnityTranslate.logger.info("Exited from main loop.")
        } catch (e: Throwable) {
            throw HandledException(e)
        }
    }

    private fun runTick() {
        if (!this.windowSurface.isAcquired) {
            this.renderFrame()
        }
    }

    private var surfaceIsInvalid = true
    private var windowSurfaceNeedsReconfiguring = true

    private fun renderFrame() {
        if (this.window.shouldClose()) {
            this.stop()
        }

        val renderStartTimer = Util.getNanos()
        val profiler = Profiler.get()
        profiler.push("update")
        if (this.windowSurfaceNeedsReconfiguring || this.windowSurface.isSuboptimal && !this.surfaceIsInvalid) {
            val width = IntArray(1)
            val height = IntArray(1)
            GLFW.glfwGetFramebufferSize(this.window.handle(), width, height)

            if (width[0] != 0 || height[0] != 0) {
                val presentMode = GpuSurface.PresentMode.getSupportedVsyncMode(this.windowSurface.supportedPresentModes(), false) // TODO: set up VSync?
                val config = GpuSurface.Configuration(width[0], height[0], presentMode)

                try {
                    this.windowSurface.configure(config)
                    this.surfaceIsInvalid = false
                } catch (e: SurfaceException) {
                    UnityTranslate.logger.warn("Couldn't configure surface to $config", e)
                    this.surfaceIsInvalid = true
                }
            }

            this.windowSurfaceNeedsReconfiguring = false
        }

        if (!this.surfaceIsInvalid) {
            try {
                this.windowSurface.acquireNextTexture()
            } catch (e: SurfaceException) {
                UnityTranslate.logger.warn("Couldn't acquire next surface texture with config ${this.windowSurface.currentConfiguration()}", e)
                this.surfaceIsInvalid = true
                this.windowSurfaceNeedsReconfiguring = true
            }
        }

        this.deltaTracker.advanceRealTime(Util.getMillis())

        // Rendering
        profiler.popPush("submit")
        val graphics = BatchedUIGraphics(BatchedGuiRenderer.DrawLayer.SCREEN)
        val partialTick = this.deltaTracker.getGameTimeDeltaPartialTick(true)
        UnityTranslateGui.submit(graphics, partialTick, Mouse.x, Mouse.y)
        UnityTranslateGui.submitLate(graphics, partialTick)
        this.screen.submit(graphics, partialTick, Mouse.x.toInt(), Mouse.y.toInt())
        graphics.flushLastLayer()

        profiler.popPush("render")
        BatchedGuiRenderer.render()

        profiler.popPush("gpuAsync")
        RenderSystem.executePendingTasks()

        // Present to frame
        profiler.popPush("present")
        if (this.windowSurface.isAcquired) {
            val colorTexture = this.framebuffer.colorTextureView
                ?: throw IllegalStateException("Can't blit to screen, color texture doesn't exist yet")

            this.windowSurface.blitFromTexture(RenderSystem.getDevice().createCommandEncoder(), colorTexture)
        }

        this.frameTimeNs = Util.getNanos() - renderStartTimer

        // Swap
        profiler.popPush("swapBuffers")
        RenderSystem.getDevice().createCommandEncoder().submit()

        if (this.windowSurface.isAcquired) {
            this.windowSurface.present()
        }

        RenderSystem.getDynamicUniforms().reset()

        // Frame limit time
        profiler.popPush("frameLimiter")
        FramerateLimiter.limitDisplayFPS(144)
    }

    fun invalidateSurfaceConfiguration() {
        this.windowSurfaceNeedsReconfiguring = true
    }

    fun stop() {
        this.running = false
        UnityTranslate.logger.info("Stopping!")
    }

    override fun framebufferSizeChanged() {
        this.invalidateSurfaceConfiguration()
        this.resizeGui()
    }

    override fun resizeGui() {
    }

    override fun cursorEntered() {
        Mouse.cursorEntered()
    }

    override fun shouldRun(task: Runnable): Boolean {
        return true
    }

    override fun getRunningThread(): Thread {
        return this.gameThread
    }

    override fun wrapRunnable(runnable: Runnable): Runnable {
        return runnable
    }
}
