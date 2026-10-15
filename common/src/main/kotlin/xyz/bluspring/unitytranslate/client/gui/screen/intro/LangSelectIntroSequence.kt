package xyz.bluspring.unitytranslate.client.gui.screen.intro

import kotlinx.coroutines.runBlocking
import net.minecraft.ChatFormatting
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.UnityTranslateApiImpl
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.*
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.transcriber.InactiveTranscriber
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.config.ClientConfig
import xyz.bluspring.unitytranslate.client.gui.screen.FirstStartupScreen
import xyz.bluspring.unitytranslate.client.gui.screen.config.entry.ConfigEntry
import xyz.bluspring.unitytranslate.client.gui.screen.config.entry.DropdownConfigEntry
import xyz.bluspring.unitytranslate.config.builders.ConfigValueBuilderImpl
import xyz.bluspring.unitytranslate.util.PlatformConversion.withStyle

class LangSelectIntroSequence(parent: FirstStartupScreen) : IntroSequence(parent) {
    var currentTranscriberId: String = UnityTranslateApi.instance.getTranscriberId(ClientConfig.transcriber)
    lateinit var nextButton: PlainUIButton

    override fun init(width: Int, height: Int) {
        super.init(width, height)

        val languages: suspend () -> Collection<Language> = {
            UnityTranslateApiImpl.translators.values.flatMap { it.getSupportedLanguages() }.distinctBy { language -> language.asBCP47 }
        }

        val currentTranscriber = UnityTranslateApi.instance.getTranscriber(this.currentTranscriberId)

        val visualizer: (Language) -> TextComponent = { language ->
            val supportLevel = runBlocking { currentTranscriber.checkLanguageSupport(language) }

            TextComponent.translatable("unitytranslate.language.native_and_localized",
                TextComponent.translatableWithFallback("unitytranslate.language.${language.serialized}.native", { language.localeBasedNativeText }),
                TextComponent.translatableWithFallback("unitytranslate.language.${language.serialized}.localized", { language.localeBasedLocalizedText })
            )
                .withColor(
                    when (supportLevel) {
                        Language.SupportLevel.PARTIAL -> ThemeConfig.warningText
                        else -> -1
                    }
                )
        }

        val font = ClientPlatformProxy.instance.defaultFont

        this.addChild(
            UILabel(
                width / 2f,
                25f,
                TextComponent.translatable("unitytranslate.intro.language_select"),
                font,
                alignX = HorizontalAlign.CENTER,
                maxWidth = (width * (3 / 4f)).toInt()
            )
        )

        val elementWidth = 160f
        val elementHeight = 15f
        val elementOffset = 12f

        val xPos = (width / 2f - elementWidth - elementOffset)
        val yPos = 25f + this.children.sumOf { it.bounds().height } + 24f

        // Right side
        run {
            val xPos = (width / 2f) + elementOffset
            var yPos = yPos

            this.addChild(
                UILabel(
                    xPos + (elementWidth / 2f),
                    yPos,
                    TextComponent.translatable("config.unitytranslate.unitytranslate.transcriber")
                        .withStyle { it.withUnderlined(true) },
                    font,
                    alignX = HorizontalAlign.CENTER,
                    alignY = VerticalAlign.CENTER
                )
            )
            yPos += 14f

            this.addChild(
                UILabel(
                    xPos,
                    yPos,
                    TextComponent.translatable("config.unitytranslate.unitytranslate.transcriber").append(": "),
                    font,
                    alignX = HorizontalAlign.LEFT,
                    alignY = VerticalAlign.CENTER
                )
            )
            val transcriber = this.addChild(
                DropdownList(
                    xPos, yPos + 6f, elementWidth, elementHeight, font,
                    { UnityTranslateApiImpl.transcribers.keys }, { id ->
                        TextComponent.translatable("unitytranslate.transcriber.$id.name")
                    }, this::currentTranscriberId, tooltip = { id ->
                        if (id != null)
                            TextComponent.translatableWithFallback("unitytranslate.transcriber.$id.description", "")
                        else TextComponent.empty()
                    })
            )

            transcriber.onUpdated.register { id ->
                this.currentTranscriberId = id!!
                this.setup(width, height) // Set up again so we have new elements
            }

            yPos += elementHeight + 20f

            val config = UnityTranslateApiImpl.transcriberConfigs[this.currentTranscriberId]
            if (config != null) {
                for (configValue in config.rootCategory.value) {
                    try {
                        val entry = this.addChild(ConfigEntry.fromValue(configValue, xPos, yPos, elementWidth, elementHeight, rootKey = "config.unitytranslate.transcriber.${this.currentTranscriberId}", font))
                        yPos += entry.bounds().height + 12
                    } catch (e: Throwable) {
                        UnityTranslate.logger.error("Failed to add config value ${configValue.fullId}!", e)
                    }
                }
            }
        }

        // Left side
        run {
            var yPos = yPos
            this.addChild(
                UILabel(
                    xPos + (elementWidth / 2f),
                    yPos,
                    TextComponent.translatable("config.unitytranslate.unitytranslate.languages")
                        .withStyle { it.withUnderlined(true) },
                    font,
                    alignX = HorizontalAlign.CENTER,
                    alignY = VerticalAlign.CENTER
                )
            )

            yPos += 20
            this.addChild(
                UILabel(
                    xPos,
                    yPos - 6f,
                    TextComponent.translatable("config.unitytranslate.unitytranslate.languages.spoken").append(": "),
                    font,
                    alignX = HorizontalAlign.LEFT,
                    alignY = VerticalAlign.CENTER
                )
            )
            this.addChild(
                DropdownList(
                xPos, yPos, elementWidth, elementHeight, font, languages, visualizer,
                UnityTranslateApiImpl::currentSpokenLanguage,
                { language ->
                    runBlocking {
                        currentTranscriber.checkLanguageSupport(language).isSupported
                    }
                }
            ) { language ->
                if (language != null) {
                    val supportLevel = runBlocking { currentTranscriber.checkLanguageSupport(language) }

                    when (supportLevel) {
                        Language.SupportLevel.NONE -> TextComponent.translatable(
                            "config.unitytranslate.language.error.transcriber.unsupported",
                            TextComponent.translatable("unitytranslate.transcriber.${this.currentTranscriberId}.name")
                        ).withStyle(ChatFormatting.RED)

                        Language.SupportLevel.PARTIAL -> TextComponent.translatable(
                            "config.unitytranslate.language.warn.transcriber.partial_support",
                            TextComponent.translatable("unitytranslate.transcriber.${this.currentTranscriberId}.name")
                        ).withStyle(ChatFormatting.GOLD)

                        else -> TextComponent.empty()
                    }
                } else TextComponent.empty()
            })

            var index = 1
            for ((id, langHolder) in UnityTranslateApiImpl.outputLanguages) {
                val offset = (index++) * (elementHeight + 20f)

                this.addChild(
                    UILabel(
                        xPos,
                        yPos + offset - 6f,
                        TextComponent.translatable("config.unitytranslate.unitytranslate.languages.$id").append(": "),
                        font,
                        alignX = HorizontalAlign.LEFT,
                        alignY = VerticalAlign.CENTER
                    )
                )
                this.addChild(
                    DropdownList(
                    xPos, yPos + offset, elementWidth, elementHeight, font, languages, visualizer,
                    langHolder::languageOrNull, DropdownList.Type.DEFAULTED,
                    { language ->
                        runBlocking {
                            currentTranscriber.checkLanguageSupport(language).isSupported
                        }
                    }
                ) { language ->
                    if (language != null) {
                        val supportLevel = runBlocking { currentTranscriber.checkLanguageSupport(language) }

                        when (supportLevel) {
                            Language.SupportLevel.NONE -> TextComponent.translatable(
                                "config.unitytranslate.language.error.transcriber.unsupported",
                                TextComponent.translatable("unitytranslate.transcriber.${this.currentTranscriberId}.name")
                            ).withStyle(ChatFormatting.RED)

                            Language.SupportLevel.PARTIAL -> TextComponent.translatable(
                                "config.unitytranslate.language.warn.transcriber.partial_support",
                                TextComponent.translatable("unitytranslate.transcriber.${this.currentTranscriberId}.name")
                            ).withStyle(ChatFormatting.GOLD)

                            else -> TextComponent.empty()
                        }
                    } else TextComponent.empty()
                })
            }
        }

        val nextText = TextComponent.translatable("unitytranslate.intro.language_select.next")
        this.nextButton = this.addChild(PlainUIButton(width - 4f - font.width(nextText), height - 16f, nextText, font) {
            ClientConfig.transcriber = UnityTranslateApi.instance.getTranscriber(this.currentTranscriberId)
            this.parent.next()
        })
    }

    override fun tick() {
        super.tick()

        var canProceed = true
        for (element in this.children) {
            if (element is DropdownList<*>) {
                if (!element.validator(element.selected)) {
                    canProceed = false
                    break
                }
            } else if (element is DropdownConfigEntry<*>) {
                if (!(element.value.validator as ConfigValueBuilderImpl<Any>).validator(element.value.property.getter.call())) {
                    canProceed = false
                    break
                }
            }
        }

        this.nextButton.isDisabled = !canProceed
    }

    override fun submit(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int, transitionProgress: Float) {
        val currentTranscriber = UnityTranslateApi.instance.getTranscriber(this.currentTranscriberId)
        if (currentTranscriber == InactiveTranscriber) {
            val font = ClientPlatformProxy.instance.defaultFont
            val splitText = font.split(TextComponent.translatable("unitytranslate.intro.language_select.warning.inactive_transcriber"), 200)
            for ((index, text) in splitText.withIndex()) {
                graphics.text(font, text, 4f,
                    ClientPlatformProxy.instance.viewportHeight - (splitText.size * font.lineHeight) - 2f + (index * font.lineHeight), ThemeConfig.warningText, true)
            }
        }
    }
}
