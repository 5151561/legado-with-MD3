package io.legado.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun Typography.toLegadoTypography(): LegadoTypography {
    return LegadoTypography(
        headlineLarge = headlineLarge,
        headlineLargeEmphasized = headlineLarge.copy(fontWeight = FontWeight.Medium),
        headlineMedium = headlineMedium,
        headlineMediumEmphasized = headlineMedium.copy(fontWeight = FontWeight.Medium),
        headlineSmall = headlineSmall,
        headlineSmallEmphasized = headlineSmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = titleLarge,
        titleLargeEmphasized = titleLarge.copy(fontWeight = FontWeight.Medium),
        titleMedium = titleMedium,
        titleMediumEmphasized = titleMedium.copy(fontWeight = FontWeight.Medium),
        titleSmall = titleSmall,
        titleSmallEmphasized = titleSmall.copy(fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge,
        bodyLargeEmphasized = bodyLarge.copy(fontWeight = FontWeight.Medium),
        bodyMedium = bodyMedium,
        bodyMediumEmphasized = bodyMedium.copy(fontWeight = FontWeight.Medium),
        bodySmall = bodySmall,
        bodySmallEmphasized = bodySmall.copy(fontWeight = FontWeight.Medium),
        labelLarge = labelLarge,
        labelLargeEmphasized = labelLarge.copy(fontWeight = FontWeight.Medium),
        labelMedium = labelMedium,
        labelMediumEmphasized = labelMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = labelSmall,
        labelSmallEmphasized = labelSmall.copy(fontWeight = FontWeight.Medium)
    )
}

fun LegadoTypography.withFont(fontFamily: FontFamily?): LegadoTypography {
    if (fontFamily == null) return this
    return copy(
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
        labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = fontFamily)
    )
}
