package xyz.bluspring.unitytranslate.standalone

import com.mojang.blaze3d.pipeline.RenderTarget
import xyz.bluspring.unitytranslate.PlatformProxy
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.client.gui.screen.UTScreen
import xyz.bluspring.unitytranslate.minecraft.Blaze3DClientPlatformProxy
import xyz.bluspring.unitytranslate.standalone.input.Mouse
import xyz.bluspring.unitytranslate.standalone.ui.StandaloneScreen
import java.nio.file.Path
import kotlin.io.path.Path

class StandalonePlatformProxy : Blaze3DClientPlatformProxy(), PlatformProxy {
    override val version: String = UnityTranslateStandalone.metadata.version
    override val rootDir: Path = Path(".")
    override val pluginsDir: Path = Path("plugins")
    override val nativesDir: Path = Path("natives")
    override val isClient: Boolean
        get() = true
    override val framebuffer: RenderTarget
        get() = UnityTranslateStandalone.framebuffer

    override fun isStandalone(): Boolean = true
    override fun isModLoaded(id: String): Boolean = false

    override val renderThread: Thread
        get() = UnityTranslateStandalone.gameThread
    override val windowHandle: Long
        get() = UnityTranslateStandalone.window.handle()
    override val mouseX: Double
        get() = Mouse.x
    override val mouseY: Double
        get() = Mouse.y
    override val windowWidth: Int
        get() = UnityTranslateStandalone.window.width
    override val windowHeight: Int
        get() = UnityTranslateStandalone.window.height

    override fun setScreen(screen: UTScreen?) {
        val screen = screen ?: StandaloneScreen()
        UnityTranslateStandalone.screen = screen
    }

    override val viewportWidth: Int
        get() = UnityTranslateStandalone.window.screenWidth
    override val viewportHeight: Int
        get() = UnityTranslateStandalone.window.screenHeight
    override val defaultFont: FontReference = FontReference.freeType(UnityTranslate::class.java.getResourceAsStream("/assets/unitytranslate/font/tiktok_sans.ttf")!!, 10f)
    override val guiScale: Double
        get() = UnityTranslateStandalone.window.guiScale.toDouble()

    override val shouldRenderGui: Boolean = true
}
