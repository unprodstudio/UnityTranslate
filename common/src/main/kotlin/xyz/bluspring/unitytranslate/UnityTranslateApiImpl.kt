package xyz.bluspring.unitytranslate

import com.mojang.serialization.MapCodec
import xyz.bluspring.sunset.SunsetConfig
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.LanguageHolder
import xyz.bluspring.unitytranslate.api.v2.PlatformAccess
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.ClientAccess
import xyz.bluspring.unitytranslate.api.v2.config.ConfigBuilder
import xyz.bluspring.unitytranslate.api.v2.display.LanguageDisplay
import xyz.bluspring.unitytranslate.api.v2.plugin.PluginMetadata
import xyz.bluspring.unitytranslate.api.v2.transcriber.InactiveTranscriber
import xyz.bluspring.unitytranslate.api.v2.transcriber.SpeechTranscriber
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriberSource
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptHolder
import xyz.bluspring.unitytranslate.api.v2.transcriber.processor.PostProcessorSettings
import xyz.bluspring.unitytranslate.api.v2.transcriber.processor.PreProcessorSettings
import xyz.bluspring.unitytranslate.api.v2.transcriber.processor.TranscriptPostProcessor
import xyz.bluspring.unitytranslate.api.v2.transcriber.processor.TranscriptPreProcessor
import xyz.bluspring.unitytranslate.api.v2.transcriber.sender.TranscriptUser
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorInstance
import xyz.bluspring.unitytranslate.api.v2.translator.TranslatorManager
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.MinecraftPlatformAccess
import xyz.bluspring.unitytranslate.client.config.ClientConfig
import xyz.bluspring.unitytranslate.client.transcriber.source.InputSource
import xyz.bluspring.unitytranslate.config.builders.SunsetWrappedConfigBuilder
import xyz.bluspring.unitytranslate.plugin.PluginManager
import xyz.bluspring.unitytranslate.transcriber.TranscriberSourceImpl
import xyz.bluspring.unitytranslate.translator.TranslatorManagerImpl
import xyz.bluspring.unitytranslate.translator.instance.InactiveTranslatorInstance
import xyz.bluspring.unitytranslate.util.DefaultedDelegate
import java.nio.file.Path
import java.util.*

object UnityTranslateApiImpl : UnityTranslateApi {
    val transcribers: MutableMap<String, SpeechTranscriber> = mutableMapOf()
    val transcriberConfigs: MutableMap<String, SunsetConfig> = mutableMapOf()
    val transcriptHolders: MutableMap<Language, TranscriptHolder> = WeakHashMap()
    val transcriberSources: MutableMap<TranscriptUser, TranscriberSourceImpl> = WeakHashMap()

    val languageDisplays: MutableMap<String, MapCodec<out LanguageDisplay>> = mutableMapOf()
    val languageDisplayLookup: MutableMap<MapCodec<out LanguageDisplay>, String> = mutableMapOf()

    val translators: MutableMap<String, TranslatorInstance> = mutableMapOf()
    val translatorConfigs: MutableMap<String, SunsetConfig> = mutableMapOf()

    val preProcessors = mutableMapOf<TranscriptPreProcessor, PreProcessorSettings>()
    val postProcessors = mutableMapOf<TranscriptPostProcessor, PostProcessorSettings>()

    val configs: MutableMap<String, SunsetConfig> = mutableMapOf()

    val outputLanguages: MutableMap<String, LanguageHolder> = mutableMapOf()

    val inputSources: MutableSet<InputSource> = mutableSetOf()

    val allConfigs: Collection<SunsetConfig>
        get() = this.translatorConfigs.values + this.translatorConfigs.values + this.configs.values

    override var currentSpokenLanguage: Language
        get() = ClientConfig.spokenLanguage
        set(value) {
            ClientConfig.spokenLanguage = value
        }
    override val translatorManager: TranslatorManager
        get() = TranslatorManagerImpl // Don't inline this! You're gonna run into a bunch of headaches otherwise.

    override var activeTranscriber: SpeechTranscriber = InactiveTranscriber
        private set

    suspend fun setActiveTranscriber(transcriber: SpeechTranscriber) {
        this.activeTranscriber.close()
        transcriber.initialSetup.await()
        this.activeTranscriber = transcriber
    }

    override val configPath: Path
        get() = PlatformProxy.instance.rootDir.resolve("config/unitytranslate")

    override val storagePath: Path
        get() = PlatformProxy.instance.rootDir.resolve("unitytranslate")

    override val client: ClientAccess
        get() = ClientPlatformProxy.instance

    override val platform: PlatformAccess
        get() = MinecraftPlatformAccess

    override val isClient: Boolean
        get() = PlatformProxy.instance.isClient

