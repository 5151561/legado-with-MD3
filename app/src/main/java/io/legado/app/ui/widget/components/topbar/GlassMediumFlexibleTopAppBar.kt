package io.legado.app.ui.widget.components.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalAppUiConfiguration
import io.legado.app.ui.widget.components.GlassDefaults
import io.legado.app.ui.widget.components.text.AdaptiveAnimatedText
import io.legado.app.ui.widget.components.text.AnimatedTextLine

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun GlassMediumFlexibleTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    useCharMode: Boolean = false,
    subtitle: String? = null,
    scrollBehavior: GlassTopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable (ColumnScope.() -> Unit)? = null
) {

    val themeSettings = LocalAppUiConfiguration.current.theme
    val containerColor = GlassDefaults.secondaryColorOr { GlassTopAppBarDefaults.containerColor() }
    val scrolledColor = GlassDefaults.secondaryColorOr { GlassTopAppBarDefaults.scrolledContainerColor() }
    val animatedColor = lerp(containerColor, scrolledColor, scrollBehavior?.collapsedFraction ?: 0f)

    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = animatedColor,
        scrolledContainerColor = animatedColor,
    )
    val subtitleText = subtitle?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
    ) {
        if (themeSettings.useFlexibleTopAppBar) {
                    MediumFlexibleTopAppBar(
                        modifier = Modifier,
                        title = {
                            AdaptiveAnimatedText(
                                text = title,
                                useCharMode = useCharMode,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        subtitle = subtitleText?.let { text ->
                            {
                                AnimatedTextLine(text = text)
                            }
                        },
                        navigationIcon = navigationIcon,
                        actions = {
                            Box(modifier = Modifier.padding(end = 12.dp)) {
                                TopBarActionsRow { actions() }
                            }
                        },
                        scrollBehavior = (scrollBehavior as? M3GlassScrollBehavior)?.m3Behavior,
                        colors = topBarColors
                    )
        } else {
                    TopAppBar(
                        modifier = Modifier,
                        title = {
                            Column {
                                AdaptiveAnimatedText(
                                    text = title,
                                    useCharMode = useCharMode,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                subtitleText?.let { text ->
                                    AnimatedTextLine(
                                        text = text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = navigationIcon,
                        actions = {
                            Box(modifier = Modifier.padding(end = 12.dp)) {
                                TopBarActionsRow { actions() }
                            }
                        },
                        scrollBehavior = (scrollBehavior as? M3GlassScrollBehavior)?.m3Behavior,
                        colors = topBarColors
                    )
        }

        bottomContent?.invoke(this)
    }
}

object GlassTopAppBarDefaults {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun defaultScrollBehavior(): GlassTopAppBarScrollBehavior {
        val configuration = LocalAppUiConfiguration.current
        return if (configuration.theme.useFlexibleTopAppBar) {
            val m3Behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            remember(m3Behavior) { M3GlassScrollBehavior(m3Behavior) }
        } else {
            val m3Behavior = TopAppBarDefaults.pinnedScrollBehavior()
            remember(m3Behavior) { M3GlassScrollBehavior(m3Behavior) }
        }
    }

    @Composable
    fun glassColors(): TopAppBarColors {

        val containerBaseColor = GlassDefaults.secondaryColorOr {
            MaterialTheme.colorScheme.surface
        }
        val containerColor = GlassDefaults.glassColor(
            noBlurColor = containerBaseColor,
            blurAlpha = GlassDefaults.TransparentAlpha
        )

        val scrolledBaseColor = GlassDefaults.secondaryColorOr {
            MaterialTheme.colorScheme.surfaceContainer
        }
        val scrolledContainerColor = scrolledBaseColor

        return TopAppBarDefaults.topAppBarColors(
            containerColor = applyTopBarOpacity(containerColor),
            scrolledContainerColor = applyTopBarOpacity(scrolledContainerColor)
        )
    }

    @Composable
    fun containerColor(): Color {
        val baseColor = GlassDefaults.secondaryColorOr { MaterialTheme.colorScheme.surface }
        val glassColor = GlassDefaults.glassColor(
            noBlurColor = baseColor,
            blurAlpha = GlassDefaults.TransparentAlpha
        )
        return applyTopBarOpacity(glassColor)
    }

    @Composable
    fun scrolledContainerColor(): Color {
        val baseColor = GlassDefaults.secondaryColorOr {
            MaterialTheme.colorScheme.surfaceContainer
        }
        val glassColor = GlassDefaults.glassColor(
            noBlurColor = baseColor,
            blurAlpha = GlassDefaults.TransparentAlpha
        )
        return applyTopBarOpacity(glassColor)
    }

    @Composable
    fun controlContainerColor(): Color {
        val baseColor = GlassDefaults.glassColor(
            noBlurColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            blurAlpha = GlassDefaults.DefaultBlurAlpha
        )
        return applyTopBarOpacity(baseColor)
    }

    @Composable
    private fun applyTopBarOpacity(color: Color): Color = color
}
