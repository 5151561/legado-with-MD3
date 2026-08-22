package io.legado.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.theme.LegadoTheme

@Composable
fun PillDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 2.dp,
    widthFraction: Float = 0.2f,
    color: Color = LegadoTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(thickness)
                .clip(CircleShape)
                .background(color)
        )
    }
}
