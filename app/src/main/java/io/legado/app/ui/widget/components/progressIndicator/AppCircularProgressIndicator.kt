package io.legado.app.ui.widget.components.progressIndicator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.component.AppCircularProgressIndicator as DesignSystemCircularProgressIndicator

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeWidth: Dp = 4.dp,
) {
    DesignSystemCircularProgressIndicator(
        modifier = modifier,
        progress = progress,
        strokeWidth = strokeWidth,
    )
}
