package xyz.bluspring.unitytranslate.network.handler

import xyz.bluspring.unitytranslate.api.v2.transcriber.sender.TranscriptUser
import java.util.*

interface ClientProxy : DisplayNameGetter {
    fun getUser(uuid: UUID): TranscriptUser
}