    override fun <T : TranslatorInstance> registerTranslator(id: String, value: T, configBuilder: ConfigBuilder.() -> Unit) {
        if (this.translators.contains(id)) {
            throw IllegalArgumentException("A translator already exists by ID $id!")
        }

        if (this.translators.containsValue(value)) {
            throw IllegalArgumentException("Tried to register duplicate translator under two different IDs! (new: $id, existing: ${this.translators.filterValues { it == value }.keys.first()})")
        }

        this.translators[id] = value
        this.translatorConfigs[id] = SunsetConfig.create(UnityTranslateApi.instance.configPath.resolve("translators/$id.json")) {
            val builder = SunsetWrappedConfigBuilder(this)
            configBuilder.invoke(builder)
        }
    }

    override fun <T : SpeechTranscriber> registerTranscriber(id: String, value: T, configBuilder: ConfigBuilder.() -> Unit) {
        if (this.transcribers.contains(id)) {
            throw IllegalArgumentException("A transcriber already exists by ID $id!")
        }

        if (this.transcribers.containsValue(value)) {
            throw IllegalArgumentException("Tried to register duplicate transcriber under two different IDs! (new: $id, existing: ${this.transcribers.filterValues { it == value }.keys.first()})")
        }

        this.transcribers[id] = value
        this.transcriberConfigs[id] = SunsetConfig.create(UnityTranslateApi.instance.configPath.resolve("transcribers/$id.json")) {
            val builder = SunsetWrappedConfigBuilder(this)
            configBuilder.invoke(builder)
        }
    }

    override fun getTranscriberId(transcriber: SpeechTranscriber): String {
        return this.transcribers.filterValues { it == transcriber }.keys.first()
    }

    override fun getTranscriber(id: String): SpeechTranscriber {
        return this.transcribers[id] ?: InactiveTranscriber
    }

    override fun getOrCreateTranscriberSource(sender: TranscriptUser): TranscriberSource {
        if (this.transcriberSources.contains(sender))
            return this.transcriberSources[sender]!!

        val source = TranscriberSourceImpl(sender, DefaultedDelegate(::activeTranscriber), DefaultedDelegate(::currentSpokenLanguage))
        this.transcriberSources[sender] = source
        return source
    }

    fun getTranslator(id: String): TranslatorInstance {
        return this.translators[id] ?: InactiveTranslatorInstance
    }

    fun getTranslatorConfig(id: String): SunsetConfig {
        return this.transcriberConfigs[id] ?: this.transcriberConfigs["inactive"]!!
    }

    fun getTranslatorId(translator: TranslatorInstance): String {
        return this.translators.filterValues { it == translator }.keys.first()
    }

    override fun getOrCreateTranscriptHolder(language: Language): TranscriptHolder {
        return this.transcriptHolders.computeIfAbsent(language, ::TranscriptHolder)
    }

    override fun hasPlugin(id: String): Boolean {
        return PluginManager.getPluginMetadataById(id) != null
    }

    override fun getPluginMetadata(id: String): PluginMetadata? {
        return PluginManager.getPluginMetadataById(id)
    }

    override fun registerConfig(id: String, builder: ConfigBuilder.() -> Unit) {
        if (this.configs.contains(id))
            throw IllegalArgumentException("A config already exists by ID $id!")

        this.configs[id] = SunsetConfig.create(UnityTranslateApi.instance.configPath.resolve("plugins/$id.json")) {
            val configBuilder = SunsetWrappedConfigBuilder(this)
            builder.invoke(configBuilder)
        }
    }

    override fun registerOutputLanguage(id: String): LanguageHolder {
        if (this.outputLanguages.contains(id))
            throw IllegalArgumentException("An output language already exists by ID $id!")

        val holder = LanguageHolder()
        this.outputLanguages[id] = holder
        return holder
    }

    override fun <T : LanguageDisplay> registerLanguageDisplay(id: String, codec: MapCodec<T>) {
        if (this.languageDisplays.contains(id))
            throw IllegalArgumentException("A language display already exists by ID $id!")

        if (this.languageDisplayLookup.contains(codec))
            throw IllegalArgumentException("Tried to register duplicate language display! (new: $id, existing: ${this.languageDisplayLookup[codec]})")

        this.languageDisplays[id] = codec
        this.languageDisplayLookup[codec] = id
    }

    override fun getLanguageDisplay(id: String): MapCodec<out LanguageDisplay>? {
        return this.languageDisplays[id]
    }

    override fun getLanguageDisplayId(codec: MapCodec<out LanguageDisplay>): String {
        return this.languageDisplayLookup[codec]!!
    }

    override fun registerTranscriptPreprocessor(processor: TranscriptPreProcessor, settings: PreProcessorSettings) {
        if (this.preProcessors.contains(processor))
            throw IllegalArgumentException("Pre-processor $processor was already registered!")

        this.preProcessors[processor] = settings
    }

    override fun registerTranscriptPostprocessor(processor: TranscriptPostProcessor, settings: PostProcessorSettings) {
        if (this.postProcessors.contains(processor))
            throw IllegalArgumentException("Post-processor $processor was already registered!")

        this.postProcessors[processor] = settings
    }
}
