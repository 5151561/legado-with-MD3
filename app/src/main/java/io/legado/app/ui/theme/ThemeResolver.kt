package io.legado.app.ui.theme

import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

object ThemeResolver {

    private const val MATERIAL_VERSION_EXPRESSIVE = "material3Expressive"

    private val appThemeModes = mapOf(
        "0" to AppThemeMode.Dynamic,
        "1" to AppThemeMode.GR,
        "2" to AppThemeMode.Lemon,
        "3" to AppThemeMode.WH,
        "4" to AppThemeMode.Elink,
        "5" to AppThemeMode.Sora,
        "6" to AppThemeMode.August,
        "7" to AppThemeMode.Carlotta,
        "8" to AppThemeMode.Koharu,
        "9" to AppThemeMode.Yuuka,
        "10" to AppThemeMode.Phoebe,
        "11" to AppThemeMode.Mujika,
        "12" to AppThemeMode.Custom,
    )

    private val materialPaletteStyles = mapOf(
        "tonalSpot" to PaletteStyle.TonalSpot,
        "neutral" to PaletteStyle.Neutral,
        "vibrant" to PaletteStyle.Vibrant,
        "expressive" to PaletteStyle.Expressive,
        "rainbow" to PaletteStyle.Rainbow,
        "fruitSalad" to PaletteStyle.FruitSalad,
        "monochrome" to PaletteStyle.Monochrome,
        "fidelity" to PaletteStyle.Fidelity,
        "content" to PaletteStyle.Content,
    )

    fun resolveThemeMode(value: String): AppThemeMode {
        return appThemeModes[value] ?: AppThemeMode.Dynamic
    }

    fun resolvePaletteStyle(value: String?): PaletteStyle {
        return materialPaletteStyles[value] ?: PaletteStyle.TonalSpot
    }

    fun resolveContrastLevel(value: String = "Default"): Double {
        return runCatching { Contrast.valueOf(value).value }
            .getOrDefault(Contrast.Default.value)
    }

    fun resolveColorSpecVersion(colorSpec: ThemeColorSpec): ColorSpec.SpecVersion {
        return when (colorSpec) {
            ThemeColorSpec.SPEC_2025 -> ColorSpec.SpecVersion.SPEC_2025
            ThemeColorSpec.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
        }
    }

    fun resolveColorSpecFromMaterialVersion(value: String?): ThemeColorSpec {
        return if (value == MATERIAL_VERSION_EXPRESSIVE) {
            ThemeColorSpec.SPEC_2025
        } else {
            ThemeColorSpec.SPEC_2021
        }
    }

}
