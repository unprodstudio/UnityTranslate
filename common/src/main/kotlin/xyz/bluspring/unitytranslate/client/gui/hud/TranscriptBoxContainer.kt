package xyz.bluspring.unitytranslate.client.gui.hud

import kotlinx.coroutines.runBlocking
import net.minecraft.util.Mth
import xyz.bluspring.unitytranslate.UnityTranslateApiImpl
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIElement
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.FocusableUIElement
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.context.ActionContextBoxElement
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.context.ContextBox
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.context.ExpandableContextBoxElement
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenAxis
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenDirection
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenRectangle
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenUtil.inflate
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptData
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriptHolder
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.alpha
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.multiplyAlpha
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.config.TranscriptBoxConfig
import xyz.bluspring.unitytranslate.client.gui.MouseHelper
import java.util.*
import kotlin.math.floor
import kotlin.math.max

class TranscriptBoxContainer(var holder: TranscriptHolder, val config: TranscriptBoxConfig) : UIElement(), FocusableUIElement {
    override var isFocused: Boolean = false
    var isInEditMode = false
    var isEditorManaged = false
    val movingDirections: EnumSet<ScreenDirection> = EnumSet.noneOf(ScreenDirection::class.java)

    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f
    var font = UnityTranslateApi.instance.client.defaultFont

    private var headerText: TextComponent = TextComponent.empty()
    private var headerX: Float = 0f
    private var headerY: Float = 0f

    private var startMouseX = 0
    private var startMouseY = 0

    override fun bounds(
        screenWidth: Int,
        screenHeight: Int
    ): ScreenRectangle {
        return ScreenRectangle(
            (this.x - this.config.padding.left - this.config.outline.thickness).toInt(),
            (this.y - this.config.padding.top - this.config.outline.thickness).toInt(),
            (this.width + this.config.padding.left + this.config.padding.right + (this.config.outline.thickness * 2)).toInt(),
            (this.height + this.config.padding.top + this.config.padding.bottom + (this.config.outline.thickness * 2)).toInt()
        )
    }

    override fun tick() {
        val transcripts = this.holder.transcripts.toList()
        var wasModified = false
        val transcriptsToRemove by lazy { mutableSetOf<TranscriptData>() }
        val ttl = (this.config.msToLive + this.config.msToFadeOut)

        for (transcript in transcripts) {
            if (System.currentTimeMillis() - transcript.timeUpdated >= ttl) {
                transcriptsToRemove.add(transcript)
                wasModified = true
            }
        }

        if (wasModified) {
            synchronized(this.holder.transcripts) {
                this.holder.transcripts.removeAll(transcriptsToRemove)
            }
        }
    }

    private val Long.ticks: Int
        get() {
            return floor(this / 50.0).toInt()
        }

    private val Int.ticks: Int
        get() {
            return floor(this / 50.0).toInt()
        }

    private val TranscriptData.ticks: Int
        get() {
            return floor((System.currentTimeMillis() - this.timeUpdated) / 50.0).toInt()
        }

