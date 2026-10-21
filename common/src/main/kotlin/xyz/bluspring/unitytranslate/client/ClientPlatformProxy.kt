package xyz.bluspring.unitytranslate.client

import com.mojang.blaze3d.pipeline.RenderTarget
import xyz.bluspring.unitytranslate.api.v2.client.ClientAccess
import xyz.bluspring.unitytranslate.client.config.ClientConfig
import java.util.*

interface ClientPlatformProxy : ClientAccess {
    val framebuffer: RenderTarget
    val shouldRenderGui: Boolean
    val renderThread: Thread

    val windowHandle: Long
    val username: String

    override val shouldAnimate: Boolean
        get() = ClientConfig.enableAnimations

    companion object {
        val instance: ClientPlatformProxy = ServiceLoader.load(ClientPlatformProxy::class.java).first()
    }
}
