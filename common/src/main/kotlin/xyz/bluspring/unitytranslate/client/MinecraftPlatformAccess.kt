package xyz.bluspring.unitytranslate.client

import net.minecraft.locale.Language
import xyz.bluspring.unitytranslate.api.v2.PlatformAccess

object MinecraftPlatformAccess : PlatformAccess {
    override fun translated(key: String): String
        = Language.getInstance().getOrDefault(key)

    override fun translated(key: String, fallback: String): String
        = Language.getInstance().getOrDefault(key, fallback)

    override fun translated(key: String, fallback: () -> String): String {
        val language = Language.getInstance()
        return if (language.has(key))
            language.getOrDefault(key)
        else
            fallback()
    }
}
