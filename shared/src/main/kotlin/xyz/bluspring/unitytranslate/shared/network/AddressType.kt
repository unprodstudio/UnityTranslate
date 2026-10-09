package xyz.bluspring.unitytranslate.shared.network

import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs

enum class AddressType {
    IPV4, IPV6,
    ;

    companion object {
        val CODEC = NetworkCodecs.enumCodec(AddressType::class.java)
    }
}
