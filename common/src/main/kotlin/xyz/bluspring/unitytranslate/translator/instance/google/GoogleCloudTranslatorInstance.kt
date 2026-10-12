package xyz.bluspring.unitytranslate.translator.instance.google

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorInstance
import xyz.bluspring.unitytranslate.api.v2.util.JsonHelper.addAll
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

object GoogleCloudTranslatorInstance : TranslatorInstance() {
    object Config {
        var apiKey: String = ""
    }

    private val logger: Logger = LoggerFactory.getLogger(GoogleCloudTranslatorInstance::class.java)
    private const val API_URL = "https://translation.googleapis.com/language/translate/v2"

    private var entries: Set<Language> = emptySet()

    private var lastApiKey = ""

    private fun tryLoadEntries(): Set<Language> {
        val url = URI("${API_URL}/languages?key=${Config.apiKey}&model=nmt").toURL()
        val json = url.openStream().use { JsonParser.parseReader(it.bufferedReader()) }
            .asJsonObject

        val langObjs = json.getAsJsonObject("data").getAsJsonArray("languages")

        val languages = mutableSetOf<Language>()

        for (langObj in langObjs) {
            val langCode = langObj.asJsonObject.get("language").asString
            val nativeName = langObj.asJsonObject.get("name").asString
            languages.add(Language.parse(langCode, localizedName = nativeName))
        }

        return languages
    }

    override suspend fun isAvailable(): Boolean {
        return super.isAvailable() && Config.apiKey.isNotBlank()
    }

    override suspend fun getSupportedLanguages(): Set<Language> {
        if (Config.apiKey.isBlank())
            return emptySet()

        if (this.lastApiKey != Config.apiKey) {
            this.entries = tryLoadEntries()
        }

        return this.entries
    }

    override suspend fun batchTranslate(
        text: List<String>,
        langPair: LangPair
    ): List<String> {
        if (Config.apiKey.isBlank())
            return text

        val reqJson = JsonObject()
        val languagesReq = JsonArray()
        languagesReq.addAll(text)

        reqJson.add("q", languagesReq)
        reqJson.addProperty("format", "text")
        reqJson.addProperty("source", langPair.from.asBCP47)
        reqJson.addProperty("target", langPair.to.asBCP47)

        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder(URI("$API_URL?key=${Config.apiKey}"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(1.minutes.toJavaDuration())
                .POST(HttpRequest.BodyPublishers.ofString(reqJson.toString()))
                .build()

            val response = withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            }

            if (response.statusCode() / 100 != 2)
                throw Exception("Got status code ${response.statusCode()} with message \"${response.body()}\" while translating under Google Cloud translator!")

            val responseJson = JsonParser.parseString(response.body()).asJsonArray

            return responseJson.map {
                if (it.isJsonObject)
                    it.asJsonObject.getAsJsonArray("translations")
                        .first().asJsonObject.get("translatedText").asString
                else
                    it.asString
            }
        } catch (e: Throwable) {
            logger.warn("Failed to translate texts (${text.joinToString(", ") { "\"$it\"" }}) from ${langPair.from} -> ${langPair.to} under Google Cloud translator!", e)
        }

        return text
    }
}
