package xyz.bluspring.unitytranslate.network.handler.minecraft

import net.minecraft.client.Minecraft
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.network.handler.DisplayNameGetter
import xyz.bluspring.unitytranslate.util.PlatformConversion.asUnityTranslate
import java.util.*

class MinecraftClientProxy(val client: Minecraft) : DisplayNameGetter {
    override fun getDisplayName(uuid: UUID): TextComponent {
        return client.level?.getPlayerByUUID(uuid)?.displayName?.asUnityTranslate() ?: TextComponent.literal("$uuid")
    }
}
