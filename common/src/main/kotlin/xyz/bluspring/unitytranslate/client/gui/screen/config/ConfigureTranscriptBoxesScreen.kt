package xyz.bluspring.unitytranslate.client.gui.screen.config

import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.InputValue
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.HorizontalAlign
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.PlainUIButton
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.UILabel
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.VerticalAlign
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.context.ActionContextBoxElement
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.context.ContextBox
import xyz.bluspring.unitytranslate.api.v2.client.gui.screen.UTScreen
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenUtil.inflate
import xyz.bluspring.unitytranslate.api.v2.display.text.Style
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.withAlpha
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.config.ClientConfig
import xyz.bluspring.unitytranslate.client.config.TranscriptBoxConfig
import xyz.bluspring.unitytranslate.client.gui.TranscriptBoxRenderer
import xyz.bluspring.unitytranslate.client.renderer.UnityTranslateGui

class ConfigureTranscriptBoxesScreen(private val onExit: () -> Unit = { ClientPlatformProxy.instance.setScreen(null) }) : UTScreen() {
    lateinit var renderer: TranscriptBoxRenderer
    lateinit var introText: UILabel
        private set

    private var contextBox: ContextBox? = null
    private var needsReinit = false

    override fun init(width: Int, height: Int) {
        super.init(width, height)
        val font = ClientPlatformProxy.instance.defaultFont
        this.introText = this.addChild(UILabel(width / 2f, height / 2f, TextComponent.translatable("unitytranslate.intro.transcript_box"), font, maxWidth = width - 20, alignX = HorizontalAlign.CENTER, alignY = VerticalAlign.CENTER))
        this.addChild(PlainUIButton(width / 2f, height - 12f, TextComponent.translatable("unitytranslate.transcript_box.done").withStyle(
            Style.EMPTY.withUnderlined(true)), font, onClick = this::onExit))

        this.renderer = this.addChild(UnityTranslateGui.transcriptRenderer)
        this.needsReinit = true
    }

    override fun onExit() {
        this.onExit.invoke()
    }

    override fun tick() {
        super.tick()

        if (ClientConfig.transcriptBoxes.isNotEmpty()) {
            this.introText.color = this.introText.color.withAlpha(0f)
        } else {
            this.introText.color = this.introText.color.withAlpha(1f)
        }

        if (this.needsReinit) {
            this.renderer.updateConfig(ClientConfig.transcriptBoxes)
            for (container in this.renderer.containers) {
                container.isEditorManaged = true
            }
            this.needsReinit = false
        }
    }

    override fun submit(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.submit(graphics, partialTick, mouseX, mouseY)

        for (container in this.renderer.containers.reversed()) {
            val bounds = container.bounds().inflate(2)
            if (bounds.containsPoint(mouseX, mouseY)) {
                if (!container.isFocused) {
                    container.startEditing(mouseX, mouseY)
                }

                container.isFocused = true
                break
            } else if (container.isFocused) {
                container.isFocused = false
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (this.contextBox != null) {
            if (this.contextBox!!.mouseClicked(mouseX, mouseY, button))
                return true

            this.closeContextBox()
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true
        }

        if (button == InputValue.MOUSE_BUTTON_RIGHT.asPlatform) {
            for (container in this.renderer.containers.reversed()) {
                val bounds = container.bounds().inflate(2)
                if (bounds.containsPoint(mouseX.toInt(), mouseY.toInt())) {
                    this.createContextBox(mouseX.toFloat(), mouseY.toFloat(), container.createContextBox(mouseX.toFloat(), mouseY.toFloat()))
                    return true
                }
            }

            this.createContextBox(mouseX.toFloat(), mouseY.toFloat())
            return true
        }

        return false
    }

    private fun getGenericContextBox(x: Float, y: Float): ContextBox {
        val defaultWidth = 200f
        val defaultHeight = 250f

        return ContextBox(x, y, elements = listOf(
            ActionContextBoxElement(TextComponent.translatable("unitytranslate.transcript_box.create")) {
                val width = ClientPlatformProxy.instance.viewportWidth
                val height = ClientPlatformProxy.instance.viewportHeight

                val x = if (x + defaultWidth > width)
                    x - (width - defaultWidth)
                else x
                val y = if (y + defaultHeight > height)
                    y - (height - defaultHeight)
                else y

                ClientConfig.transcriptBoxes.add(TranscriptBoxConfig(UnityTranslateApi.instance.currentSpokenLanguage,
                    TranscriptBoxConfig.Transforms(
                        TranscriptBoxConfig.Transforms.Position.Relative(x / width, y / height),
                        TranscriptBoxConfig.Transforms.Size.Anchored(defaultWidth / ClientPlatformProxy.instance.viewportWidth, defaultHeight / ClientPlatformProxy.instance.viewportHeight, defaultWidth, defaultHeight)
                    )
                ))
                this.renderer.updateConfig(ClientConfig.transcriptBoxes)
                this.closeContextBox()
            }
        ))
    }

    private fun createContextBox(x: Float, y: Float, context: ContextBox = this.getGenericContextBox(x, y)) {
        if (this.contextBox != null) {
            this.removeChild(this.contextBox!!)
            this.contextBox = null
        }

        this.contextBox = this.addChild(context)
        context.setup(ClientPlatformProxy.instance.viewportWidth, ClientPlatformProxy.instance.viewportHeight)
    }

    private fun closeContextBox() {
        val contextBox = this.contextBox

        if (contextBox != null) {
            this.removeChild(contextBox)
            this.contextBox = null
        }
    }
}
