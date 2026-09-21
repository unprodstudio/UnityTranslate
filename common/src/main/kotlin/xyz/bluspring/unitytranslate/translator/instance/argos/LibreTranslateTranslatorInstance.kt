package xyz.bluspring.unitytranslate.translator.instance.argos

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraft.util.HttpUtil
import net.minecraft.util.Mth
import net.minecraft.util.Util
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.PlatformProxy
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorInstance
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

object LibreTranslateTranslatorInstance : TranslatorInstance() {
    private val logger = LoggerFactory.getLogger(LibreTranslateTranslatorInstance::class.java)
    private val CHECK_INTERVAL = 30.minutes.inWholeMilliseconds
    var entries = mutableListOf<Entry>()

    data class Entry(val url: String, val authKey: String? = null) {
        constructor(url: String, authKey: Optional<String>) : this(url, authKey.orElse(null))

        private var lastCheck = -1L
        private val cachedSupportedLanguages = mutableListOf<LangPair>()
        private var isAvailable = false

        private val authKeyOpt: Optional<String> = Optional.ofNullable(this.authKey)

        suspend fun getSupportedLanguages(): Collection<LangPair> {
            if (System.currentTimeMillis() - this.lastCheck >= CHECK_INTERVAL) {
                try {
                    val url = URI.create("${this.url}/languages").toURL()
                    val json = withContext(Dispatchers.IO) {
                        url.openStream()
                    }.use { JsonParser.parseReader(it.bufferedReader()) }.asJsonArray

                    synchronized(this.cachedSupportedLanguages) {
                        this.cachedSupportedLanguages.clear()
                        for (element in json) {
                            val langObj = element.asJsonObject

                            val fromCode = langObj.get("code").asString

                            if (langObj.has("targets")) {
                                for (ele in langObj.getAsJsonArray("targets")) {
                                    val toCode = ele.asString
                                    this.cachedSupportedLanguages.add(LangPair(Language.parse(fromCode), Language.parse(toCode)))
                                }
                            }
                        }
                    }

                    this.isAvailable = true
                } catch (e: Throwable) {
                    this.isAvailable = false
                    logger.error("Failed to load LibreTranslate URL \"${this.url}/languages\"!", e)
                }

                this.lastCheck = System.currentTimeMillis()
            }

            if (!this.isAvailable)
                return emptyList()

            synchronized(this.cachedSupportedLanguages) {
                return this.cachedSupportedLanguages
            }
        }

