package xyz.bluspring.unitytranslate.client.config

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.ChatFormatting
import org.joml.Vector2f
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.client.gui.TextureReference
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.display.LanguageDisplay
import xyz.bluspring.unitytranslate.api.v2.display.text.Style
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptData
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper
import xyz.bluspring.unitytranslate.transcriber.display.BuiltinLanguageDisplays
import xyz.bluspring.unitytranslate.util.PlatformConversion.withColor
import java.util.*

class TranscriptBoxConfig(
    var language: Language,
    val transforms: Transforms,

    private var _outline: Optional<Outline> = Optional.empty(),
    private var _background: Optional<Background> = Optional.empty(),
    private var _textColor: Optional<Int> = Optional.empty(),
    private var _shadowColor: Optional<Int> = Optional.empty(),
    private var _fontScale: Optional<Float> = Optional.empty(),
    private var _cornerRadius: Optional<Float> = Optional.empty(),
    private var _header: Optional<Header> = Optional.empty(),
    private var _transcriptDisplay: Optional<TranscriptDisplay> = Optional.empty(),
    private var _padding: Optional<Padding> = Optional.empty(),
    private var _msToLive: Optional<Int> = Optional.empty(),
    private var _msToFadeOut: Optional<Int> = Optional.empty(),
) : TranscriptBoxConfigHolder {
    object Defaults : TranscriptBoxConfigHolder {
        override var outline: Outline = Outline()
        override var background: Background = Background.default()
        override var textColor: Int = DEFAULT_TEXT_COLOR
        override var shadowColor: Int = DEFAULT_SHADOW_COLOR
        override var fontScale: Float = 0f // 0 = MC GUI scale
        override var cornerRadius: Float = DEFAULT_CORNER_RADIUS

        override var header: Header = Header.default()
        override var transcriptDisplay: TranscriptDisplay = TranscriptDisplay.default()

        override var padding: Padding = Padding.default()
        override var msToLive: Int = 20_000 // 20 seconds
        override var msToFadeOut: Int = 500 // 0.5 seconds
    }

    override var outline: Outline
        get() = _outline.orElse(Defaults.outline)!!
        set(value) {
            _outline = Optional.of(value)
        }

    override var background: Background
        get() = _background.orElse(Defaults.background)!!
        set(value) {
            _background = Optional.of(value)
        }

    override var textColor: Int
        get() = _textColor.orElse(Defaults.textColor)!!
        set(value) {
            _textColor = Optional.of(value)
        }

    override var shadowColor: Int
        get() = _shadowColor.orElse(Defaults.shadowColor)!!
        set(value) {
            _shadowColor = Optional.of(value)
        }

    override var fontScale: Float
        get() = _fontScale.orElse(Defaults.fontScale)!!
        set(value) {
            _fontScale = Optional.of(value)
        }

    override var cornerRadius: Float
        get() = _cornerRadius.orElse(Defaults.cornerRadius)!!
        set(value) {
            _cornerRadius = Optional.of(value)
        }

    override var header: Header
        get() = _header.orElse(Defaults.header)!!
        set(value) {
            _header = Optional.of(value)
        }

    override var transcriptDisplay: TranscriptDisplay
        get() = _transcriptDisplay.orElse(Defaults.transcriptDisplay)!!
        set(value) {
            _transcriptDisplay = Optional.of(value)
        }

    override var padding: Padding
        get() = _padding.orElse(Defaults.padding)!!
        set(value) {
            _padding = Optional.of(value)
        }

    override var msToLive: Int
        get() = _msToLive.orElse(Defaults.msToLive)!!
        set(value) {
            _msToLive = Optional.of(value)
        }

    override var msToFadeOut: Int
        get() = _msToFadeOut.orElse(Defaults.msToFadeOut)!!
        set(value) {
            _msToFadeOut = Optional.of(value)
        }

    companion object {
        @JvmField val DEFAULT_TEXT_COLOR = ARGBHelper.color(255, 255, 255, 255)
        @JvmField val DEFAULT_SHADOW_COLOR = ARGBHelper.color(255, 0, 0, 0)
        const val DEFAULT_CORNER_RADIUS = 1f

        @JvmField
        val CODEC: Codec<TranscriptBoxConfig> = RecordCodecBuilder.create { instance ->
            instance.group(
                Language.CODEC.fieldOf("language")
                    .forGetter(TranscriptBoxConfig::language),
                Transforms.CODEC.fieldOf("transforms")
                    .forGetter(TranscriptBoxConfig::transforms),

                Outline.CODEC.optionalFieldOf("outline")
                    .forGetter(TranscriptBoxConfig::_outline),
                Background.CODEC.optionalFieldOf("background")
                    .forGetter(TranscriptBoxConfig::_background),
                Codec.INT.optionalFieldOf("text_color")
                    .forGetter(TranscriptBoxConfig::_textColor),
                Codec.INT.optionalFieldOf("shadow_color")
                    .forGetter(TranscriptBoxConfig::_shadowColor),
                Codec.FLOAT.optionalFieldOf("font_scale")
                    .forGetter(TranscriptBoxConfig::_fontScale),
                Codec.FLOAT.optionalFieldOf("corner_radius")
                    .forGetter(TranscriptBoxConfig::_cornerRadius),
                Header.CODEC.optionalFieldOf("header")
                    .forGetter(TranscriptBoxConfig::_header),
                TranscriptDisplay.CODEC.optionalFieldOf("transcript_display")
                    .forGetter(TranscriptBoxConfig::_transcriptDisplay),
                Padding.CODEC.optionalFieldOf("padding")
                    .forGetter(TranscriptBoxConfig::_padding),
            )
                .apply(instance, ::TranscriptBoxConfig)
        }
    }

    data class Padding(
        var left: Float,
        var right: Float,
        var top: Float,
        var bottom: Float,
    ) {
        constructor(padding: Float) : this(padding, padding, padding, padding)

        companion object {
            fun default(): Padding = Padding(2f)

            @JvmField
            val CODEC: Codec<Padding> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.FLOAT.optionalFieldOf("left", 0f)
                        .forGetter(Padding::left),
                    Codec.FLOAT.optionalFieldOf("right", 0f)
                        .forGetter(Padding::right),
                    Codec.FLOAT.optionalFieldOf("top", 0f)
                        .forGetter(Padding::top),
                    Codec.FLOAT.optionalFieldOf("bottom", 0f)
                        .forGetter(Padding::bottom),
                )
                    .apply(instance, ::Padding)
            }
        }
    }

    data class Transforms(
        var position: Position,
        var size: Size,
    ) {
        companion object {
            val CODEC: Codec<Transforms> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Position.CODEC.fieldOf("position")
                        .forGetter(Transforms::position),
                    Size.CODEC.fieldOf("size")
                        .forGetter(Transforms::size)
                )
                    .apply(instance, ::Transforms)
            }
        }

        sealed class Position(val type: String) {
            companion object {
                @JvmField val CODEC: Codec<Position> = Codec.STRING.dispatch("type", Position::type) { type ->
                    when (type) {
                        "absolute" -> Absolute.CODEC
                        "relative" -> Relative.CODEC
                        else -> throw IllegalArgumentException("No position anchor exists by type $type!")
                    }
                }
            }

            abstract fun calculatePos(dimensions: Vector2f, screenWidth: Int, screenHeight: Int): Vector2f

            data class Absolute(var x: Int, var y: Int) : Position("absolute") {
                override fun calculatePos(dimensions: Vector2f, screenWidth: Int, screenHeight: Int): Vector2f {
                    return Vector2f(
                        this.x.coerceAtMost(screenWidth).toFloat(),
                        this.y.coerceAtMost(screenHeight).toFloat()
                    )
                }

                companion object {
                    @JvmField val CODEC: MapCodec<Absolute> = RecordCodecBuilder.mapCodec { instance ->
                        instance.group(
                            Codec.INT.fieldOf("x")
                                .forGetter(Absolute::x),
                            Codec.INT.fieldOf("y")
                                .forGetter(Absolute::y)
                        )
                            .apply(instance, ::Absolute)
                    }
                }
            }

            data class Relative( // preferred, default.
                var xAlign: Float, // screen %
                var yAlign: Float,
            ) : Position("relative") {
                override fun calculatePos(dimensions: Vector2f, screenWidth: Int, screenHeight: Int): Vector2f {
                    return Vector2f((this.xAlign - (dimensions.x / screenWidth)) * screenWidth, (this.yAlign - (dimensions.y / screenHeight)) * screenHeight)
                }

                companion object {
                    @JvmField val CODEC: MapCodec<Relative> = RecordCodecBuilder.mapCodec { instance ->
                        instance.group(
                            Codec.FLOAT.fieldOf("align_x")
                                .forGetter(Relative::xAlign),
                            Codec.FLOAT.fieldOf("align_y")
                                .forGetter(Relative::yAlign)
                        )
                            .apply(instance, ::Relative)
                    }
                }
            }
        }

        /*
        Size should build using the determined position as the origin point for the size.
        For instance, if the position on the screen is (0.45, 0.5) and the size is (350px, 200px), the size
        is determined by expanding by 45% of 350px towards the left and 55% of 350px towards the right.
         */
        sealed class Size(val type: String) {
            companion object {
                @JvmField val CODEC: Codec<Size> = Codec.STRING.dispatch("type", Size::type) { type ->
                    when (type) {
                        "absolute" -> Absolute.CODEC
                        "anchored" -> Anchored.CODEC
                        else -> throw IllegalArgumentException("No size anchor exists by type $type!")
                    }
                }
            }

            abstract fun calculateDimensions(screenWidth: Int, screenHeight: Int): Vector2f

            data class Absolute(var width: Float, var height: Float) : Size("absolute") {
                override fun calculateDimensions(screenWidth: Int, screenHeight: Int): Vector2f {
                    return Vector2f(this.width, this.height)
                }

                companion object {
                    @JvmField val CODEC: MapCodec<Absolute> = RecordCodecBuilder.mapCodec { instance ->
                        instance.group(
                            Codec.FLOAT.fieldOf("width")
                                .forGetter(Absolute::width),
                            Codec.FLOAT.fieldOf("height")
                                .forGetter(Absolute::height)
                        )
                            .apply(instance, ::Absolute)
                    }
                }
            }

            data class Anchored(
                var expectedWidth: Float, // % of screen at the time
                var expectedHeight: Float,

                var targetWidth: Float, // px
                var targetHeight: Float,
            ) : Size("anchored") {
                override fun calculateDimensions(screenWidth: Int, screenHeight: Int): Vector2f {
                    val expectedWidth = this.expectedWidth * screenWidth.toFloat()
                    val expectedHeight = this.expectedHeight * screenHeight.toFloat()
                    val aspectRatio = this.targetWidth / this.targetHeight

                    val actualWidth = if (expectedWidth >= this.targetWidth && expectedHeight >= this.targetHeight)
                        this.targetWidth
                    else if (expectedWidth >= this.targetWidth)
                        this.targetWidth
                    else if (expectedHeight >= this.targetHeight)
                        this.targetHeight * aspectRatio
                    else
                        expectedWidth

                    val actualHeight = if (expectedHeight >= this.targetHeight && expectedWidth >= this.targetWidth)
                        this.targetHeight
                    else if (expectedHeight >= this.targetHeight)
                        this.targetHeight
                    else if (expectedWidth >= this.targetWidth)
                        this.targetWidth * (1f / aspectRatio)
                    else
                        expectedHeight

                    val aspectCorrectedWidth = (actualHeight * this.targetWidth) / this.targetHeight
                    val aspectCorrectedHeight = (actualWidth * this.targetHeight) / this.targetWidth

                    return Vector2f(aspectCorrectedWidth, aspectCorrectedHeight)
                }

                companion object {
                    @JvmField val CODEC: MapCodec<Anchored> = RecordCodecBuilder.mapCodec { instance ->
                        instance.group(
                            Codec.FLOAT.fieldOf("expected_width")
                                .forGetter(Anchored::expectedWidth),
                            Codec.FLOAT.fieldOf("expected_height")
                                .forGetter(Anchored::expectedHeight),

                            Codec.FLOAT.fieldOf("target_width")
                                .forGetter(Anchored::targetWidth),
                            Codec.FLOAT.fieldOf("target_height")
                                .forGetter(Anchored::targetHeight)
                        )
                            .apply(instance, ::Anchored)
                    }
                }
            }
        }
    }

    data class Outline(
        var color: ColorConfig = DEFAULT_COLOR,
        var thickness: Float = DEFAULT_THICKNESS
    ) {
        companion object {
            @JvmField val DEFAULT_COLOR = ColorConfig.Gradient(ColorConfig.Gradient.GradientDirection.BOTTOM, ARGBHelper.color(172, 0, 0, 0), ARGBHelper.color(200, 0, 0, 0))
            const val DEFAULT_THICKNESS = 1f

            @JvmField
            val CODEC: Codec<Outline> = RecordCodecBuilder.create { instance ->
                instance.group(
                    ColorConfig.CODEC.optionalFieldOf("color", DEFAULT_COLOR)
                        .forGetter(Outline::color),
                    Codec.floatRange(0f, 128f).optionalFieldOf("thickness", DEFAULT_THICKNESS)
                        .forGetter(Outline::thickness)
                )
                    .apply(instance, ::Outline)
            }
        }
    }

    sealed class Background(val type: String) {
        companion object {
            @JvmStatic
            fun default(): Background = Color(
                ColorConfig.Gradient(ColorConfig.Gradient.GradientDirection.BOTTOM,
                ARGBHelper.color(128, 0, 0, 0),
                ARGBHelper.color(172, 0, 0, 0)
                )
            )

            @JvmField
            val CODEC: Codec<Background> = Codec.STRING.dispatch("type", Background::type) { type ->
                when (type) {
                    "color" -> Color.CODEC
                    "image" -> Image.CODEC
                    "image_overlay" -> ImageWithOverlay.CODEC
                    else -> throw IllegalArgumentException("No background found by type $type!")
                }
            }
        }

        data class Color(var color: ColorConfig) : Background("color") {
            companion object {
                @JvmField
                val CODEC: MapCodec<Color> = ColorConfig.CODEC.fieldOf("color")
                    .xmap(::Color, Color::color)
            }
        }

        data class Image(
            var texture: TextureReference,
            var u: Float, var v: Float,
            var uWidth: Float, var vHeight: Float,
        ) : Background("image") {
            companion object {
                @JvmField
                val CODEC: MapCodec<Image> = RecordCodecBuilder.mapCodec { instance ->
                    instance.group(
                        TextureReference.CODEC.fieldOf("texture")
                            .forGetter(Image::texture),
                        Codec.FLOAT.fieldOf("u")
                            .forGetter(Image::u),
                        Codec.FLOAT.fieldOf("v")
                            .forGetter(Image::v),
                        Codec.FLOAT.fieldOf("width")
                            .forGetter(Image::uWidth),
                        Codec.FLOAT.fieldOf("height")
                            .forGetter(Image::vHeight),
                    )
                        .apply(instance, ::Image)
                }
            }
        }

        data class ImageWithOverlay(
            var image: Image,
            var color: Color,
        ) : Background("image_overlay") {
            companion object {
                @JvmField
                val CODEC: MapCodec<ImageWithOverlay> = RecordCodecBuilder.mapCodec { instance ->
                    instance.group(
                        Image.CODEC
                            .forGetter(ImageWithOverlay::image),
                        Color.CODEC
                            .forGetter(ImageWithOverlay::color),
                    )
                        .apply(instance, ::ImageWithOverlay)
                }
            }
        }
    }

    data class Header(
        var display: HeaderDisplay = HeaderDisplay.Transcript,
        var style: Style = DEFAULT_STYLE,
        var langDisplay: LanguageDisplay = BuiltinLanguageDisplays.LangCodeShortUppercase,
        var langStyle: Style = DEFAULT_LANG_STYLE,
        var langDecoration: LanguageDecoration = LanguageDecoration.None,
        var alignX: HorizontalAlignment = HorizontalAlignment.Center(false),
        var alignY: VerticalAlignment = VerticalAlignment.Top,
        var hasShadow: Boolean = true,
    ) {
        fun text(language: Language): TextComponent
            = this.display.text(this.langDecoration.decorate(this.langDisplay.text(language, this.langStyle))).copy().withStyle(this.style)

        companion object {
            @JvmField val DEFAULT_STYLE = Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(true)
            @JvmField val DEFAULT_LANG_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY).withBold(true)
            fun default(): Header = Header()

            @JvmField
            val CODEC: Codec<Header> = RecordCodecBuilder.create { instance ->
                instance.group(
                    HeaderDisplay.CODEC.optionalFieldOf("display", HeaderDisplay.Transcript)
                        .forGetter(Header::display),
                    Style.CODEC.optionalFieldOf("style", DEFAULT_STYLE)
                        .forGetter(Header::style),
                    LanguageDisplay.CODEC.optionalFieldOf("lang_display", BuiltinLanguageDisplays.LangCodeShortUppercase)
                        .forGetter(Header::langDisplay),
                    Style.CODEC.optionalFieldOf("style", DEFAULT_STYLE)
                        .forGetter(Header::langStyle),
                    LanguageDecoration.CODEC.optionalFieldOf("lang_decoration", LanguageDecoration.None)
                        .forGetter(Header::langDecoration),
                    HorizontalAlignment.CODEC.optionalFieldOf("align_x", HorizontalAlignment.Center(false))
                        .forGetter(Header::alignX),
                    VerticalAlignment.CODEC.optionalFieldOf("align_y", VerticalAlignment.Top)
                        .forGetter(Header::alignY),
                    Codec.BOOL.optionalFieldOf("has_shadow", true)
                        .forGetter(Header::hasShadow),
                )
                    .apply(instance, ::Header)
            }
        }

        sealed class HorizontalAlignment(val type: String) {
            companion object {
                @JvmField val CODEC: Codec<HorizontalAlignment> = Codec.STRING.dispatch("type", HorizontalAlignment::type) { type ->
                    when (type) {
                        "left" -> Left.CODEC
                        "center" -> Center.CODEC
                        "right" -> Right.CODEC
                        else -> throw IllegalArgumentException("No horizontal alignment found by type $type!")
                    }
                }
            }

            abstract fun align(width: Float, headerLength: Int, langLength: Int): Float

            object Left : HorizontalAlignment("left") {
                @JvmField val CODEC: MapCodec<Left> = MapCodec.unit(Left)

                override fun align(width: Float, headerLength: Int, langLength: Int): Float = 0f
            }

            data class Center(val includesLang: Boolean) : HorizontalAlignment("center") {
                companion object {
                    @JvmField val CODEC: MapCodec<Center> = RecordCodecBuilder.mapCodec { instance ->
                        instance.group(
                            Codec.BOOL.optionalFieldOf("include_lang", true)
                                .forGetter(Center::includesLang)
                        )
                            .apply(instance, ::Center)
                    }
                }

                override fun align(width: Float, headerLength: Int, langLength: Int): Float = if (includesLang)
                    width / 2f - ((headerLength + langLength) / 2f)
                else
                    width / 2f - (headerLength / 2f)
            }

            object Right : HorizontalAlignment("right") {
                @JvmField val CODEC: MapCodec<Right> = MapCodec.unit(Right)

                override fun align(width: Float, headerLength: Int, langLength: Int): Float = (width - headerLength - langLength)
            }
        }

        sealed class VerticalAlignment(val type: String) {
            companion object {
                @JvmField val CODEC: Codec<VerticalAlignment> = Codec.STRING.dispatch("type", VerticalAlignment::type) { type ->
                    when (type) {
                        "top" -> Top.CODEC
                        "bottom" -> Bottom.CODEC
                        else -> throw IllegalArgumentException("No vertical alignment found by type $type!")
                    }
                }
            }

            abstract fun align(height: Float): Float

            object Top : VerticalAlignment("top") {
                @JvmField val CODEC: MapCodec<Top> = MapCodec.unit(Top)

                override fun align(height: Float): Float = 0f
            }

            object Bottom : VerticalAlignment("bottom") {
                @JvmField val CODEC: MapCodec<Bottom> = MapCodec.unit(Bottom)

                override fun align(height: Float): Float = height - 10
            }
        }
    }

    sealed class LanguageDecoration(val type: String) {
        companion object {
            const val DECORATION_KEY = "unitytranslate.transcript.decoration"

            @JvmField val CODEC: Codec<LanguageDecoration> = Codec.STRING.dispatch("type", LanguageDecoration::type) { type ->
                when (type) {
                    "none" -> None.CODEC
                    "parentheses" -> Parentheses.CODEC
                    "brackets" -> Brackets.CODEC
                    else -> throw IllegalArgumentException("No language decoration found by type $type!")
                }
            }
        }

        abstract fun decorate(languageDisplay: TextComponent): TextComponent

        object None : LanguageDecoration("none") {
            @JvmField val CODEC: MapCodec<None> = MapCodec.unit(None)

            override fun decorate(languageDisplay: TextComponent): TextComponent = languageDisplay
        }

        object Parentheses : LanguageDecoration("parentheses") {
            @JvmField val CODEC: MapCodec<Parentheses> = MapCodec.unit(Parentheses)

            override fun decorate(languageDisplay: TextComponent): TextComponent = TextComponent.translatable("$DECORATION_KEY.parentheses", languageDisplay)
        }

        object Brackets : LanguageDecoration("brackets") {
            @JvmField val CODEC: MapCodec<Brackets> = MapCodec.unit(Brackets)

            override fun decorate(languageDisplay: TextComponent): TextComponent = TextComponent.translatable("$DECORATION_KEY.brackets", languageDisplay)
        }
    }

    sealed class HeaderDisplay(val type: String) {
        companion object {
            const val HEADER_LANG = "unitytranslate.transcript.header"

            @JvmField val CODEC: Codec<HeaderDisplay> = Codec.STRING.dispatch("type", HeaderDisplay::type) { type ->
                when (type) {
                    "none" -> None.CODEC
                    "transcript" -> Transcript.CODEC
                    "translation" -> Translation.CODEC
                    "translations" -> Translations.CODEC
                    "mod_translation" -> UTTranslation.CODEC
                    "mod_translations" -> UTTranslations.CODEC
                    "custom" -> Custom.CODEC
                    "custom_rich" -> CustomRich.CODEC
                    else -> throw IllegalArgumentException("No header display found by type $type!")
                }
            }
        }

        abstract fun text(languageDisplay: TextComponent): TextComponent

        object None : HeaderDisplay("none") {
            @JvmField val CODEC: MapCodec<None> = MapCodec.unit(None)

            override fun text(languageDisplay: TextComponent): TextComponent = TextComponent.empty().append(languageDisplay)
        }

        object Transcript : HeaderDisplay("transcript") {
            @JvmField val CODEC: MapCodec<Transcript> = MapCodec.unit(Transcript)

            override fun text(languageDisplay: TextComponent): TextComponent = TextComponent.translatable("$HEADER_LANG.transcript", languageDisplay)
        }

        object Translation : HeaderDisplay("translation") {
            @JvmField val CODEC: MapCodec<Translation> = MapCodec.unit(Translation)

            override fun text(languageDisplay: TextComponent): TextComponent = TextComponent.translatable("$HEADER_LANG.translation", languageDisplay)
        }

        object Translations : HeaderDisplay("translations") {
            @JvmField val CODEC: MapCodec<Translations> = MapCodec.unit(Translations)

            override fun text(languageDisplay: TextComponent): TextComponent = TextComponent.translatable("$HEADER_LANG.translations", languageDisplay)
        }

        object UTTranslation : HeaderDisplay("mod_translation") {
            @JvmField val CODEC: MapCodec<UTTranslation> = MapCodec.unit(UTTranslation)

            override fun text(languageDisplay: TextComponent): TextComponent = TextComponent.translatable("$HEADER_LANG.mod_translation", languageDisplay)
        }

        object UTTranslations : HeaderDisplay("mod_translations") {
            @JvmField val CODEC: MapCodec<UTTranslations> = MapCodec.unit(UTTranslations)

            override fun text(languageDisplay: TextComponent): TextComponent = TextComponent.translatable("$HEADER_LANG.mod_translations", languageDisplay)
        }

        data class Custom(val text: String, val appendLanguage: Boolean) : HeaderDisplay("custom") {
            override fun text(languageDisplay: TextComponent): TextComponent = TextComponent.literal(this.text).run {
                if (appendLanguage)
                    this.append(" ").append(languageDisplay)
                else this
            }

            companion object {
                @JvmField val CODEC: MapCodec<Custom> = RecordCodecBuilder.mapCodec { instance ->
                    instance.group(
                        Codec.STRING.fieldOf("text")
                            .forGetter(Custom::text),
                        Codec.BOOL.optionalFieldOf("append_language", true)
                            .forGetter(Custom::appendLanguage),
                    )
                        .apply(instance, ::Custom)
                }
            }
        }

        data class CustomRich(val text: TextComponent, val appendLanguage: Boolean) : HeaderDisplay("custom_rich") {
            override fun text(languageDisplay: TextComponent): TextComponent = this.text.copy().run {
                if (appendLanguage)
                    this.append(" ").append(languageDisplay)
                else this
            }

            companion object {
                @JvmField val CODEC: MapCodec<CustomRich> = RecordCodecBuilder.mapCodec { instance ->
                    instance.group(
                        TextComponent.CODEC.fieldOf("text")
                            .forGetter(CustomRich::text),
                        Codec.BOOL.optionalFieldOf("append_language", true)
                            .forGetter(CustomRich::appendLanguage),
                    )
                        .apply(instance, ::CustomRich)
                }
            }
        }
    }

    data class TranscriptDisplay(
        var langDisplay: LanguageDisplay = BuiltinLanguageDisplays.LangCodeShortUppercase,
        var langStyle: Style = DEFAULT_STYLE,
        var langDecoration: LanguageDecoration = LanguageDecoration.Parentheses,
    ) {
        companion object {
            const val MESSAGE_LANG = "unitytranslate.transcript.message"

            @JvmField val DEFAULT_STYLE = Style.EMPTY.withColor(ChatFormatting.GREEN)
            @JvmField val CODEC: Codec<TranscriptDisplay> = RecordCodecBuilder.create { instance ->
                instance.group(
                    LanguageDisplay.CODEC.optionalFieldOf("lang_display", BuiltinLanguageDisplays.LangCodeShortUppercase)
                        .forGetter(TranscriptDisplay::langDisplay),
                    Style.CODEC.optionalFieldOf("lang_style", DEFAULT_STYLE)
                        .forGetter(TranscriptDisplay::langStyle),
                    LanguageDecoration.CODEC.optionalFieldOf("lang_decoration", LanguageDecoration.Parentheses)
                        .forGetter(TranscriptDisplay::langDecoration)
                )
                    .apply(instance, ::TranscriptDisplay)
            }

            @JvmStatic fun default(): TranscriptDisplay = TranscriptDisplay()
        }

        fun text(data: TranscriptData): TextComponent = TextComponent.translatable(MESSAGE_LANG,
            data.sender.displayName,
            this.langDecoration.decorate(this.langDisplay.text(data.language, this.langStyle)),
            data.message
        )
    }
}
