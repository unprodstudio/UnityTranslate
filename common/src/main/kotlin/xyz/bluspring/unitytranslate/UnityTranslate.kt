package xyz.bluspring.unitytranslate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.config.ConfigBuilder
import xyz.bluspring.unitytranslate.api.v2.transcriber.InactiveTranscriber
import xyz.bluspring.unitytranslate.integration.UnityTranslateIntegration
import xyz.bluspring.unitytranslate.network.UnityTranslateGamePackets
import xyz.bluspring.unitytranslate.plugin.PluginManager
import xyz.bluspring.unitytranslate.server.UnityTranslateServer
import xyz.bluspring.unitytranslate.shared.Constants
import xyz.bluspring.unitytranslate.transcriber.TranscriberManager
import xyz.bluspring.unitytranslate.transcriber.display.BuiltinLanguageDisplays
import xyz.bluspring.unitytranslate.translator.TranslatorManagerImpl
import xyz.bluspring.unitytranslate.translator.instance.InactiveTranslatorInstance
import xyz.bluspring.unitytranslate.translator.instance.argos.LibreTranslateTranslatorInstance
import xyz.bluspring.unitytranslate.translator.instance.argos.UnityTranslateLibTranslatorInstance
import xyz.bluspring.unitytranslate.translator.instance.google.GoogleCloudTranslatorInstance
import xyz.bluspring.unitytranslate.translator.instance.google.GoogleInternalTranslatorInstance
import xyz.bluspring.unitytranslate.translator.instance.microsoft.MicrosoftInternalTranslatorInstance
import kotlin.reflect.KMutableProperty

object UnityTranslate {
    const val MOD_ID = Constants.MOD_ID
    val logger: Logger = LoggerFactory.getLogger("UnityTranslate")
    val transcriberManager by lazy {
        TranscriberManager() // Server transcriber manager, shouldn't be active unless it's enabled.
    }

    fun initGame() {
        UnityTranslateGamePackets.adaptFromCommon()
        init()
    }

