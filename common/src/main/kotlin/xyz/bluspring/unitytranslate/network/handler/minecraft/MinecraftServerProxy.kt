package xyz.bluspring.unitytranslate.network.handler.minecraft

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.minecraft.api.v2.packet.MinecraftServerPacketHandlers
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.network.handler.ServerProxy
import xyz.bluspring.unitytranslate.util.PlatformConversion.asUnityTranslate
import java.util.*

class MinecraftServerProxy(val player: ServerPlayer, val server: MinecraftServer) : ServerProxy {
    override fun getDisplayName(uuid: UUID): TextComponent {
        return server.playerList.getPlayer(uuid)?.displayName?.asUnityTranslate() ?: TextComponent.literal("$uuid")
    }

    override fun <T : NetworkPacket> sendToSender(packet: T) {
        MinecraftServerPacketHandlers.PLAY.send(player, packet)
    }

    override fun <T : NetworkPacket> broadcast(packet: T) {
        for (player in this.server.playerList.players) {
            MinecraftServerPacketHandlers.PLAY.send(player, packet)
        }
    }
}
