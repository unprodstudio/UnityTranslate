package xyz.bluspring.unitytranslate.network.handler.minecraft

import net.minecraft.server.MinecraftServer
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.network.handler.DisplayNameGetter
import xyz.bluspring.unitytranslate.util.PlatformConversion.asUnityTranslate
import java.util.*

class MinecraftServerProxy(val server: MinecraftServer) : DisplayNameGetter {
    override fun getDisplayName(uuid: UUID): TextComponent {
        return server.playerList.getPlayer(uuid)?.displayName?.asUnityTranslate() ?: TextComponent.literal("$uuid")
    }
}