    fun init() {
        UnityTranslateServer.init()

        UnityTranslateApi.instance.registerTranscriber("inactive", InactiveTranscriber) {}
        UnityTranslateApi.instance.registerTranslator("inactive", InactiveTranslatorInstance) {}

        UnityTranslateApi.instance.registerTranslator("unitytranslatelib", UnityTranslateLibTranslatorInstance) {
            boolean("enable_gpu", UnityTranslateLibTranslatorInstance::enableGpu)
        }

        UnityTranslateApi.instance.registerTranslator("libretranslate", LibreTranslateTranslatorInstance) {
            listValue("entries", LibreTranslateTranslatorInstance.Entry.CODEC, LibreTranslateTranslatorInstance::entries)
            boolean("enable_legacy_local", LibreTranslateTranslatorInstance::enableLegacyLocal)
            integer("legacy_local_threads", 1, Runtime.getRuntime().availableProcessors(), 1, LibreTranslateTranslatorInstance::legacyThreads)
        }

        UnityTranslateApi.instance.registerTranslator("microsoft_internal", MicrosoftInternalTranslatorInstance) {
        }

        UnityTranslateApi.instance.registerTranslator("google_internal", GoogleInternalTranslatorInstance) {
        }

        UnityTranslateApi.instance.registerTranslator("google_cloud", GoogleCloudTranslatorInstance) {
            string("api_key", GoogleCloudTranslatorInstance.Config::apiKey)
        }

        UnityTranslateApi.instance.registerLanguageDisplay("none", BuiltinLanguageDisplays.None.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_code/short/lowercase", BuiltinLanguageDisplays.LangCodeShort.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_code/short/uppercase", BuiltinLanguageDisplays.LangCodeShortUppercase.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_code/long/lowercase", BuiltinLanguageDisplays.LangCodeLong.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_code/long/uppercase", BuiltinLanguageDisplays.LangCodeLongUppercase.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_name/short/native", BuiltinLanguageDisplays.LangNameNativeShort.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_name/short/localized", BuiltinLanguageDisplays.LangNameLocalizedShort.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_name/long/native", BuiltinLanguageDisplays.LangNameNative.CODEC)
        UnityTranslateApi.instance.registerLanguageDisplay("lang_name/long/localized", BuiltinLanguageDisplays.LangNameLocalized.CODEC)

        UnityTranslateIntegration.setup()
        PluginManager.loadPlugins()

        UnityTranslateApi.instance.registerConfig("${MOD_ID}_common") {
            category("translator") {
                integer("max_threads", 1, Runtime.getRuntime().availableProcessors(), 1, TranslatorManagerImpl.Config::maxThreads)
                integer("batch_size", 1, 50, 1, TranslatorManagerImpl.Config::batchSize)
                integer("delay_between_batches", 0, 5000, 250, TranslatorManagerImpl.Config::delayBetweenBatches)
                integer("max_translation_words", 50, step = 50, property = TranslatorManagerImpl.Config::maxTranslationWords)
                integer("max_translation_timeout", 1000, 100_000, 250, TranslatorManagerImpl.Config::maxTranslationTimeout)

                listValue("instances", Codec.STRING.dispatch("type", { UnityTranslateApiImpl.getTranslatorId(it) }, {
                    MapCodec.unit { UnityTranslateApiImpl.getTranslator(it) }
                }), TranslatorManagerImpl::instances)
            }
        }

        UnityTranslateApi.instance.registerConfig("unitytranslate_theme") {
            fun ConfigBuilder.color(id: String, property: KMutableProperty<ColorConfig>) {
                value(id, ColorConfig.CODEC, property)
            }

            category("background") {
                color("main", ThemeConfig::mainBackground)
            }

            category("text") {
                intColor("main", ThemeConfig::textColor)
                intColor("warning", ThemeConfig::warningText)
                intColor("off", ThemeConfig::disabledText)
                intColor("on", ThemeConfig::enabledText)
            }

            category("dropdown") {
                color("background", ThemeConfig::dropdownBackground)
                color("open_background", ThemeConfig::dropdownOpenBackground)
                color("open_outline", ThemeConfig::dropdownOpenOutline)

                category("text") {
                    intColor("item", ThemeConfig::dropdownTextItem)
                    intColor("item_hover", ThemeConfig::dropdownTextItemHover)
                    intColor("item_selected", ThemeConfig::dropdownTextItemSelected)
                    intColor("item_disabled", ThemeConfig::dropdownTextItemDisabled)
                    intColor("disabled", ThemeConfig::dropdownTextDisabled)
                }
            }

            category("tooltip") {
                color("background", ThemeConfig::tooltipBackground)
                intColor("text", ThemeConfig::tooltipText)
            }

            category("scrollbar") {
                color("thumb", ThemeConfig::scrollbar)
            }

            category("button") {
                category("plain") {
                    intColor("text", ThemeConfig::plainButton)
                    intColor("hover", ThemeConfig::plainButtonHover)
                    intColor("disabled", ThemeConfig::plainButtonDisabled)
                }

                category("toggle") {
                    color("outline", ThemeConfig::toggleOutline)
                    color("outline_focused", ThemeConfig::toggleOutlineFocused)
                    color("disabled_fill", ThemeConfig::toggleDisabledFill)
                    color("enabled_fill", ThemeConfig::toggleEnabledFill)
                }
            }

            category("config_entry") {
                intColor("text", ThemeConfig::configEntryText)
                intColor("text_focused", ThemeConfig::configEntryTextFocused)
            }

            category("slider") {
                color("track", ThemeConfig::sliderTrack)
                color("track_focused", ThemeConfig::sliderTrackFocused)

                color("notch", ThemeConfig::sliderNotch)
                color("notch_focused", ThemeConfig::sliderNotchFocused)

                intColor("value", ThemeConfig::sliderValue)
                intColor("value_focused", ThemeConfig::sliderValueFocused)
            }

            category("context_box") {
                color("background", ThemeConfig::contextBoxBackground)
                color("outline", ThemeConfig::contextBoxOutline)

                category("element") {
                    color("outline", ThemeConfig::contextBoxElementOutline)
                    color("outline_focused", ThemeConfig::contextBoxElementOutlineFocused)
                    color("background", ThemeConfig::contextBoxElementBackground)
                    color("background_focused", ThemeConfig::contextBoxElementBackgroundFocused)
                    intColor("text", ThemeConfig::contextBoxElementText)
                    intColor("text_focused", ThemeConfig::contextBoxElementTextFocused)
                }
            }

            category("transcript_box") {
                category("highlight") {
                    color("focused", ThemeConfig::transcriptBoxOutlineFocused)
                    color("moving", ThemeConfig::transcriptBoxOutlineMoving)
                }
            }
        }

        loadConfigs()
        TranslatorManagerImpl.startTicking()
    }

    fun loadConfigs() {
        for ((_, config) in UnityTranslateApiImpl.configs) {
            config.load()
        }
    }

    @JvmStatic
    fun id(path: String): Identifier {
        return Identifier.fromNamespaceAndPath(MOD_ID, path)
    }
}
