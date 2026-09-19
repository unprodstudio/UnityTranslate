package xyz.bluspring.unitytranslate.fabric.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.UnityTranslateClient
import xyz.bluspring.unitytranslate.client.gui.LogoTransitionOverlay
import xyz.bluspring.unitytranslate.client.renderer.UnityTranslateGui
import xyz.bluspring.unitytranslate.client.renderer.ui.MinecraftUIGraphics

class UnityTranslateFabricClient : ClientModInitializer {
    //? if >= 26.2 {
    private val Minecraft.screen: Screen?
        get() = this.gui.screen()
    //? }

    override fun onInitializeClient() {
        UnityTranslateClient.init()

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, UnityTranslate.id("transcript_boxes")) { graphics, deltaTracker ->
            val mouse = Minecraft.getInstance().mouseHandler
            val g = MinecraftUIGraphics(graphics)
            UnityTranslateGui.submit(g, deltaTracker.getGameTimeDeltaPartialTick(true), mouse.xpos() / ClientPlatformProxy.instance.guiScale, mouse.ypos() / ClientPlatformProxy.instance.guiScale)
            g.flushLastLayer()
        }

        var shouldStartFirstLaunch = false
        var ticksUntilFirstLaunch = 45

        ClientLifecycleEvents.CLIENT_STARTED.register { _ ->
            if (!UnityTranslateClient.handledFirstJoin) {
                shouldStartFirstLaunch = true
            }
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register { _ ->
            UnityTranslateClient.onClose()
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            UnityTranslateClient.tick()

            if (!UnityTranslateClient.handledFirstJoin && client.screen is TitleScreen && shouldStartFirstLaunch && ticksUntilFirstLaunch-- <= 0) {
                LogoTransitionOverlay.currentTick = 0
                UnityTranslateClient.handledFirstJoin = true
            }
        }

//        runBlocking {
//            UnityTranslateApiImpl.setActiveTranscriber(UnityTranslateApi.instance.getTranscriber("unitytranslate_whisper"))
//        }
    }
}
