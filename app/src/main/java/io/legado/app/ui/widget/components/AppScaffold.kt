package io.legado.app.ui.widget.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.legado.app.domain.model.settings.hasBackgroundImage
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.LocalAppUiConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentColor: Color = contentColorFor(LegadoTheme.colorScheme.surface),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    alwaysDrawBehindBars: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val isDark = LegadoTheme.isDark
    val configuration = LocalAppUiConfiguration.current
    val themeSettings = configuration.theme
    val hasImageBg = themeSettings.hasBackgroundImage(isDark)
    val contentDrawsBehindBars = alwaysDrawBehindBars

    val containerColor = if (hasImageBg) {
        Color.Transparent
    } else {
        LegadoTheme.colorScheme.background
    }
    Box(modifier = modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BackgroundImageContent(isDark = isDark)
                    }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            topBar()
                        },
                        bottomBar = bottomBar,
                        snackbarHost = snackbarHost,
                        floatingActionButton = floatingActionButton,
                        floatingActionButtonPosition = floatingActionButtonPosition,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        contentWindowInsets = contentWindowInsets
                    ) { paddingValues ->
                        val scaffoldPadding = paddingValues
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (contentDrawsBehindBars) Modifier
                                    else Modifier
                                        .padding(scaffoldPadding)
                                        .consumeWindowInsets(scaffoldPadding)
                                )
                        ) {
                            content(
                                if (contentDrawsBehindBars) scaffoldPadding
                                else PaddingValues(0.dp)
                            )
                        }
                    }
    }
}


@Composable
private fun BackgroundImageContent(
    isDark: Boolean,
) {
    val themeSettings = LocalAppUiConfiguration.current.theme
    val hasImageBg = themeSettings.hasBackgroundImage(isDark)
    val bgImagePath = if (isDark) {
        themeSettings.backgroundImageDark
    } else {
        themeSettings.backgroundImageLight
    }
    val blur = if (isDark) {
        themeSettings.backgroundImageDarkBlurring
    } else {
        themeSettings.backgroundImageBlurring
    }

    if (hasImageBg && !bgImagePath.isNullOrBlank()) {
        AsyncImage(
            model = bgImagePath,
            contentDescription = null,
            imageLoader = org.koin.compose.koinInject(),
            modifier = Modifier
                .fillMaxSize()
                .blur(blur.dp),
            contentScale = ContentScale.Crop
        )
    }
}
