package xyz.bluspring.unitytranslate.api.v2

interface PlatformAccess {
    fun translated(key: String): String
    fun translated(key: String, fallback: String): String
    fun translated(key: String, fallback: () -> String): String

    fun translatedFormat(key: String, vararg args: Any?): String
        = this.translated(key).format(*args)
    fun translatedFormatWithFallback(key: String, fallback: String, vararg args: Any?): String
        = this.translated(key, fallback).format(*args)
}
