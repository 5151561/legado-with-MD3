package io.legado.app.core.designsystem.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.legado.app.core.designsystem.theme.AppTheme

/**
 * 线性进度条。厚 4dp、圆角 2dp——阅读进度、下载进度、搜索来源进度共用
 * （画板 M-01 继续阅读卡、S-01 全网搜索、C-02 缓存队列）。
 *
 * @param progress 0f..1f，超出范围按边界截断。
 */
@Composable
fun AppLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = AppTheme.colorScheme.surfaceContainerHigh,
    indicatorColor: Color = AppTheme.colorScheme.primary,
) {
    val dimens = AppTheme.dimens
    val shape = AppTheme.shapes.indicator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.progressThickness)
            .clip(shape)
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(shape)
                .background(indicatorColor),
        )
    }
}
