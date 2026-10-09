package xyz.bluspring.unitytranslate.translator.instance.argos

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorInstance
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import xyz.bluspring.unitytranslate.library.UnityTranslateLib
import xyz.bluspring.unitytranslate.library.UnityTranslateLibInstance
import xyz.bluspring.unitytranslate.translator.instance.argos.index.ArgosPackageIndex
import xyz.bluspring.unitytranslate.translator.instance.argos.index.ModelInfo
import xyz.bluspring.unitytranslate.translator.instance.argos.index.ModelPackage
import xyz.bluspring.unitytranslate.translator.instance.argos.index.PackageIndex
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object UnityTranslateLibTranslatorInstance : TranslatorInstance() {
    var enableGpu = false
    private var lastEnabledGpu = false

    private val packagePrepareDispatcher = Executors.newWorkStealingPool(4.coerceAtMost(Runtime.getRuntime().availableProcessors()))
        .asCoroutineDispatcher() + CoroutineName("UnityTranslate Library Package Prepare")
    private val packagePrepareScope = CoroutineScope(this.packagePrepareDispatcher)

    val library by lazy {
        if (this.wasLoadedSuccessfully)
            UnityTranslateLib()
        else null
    }
    val packageIndexes = listOf<PackageIndex<*>>(
        ArgosPackageIndex(UnityTranslateApi.instance.storagePath.resolve("models/translator/argos")),
    )

    val instances: MutableMap<LangPair, UnityTranslateLibInstance> = Collections.synchronizedMap(mutableMapOf())
    private val instanceLocks: MutableMap<LangPair, Mutex> = Collections.synchronizedMap(mutableMapOf())
    private val queuedWaitingForInstance: Multimap<LangPair, CompletableDeferred<Unit>> = HashMultimap.create()

    private val cachedTranslationPath = ConcurrentHashMap<LangPair, List<LangPair>>()

    private val gpuEnabledLock = Mutex()
    private val wasLoadedSuccessfully by lazy {
        try {
            UnityTranslateLib.autoLoad()
            true
        } catch (e: Throwable) {
            UnityTranslate.logger.error("Failed to load UnityTranslateLib!", e)
            false
        }
    }

    override suspend fun isAvailable(): Boolean = UnityTranslateLib.isAvailable() && this.wasLoadedSuccessfully

    override suspend fun supportsLanguage(langPair: LangPair): Boolean {
        return this.getTranslationPath(langPair).isNotEmpty()
    }

    private var isLoaded = false
    private suspend fun ensureIndexLoaded() {
        if (this.isLoaded)
            return

        for (index in this.packageIndexes) {
            index.loadIndexOrCache(true)
        }

        this.isLoaded = true
    }

    override suspend fun getSupportedLanguages(): Set<Language> {
        this.ensureIndexLoaded()
        val entries = this.packageIndexes.flatMap {
            synchronized(it.packages) {
                it.packages.map { pkg -> pkg.langPair }
            }
        }
        return getAllSupportedLanguages(entries)
    }

    private fun getTranslationPath(langPair: LangPair): List<LangPair> {
        // When iterating through all the translation indexes like this, we can actually figure out a translation path
        // based on multiple indexes.
        if (this.cachedTranslationPath.contains(langPair))
            return this.cachedTranslationPath[langPair]!!

        val availableLanguages = this.packageIndexes.flatMap { index -> index.packages }.map { it.langPair }.distinct()
        val translationPath = getAvailableTranslationPath(langPair, availableLanguages)
        this.cachedTranslationPath[langPair] = translationPath

        return translationPath
    }

    private fun acquireLock(langPair: LangPair): Mutex {
        synchronized(this.instanceLocks) {
            return this.instanceLocks.computeIfAbsent(langPair) { Mutex() }
        }
    }

    override suspend fun prepareTranslationModels(langPair: LangPair) {
        if (this.library == null)
            throw IllegalStateException("Tried to prepare translation models on UnityTranslateLib instance while none exist!")

        super.prepareTranslationModels(langPair)

        val translationPath = this.getTranslationPath(langPair)
        if (translationPath.isEmpty()) // Is not supported
            return

        // we're already prepared, we don't need any further setup.
        if (translationPath.all { this.instances.contains(it) }) {
            for (pair in translationPath) {
                this.unsuspendWaitingTranslators(pair)
            }
            return
        }

        val packageMap = translationPath.mapNotNull {
            for (index in this.packageIndexes) {
                val pkg = index.getDirectTranslationPackage(langPair)

                if (pkg != null) {
                    return@mapNotNull index to pkg
                }
            }

            null
        }

        val deferreds = mutableListOf<Deferred<ModelInfo?>>()
        for ((index, pkg) in packageMap) {
            if (this.instances.contains(pkg.langPair)) { // We don't need another instance, fortunately.
                this.unsuspendWaitingTranslators(pkg.langPair)
                continue
            }

            deferreds += this.packagePrepareScope.async {
                acquireLock(pkg.langPair).withLock { // Might as well lock it for the meantime.
                    if (instances.contains(pkg.langPair)) { // Looks like someone got to it before us, we can just skip over us then.
                        unsuspendWaitingTranslators(pkg.langPair)
                        return@withLock null
                    }

                    (index as PackageIndex<ModelPackage>).tryDownloadModelInfo(pkg)
                }
            }
        }

        val infos = deferreds.awaitAll()
        for (info in infos) {
            if (info == null) // We already determined that our model is already loaded, we don't need it then.
                continue

            if (this.instances.contains(info.langPair)) {// Looks like someone already got to this before us, we can just skip over us then.
                this.unsuspendWaitingTranslators(langPair)
                continue
            }

            acquireLock(info.langPair).withLock { // Lock so we can store into the instance map.
                synchronized(this.instances) {
                    this.instances[info.langPair] = this.library!!.createInstance(info.langPair.asLibraryPair, info.tokenizerType, info.tokenizerModelPath, info.translationModelPath, this.enableGpu)
                    this.unsuspendWaitingTranslators(info.langPair)
                }
            }
        }
    }

    private fun unsuspendWaitingTranslators(langPair: LangPair) {
        synchronized(this.queuedWaitingForInstance) {
            this.queuedWaitingForInstance[langPair].forEach { it.complete(Unit) }
            this.queuedWaitingForInstance.removeAll(langPair)
        }
    }

    override suspend fun batchTranslate(text: List<String>, langPair: LangPair): List<String> {
        if (!this.supportsLanguage(langPair)) // User error if we get to this point.
            throw IllegalArgumentException("UnityTranslateLib instance does not support language pair $langPair!")

        // Reset the instances so we actually switch to using the GPU.
        this.gpuEnabledLock.withLock {
            if (this.lastEnabledGpu != this.enableGpu) {
                for ((langPair, instance) in this.instances) {
                    val lock = this.acquireLock(langPair)
                    lock.withLock {
                        instance.free()
                    }
                }

                this.instances.clear()
                this.lastEnabledGpu = this.enableGpu
            }
        }

        if (!this.instances.contains(langPair)) {
            // Assume we're waiting for a download, we'll pause execution here in the meantime.
            val deferred = CompletableDeferred<Unit>()
            synchronized(this.queuedWaitingForInstance) {
                this.queuedWaitingForInstance.put(langPair, deferred)
            }

            // check again rq
            if (this.instances.contains(langPair)) {
                deferred.complete(Unit)
                this.unsuspendWaitingTranslators(langPair)
            }

            yield()
            deferred.await()
        } else {
            this.unsuspendWaitingTranslators(langPair)
        }

        val lock = this.acquireLock(langPair)

        lock.withLock {
            val instance = this.instances[langPair]!!
            return instance.batchTranslate(text)
        }
    }

    override suspend fun close() {
        super.close()

        val keys = this.instances.keys.toList()
        for (langPair in keys) {
            this.acquireLock(langPair).withLock {
                val instance = this.instances[langPair]!!
                instance.free()

                this.instances.remove(langPair)
            }
        }
    }
}
