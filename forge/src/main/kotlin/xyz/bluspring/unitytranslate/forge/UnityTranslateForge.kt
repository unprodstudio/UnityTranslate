package xyz.bluspring.unitytranslate.forge

import net.minecraftforge.fml.common.Mod
import xyz.bluspring.unitytranslate.UnityTranslate

@Mod(UnityTranslate.MOD_ID)
class UnityTranslateForge {
    init {
        UnityTranslate.initGame()
    }
}
