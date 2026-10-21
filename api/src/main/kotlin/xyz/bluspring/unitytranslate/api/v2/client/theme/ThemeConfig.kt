package xyz.bluspring.unitytranslate.api.v2.client.theme

import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import java.awt.Color

object ThemeConfig {
    var mainBackground: ColorConfig = ColorConfig.Gradient(ColorConfig.Gradient.GradientDirection.BOTTOM,
        Color(0x380648).rgb,
        Color(0x130b19).rgb,
    )
    var textColor: Int = Color(0xFFFFFF).rgb

    var dangerText: Int = Color(196, 0, 0).rgb

    var enabledText: Int = Color(99, 255, 99).rgb
    var disabledText: Int = Color(255, 99, 99).rgb

    var dropdownBackground: ColorConfig = ColorConfig.Gradient(ColorConfig.Gradient.GradientDirection.BOTTOM,
        Color(0, 0, 0, 0).rgb,
        Color(0, 0, 0, 89).rgb
    )
    var dropdownOpenBackground: ColorConfig = ColorConfig.Solid(
        Color(0, 0, 0, 230).rgb
    )
    var dropdownOpenOutline: ColorConfig = ColorConfig.Solid(
        Color(255, 255, 255, 25).rgb
    )
    var dropdownTextItemHover: Int = Color(255, 255, 255, 255).rgb
    var dropdownTextItemSelected: Int = Color(255, 255, 0, 255).rgb
    var dropdownTextItemDisabled: Int = Color(100, 100, 100, 255).rgb
    var dropdownTextItem: Int = Color(185, 185, 185, 255).rgb
    var dropdownTextDisabled: Int = Color(190, 190, 190, 255).rgb

    var tooltipBackground: ColorConfig = ColorConfig.Solid(
        Color(0, 0, 0, 220).rgb
    )
    var tooltipText: Int = Color(255, 255, 255, 255).rgb

    var scrollbar: ColorConfig = ColorConfig.Solid(
        Color(255, 255, 255, 255).rgb
    )

    var plainButton: Int = Color(210, 210, 210, 255).rgb
    var plainButtonHover: Int = Color(255, 255, 255, 255).rgb
    var plainButtonDisabled: Int = Color(100, 100, 100).rgb

    var configEntryText: Int = Color(170, 170, 170, 255).rgb
    var configEntryTextFocused: Int = Color(255, 255, 255, 255).rgb

    var toggleOutline: ColorConfig = ColorConfig.Solid(
        Color(170, 170, 170).rgb
    )
    var toggleOutlineFocused: ColorConfig = ColorConfig.Solid(
        Color(255, 255, 255).rgb
    )
    var toggleDisabledFill: ColorConfig = ColorConfig.None
    var toggleEnabledFill: ColorConfig = ColorConfig.Solid(
        Color(255, 255, 255).rgb
    )

    var sliderTrack: ColorConfig = ColorConfig.Solid(
        Color(114, 114, 114).rgb
    )
    var sliderTrackFocused: ColorConfig = ColorConfig.Solid(
        Color(200, 200, 200).rgb
    )
    var sliderNotch: ColorConfig = ColorConfig.Solid(
        Color(170, 170, 170).rgb
    )
    var sliderNotchFocused: ColorConfig = ColorConfig.Solid(
        Color(255, 255, 255).rgb
    )
    var sliderValue: Int = Color(170, 170, 170).rgb
    var sliderValueFocused: Int = Color(255, 255, 255).rgb

    var warningText: Int = Color(255, 156, 27).rgb

    var contextBoxBackground: ColorConfig = ColorConfig.Solid(
        Color(0, 0, 0, 230).rgb
    )
    var contextBoxOutline: ColorConfig = ColorConfig.Solid(
        Color(40, 40, 40, 170).rgb
    )
    var contextBoxElementOutline: ColorConfig = ColorConfig.Solid(
        Color(40, 40, 40, 170).rgb
    )
    var contextBoxElementOutlineFocused: ColorConfig = ColorConfig.Solid(
        Color(80, 80, 80, 170).rgb
    )
    var contextBoxElementBackground: ColorConfig = ColorConfig.Solid(
        Color(20, 20, 20, 140).rgb
    )
    var contextBoxElementBackgroundFocused: ColorConfig = ColorConfig.Solid(
        Color(255, 255, 255, 80).rgb
    )
    var contextBoxElementText: Int = Color(210, 210, 210).rgb
    var contextBoxElementTextFocused: Int = Color(255, 255, 255).rgb

    var transcriptBoxOutlineFocused: ColorConfig = ColorConfig.Solid(
        Color(225, 225, 225).rgb
    )
    var transcriptBoxOutlineMoving: ColorConfig = ColorConfig.Solid(
        Color(255, 255, 255).rgb
    )

    var downloadBackground: ColorConfig = ColorConfig.Gradient(ColorConfig.Gradient.GradientDirection.BOTTOM,
        Color(0, 0, 0, 128).rgb,
        Color(0, 0, 0, 172).rgb,
    )
    var downloadOutline: ColorConfig = ColorConfig.Gradient(ColorConfig.Gradient.GradientDirection.BOTTOM,
        Color(0, 0, 0, 172).rgb,
        Color(0, 0, 0, 200).rgb,
    )
    var downloadText: Int = Color(255, 255, 255).rgb
    var downloadProgressBackground: ColorConfig = ColorConfig.Solid(Color(76, 76, 76).rgb)
    var downloadProgressFill: ColorConfig = ColorConfig.Gradient(
        ColorConfig.Gradient.GradientDirection.RIGHT,
        Color(0, 200, 0).rgb,
        Color(0, 255, 0).rgb
    )
}
