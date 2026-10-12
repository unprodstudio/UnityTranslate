package xyz.bluspring.unitytranslate.network.handler

import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import java.util.*

interface DisplayNameGetter {
    fun getDisplayName(uuid: UUID): TextComponent
}