    override fun submit(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.pushMatrix()
        graphics.translate(this.x, this.y)

        // Background
        graphics.pushMatrix()

        when (val background = this.config.background) {
            is TranscriptBoxConfig.Background.Color -> {
                val (topLeft, topRight, bottomLeft, bottomRight) = ColorConfig.separateMatrix(background.color)
                graphics.fill(
                    -this.config.padding.left, -this.config.padding.top,
                    width + this.config.padding.right, height + this.config.padding.bottom,
                    topLeft, topRight,
                    bottomLeft, bottomRight
                )
            }

            is TranscriptBoxConfig.Background.Image -> {
                graphics.blit(
                    -this.config.padding.left, -this.config.padding.top,
                    width + this.config.padding.right, height + this.config.padding.bottom,
                    background.u, background.v,
                    background.uWidth, background.vHeight,
                    background.texture
                )
            }

            is TranscriptBoxConfig.Background.ImageWithOverlay -> {
                val image = background.image
                val (topLeft, topRight, bottomLeft, bottomRight) = ColorConfig.separateMatrix(background.color.color)
                graphics.blitWithColor(
                    -this.config.padding.left, -this.config.padding.top,
                    width + this.config.padding.right, height + this.config.padding.bottom,
                    image.u, image.v,
                    image.uWidth, image.vHeight,
                    image.texture,
                    topLeft, topRight,
                    bottomLeft, bottomRight
                )
            }
        }

        val outline = this.config.outline
        if (outline.thickness > 0) {
            val thickness = outline.thickness
            val (topLeft, topRight, bottomLeft, bottomRight) = ColorConfig.separateMatrix(outline.color)

            graphics.outline(-this.config.padding.left - thickness, -this.config.padding.top - thickness, width + this.config.padding.right + thickness, height + this.config.padding.bottom + thickness, thickness, topLeft, topRight, bottomLeft, bottomRight)
        }

        graphics.popMatrix()

        // Header
        graphics.text(font, headerText, this.headerX, this.headerY + font.lineHeight, -1, this.config.header.hasShadow)

        graphics.enableScissor(0, font.lineHeight + 2, this.width.toInt(), this.height.toInt() - 7)
        graphics.pushMatrix()
        graphics.translate(0f, this.height - font.lineHeight - 2)
        val transcripts = synchronized(this.holder.transcripts) { this.holder.transcripts.toList() }
            .sortedBy { it.timeUpdated }
        var offset = 0f
        val time = System.currentTimeMillis()
        val fadeTicks = this.config.msToFadeOut.ticks

        for (transcript in transcripts.reversed()) {
            val text = this.config.transcriptDisplay.text(transcript)
            val timeLived = time - transcript.timeUpdated

            val fadeMultiplier = if (timeLived >= this.config.msToLive)
                1f - Mth.clamp((timeLived.ticks - this.config.msToLive.ticks - partialTick) / fadeTicks.toFloat(), 0f, 1f)
            else 1f

            for (sequence in font.split(text, this.width.toInt() - 4).reversed()) {
                val hasShadow = this.config.shadowColor.alpha() <= 10
                graphics.text(font, sequence, 0f, -offset, this.config.textColor.multiplyAlpha(fadeMultiplier), hasShadow) // TODO: shadow
                offset += font.lineHeight
            }

            offset += 2
        }
        graphics.popMatrix()
        graphics.disableScissor()

        graphics.popMatrix()

        super.submit(graphics, partialTick, mouseX, mouseY)

        if (this.isInEditMode) {
            val bounds = this.bounds()
            if (!this.isEditorManaged) {
                val newFocus = bounds.inflate(2).containsPoint(mouseX, mouseY)
                if (!this.isFocused && newFocus) {
                    this.startEditing(mouseX, mouseY)
                }

                this.isFocused = newFocus
            }

            if (this.isFocused || this.movingDirections.isNotEmpty()) {
                val matrix = ColorConfig.separateMatrix(
                    if (this.movingDirections.isNotEmpty())
                        ThemeConfig.transcriptBoxOutlineMoving
                    else
                        ThemeConfig.transcriptBoxOutlineFocused
                )

                val directions = this.movingDirections.ifEmpty {
                    val directions = EnumSet.noneOf(ScreenDirection::class.java)
                    this.setupMovingDirections(mouseX, mouseY, directions)
                    directions
                }

                graphics.outline(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), 1f,
                    this.colorOrNone(matrix.topLeft, directions, ScreenDirection.UP, ScreenDirection.LEFT), this.colorOrNone(matrix.topRight, directions, ScreenDirection.UP, ScreenDirection.RIGHT),
                    this.colorOrNone(matrix.bottomLeft, directions, ScreenDirection.DOWN, ScreenDirection.LEFT), this.colorOrNone(matrix.bottomRight, directions, ScreenDirection.DOWN, ScreenDirection.RIGHT)
                )

                // cursor handling
                if (!directions.containsAll(ScreenDirection.entries)) {
                    if ((directions.contains(ScreenDirection.UP) && directions.contains(ScreenDirection.LEFT)) ||
                        (directions.contains(ScreenDirection.DOWN) && directions.contains(ScreenDirection.RIGHT))) {
                        MouseHelper.cursorToTopLeftToBottomRightResize()
                    } else if ((directions.contains(ScreenDirection.UP) && directions.contains(ScreenDirection.RIGHT)) ||
                        (directions.contains(ScreenDirection.DOWN) && directions.contains(ScreenDirection.LEFT))) {
                        MouseHelper.cursorToTopRightToBottomLeftResize()
                    } else if (directions.contains(ScreenDirection.UP) || directions.contains(ScreenDirection.DOWN)) {
                        MouseHelper.cursorToVerticalResize()
                    } else if (directions.contains(ScreenDirection.LEFT) || directions.contains(ScreenDirection.RIGHT)) {
                        MouseHelper.cursorToHorizontalResize()
                    }
                } else {
                    MouseHelper.cursorToOmniResize()
                }

                // start moving
                if (this.movingDirections.isNotEmpty()) {
                    val deltaX = mouseX - this.startMouseX
                    val deltaY = mouseY - this.startMouseY
                    val modified = mutableMapOf<ScreenDirection, Int>()

                    modified[ScreenDirection.LEFT] = this.x.toInt()
                    modified[ScreenDirection.UP] = this.y.toInt()
                    modified[ScreenDirection.RIGHT] = (this.x + this.width).toInt()
                    modified[ScreenDirection.DOWN] = (this.y + this.height).toInt()

                    for (direction in this.movingDirections) {
                        val existing = modified[direction]!!
                        if (direction.axis == ScreenAxis.HORIZONTAL) {
                            modified[direction] = existing + deltaX
                        } else {
                            modified[direction] = existing + deltaY
                        }
                    }

                    val screenWidth = ClientPlatformProxy.instance.viewportWidth
                    val screenHeight = ClientPlatformProxy.instance.viewportHeight

                    this.width = (modified[ScreenDirection.RIGHT]!! - modified[ScreenDirection.LEFT]!!).toFloat().coerceAtLeast(2f)
                    this.height = (modified[ScreenDirection.DOWN]!! - modified[ScreenDirection.UP]!!).toFloat().coerceAtLeast(2f)
                    this.x = Mth.clamp(modified[ScreenDirection.LEFT]!!, 0, screenWidth).toFloat()
                    this.y = Mth.clamp(modified[ScreenDirection.UP]!!, 0, screenHeight).toFloat()

                    if (this.x + this.width > screenWidth) {
                        if (this.movingDirections.contains(ScreenDirection.LEFT))
                            this.x -= (this.x + this.width) - screenWidth
                        else
                            this.width -= (this.x + this.width) - screenWidth
                    }

                    if (this.y + this.height > screenHeight) {
                        if (this.movingDirections.contains(ScreenDirection.UP))
                            this.y -= (this.y + this.height) - screenHeight
                        else
                            this.height -= (this.y + this.height) - screenHeight
                    }

                    if (modified[ScreenDirection.LEFT]!! < 0 && !this.movingDirections.contains(ScreenDirection.RIGHT)) {
                        this.width += modified[ScreenDirection.LEFT]!!
                    }

                    if (modified[ScreenDirection.UP]!! < 0 && !this.movingDirections.contains(ScreenDirection.DOWN)) {
                        this.height += modified[ScreenDirection.UP]!!
                    }

                    this.x = this.x.coerceAtLeast(0f)
                    this.y = this.y.coerceAtLeast(0f)

                    updateHeader()

                    this.startMouseX = mouseX
                    this.startMouseY = mouseY
                }
            }
        }
    }

    private fun colorOrNone(color: Int, movingDirections: EnumSet<ScreenDirection>, vararg directions: ScreenDirection): Int {
        if (movingDirections.containsAll(directions.toList()))
            return color

        if (movingDirections.size == 1) {
            for (direction in directions) {
                if (movingDirections.contains(direction))
                    return color
            }
        }

        return 0
    }

    fun startEditing(mouseX: Int, mouseY: Int) {
        this.isInEditMode = true
        this.startMouseX = mouseX
        this.startMouseY = mouseY
    }

    private fun inRange(pos: Int, target: Int): Boolean {
        val range = 3
        return pos >= target - range && pos <= target + range
    }

    private fun setupMovingDirections(mouseX: Int, mouseY: Int, directions: EnumSet<ScreenDirection>) {
        val bounds = this.bounds()
        if (inRange(mouseX, bounds.left))
            directions.add(ScreenDirection.LEFT)

        if (inRange(mouseY, bounds.top))
            directions.add(ScreenDirection.UP)

        if (inRange(mouseY, bounds.bottom))
            directions.add(ScreenDirection.DOWN)

        if (inRange(mouseX, bounds.right))
            directions.add(ScreenDirection.RIGHT)

        if (directions.isEmpty()) {
            directions.addAll(ScreenDirection.entries)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (this.isInEditMode && button == 0 && this.isFocused) {
            this.startEditing(mouseX.toInt(), mouseY.toInt())
            this.setupMovingDirections(mouseX.toInt(), mouseY.toInt(), this.movingDirections)
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (this.isInEditMode && button == 0 && this.movingDirections.isNotEmpty()) {
            this.movingDirections.clear()

            val screenWidth = ClientPlatformProxy.instance.viewportWidth
            val screenHeight = ClientPlatformProxy.instance.viewportHeight
            when (this.config.transforms.position) {
                is TranscriptBoxConfig.Transforms.Position.Absolute -> {
                    this.config.transforms.position = TranscriptBoxConfig.Transforms.Position.Absolute(this.x.toInt(), this.y.toInt())
                }

                is TranscriptBoxConfig.Transforms.Position.Relative -> {
                    this.config.transforms.position = TranscriptBoxConfig.Transforms.Position.Relative((this.x / screenWidth) + (this.width / screenWidth), (this.y / screenHeight) + (this.height / screenHeight))
                }
            }

            when (this.config.transforms.size) {
                is TranscriptBoxConfig.Transforms.Size.Absolute -> {
                    this.config.transforms.size = TranscriptBoxConfig.Transforms.Size.Absolute(this.width, this.height)
                }

                is TranscriptBoxConfig.Transforms.Size.Anchored -> {
                    this.config.transforms.size = TranscriptBoxConfig.Transforms.Size.Anchored(this.width / screenWidth.toFloat(), this.height / screenHeight.toFloat(), this.width, this.height)
                }
            }

            this.updateConfig()

            return true
        }

        return super.mouseReleased(mouseX, mouseY, button)
    }

    val languages: suspend () -> Collection<Language> = {
        UnityTranslateApiImpl.translators.values.flatMap { it.getSupportedLanguages() }.distinct()
    }

    fun createContextBox(x: Float, y: Float): ContextBox {
        val languages = runBlocking {
            languages()
        }

        val font = ClientPlatformProxy.instance.defaultFont
        val width = languages.maxOfOrNull {
            font.width(TextComponent.translatable("unitytranslate.language.native_and_localized", it.nativeText, it.localizedText))
        } ?: 0

        return ContextBox(x, y, elements = listOf(
            ExpandableContextBoxElement(
                TextComponent.translatable("unitytranslate.language").append(": ")
                    .append(this.config.language.nativeText),
                languages
                    .sorted()
                    .map {
                        ActionContextBoxElement(TextComponent.literal(it.nativeText)) {
                            this.config.language = it
                            this.updateConfig()
                        }
                    }
            ).apply {
                this.elementWidth = max(width + 4f, this.elementWidth)
            },
            ExpandableContextBoxElement(TextComponent.translatable("config.unitytranslate.unitytranslate.hud.default_box_settings.header"), listOf(
//                CyclingContextBoxElement(listOf(
//
//                )),

            ))
        ))
    }

    fun updateConfig() {
        if (this.holder.language != this.config.language)
            this.holder = UnityTranslateApi.instance.getOrCreateTranscriptHolder(this.config.language)

        val screenWidth = (ClientPlatformProxy.instance.windowWidth / ClientPlatformProxy.instance.guiScale).toInt()
        val screenHeight = (ClientPlatformProxy.instance.windowHeight / ClientPlatformProxy.instance.guiScale).toInt()

        val dimensions = config.transforms.size.calculateDimensions(screenWidth, screenHeight)
        val pos = config.transforms.position.calculatePos(dimensions, screenWidth, screenHeight)

        this.x = pos.x
        this.y = pos.y
        this.width = dimensions.x
        this.height = dimensions.y

        this.updateHeader()
    }

    private fun updateHeader() {
        this.headerText = this.config.header.text(this.holder.language)

        val headerLength = font.width(this.config.header.display.text(TextComponent.empty()))
        val languageLength = font.width(this.config.header.langDecoration.decorate(this.config.header.langDisplay.text(this.holder.language, this.config.header.langStyle)))
        this.headerX = this.config.header.alignX.align(this.width, headerLength, languageLength)
        this.headerY = this.config.header.alignY.align(this.height)
    }
}
