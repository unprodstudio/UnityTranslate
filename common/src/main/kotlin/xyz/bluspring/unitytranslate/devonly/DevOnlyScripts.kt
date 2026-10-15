package xyz.bluspring.unitytranslate.devonly

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import xyz.bluspring.unitytranslate.UnityTranslateApiImpl
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi

/**
 * Developer-only scripts. No, this does not mean you get unrestricted access to things.
 * This is just something I have so I can update any languages I don't have listed yet.
 */
object DevOnlyScripts {
    fun getAllSupportedLanguages(): List<Language> {
        return runBlocking {
            UnityTranslateApiImpl.translators.values.flatMap { it.getSupportedLanguages() }.distinctBy { language -> language.languageCode }
        }
    }

    fun listUntranslatedLanguages() {
        val json = JsonObject()
        val languages = this.getAllSupportedLanguages()
        for (language in languages.sortedBy { it.formatted }) {
            if (isTranslated(language))
                continue

            if (!isTranslated("unitytranslate.language.${language.serialized}.native"))
                json.addProperty("unitytranslate.language.${language.serialized}.native", "")

            if (!isTranslated("unitytranslate.language.${language.serialized}.localized"))
                json.addProperty("unitytranslate.language.${language.serialized}.localized", "")
        }

        println(json.toString())
    }

    private fun isTranslated(language: Language): Boolean {
        return isTranslated("unitytranslate.language.${language.serialized}.native") && isTranslated("unitytranslate.language.${language.serialized}.localized")
    }

    private fun isTranslated(key: String): Boolean {
        return UnityTranslateApi.instance.platform.translated(key) != key
    }
}
