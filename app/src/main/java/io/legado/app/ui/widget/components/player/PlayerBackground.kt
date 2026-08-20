package io.legado.app.ui.widget.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.constant.ReadAloudBgMode
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.image.cover.CoverBlurBackdrop

/**
 * 播放器通用背景：纯色 / 封面模糊 / 流动光 / 透明 四种模式。
 */
@Composable
fun PlayerBackground(
    name: String?,
    author: String?,
    path: String?,
    sourceOrigin: String?,
    bgMode: Int,
    modifier: Modifier = Modifier,
) {
    when (bgMode) {
        ReadAloudBgMode.Blur -> {
            CoverBlurBackdrop(
                name, author, path, sourceOrigin,
                modifier = modifier,
            )
        }

        ReadAloudBgMode.FlowingLight -> {
            CoverBlurBackdrop(
                name, author, path, sourceOrigin,
                modifier = modifier,
            )
        }

        ReadAloudBgMode.Transparent -> {
            Box(modifier = modifier.fillMaxSize())
        }

        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(LegadoTheme.colorScheme.surface),
            )
        }
    }
}

@Composable
fun playerBgModeLabel(mode: Int): String = when (mode) {
    ReadAloudBgMode.Solid -> stringResource(R.string.read_aloud_bg_solid)
    ReadAloudBgMode.Blur -> stringResource(R.string.read_aloud_bg_blur)
    ReadAloudBgMode.FlowingLight -> stringResource(R.string.read_aloud_bg_flowing_light)
    ReadAloudBgMode.Transparent -> stringResource(R.string.read_aloud_bg_transparent)
    else -> stringResource(R.string.read_aloud_bg_blur)
}
