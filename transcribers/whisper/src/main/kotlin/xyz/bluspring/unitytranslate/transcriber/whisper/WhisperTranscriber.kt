package xyz.bluspring.unitytranslate.transcriber.whisper

import io.github.ggerganov.whispercpp.WhisperCpp
import io.github.ggerganov.whispercpp.params.WhisperFullParams
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.Languages
import xyz.bluspring.unitytranslate.api.v2.download.DownloadHelper
import xyz.bluspring.unitytranslate.api.v2.transcriber.SpeechTranscriber
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.Executors
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

object WhisperTranscriber : SpeechTranscriber() {
    var model: WhisperModel = WhisperModel.TINY
        set(value) {
            field = value
            this.close() // Invalidate all existing instances
        }
    var maxWhisperThreads: Int = 3
        set(value) {
            field = value
            this.scope.runCatching { cancel("Threads adjusted") }
            this.context.runCatching { cancel() }
            this.context = this.createContextThreads() // Recreate the coroutine contexts
            this.scope = CoroutineScope(this.context)
        }
    var enableGpu: Boolean = false
        set(value) {
            field = value
            this.close() // Invalidate all existing instances
        }

    private var context = createContextThreads()
    private var scope = CoroutineScope(context)
    private val logger: Logger = LoggerFactory.getLogger(WhisperTranscriber::class.java)

    private fun createContextThreads() = Executors.newFixedThreadPool(maxWhisperThreads) {
        Thread(it).apply {
            isDaemon = true
        }
    }.asCoroutineDispatcher() + CoroutineName("UnityTranslate Whisper Transcriber")

    private val whisperInstances = Collections.synchronizedMap(mutableMapOf<Language, WhisperInstance>())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val initialSetup: Deferred<Unit>
        get() = CoroutineScope(Dispatchers.IO).async {
            model.path.createParentDirectories()
            val storage = Files.getFileStore(model.path.parent)
            if (storage.usableSpace < model.minimumBytes) {
                logger.error("Not enough storage to download ${model.fileName}! (expected: ${model.minimumBytes}, actual: ${storage.usableSpace})")
                throw IOException("Not enough storage to download ${model.fileName}! (expected: ${model.minimumBytes}, actual: ${storage.usableSpace})")
            }

            if (!model.path.exists()) {
                val download = DownloadHelper.queue(model)
                download.onStartDownload.register {
                    logger.info("Downloading ${model.fileName}...")
                }

                download.onFinishDownload.register {
                    if (download.deferred.isCancelled || download.deferred.getCompletionExceptionOrNull() != null) {
                        logger.error("Failed to download ${model.fileName}!")
                    } else {
                        logger.info("Successfully downloaded ${model.fileName}!")
                    }
                }

                download.deferred.await()
            }
        }

    // https://developers.openai.com/api/docs/guides/speech-to-text#supported-languages
    val supportedLanguages = listOf(
        Languages.AFRIKAANS, Languages.ARABIC, Languages.ARMENIAN, Languages.AZERBAIJANI, Languages.BELARUSIAN, Languages.BOSNIAN,
        Languages.BULGARIAN, Languages.CATALAN, Languages.CHINESE_SIMPLIFIED, Languages.CHINESE_TRADITIONAL, Languages.CROATIAN,
        Languages.CZECH, Languages.DANISH, Languages.DUTCH, Languages.ENGLISH, Languages.ESTONIAN, Languages.FINNISH, Languages.FRENCH,
        Languages.GALICIAN, Languages.GERMAN, Languages.GREEK, Languages.HEBREW, Languages.HINDI, Languages.HUNGARIAN, Languages.ICELANDIC,
        Languages.INDONESIAN, Languages.ITALIAN, Languages.JAPANESE, Languages.KANNADA, Languages.KAZAKH, Languages.KOREAN, Languages.LATVIAN,
        Languages.LITHUANIAN, Languages.MACEDONIAN, Languages.MALAY, Languages.MARATHI, Languages.MAORI, Languages.NEPALI, Languages.NORWEGIAN,
        Languages.PERSIAN, Languages.POLISH, Languages.PORTUGUESE, Languages.ROMANIAN, Languages.RUSSIAN, Languages.SERBIAN, Languages.SLOVAK,
        Languages.SLOVENIAN, Languages.SPANISH, Languages.SWAHILI, Languages.SWEDISH, Languages.TAGALOG, Languages.TAMIL, Languages.THAI,
        Languages.TURKISH, Languages.UKRAINIAN, Languages.URDU, Languages.VIETNAMESE, Languages.WELSH
    )

    override suspend fun supportsLanguage(language: Language): Boolean {
        return this.supportedLanguages.contains(language)
    }

    override fun transcribeSamples(samples: FloatArray, language: Language): Deferred<String> {
        return this.scope.async(this.context) {
            // Wait for the Whisper model to be downloaded first.
            if (!model.path.exists()) {
                return@async ""
            }

            // Initialize Whisper instance for this specific language.
            val instance = synchronized(whisperInstances) {
                whisperInstances.computeIfAbsent(language) {
                    createWhisperInstance(language)
                }
            }

            instance.whisper.fullTranscribe(instance.params, samples)
        }
    }

    override fun close() {
        synchronized(this.whisperInstances) {
            for ((_, instance) in this.whisperInstances) {
                instance.whisper.close()
            }

            this.whisperInstances.clear()
        }
    }

    private fun createWhisperInstance(language: Language): WhisperInstance {
        if (!this.model.path.exists())
            throw IllegalStateException("Whisper model ${this.model.name} has not been downloaded yet!")

        val whisper = WhisperCpp()
        whisper.initContext(this.model.path.absolutePathString())

        val params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH)
        params.language = language.formatted
        params.suppressBlanks(true)
        params.suppressNonSpeechTokens(true)
        params.transcribeMode()

        return WhisperInstance(whisper, params)
    }

    @JvmRecord
    private data class WhisperInstance(
        val whisper: WhisperCpp,
        val params: WhisperFullParams,
    )
}