        companion object {
            val CODEC: Codec<Entry> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.STRING.fieldOf("url")
                        .forGetter(Entry::url),
                    Codec.STRING.optionalFieldOf("auth_key")
                        .forGetter(Entry::authKeyOpt)
                )
                    .apply(instance, ::Entry)
            }
        }
    }

    override suspend fun getSupportedLanguages(): Set<Language> {
        val entries = this.entries.flatMap { it.getSupportedLanguages() }
        return getAllSupportedLanguages(entries)
    }

    override suspend fun isAvailable(): Boolean = this.entries.any { it.getSupportedLanguages().isNotEmpty() }

    override suspend fun supportsLanguage(langPair: LangPair): Boolean {
        return this.entries.any { it.getSupportedLanguages().contains(langPair) }
    }

    override suspend fun batchTranslate(text: List<String>, langPair: LangPair): List<String> {
        if (this.enableLegacyLocal && this.lastPid == -1L) {
            this.loadLegacyTranslator()
        } else if (!this.enableLegacyLocal && this.lastPid != -1L) {
            this.killOpenInstances()
        }

        val textArray = JsonArray().apply {
            for (string in text) {
                this.add(string)
            }
        }

        for (entry in this.entries) {
            // Ignore the ones that don't support it.
            if (!entry.getSupportedLanguages().contains(langPair))
                continue

            try {
                val reqJson = JsonObject()
                reqJson.addProperty("source", langPair.from.formatted)
                reqJson.addProperty("target", langPair.to.formatted)
                reqJson.add("q", textArray)

                if (entry.authKey?.isNotBlank() == true)
                    reqJson.addProperty("api_key", entry.authKey)

                val client = HttpClient.newHttpClient()
                val request = HttpRequest.newBuilder(URI.create(entry.url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(1.minutes.toJavaDuration())
                    .POST(HttpRequest.BodyPublishers.ofString(reqJson.toString()))
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
                }

                if (response.statusCode() / 100 != 2)
                    throw Exception("Got status code ${response.statusCode()} with message \"${response.body()}\"!")

                val responseJson = JsonParser.parseString(response.body()).asJsonObject

                return responseJson.getAsJsonArray("translatedText").map {
                    it.asString
                }
            } catch (e: Throwable) {
                logger.error("LibreTranslate instance ${entry.url} failed to translate texts (${text.joinToString(", ") { "\"it\"" }}) from ${langPair.from} -> ${langPair.to}!", e)
            }
        }

        // We shouldn't be able to reach this point, but warn anyway.
        logger.warn("Failed to translate texts (${text.joinToString(", ") { "\"it\"" }}) from ${langPair.from} -> ${langPair.to}! Not sure what happened here.")
        return text
    }

    // Legacy local translator code

    private var lastPid = -1L
    var enableLegacyLocal = false // tries to load the old legacy translator, if it exists.
    var legacyThreads = 4.coerceAtMost(Runtime.getRuntime().availableProcessors() - 3).coerceAtLeast(1)
    private var hasStarted = false
    private var isStarting = false

    private val libreTranslateDir = PlatformProxy.instance.rootDir.resolve(".unitytranslate")

    fun loadLegacyTranslator() {
        if (this.isStarting)
            return

        this.isStarting = true
        if (this.lastPid != -1L) {
            killOpenInstances()
        }

        val port = if (HttpUtil.isPortAvailable(5000)) 5000 else HttpUtil.getAvailablePort()
        clearDeadDirectories()

        val source = this.libreTranslateFile?.toFile() ?: return
        if (!source.exists()) return

        if (!source.canExecute()) {
            if (!source.setExecutable(true)) {
                UnityTranslate.logger.error("Unable to start local LibreTranslate instance! You may have to manually set the execute permission on the file yourself!")
                UnityTranslate.logger.error("File path: ${source.absolutePath}")
                return
            }
        }

        val processBuilder = ProcessBuilder(listOf(
            source.absolutePath,
            "--update-models",
            "--port",
            "$port",
            "--threads",
            "${Mth.clamp(this.legacyThreads, 1, Runtime.getRuntime().availableProcessors())}",
            "--disable-web-ui",
            "--disable-files-translation"
        ))

        processBuilder.directory(this.libreTranslateDir.toFile())

        val environment = processBuilder.environment()
        environment["PYTHONIOENCODING"] = "utf-8"
        environment["PYTHONLEGACYWINDOWSSTDIO"] = "utf-8"

        if (System.getenv("unitytranslate.enableLogging") == "true") {
            processBuilder
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
        }

        val process = processBuilder.start()
        lastPid = process.pid()

        val timer = Timer(true)

        process.onExit()
            .whenCompleteAsync { process, e ->
                e?.printStackTrace()

                if (!hasStarted) {
                    timer.cancel()
                    UnityTranslate.logger.warn("LibreTranslate appears to have exited with code ${process.exitValue()}, not proceeding with local translator instance.")
                }

                hasStarted = false
                isStarting = false
            }

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    val entry = Entry("http://localhost:$port")
                    entries.addFirst(entry)

                    timer.cancel()
                    hasStarted = true
                    isStarting = false
                } catch (_: Exception) {
                }
            }
        }, 2500L, 2500L)
    }

    fun killOpenInstances() {
        if (lastPid == -1L)
            return

        ProcessHandle.of(lastPid)
            .ifPresent {
                UnityTranslate.logger.info("Detected LibreTranslate instance ${lastPid}, killing.")
                it.destroyForcibly()

                lastPid = -1L
            }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun clearDeadDirectories() {
        val files = libreTranslateDir.listDirectoryEntries()

        if (files != null) {
            for (file in files) {
                if (!file.isDirectory())
                    continue

                if (file.name.startsWith("_MEI")) {
                    file.deleteRecursively()
                    UnityTranslate.logger.warn("Failed to delete unused LibreTranslate directories, this may mean a dead LibreTranslate instance is running on your computer!")
                    UnityTranslate.logger.warn("Please try to terminate any \"libretranslate.exe\" processes that you see running, then restart your game.")
                }
            }
        }
    }

    val libreTranslateFile: Path?
        get() {
            val supportsCuda = false
            val platform = Util.getPlatform()

            if (platform != Util.OS.WINDOWS && platform != Util.OS.OSX && platform != Util.OS.LINUX) {
                return null
            }

            return libreTranslateDir.resolve("libretranslate/libretranslate${if (supportsCuda) "_cuda" else ""}${if (platform == Util.OS.WINDOWS) ".exe" else ""}")
        }
}
