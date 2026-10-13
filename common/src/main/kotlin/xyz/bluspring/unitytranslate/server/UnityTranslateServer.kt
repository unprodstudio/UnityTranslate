package xyz.bluspring.unitytranslate.server

import kotlinx.coroutines.ExperimentalCoroutinesApi
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.util.LangPair
import xyz.bluspring.unitytranslate.api.v2.util.onComplete
import xyz.bluspring.unitytranslate.network.handler.UnityTranslateNetworkHandlers
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.TranslatePacket
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.UpdateUserTranscriptPacket
import xyz.bluspring.unitytranslate.translator.TranslatorManagerImpl
import java.util.*

object UnityTranslateServer {
    val languageInfos: MutableMap<UUID, UserLanguageInfo> = Collections.synchronizedMap(mutableMapOf<UUID, UserLanguageInfo>())
    val languagesToTranslate: Collection<Language>
        get() = this.languageInfos.flatMap { it.value.languages }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun init() {
        UnityTranslateApi.instance.registerConfig("${UnityTranslate.MOD_ID}_server") {
            boolean("allow_self_translation", ServerConfig::allowSelfTranslation)
        }

        UnityTranslateNetworkHandlers.SERVER.register(UnityTranslateCommonPackets.CLIENT_INFO) { packet, ctx ->
            UnityTranslate.logger.debug("User {} connected on {} using UnityTranslate {} on protocol version {}", ctx.source, packet.platform, packet.clientVersion, packet.protocolVersion)
            // TODO: do something with this info
        }

        UnityTranslateNetworkHandlers.SERVER.register(UnityTranslateCommonPackets.REQUEST_TRANSLATION) { packet, ctx ->
            val deferred = UnityTranslateApi.instance.translatorManager.queue(packet.text, packet.sourceLanguage, packet.targetLanguage)
            deferred.onComplete { text ->
                ctx.serverProxy.sendToSender(TranslatePacket(packet.requestId, text, packet.sourceLanguage, packet.targetLanguage))
            }
        }

        UnityTranslateNetworkHandlers.SERVER.register(UnityTranslateCommonPackets.UPDATE_LANGUAGES) { packet, ctx ->
            UnityTranslate.logger.debug("User {} updated languages: {} (self-translating: {})", ctx.source,
                packet.languages.joinToString(", "), packet.selfTranslating.joinToString(", "))
            this.languageInfos[ctx.source] = UserLanguageInfo(packet.languages, packet.selfTranslating)
        }

        UnityTranslateNetworkHandlers.SERVER.register(UnityTranslateCommonPackets.UPDATE_TRANSCRIPT) { packet, ctx ->
            ctx.serverProxy.broadcast(UpdateUserTranscriptPacket(ctx.source, packet.transcript, packet.sourceLanguage, packet.targetLanguage.orElse(packet.sourceLanguage)!!, packet.timeCreated, packet.timeUpdated))

            // Handle the actual translation part
            if (packet.targetLanguage.isEmpty) {
                val userInfo = this.languageInfos[ctx.source]

                for (targetLanguage in this.languagesToTranslate) {
                    // if the user is translating this themselves, trust them, hopefully.
                    if (ServerConfig.allowSelfTranslation && userInfo != null && userInfo.selfTranslating.contains(targetLanguage))
                        continue

                    val deferred = TranslatorManagerImpl.queueWithId(packet.transcript, LangPair(packet.sourceLanguage, targetLanguage), "${ctx.source}/${packet.timeCreated}", packet.timeUpdated)
                    deferred.onComplete { text ->
                        ctx.serverProxy.broadcast(UpdateUserTranscriptPacket(ctx.source, text, packet.sourceLanguage, targetLanguage, packet.timeCreated, packet.timeUpdated))
                    }
                }
            }
        }
    }

    fun removePlayer(player: UUID) {
        this.languageInfos.remove(player)
    }
}
