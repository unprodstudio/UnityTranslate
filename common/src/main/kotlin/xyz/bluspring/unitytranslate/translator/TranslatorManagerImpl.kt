package xyz.bluspring.unitytranslate.translator

import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import xyz.bluspring.unitytranslate.UnityTranslateApiImpl
import xyz.bluspring.unitytranslate.api.v2.Language.Companion.isSupported
import xyz.bluspring.unitytranslate.api.v2.event.TranscriptEvent
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptData.Companion.id
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorInstance
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorManager
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import xyz.bluspring.unitytranslate.transcriber.DirectTranscriptData
import xyz.bluspring.unitytranslate.transcriber.TranslatedTranscriptData
import xyz.bluspring.unitytranslate.translator.instance.InactiveTranslatorInstance
import xyz.bluspring.unitytranslate.translator.instance.argos.UnityTranslateLibTranslatorInstance
import xyz.bluspring.unitytranslate.translator.instance.microsoft.MicrosoftInternalTranslatorInstance
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object TranslatorManagerImpl : TranslatorManager {
    object Config {
        var maxThreads: Int = 3.coerceAtMost(Runtime.getRuntime().availableProcessors())
        var batchSize: Int = 15
        var delayBetweenBatches: Int = 500
        var maxTranslationWords: Int = 50
    }

    private lateinit var pool: ExecutorService
    private lateinit var context: CoroutineContext
    private lateinit var scope: CoroutineScope
    private lateinit var prepareScope: CoroutineScope

    private val tickScope = CoroutineScope(Dispatchers.Default) + CoroutineName("UnityTranslate Translator Manager Tick")

    init {
        this.updateConfig()

        TranscriptEvent.UPDATED.register { holder, data ->
            // make sure we're only handling direct transcript data
            if (data !is DirectTranscriptData)
                return@register

            UnityTranslateApiImpl.transcriptHolders.forEach { (toLang, otherHolder) ->
                this.queueWithId(data.message, LangPair(holder.language, toLang), data.id, data.timeUpdated)
                    .asCompletableFuture()
                    .thenAccept { translated ->
                        otherHolder.update(TranslatedTranscriptData(
                            data.timeCreated,
                            data.sender,
                            data.language,
                            data.message,
                            translated,
                            data.timeUpdated,
                        ))
                    }
            }
        }
    }

    override var instances = mutableListOf<TranslatorInstance>(
        UnityTranslateLibTranslatorInstance,
        MicrosoftInternalTranslatorInstance,
    )

    override fun getInstanceById(id: String): TranslatorInstance? {
        return UnityTranslateApiImpl.translators[id]
    }

    private val queued = ConcurrentHashMap<LangPair, Queue<Entry>>()
    private var lastBatchTime: Long = 0L

    @JvmRecord
    private data class Entry(
        val original: String,
        val deferred: CompletableDeferred<String>,
        val id: String,
        val lastUpdated: Long,
    )

    private var lastMaxThreads = -1

    private fun updateConfig() {
        if (this.lastMaxThreads != Config.maxThreads) {
            if (this::scope.isInitialized) {
                this.scope.cancel("Thread count updated (${this.lastMaxThreads} -> ${Config.maxThreads})")
            }

            if (this::prepareScope.isInitialized) {
                this.prepareScope.cancel("Thread count updated (${this.lastMaxThreads} -> ${Config.maxThreads})")
            }

            val tasks = if (this::pool.isInitialized) {
                this.pool.shutdownNow()
            } else emptyList()

            this.pool = Executors.newWorkStealingPool(Config.maxThreads)
            this.context = pool.asCoroutineDispatcher() + CoroutineName("UnityTranslate Translator Manager")
            this.scope = CoroutineScope(this.context) + CoroutineName("UnityTranslate Translator Manager (Translation Scope)")
            this.prepareScope = CoroutineScope(this.context) + CoroutineName("UnityTranslate Translator Manager (Preparation Scope)")

            for (task in tasks) {
                this.pool.submit(task)
            }

            this.lastMaxThreads = Config.maxThreads
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun queue(text: String, langPair: LangPair): Deferred<String> {
        return queueWithId(text, langPair, "random_${Uuid.generateV7()}", System.currentTimeMillis())
    }

    fun queueWithId(text: String, langPair: LangPair, id: String, timeUpdated: Long): Deferred<String> {
        val deferred: CompletableDeferred<String> = CompletableDeferred()

        // Safeguard so we don't waste bandwidth for no good reason
        if (langPair.from == langPair.to) {
            deferred.complete(text)
            return deferred
        }

        this.queued.computeIfAbsent(langPair) { ConcurrentLinkedQueue() }
            .add(Entry(text, deferred, id, timeUpdated))

        return deferred
    }

    fun startTicking() {
        this.tickScope.launch {
            while (true) {
                tick()
                yield()
                LockSupport.parkNanos("Ticking for UnityTranslate translator", 50.milliseconds.inWholeNanoseconds)
            }
        }
    }

    private suspend fun tick() {
        this.updateConfig()

        // just to try to collect enough to even batch translate.
        if (System.currentTimeMillis() - this.lastBatchTime <= Config.delayBetweenBatches)
            return

        var anyTranslationsQueued = false
        for ((langPair, queue) in this.queued) {
            if (queue.isEmpty())
                continue

            val instance = this.instances.firstOrNull {
                it.isAvailable() && it.checkLanguageSupport(langPair).isSupported
            } ?: InactiveTranslatorInstance
            val langPair = instance.getEffectiveLanguagePair(langPair) ?: langPair

            val entries = ArrayList<Entry>(Config.batchSize)
            while (queue.isNotEmpty()) {
                val entry = queue.poll()!!

                // skip over any entries that may have been superseded.
                if (entry.deferred.isCancelled)
                    continue

                if (entries.none { it.id == entry.id }) {
                    entries.add(entry)
                } else {
                    val existing = entries.filter { it.id == entry.id && entry.lastUpdated > it.lastUpdated }

                    if (existing.isNotEmpty()) {
                        for (existingEntry in existing) {
                            existingEntry.deferred.cancel("Superseded")
                        }

                        entries.removeAll(existing.toSet())
                        entries.add(entry)
                    }
                }

                if (entries.size >= Config.batchSize)
                    break
            }

            this.scope.launch(this.context) {
                prepareScope.async { instance.prepareTranslationModels(langPair) }.await() // Make sure they're ready first.

                val spliced = entries.map { // split them up so we don't have a Resa moment
                    it.original.split(" ")
                        .chunked(Config.maxTranslationWords)
                        .map { b -> b.joinToString(" ") }
                }

                val batchTranslated = instance.batchTranslate(spliced.flatten(), langPair)
                val combined = mutableListOf<String>()

                // now join them back so they're not that bad
                for ((i, texts) in spliced.withIndex()) {
                    val length = texts.size

                    combined += batchTranslated.slice(i until i + length).joinToString(" ")
                }

                for ((i, translated) in combined.withIndex()) {
                    val original = translated
                    var translated = original

                    for ((processor, settings) in UnityTranslateApiImpl.postProcessors) {
                        if ((settings.translatorInputLanguages == null || settings.translatorInputLanguages!!.contains(langPair.from)) && (settings.translatorOutputLanguages == null || settings.translatorOutputLanguages!!.contains(langPair.to)))
                            translated = processor.processFinalTranscript(translated, original, langPair.from, langPair.to)
                    }

                    entries[i].deferred.complete(translated)
                }
            }

            anyTranslationsQueued = true
        }

        if (anyTranslationsQueued) {
            this.lastBatchTime = System.currentTimeMillis()
        }
    }
}
