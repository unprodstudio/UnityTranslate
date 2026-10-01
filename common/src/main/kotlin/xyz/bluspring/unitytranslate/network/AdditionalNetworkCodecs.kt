package xyz.bluspring.unitytranslate.network

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodec
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.api.v2.Language

object AdditionalNetworkCodecs {
    // TODO: this should probably be in Language itself
    val LANGUAGE: NetworkCodec<ByteBuf, Language> = NetworkCodecs.STRING_UTF8.xmap(Language::parse, Language::formatted)
}
