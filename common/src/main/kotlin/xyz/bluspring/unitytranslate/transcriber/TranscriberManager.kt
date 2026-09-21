package xyz.bluspring.unitytranslate.transcriber

import kotlinx.coroutines.*
import xyz.bluspring.unitytranslate.UnityTranslateApiImpl
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.client.config.ClientConfig
import java.util.concurrent.Executors

class TranscriberManager {
    private var lastTranscriptionTime = 0L
    private var isTranscribing = false

    private val scope = CoroutineScope(Executors.newSingleThreadExecutor({
        Thread(it).apply {
            isDaemon = true
        }
    }).asCoroutineDispatcher()) + CoroutineName("UnityTranslate Transcriber Manager")

    fun tick() {
        if (!this.isTranscribing && System.currentTimeMillis() - this.lastTranscriptionTime >= ClientConfig.transcriptionInterval) {
            this.isTranscribing = true
            this.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    processTranscriptions()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    throw e
                } finally {
                    isTranscribing = false
                }
            }

            this.lastTranscriptionTime = System.currentTimeMillis()
        }
    }

    private suspend fun processTranscriptions() {
        val sources = UnityTranslateApiImpl.transcriberSources.values.toList()

        for (source in sources) {
            if (!source.isReadyToProcess)
                continue

            val processedTexts = source.processSamples()
            if (processedTexts.isNotEmpty()) {
                for (processed in processedTexts) {
                    // Skip over blank text, we don't want to show fake stuff.
                    if (processed.isBlank())
                        continue

                    val holder = UnityTranslateApi.instance.getOrCreateTranscriptHolder(source.language)
                    val original = processed
                    var processed = original

                    for ((processor, settings) in UnityTranslateApiImpl.preProcessors) {
                        if (settings.transcriberLanguages == null || settings.transcriberLanguages!!.contains(source.language))
                            processed = processor.processTranscript(processed, original, source.language)
                    }

                    val transcriptData = DirectTranscriptData(source.sessionTimestamp, source.sender, source.language, processed, System.currentTimeMillis())

                    holder.update(transcriptData)
                }
            }
        }
    }
}
