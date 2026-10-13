package xyz.bluspring.unitytranslate.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.minecraft.UnityTranslateMCServer

class UnityTranslateFabric : ModInitializer {
    override fun onInitialize() {
        UnityTranslate.initGame()

        ServerPlayerEvents.JOIN.register { player ->
            UnityTranslateMCServer.onPlayerJoin(player)
        }

        ServerPlayerEvents.LEAVE.register { player ->
            UnityTranslateMCServer.onPlayerLeave(player)
        }
    }
}
