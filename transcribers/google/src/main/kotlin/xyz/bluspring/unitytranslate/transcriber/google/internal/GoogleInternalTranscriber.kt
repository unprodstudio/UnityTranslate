@file:OptIn(ExperimentalAtomicApi::class)

package xyz.bluspring.unitytranslate.transcriber.google.internal

import com.google.gson.JsonParser
import io.nayuki.flac.common.StreamInfo
import io.nayuki.flac.encode.BitOutputStream
import io.nayuki.flac.encode.FlacEncoder
import io.nayuki.flac.encode.SubframeEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.client.AudioHelper
import xyz.bluspring.unitytranslate.api.v2.transcriber.SpeechTranscriber
import xyz.bluspring.unitytranslate.api.v2.util.AudioConverters
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.random.nextULong

/**
 * This is the internal Google Speech API, utilized by Google Chrome and its forks that don't remove all Google services.
 * The implementation of this is referenced directly from network_speech_recognition_engine_impl.cc in Chromium.
 * However, these *are* internal APIs and keys that can very well go away at any time, so proceed with caution.
 * We do however provide the official paid Google Cloud API transcriber too.
 */
object GoogleInternalTranscriber : SpeechTranscriber() {
    private val logger: Logger = LoggerFactory.getLogger(GoogleInternalTranscriber::class.java)

    // https://giulianopz.github.io/full-duplex-http-streaming-in-go
    // https://gist.github.com/offlinehacker/5780124
    // https://blog.travispayton.com/wp-content/uploads/2014/03/Google-Speech-API.pdf

    // https://github.com/StainlessStlRat/FullDuplexNettyExample
    // https://github.com/gillesdemey/google-speech-v2/blob/master/README.md

    private const val LOW_BITS = 0x00000000_FFFFFFFFuL
    private const val HIGH_BITS = 0xFFFFFFFF_00000000uL

    const val V2_URL = "https://www.google.com/speech-api/v2/recognize"

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"

    const val SAMPLE_RATE = AudioHelper.SAMPLE_RATE
    const val FRAME_SIZE = AudioHelper.BUFFER_SIZE
    const val BYTES_PER_SAMPLE = AudioHelper.SAMPLE_SIZE
    const val CHANNELS = AudioHelper.CHANNELS

    private val context = Executors.newSingleThreadExecutor({
        Thread(it).apply {
            isDaemon = true
        }
    }).asCoroutineDispatcher()
    private val scope = CoroutineScope(context)

    private fun generateRequestKey(): String {
        val time = System.currentTimeMillis().toULong()
        val timeLow = time and LOW_BITS

        val random = Random.nextULong()
        val randomHigh = random and HIGH_BITS

        return (timeLow or randomHigh).toHexString()
    }

    override suspend fun supportsLanguage(language: Language): Boolean {
        return true
    }

    override fun transcribeSamples(samples: FloatArray, language: Language): Deferred<String> {
        return this.scope.async {
            val byteOutputStream = ByteArrayOutputStream()
            val bitStream = BitOutputStream(byteOutputStream)
            bitStream.writeInt(32, 0x664C6143)

            val intSamples = AudioConverters.floatPcm16ToInt(samples)

            val info = StreamInfo().apply {
                sampleRate = SAMPLE_RATE
                numChannels = CHANNELS
                sampleDepth = BYTES_PER_SAMPLE
                numSamples = samples.size.toLong()
            }
            info.write(true, bitStream)
            FlacEncoder(info, arrayOf(intSamples), 4096, SubframeEncoder.SearchOptions.SUBSET_BEST, bitStream)

            bitStream.flush()
            info.write(true, bitStream)
            bitStream.flush()

            // TODO: use full duplex 
            val client = HttpClient.newHttpClient()
            val req = HttpRequest.newBuilder(URI("$V2_URL?key=${GoogleApiKeys.GOOGLE_API_KEY}&lang=${language.asBCP47}&output=json"))
                .headers(
                    "User-Agent", USER_AGENT,
                    "Content-Type", "audio/x-flac; rate=$SAMPLE_RATE"
                )
                .POST(HttpRequest.BodyPublishers.ofByteArray(byteOutputStream.toByteArray()))
                .build()

            val res = client.send(req, HttpResponse.BodyHandlers.ofString())
            val data = res.body().trim().split("\n").lastOrNull()

            if (!data.isNullOrBlank()) {
                val json = JsonParser.parseString(data).asJsonObject
                val results = json.getAsJsonArray("result")

                if (!results.isEmpty) {
                    val alts = results[0].asJsonObject.getAsJsonArray("alternative")
                    if (!alts.isEmpty) {
                        return@async alts[0].asJsonObject.get("transcript").asString
                    }
                }
            }

            ""
        }
    }

    override fun close() {
    }
}
