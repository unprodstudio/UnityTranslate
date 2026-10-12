package xyz.bluspring.unitytranslate.translator.instance.google

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.Languages
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorInstance
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import xyz.bluspring.unitytranslate.api.v2.util.reverse
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

object GoogleInternalTranslatorInstance : TranslatorInstance() {
    private const val API_URL = "https://translate.googleapis.com/translate_a"

    private val logger: Logger = LoggerFactory.getLogger(GoogleInternalTranslatorInstance::class.java)
    private val languageRemap = mapOf(
        "iw" to Languages.HEBREW,
    )
    private val reverseLanguageRemap = languageRemap.reverse()

    private val entries: Set<Language> by lazy {
        val url = URI("$API_URL/l?client=chrome").toURL()
        val json = url.openStream().use { JsonParser.parseReader(it.bufferedReader()) }
            .asJsonObject

        val fromLangs = json.getAsJsonObject("sl")
        val toLangs = json.getAsJsonObject("tl")

        val actualLanguages = fromLangs.asMap().mapValues { it.value.asString }
            .filterKeys { toLangs.has(it) }

        val languages = mutableSetOf<Language>()

        for ((langCode, nativeName) in actualLanguages) {
            if (langCode == "auto")
                continue

            if (languageRemap.contains(langCode))
                languages.add(languageRemap[langCode]!!)
            else
                languages.add(Language.parse(langCode, localizedName = nativeName))
        }

        languages
    }

    private fun buildTranslationQuery(text: String, from: Language, to: Language): URI {
        val from = reverseLanguageRemap[from] ?: from.asBCP47
        val to = reverseLanguageRemap[to] ?: to.asBCP47

        return URI("$API_URL/single?client=chrome&dt=t&sl=${from}&tl=${to}&q=${URLEncoder.encode(text, Charsets.UTF_8)}")
    }

    override suspend fun getSupportedLanguages(): Set<Language> {
        return this.entries
    }

    override suspend fun batchTranslate(
        text: List<String>,
        langPair: LangPair
    ): List<String> {
        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder(buildTranslationQuery(text.joinToString(" § "), langPair.from, langPair.to))
                .timeout(1.minutes.toJavaDuration())
                .GET()
                .build()

            val response = withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            }

            if (response.statusCode() / 100 != 2)
                throw Exception("Got status code ${response.statusCode()} with message \"${response.body()}\" while translating under Google translator!")

            val responseJson = JsonParser.parseString(response.body()).asJsonArray

            return responseJson[0].asJsonArray[0].asJsonArray[0].asString.split(" § ")
        } catch (e: Throwable) {
            logger.warn("Failed to translate texts (${text.joinToString(", ") { "\"$it\"" }}) from ${langPair.from} -> ${langPair.to} under Google translator!", e)
        }

        return text
    }
}
