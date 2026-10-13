package xyz.bluspring.unitytranslate.minecraft

import net.minecraft.server.level.ServerPlayer
import xyz.bluspring.unitytranslate.network.handler.UnityTranslateNetworkHandlers
import xyz.bluspring.unitytranslate.network.handler.minecraft.MinecraftServerPacketHandlerRegistry.send
import xyz.bluspring.unitytranslate.server.ServerConfig
import xyz.bluspring.unitytranslate.server.UnityTranslateServer
import xyz.bluspring.unitytranslate.shared.Constants
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.NegotiationPacket

object UnityTranslateMCServer {
    fun onPlayerJoin(player: ServerPlayer) {
        UnityTranslateNetworkHandlers.SERVER.send(player, NegotiationPacket(Constants.COMMON_PROTOCOL_VERSION,
            ServerConfig.allowSelfTranslation))
    }

    fun onPlayerLeave(player: ServerPlayer) {
        UnityTranslateServer.removePlayer(player.uuid)
    }
}
