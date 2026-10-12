package xyz.bluspring.unitytranslate.network.handler.minecraft

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.modernnetworking.api.v2.packet.registry.handler.PacketHandlerRegistry
import xyz.bluspring.modernnetworking.api.v2.packet.registry.handler.SingleReceiverPacketHandlerRegistry
import xyz.bluspring.modernnetworking.minecraft.api.v2.packet.client.MinecraftClientPacketHandlers
import xyz.bluspring.unitytranslate.network.handler.ClientHandlerContext

object MinecraftClientPacketHandlerRegistry : SingleReceiverPacketHandlerRegistry<ClientHandlerContext> {
    override fun <B : ByteBuf, T : NetworkPacket> register(definition: PacketDefinition<B, T>, handler: PacketHandlerRegistry.PacketHandler<T, ClientHandlerContext>) {
        MinecraftClientPacketHandlers.PLAY.register(definition) { packet, context ->
            handler.handle(packet, ClientHandlerContext(MinecraftClientProxy(context.client)))
        }
    }

    override fun <T : NetworkPacket> send(packet: T) {
        MinecraftClientPacketHandlers.PLAY.send(packet)
    }
}
