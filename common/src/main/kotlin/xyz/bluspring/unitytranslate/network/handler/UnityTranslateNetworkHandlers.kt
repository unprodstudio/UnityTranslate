package xyz.bluspring.unitytranslate.network.handler

import xyz.bluspring.unitytranslate.PlatformProxy
import xyz.bluspring.unitytranslate.network.handler.minecraft.MinecraftClientPacketHandlerRegistry
import xyz.bluspring.unitytranslate.network.handler.minecraft.MinecraftServerPacketHandlerRegistry
import xyz.bluspring.unitytranslate.network.handler.standalone.StandaloneClientPacketHandlerRegistry
import xyz.bluspring.unitytranslate.network.handler.standalone.StandaloneServerPacketHandlerRegistry

object UnityTranslateNetworkHandlers {
    val SERVER by lazy {
        if (PlatformProxy.instance.isStandalone())
            StandaloneServerPacketHandlerRegistry
        else
            MinecraftServerPacketHandlerRegistry
    }

    val CLIENT by lazy {
        if (PlatformProxy.instance.isStandalone())
            StandaloneClientPacketHandlerRegistry
        else
            MinecraftClientPacketHandlerRegistry
    }
}
