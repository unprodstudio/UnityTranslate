package xyz.bluspring.unitytranslate

import java.nio.file.Path
import java.util.*

interface PlatformProxy {
    val version: String
    val rootDir: Path
    val pluginsDir: Path
    val nativesDir: Path
    val isClient: Boolean
    val platform: String

    fun isStandalone(): Boolean = false
    fun isModLoaded(id: String): Boolean

    companion object {
        val instance: PlatformProxy = ServiceLoader.load(PlatformProxy::class.java).first()
    }
}
