package xyz.bluspring.unitytranslate.network.handler.minecraft

import net.minecraft.client.Minecraft
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.transcriber.sender.NameOnlyUser
import xyz.bluspring.unitytranslate.api.v2.transcriber.sender.PlayerUser
import xyz.bluspring.unitytranslate.api.v2.transcriber.sender.TranscriptUser
import xyz.bluspring.unitytranslate.client.transcriber.sender.MinecraftLocalTranscriptUser
import xyz.bluspring.unitytranslate.network.handler.ClientProxy
import xyz.bluspring.unitytranslate.util.PlatformConversion.asUnityTranslate
import java.util.*

class MinecraftClientProxy(val client: Minecraft) : ClientProxy {
    override fun getDisplayName(uuid: UUID): TextComponent {
        return client.level?.getPlayerByUUID(uuid)?.displayName?.asUnityTranslate() ?: TextComponent.literal("$uuid")
    }

    override fun getUser(uuid: UUID): TranscriptUser {
        val player = client.level?.getPlayerByUUID(uuid)

        if (player != null && player == client.player)
            return MinecraftLocalTranscriptUser

        val displayName = this.getDisplayName(uuid)
        if (player != null)
            return PlayerUser(uuid, player.position().toVector3f(), displayName)

        return NameOnlyUser(displayName)
    }
}
