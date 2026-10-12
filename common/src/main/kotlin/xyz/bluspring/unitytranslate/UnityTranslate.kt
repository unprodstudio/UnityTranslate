package xyz.bluspring.unitytranslate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.config.ConfigBuilder
import xyz.bluspring.unitytranslate.api.v2.display.LanguageDisplay
import xyz.bluspring.unitytranslate.api.v2.display.text.Style
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.transcriber.InactiveTranscriber
import xyz.bluspring.unitytranslate.api.v2.transcriber.SpeechTranscriber
import xyz.bluspring.unitytranslate.client.UnityTranslateClient
import xyz.bluspring.unitytranslate.client.config.ClientConfig
import xyz.bluspring.unitytranslate.client.config.TranscriptBoxConfig
import xyz.bluspring.unitytranslate.config.builders.SunsetWrappedConfigBuilder
import xyz.bluspring.unitytranslate.config.values.HiddenReflectingConfigValue
import xyz.bluspring.unitytranslate.integration.UnityTranslateIntegration
import xyz.bluspring.unitytranslate.plugin.PluginManager
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
import java.text.DecimalFormat
import kotlin.reflect.KMutableProperty

object UnityTranslate {
    const val MOD_ID = Constants.MOD_ID
    val logger: Logger = LoggerFactory.getLogger("UnityTranslate")
    val transcriberManager by lazy {
        TranscriberManager() // Server transcriber manager, shouldn't be active unless it's enabled.
    }

    fun init() {
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

        UnityTranslateApi.instance.registerConfig("unitytranslate") {
            category("hud") {
                category("default_box_settings") {
                    intColor("text", TranscriptBoxConfig.Defaults::textColor)
                    intColor("shadow", TranscriptBoxConfig.Defaults::shadowColor)

                    value("outline", TranscriptBoxConfig.Outline.CODEC, TranscriptBoxConfig.Defaults::outline)
                    value("background", TranscriptBoxConfig.Background.CODEC, TranscriptBoxConfig.Defaults::background)
                    value("transcript_display", TranscriptBoxConfig.TranscriptDisplay.CODEC, TranscriptBoxConfig.Defaults::transcriptDisplay)

                    category("header") {
                        value("display", TranscriptBoxConfig.HeaderDisplay.CODEC, TranscriptBoxConfig.Defaults.header::display)
                        value("style", Style.CODEC, TranscriptBoxConfig.Defaults.header::style)
                        value("lang_display", LanguageDisplay.CODEC, TranscriptBoxConfig.Defaults.header::langDisplay)
                        value("lang_style", Style.CODEC, TranscriptBoxConfig.Defaults.header::langStyle)
                        value("lang_decoration", TranscriptBoxConfig.LanguageDecoration.CODEC, TranscriptBoxConfig.Defaults.header::langDecoration)
                        value("align_x", TranscriptBoxConfig.Header.HorizontalAlignment.CODEC, TranscriptBoxConfig.Defaults.header::alignX)
                        value("align_y", TranscriptBoxConfig.Header.VerticalAlignment.CODEC, TranscriptBoxConfig.Defaults.header::alignY)
                        boolean("has_shadow", TranscriptBoxConfig.Defaults.header::hasShadow)
                    }

                    category("padding") {
                        float("left", 0f, 32f, step = 0.1f, property = TranscriptBoxConfig.Defaults.padding::left) {
                            formatting {
                                TextComponent.literal("${"%.1f".format(it)} px")
                            }
                        }

                        float("right", 0f, 32f, step = 0.1f, property = TranscriptBoxConfig.Defaults.padding::right) {
                            formatting {
                                TextComponent.literal("${"%.1f".format(it)} px")
                            }
                        }

                        float("top", 0f, step = 0.1f, property = TranscriptBoxConfig.Defaults.padding::top) {
                            formatting {
                                TextComponent.literal("${"%.1f".format(it)} px")
                            }
                        }

                        float("bottom", 0f, step = 0.1f, property = TranscriptBoxConfig.Defaults.padding::bottom) {
                            formatting {
                                TextComponent.literal("${"%.1f".format(it)} px")
                            }
                        }
                    }

                    float("corner_radius", min = 0f, max = 16f, step = 0.5f, property = TranscriptBoxConfig.Defaults::cornerRadius) {
                        formatting {
                            TextComponent.literal("${"%.1f".format(it)} px")
                        }
                    }

                    float("font_scale", min = 0f, max = 16f, step = 0.5f, property = TranscriptBoxConfig.Defaults::fontScale) {
                        formatting {
                            if (it == 0f)
                                TextComponent.translatable("unitytranslate.config.none")
                            else
                                TextComponent.literal("${"%.1f".format(it * 100f)}%")
                        }
                    }

                    integer("time_to_live", min = 0, max = 180_000, step = 250, property = TranscriptBoxConfig.Defaults::msToLive) {
                        formatting {
                            if (it == 0)
                                TextComponent.translatable("unitytranslate.measurement.seconds", "∞")
                            else
                                TextComponent.translatable("unitytranslate.measurement.seconds", DecimalFormat("0.###").format((it / 1000.0)))
                        }
                    }

                    integer("time_to_fade_out", min = 0, max = 5_000, step = 250, property = TranscriptBoxConfig.Defaults::msToFadeOut) {
                        formatting {
                            TextComponent.translatable("unitytranslate.measurement.seconds", DecimalFormat("0.###").format((it / 1000.0)))
                        }
                    }
                }

                listValue("transcript_boxes", TranscriptBoxConfig.CODEC, ClientConfig::transcriptBoxes)
            }

            category("transcriber") {
                value("type", SpeechTranscriber.CODEC, ClientConfig::transcriber)
                integer("interval", 250, 15_000, 1, ClientConfig::transcriptionInterval)
            }

            category("languages") {
                string("spoken", ClientConfig::spokenLanguage)

                for ((langKey, langHolder) in UnityTranslateApiImpl.outputLanguages) {
                    value(langKey, Language.CODEC, langHolder::language)
                }
            }

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

            if (this is SunsetWrappedConfigBuilder) {
                this.wrapped.custom(HiddenReflectingConfigValue("handled_first_join", Codec.BOOL, UnityTranslateClient::handledFirstJoin))
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

        for ((_, config) in UnityTranslateApiImpl.configs) {
            config.load()
        }

        TranslatorManagerImpl.startTicking()
    }

    @JvmStatic
    fun id(path: String): Identifier {
        return Identifier.fromNamespaceAndPath(MOD_ID, path)
    }
}
