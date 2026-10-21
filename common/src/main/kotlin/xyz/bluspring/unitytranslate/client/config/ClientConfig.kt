package xyz.bluspring.unitytranslate.client.config

import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.transcriber.InactiveTranscriber
import xyz.bluspring.unitytranslate.api.v2.transcriber.SpeechTranscriber

object ClientConfig {
    var spokenLanguage: String = "en"
    var transcriptBoxes: MutableList<TranscriptBoxConfig> = mutableListOf()
    var selfTranslating: MutableList<Language> = mutableListOf()
    var transcriber: SpeechTranscriber = InactiveTranscriber
    var transcriptionInterval: Int = 250

    var wereBoxesUpdated = false
}
