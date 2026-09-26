package xyz.bluspring.unitytranslate.integration.talk_balloons

import com.cerbon.talk_balloons.api.TalkBalloonsApi
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.event.TranscriptEvent
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptData.Companion.id
import xyz.bluspring.unitytranslate.api.v2.transcriber.sender.PlayerUser
import xyz.bluspring.unitytranslate.client.transcriber.sender.MinecraftLocalTranscriptUser
import java.util.*

object TalkBalloonsIntegration {
    fun setup() {
        val balloonLanguage = UnityTranslateApi.instance.registerOutputLanguage("balloon")

        val existingMessages = Collections.synchronizedMap<String, Component>(mutableMapOf())
        TranscriptEvent.UPDATED.register { holder, data ->
            if (holder == balloonLanguage.transcriptHolder) {
                if (data.message.isBlank())
                    return@register

                if (data.sender is PlayerUser) {
                    Minecraft.getInstance().execute {
                        val messages = TalkBalloonsApi.INSTANCE.getBalloonMessages(Minecraft.getInstance().player!!)
                        val player = Minecraft.getInstance().level?.getPlayerByUUID((data.sender as PlayerUser).uuid)
                            ?: return@execute

                        if (existingMessages.contains(data.id)) {
                            val message = existingMessages[data.id]!!
                            TalkBalloonsApi.INSTANCE.getBalloonMessages(player).remove(message)
                            existingMessages.remove(data.id)
                        }

                        val message = Component.literal(data.message)
                        synchronized(messages) {
                            TalkBalloonsApi.INSTANCE.createBalloonMessage(player, message, TalkBalloonsApi.INSTANCE.defaultDuration * 20)
                        }
                        existingMessages[data.id] = message
                    }
                } else if (data.sender is MinecraftLocalTranscriptUser) {
                    Minecraft.getInstance().execute {
                        val messages = TalkBalloonsApi.INSTANCE.getBalloonMessages(Minecraft.getInstance().player!!)

                        if (existingMessages.contains(data.id)) {
                            val message = existingMessages[data.id]!!
                            synchronized(messages) {
                                messages.remove(message)
                            }

                            existingMessages.remove(data.id)
                        }

                        val message = Component.literal(data.message)
                        synchronized(messages) {
                            TalkBalloonsApi.INSTANCE.createBalloonMessage(Minecraft.getInstance().player!!, message, TalkBalloonsApi.INSTANCE.defaultDuration * 20)
                        }
                        existingMessages[data.id] = message
                    }
                }
            }
        }
    }
}
