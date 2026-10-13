package xyz.bluspring.unitytranslate.transcriber

import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptData
import xyz.bluspring.unitytranslate.api.v2.transcriber.sender.TranscriptUser

@JvmRecord
data class TranslatedTranscriptData(
    override val timeCreated: Long,
    override val sender: TranscriptUser,
    override val language: Language,
    override val message: String,
    override val timeUpdated: Long,
) : TranscriptData
