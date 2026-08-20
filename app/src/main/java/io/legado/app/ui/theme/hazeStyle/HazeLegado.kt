package io.legado.app.ui.theme.hazeStyle

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import io.legado.app.ui.theme.LocalAppUiConfiguration

object HazeLegado {

    @Composable
    @ReadOnlyComposable
    fun ultraThinPlus(
        containerColor: Color = MaterialTheme.colorScheme.surface,
    ): HazeStyle = hazeLegado(
        containerColor = containerColor,
        lightAlpha = 0.0f,
        darkAlpha = 0.0f,
    )

    @Composable
    @ReadOnlyComposable
    fun ultraThin(
        containerColor: Color = MaterialTheme.colorScheme.surface,
    ): HazeStyle {
        val blurAlpha = LocalAppUiConfiguration.current.theme.topBarBlurAlpha / 100f
        return hazeLegado(
            containerColor = containerColor,
            lightAlpha = blurAlpha * 0.35f / 0.73f,
            darkAlpha = blurAlpha * 0.55f / 0.8f,
        )
    }

    @Composable
    @ReadOnlyComposable
    fun regular(
        containerColor: Color = MaterialTheme.colorScheme.surface,
    ): HazeStyle {
        val blurAlpha = LocalAppUiConfiguration.current.theme.topBarBlurAlpha / 100f
        return hazeLegado(
            containerColor = containerColor,
            lightAlpha = blurAlpha,
            darkAlpha = blurAlpha,
        )
    }

    @Composable
    @ReadOnlyComposable
    fun custom(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        blurRadius: Int = LocalAppUiConfiguration.current.theme.topBarBlurRadius,
        blurAlpha: Int = LocalAppUiConfiguration.current.theme.topBarBlurAlpha,
    ): HazeStyle = hazeLegado(
        containerColor = containerColor,
        blurRadius = blurRadius,
        lightAlpha = blurAlpha / 100f,
        darkAlpha = blurAlpha / 100f,
    )

    private fun hazeLegado(
        containerColor: Color,
        blurRadius: Int = 24,
        lightAlpha: Float,
        darkAlpha: Float,
    ): HazeStyle = HazeStyle(
        blurRadius = blurRadius.dp,
        backgroundColor = containerColor,
        tint = HazeTint(
            containerColor.copy(alpha = if (containerColor.luminance() >= 0.5) lightAlpha else darkAlpha),
        ),
    )
}
