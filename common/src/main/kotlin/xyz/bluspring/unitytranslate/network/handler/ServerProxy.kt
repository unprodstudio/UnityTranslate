package xyz.bluspring.unitytranslate.network.handler

import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket

interface ServerProxy : DisplayNameGetter {
    fun <T : NetworkPacket> sendToSender(packet: T)
    fun <T : NetworkPacket> broadcast(packet: T)
}
