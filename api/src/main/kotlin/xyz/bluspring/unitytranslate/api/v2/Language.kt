package xyz.bluspring.unitytranslate.api.v2

import com.google.common.cache.CacheBuilder
import com.mojang.serialization.Codec
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.api.v2.util.reverse
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@JvmRecord
data class Language @JvmOverloads constructor(
    val languageCode: String, // ISO 639-1 codes
    val regionCode: String? = null, // ISO 3166-1 codes
    val fallbackNativeName: String? = null,
    val fallbackLocalizedName: String? = null,
) : Comparable<Language> {
    val locale: Locale?
        get() = localeCache.get(this) { Locale(this.languageCode) }
            .let { if (availableLocales.contains(it)) it else null }

    val fullLocale: Locale?
        get() = fullLocaleCache.get(this) { Locale.forLanguageTag(this.asBCP47) }
            .let { if (availableLocales.contains(it)) it else null }

    val formatted: String
        get() = if (this.regionCode == null)
            this.languageCode
        else
            "${this.languageCode}-${this.regionCode}"

    val serialized: String
        get() = if (this.regionCode == null)
            this.languageCode
        else
            "${this.languageCode}_${this.regionCode.lowercase()}"

    val localeBasedNativeText: String
        get() = this.fullLocale?.let { it.getDisplayName(it) }
            ?: this.formatted

    val localeBasedLocalizedText: String
        get() = this.fullLocale?.getDisplayName(findValidLocale(UnityTranslateApi.instance.client.activeLanguageCode))
            ?: this.formatted

    val nativeText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.native", this.fallbackNativeName
            ?: this.localeBasedNativeText)
    val nativeShortText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.native.short", this.fallbackNativeName
            ?: this.locale?.let { it.getDisplayName(it) }
            ?: this.nativeText)
    val localizedText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.localized", this.fallbackLocalizedName
            ?: this.localeBasedLocalizedText)
    val localizedShortText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.localized.short", this.fallbackLocalizedName
            ?: this.locale?.getDisplayName(findValidLocale(UnityTranslateApi.instance.client.activeLanguageCode))
            ?: this.localizedText)

    val asBCP47: String
        get() {
            if (this.regionCode != null) {
                return "$languageCode-$regionCode"
            }

            return languageCode
        }

    val withoutRegion: Language
        get() = Language(this.languageCode)

    override fun toString(): String = this.formatted

    override fun compareTo(other: Language): Int {
        return this.formatted.compareTo(other.formatted)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Language

        if (languageCode != other.languageCode) return false
        if (regionCode != other.regionCode) return false

        return true
    }

    override fun hashCode(): Int {
        var hash = this.languageCode.hashCode()
        hash = 31 * hash + this.regionCode.hashCode()
        return hash
    }

    companion object {
        @JvmField val CODEC: Codec<Language> = Codec.STRING.xmap(Language::parse, Language::formatted)
        @JvmField val NETWORK_CODEC = NetworkCodecs.STRING_UTF8.xmap(Language::parse, Language::formatted)

        private val localeCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5.minutes.toJavaDuration())
            .weakKeys()
            .build<Language, Locale>()

        private val fullLocaleCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5.minutes.toJavaDuration())
            .weakKeys()
            .build<Language, Locale>()

        private val availableLocales = Locale.getAvailableLocales()

        private fun findValidLocale(code: String): Locale {
            var locale = Locale.forLanguageTag(code)

            if (availableLocales.contains(locale))
                // oh, nice, it's valid
                return locale

            // fuck, okay, need to find the right locale then.
            locale = Locale(locale.language)

            if (availableLocales.contains(locale))
                return locale

            // worst case scenario, let's just use the US locale, since it's always supposed to exist. key word: supposed.
            return Locale.US
        }

        @JvmStatic fun codecWithAliasing(aliases: Map<Language, String>): Codec<Language> {
            val reverseLookup = aliases.reverse()

            return Codec.STRING.xmap({
                reverseLookup.getOrElse(it) { parse(it) }
            }, {
                aliases.getOrElse(it) { it.formatted }
            })
        }

        @JvmStatic @JvmOverloads
        fun parse(code: String, nativeName: String? = null, localizedName: String? = null): Language {
            if (code.contains("-")) {
                val split = code.split("-")
                return Language(split[0], split[1], nativeName, localizedName)
            }

            return Language(code, null, nativeName, localizedName)
        }

        @JvmStatic
        val Pair<SupportLevel, SupportLevel>.isSupported: Boolean
            get() = this.first.isSupported && this.second.isSupported
    }

    /**
     * Represents the support level of a system, e.g. a transcriber or a translator.
     */
    enum class SupportLevel {
        /**
         * This system fully supports this language, including its specific regional dialect.
         */
        FULL,

        /**
         * This system supports the language, however it does not support its regional dialect.
         */
        PARTIAL,

        /**
         * This system does not support the language at all.
         */
        NONE,
        ;

        val isSupported: Boolean
            get() = this != NONE
    }
}
