package xyz.bluspring.unitytranslate.neoforge

import net.neoforged.fml.common.Mod
import xyz.bluspring.unitytranslate.UnityTranslate

@Mod(UnityTranslate.MOD_ID)
class UnityTranslateNeoForge {
    init {
        UnityTranslate.initGame()
    }
}
