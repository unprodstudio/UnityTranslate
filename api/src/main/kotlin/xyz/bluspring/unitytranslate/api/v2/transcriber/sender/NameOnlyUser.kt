package xyz.bluspring.unitytranslate.api.v2.transcriber.sender

import org.joml.Vector3f
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent

@JvmRecord
data class NameOnlyUser(
    override val displayName: TextComponent,
) : TranscriptUser {
    constructor(name: String) : this(TextComponent.literal(name))

    override val pos: Vector3f?
        get() = null
}
