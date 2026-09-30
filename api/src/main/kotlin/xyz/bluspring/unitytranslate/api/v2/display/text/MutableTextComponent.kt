package xyz.bluspring.unitytranslate.api.v2.display.text

import java.util.*

class MutableTextComponent @JvmOverloads constructor(
    override val contents: ComponentContents,
    override var style: Style = Style.EMPTY,
    override val siblings: MutableList<TextComponent> = mutableListOf(),
) : TextComponent {
    fun withStyle(style: Style): MutableTextComponent {
        this.style = this.style.merge(style)
        return this
    }

    fun withStyle(styleBuilder: (Style) -> Style): MutableTextComponent {
        this.style = styleBuilder(this.style)
        return this
    }

    fun withColor(color: Int): MutableTextComponent {
        this.style = this.style.withColor(color)
        return this
    }

    fun append(text: String): MutableTextComponent {
        this.siblings.add(TextComponent.literal(text))
        return this
    }

    fun append(text: TextComponent): MutableTextComponent {
        this.siblings.add(text)
        return this
    }

    override fun hashCode(): Int {
        var hash = Objects.hash(this.contents, this.style)

        for (sibling in this.siblings) {
            hash = 31 * hash + sibling.hashCode()
        }

        return hash
    }

    override fun toString(): String {
        return "MutableTextComponent[text=\"${this.string}\",style=${this.style}]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        if (other !is TextComponent)
            return false

        if (contents != other.contents) return false
        if (style != other.style) return false
        if (siblings != other.siblings) return false

        return true
    }
}
