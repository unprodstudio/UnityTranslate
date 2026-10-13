package xyz.bluspring.unitytranslate.server

import xyz.bluspring.unitytranslate.api.v2.Language

@JvmRecord
data class UserLanguageInfo(
    val languages: Collection<Language>,
    val selfTranslating: Collection<Language>,
)
