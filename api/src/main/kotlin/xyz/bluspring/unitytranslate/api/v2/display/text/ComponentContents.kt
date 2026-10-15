package xyz.bluspring.unitytranslate.api.v2.display.text

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.display.text.ComponentUtils.join
import java.util.*
import java.util.function.Function

sealed interface ComponentContents {
    val text: String

    fun visit(visitor: TextComponent.TextVisitor)
    fun visit(visitor: TextComponent.StyledTextVisitor, parentStyle: Style = Style.EMPTY)

    @JvmRecord
    data class Literal(override val text: String) : ComponentContents {
        override fun visit(visitor: TextComponent.StyledTextVisitor, parentStyle: Style) {
            visitor.visit(this.text, parentStyle)
        }

        override fun visit(visitor: TextComponent.TextVisitor) {
            visitor.visit(this.text)
        }

        companion object {
            @JvmField val CODEC: MapCodec<Literal> = Codec.STRING.fieldOf("text").xmap(::Literal, Literal::text)
        }
    }

    @JvmRecord
    data class Translatable @JvmOverloads constructor(val key: String, val args: List<Any?>, val fallbackGetter: () -> Optional<String> = Optional<String>::empty) : ComponentContents {
        constructor(key: String, args: List<Any?>, fallback: Optional<String>) : this(key, args, { fallback })
        constructor(key: String, args: List<Any?>, fallback: String) : this(key, args, Optional.of(fallback))

        val fallback: Optional<String>
            get() = this.fallbackGetter()

        private fun decomposed(style: Style = Style.EMPTY): List<TextComponent> {
            val texts = mutableListOf<TextComponent>()
            val translated = if (this.fallback.isPresent)
                UnityTranslateApi.instance.platform.translated(this.key, this.fallback.orElseThrow())
            else
                UnityTranslateApi.instance.platform.translated(this.key)
            val replacements = args.map { arg ->
                if (arg is TextComponent) {
                    if (arg.contents is Translatable) {
                        val contents = arg.contents as Translatable
                        contents.decomposed(style.merge(arg.style)).join()
                    } else if (arg.contents is Literal) {
                        arg.copy().withStyle(style.merge(arg.style))
                    } else {
                        TextComponent.literal("$arg").withStyle(style)
                    }
                } else {
                    TextComponent.literal("$arg").withStyle(style)
                }
            }

            var argIndex = 0
            var lastTextIndex = 0

            val matched = FORMAT_PATTERN.findAll(translated).toList()
            for (result in matched) {
                if (result.range.first != 0) {
                    val text = translated.substring(lastTextIndex, result.range.first)
                    texts.add(TextComponent.literal(text).withStyle(style))
                }

                if (result.value == "%%") {
                    texts.add(TextComponent.literal("%"))
                } else {
                    val formatType = result.groupValues.getOrNull(2)
                    if (formatType != "s") {
                        throw IllegalArgumentException("Unsupported format: ${result.value}")
                    }

                    val potentialIndex = result.groupValues.getOrNull(1)
                    val index = (potentialIndex?.toIntOrNull() ?: (argIndex++ + 1)) - 1
                    texts.add(replacements.getOrNull(index) ?: TextComponent.literal(result.value).withStyle(style))
                }

                lastTextIndex = result.range.last + 1
            }

            if (lastTextIndex < translated.length) {
                texts.add(TextComponent.literal(translated.substring(lastTextIndex, translated.length)).withStyle(style))
            }

            return texts
        }

        override val text: String
            get() {
                var text = ""
                this.visit { text += it }
                return text
            }

        override fun visit(visitor: TextComponent.StyledTextVisitor, parentStyle: Style) {
            for (component in decomposed(parentStyle)) {
                component.visit(visitor)
            }
        }

        override fun visit(visitor: TextComponent.TextVisitor) {
            for (component in decomposed()) {
                component.visit(visitor)
            }
        }

        companion object {
            @JvmField val CODEC: MapCodec<Translatable> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.STRING.fieldOf("translate")
                        .forGetter(Translatable::key),
                    Codec.STRING.listOf().optionalFieldOf("with", emptyList())
                        .xmap<List<Any?>>({ it }, { it.map { b -> b.toString() } })
                        .forGetter(Translatable::args),
                    Codec.STRING.optionalFieldOf("fallback")
                        .forGetter(Translatable::fallback),
                )
                    .apply(instance, ::Translatable)
            }

            private val FORMAT_PATTERN = Regex("%(?:(\\d+)\\$)?([A-Za-z%]|$)")
        }
    }

    companion object {
        @JvmField val CODEC: MapCodec<ComponentContents> = Codec.mapEither(Literal.CODEC, Translatable.CODEC)
            .xmap({ it.map(Function.identity(), Function.identity()) }, { it ->
                when (it) {
                    is Literal -> Either.left(it)
                    is Translatable -> Either.right(it)
                }
            })
    }
}
