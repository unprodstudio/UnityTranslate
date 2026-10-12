package xyz.bluspring.unitytranslate.translator.instance.microsoft

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorInstance
import xyz.bluspring.unitytranslate.api.v2.util.JsonHelper.addAll
import xyz.bluspring.unitytranslate.api.v2.util.JsonHelper.getStringOrNull
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

object MicrosoftInternalTranslatorInstance : TranslatorInstance() {
    // https://github.com/rugved-danej/discord-plugins/blob/main/plugins/next-translator/src/api/Bing.ts
    // https://github.com/fishjar/kiss-translator/commit/0c5df0e8875f579f1c38fdc6a82100fd0417d766

    private val logger: Logger = LoggerFactory.getLogger(MicrosoftInternalTranslatorInstance::class.java)

    private const val API_URL = "https://api.cognitive.microsofttranslator.com"
    private const val API_VERSION = "3.0"

    private const val TRANSLATE_URL = "https://edge.microsoft.com/translate/translatetext"

    private val entries: Set<Language> by lazy {
        val url = URI("$API_URL/languages?api-version=$API_VERSION&scope=translation").toURL()
        val json = url.openStream().use { JsonParser.parseReader(it.bufferedReader()) }
            .asJsonObject

        val translations = json.getAsJsonObject("translation")
        val languages = mutableSetOf<Language>()

        for ((langCode, entry) in translations.entrySet()) {
            val nativeName = entry.asJsonObject.getStringOrNull("nativeName")
            val localizedName = entry.asJsonObject.getStringOrNull("name")

            languages.add(Language.parse(langCode, nativeName, localizedName))
        }

        languages
    }

    override suspend fun getSupportedLanguages(): Set<Language> {
        return this.entries
    }

    override suspend fun batchTranslate(text: List<String>, langPair: LangPair): List<String> {
        val reqJson = JsonArray()
        reqJson.addAll(text)

        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder(URI("$TRANSLATE_URL?from=${langPair.from.asBCP47}&to=${langPair.to.asBCP47}"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(1.minutes.toJavaDuration())
                .POST(HttpRequest.BodyPublishers.ofString(reqJson.toString()))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            }

            if (response.statusCode() / 100 != 2)
                throw Exception("Got status code ${response.statusCode()} with message \"${response.body()}\" while translating under Bing translator!")

            val responseJson = JsonParser.parseString(response.body()).asJsonArray

            return responseJson.map {
                if (it.isJsonObject)
                    it.asJsonObject.getAsJsonArray("translations")
                        .first().asJsonObject.get("text").asString
                else
                    it.asString
            }
        } catch (e: Throwable) {
            logger.warn("Failed to translate texts (${text.joinToString(", ") { "\"$it\"" }}) from ${langPair.from} -> ${langPair.to} under Bing translator!", e)
        }

        return text
    }
}
