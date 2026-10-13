package xyz.bluspring.unitytranslate.client

import com.mojang.serialization.Codec
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.bluspring.unitytranslate.PlatformProxy
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.UnityTranslateApiImpl
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.display.LanguageDisplay
import xyz.bluspring.unitytranslate.api.v2.display.text.Style
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.event.TranscriptEvent
import xyz.bluspring.unitytranslate.api.v2.transcriber.InactiveTranscriber
import xyz.bluspring.unitytranslate.api.v2.transcriber.SpeechTranscriber
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptData.Companion.id
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import xyz.bluspring.unitytranslate.api.v2.util.onComplete
import xyz.bluspring.unitytranslate.client.config.ClientConfig
import xyz.bluspring.unitytranslate.client.config.TranscriptBoxConfig
import xyz.bluspring.unitytranslate.client.gui.MouseHelper
import xyz.bluspring.unitytranslate.client.renderer.UnityTranslateGui
import xyz.bluspring.unitytranslate.config.builders.SunsetWrappedConfigBuilder
import xyz.bluspring.unitytranslate.config.values.HiddenReflectingConfigValue
import xyz.bluspring.unitytranslate.network.handler.UnityTranslateNetworkHandlers
import xyz.bluspring.unitytranslate.shared.Constants
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.ClientInfoPacket
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.UpdateLanguagesPacket
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.UpdateTranscriptPacket
import xyz.bluspring.unitytranslate.transcriber.DirectTranscriptData
import xyz.bluspring.unitytranslate.transcriber.TranscriberManager
import xyz.bluspring.unitytranslate.transcriber.TranslatedTranscriptData
import xyz.bluspring.unitytranslate.translator.TranslatorManagerImpl
import java.text.DecimalFormat
import java.util.*

object UnityTranslateClient {
    var handledFirstJoin = false
    val transcriberManager by lazy {
        TranscriberManager() // Client transcriber manager
    }

    fun init() {
        if (!PlatformProxy.instance.isStandalone()) {
            UnityTranslateMCClient.init()
        }

        UnityTranslateApi.instance.registerConfig("${UnityTranslate.MOD_ID}_client") {
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

                listValue("self_translating", Language.CODEC, ClientConfig::selfTranslating)
            }

            if (this is SunsetWrappedConfigBuilder) {
                this.wrapped.custom(HiddenReflectingConfigValue("handled_first_join", Codec.BOOL, UnityTranslateClient::handledFirstJoin))
            }
        }

        TranscriptEvent.UPDATED.register { holder, data ->
            // make sure we're only handling direct transcript data
            if (data !is DirectTranscriptData)
                return@register

            if (UnityTranslateMCClient.serverSupportsTranslator) {
                UnityTranslateNetworkHandlers.CLIENT.send(UpdateTranscriptPacket(data.message, data.language, Optional.empty(), data.timeCreated, data.timeUpdated))
            }

            UnityTranslateApiImpl.transcriptHolders.forEach { (toLang, otherHolder) ->
                if (UnityTranslateMCClient.serverSupportsTranslator && !ClientConfig.selfTranslating.contains(toLang))
                    return@forEach

                TranslatorManagerImpl.queueWithId(data.message, LangPair(holder.language, toLang), data.id, data.timeUpdated)
                    .onComplete { translated ->
                        otherHolder.update(TranslatedTranscriptData(
                            data.timeCreated,
                            data.sender,
                            data.language,
                            translated,
                            data.timeUpdated,
                        ))

                        if (UnityTranslateMCClient.serverSupportsTranslator) {
                            UnityTranslateNetworkHandlers.CLIENT.send(UpdateTranscriptPacket(translated, data.language, Optional.of(toLang), data.timeCreated, data.timeUpdated))
                        }
                    }
            }
        }

        UnityTranslateNetworkHandlers.CLIENT.register(UnityTranslateCommonPackets.NEGOTIATE) { packet, ctx ->
            // TODO: do something with the protocol version
            UnityTranslateMCClient.serverSupportsTranslator = true
            onJoinServer()
        }

        UnityTranslateNetworkHandlers.CLIENT.register(UnityTranslateCommonPackets.UPDATE_USER_TRANSCRIPT) { packet, ctx ->
            UnityTranslateApi.instance.getOrCreateTranscriptHolder(packet.targetLanguage)
                .update(TranslatedTranscriptData(packet.timeCreated, ctx.proxy.getUser(packet.source), packet.sourceLanguage, packet.transcript, packet.timeUpdated))
        }

        UnityTranslate.loadConfigs()

        if (ClientConfig.transcriber !is InactiveTranscriber) {
            runBlocking {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    UnityTranslateApiImpl.setActiveTranscriber(ClientConfig.transcriber)
                }
            }
        }
    }

    fun onJoinServer() {
        UnityTranslateNetworkHandlers.CLIENT.send(ClientInfoPacket(Constants.COMMON_PROTOCOL_VERSION, PlatformProxy.instance.version,
            PlatformProxy.instance.platform, ClientPlatformProxy.instance.username))
        this.updateServerLanguages()
    }

    fun updateServerLanguages() {
        if (!UnityTranslateMCClient.serverSupportsTranslator)
            return

        UnityTranslateNetworkHandlers.CLIENT.send(UpdateLanguagesPacket(UnityTranslateApiImpl.transcriptHolders.keys, ClientConfig.selfTranslating.toSet()))
    }

    fun tick() {
        UnityTranslateGui.tick()
        this.transcriberManager.tick()

        MouseHelper.tick()
    }

    fun onClose() {
        MouseHelper.close()

        runBlocking {
            for (source in UnityTranslateApiImpl.transcriberSources.values) {
                source.reset()
            }

            for (transcriber in UnityTranslateApiImpl.transcribers.values) {
                transcriber.close()
            }

            for (instance in UnityTranslateApiImpl.translators.values) {
                instance.close()
            }
        }
    }
}
