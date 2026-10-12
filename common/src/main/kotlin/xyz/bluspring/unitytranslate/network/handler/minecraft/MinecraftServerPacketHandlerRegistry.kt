package xyz.bluspring.unitytranslate.network.handler.minecraft

import io.netty.buffer.ByteBuf
import net.minecraft.server.level.ServerPlayer
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.modernnetworking.api.v2.packet.registry.handler.PacketHandlerRegistry
import xyz.bluspring.modernnetworking.minecraft.api.v2.packet.MinecraftServerPacketHandlers
import xyz.bluspring.unitytranslate.network.handler.ServerHandlerContext
import java.util.*

object MinecraftServerPacketHandlerRegistry : PacketHandlerRegistry<ServerHandlerContext, UUID> {
    override fun <B : ByteBuf, T : NetworkPacket> register(definition: PacketDefinition<B, T>, handler: PacketHandlerRegistry.PacketHandler<T, xyz.bluspring.unitytranslate.network.handler.ServerHandlerContext>) {
        MinecraftServerPacketHandlers.PLAY.register(definition) { packet, context ->
            handler.handle(packet,
                ServerHandlerContext(
                    context.player.uuid,
                    MinecraftServerProxy(context.server)
                )
            )
        }
    }

    override fun <T : NetworkPacket> send(receiver: UUID, packet: T) {
    }

    @Suppress("UnusedReceiverParameter") // funny workaround
    fun <T : NetworkPacket> PacketHandlerRegistry<ServerHandlerContext, UUID>.send(receiver: ServerPlayer, packet: T) {
        MinecraftServerPacketHandlers.PLAY.send(receiver, packet)
    }
}
