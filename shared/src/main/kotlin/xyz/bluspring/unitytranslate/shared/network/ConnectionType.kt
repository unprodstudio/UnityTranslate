package xyz.bluspring.unitytranslate.shared.network

import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs

enum class ConnectionType {
    P2P, PROXY,
    ;

    companion object {
        val CODEC = NetworkCodecs.enumCodec(ConnectionType::class.java)
    }
}
