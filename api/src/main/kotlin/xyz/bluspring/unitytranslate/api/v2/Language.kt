package xyz.bluspring.unitytranslate.api.v2

import com.mojang.serialization.Codec
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.api.v2.util.reverse

@JvmRecord
data class Language @JvmOverloads constructor(
    val languageCode: String, // ISO 639-1 codes
    val regionCode: String? = null, // ISO 3166-1 codes
    val fallbackNativeName: String? = null,
    val fallbackLocalizedName: String? = null,
) : Comparable<Language> {
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

    val nativeText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.native", this.fallbackNativeName ?: this.formatted)
    val nativeShortText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.native.short", this.fallbackNativeName ?: this.nativeText)
    val localizedText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.localized", this.fallbackLocalizedName ?: this.formatted)
    val localizedShortText: String
        get() = UnityTranslateApi.instance.platform.translated("unitytranslate.language.$serialized.localized.short", this.fallbackLocalizedName ?: this.nativeText)

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
