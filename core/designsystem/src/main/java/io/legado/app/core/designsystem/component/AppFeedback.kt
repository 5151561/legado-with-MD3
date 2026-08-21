package io.legado.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.legado.app.core.designsystem.theme.LegadoTheme

@Immutable
sealed interface AppFeedbackState {
    data object Loading : AppFeedbackState
    data object Empty : AppFeedbackState
    data object Error : AppFeedbackState
}

@Composable
fun AppFeedback(
    state: AppFeedbackState,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(LegadoTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.small),
    ) {
        if (state is AppFeedbackState.Loading) {
            CircularProgressIndicator()
        }
        Text(
            text = message,
            color = when (state) {
                AppFeedbackState.Error -> LegadoTheme.colorScheme.error
                AppFeedbackState.Empty,
                AppFeedbackState.Loading -> LegadoTheme.colorScheme.onSurfaceVariant
            },
            style = LegadoTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            PrimaryButton(onClick = onAction) {
                Text(text = actionText, style = LegadoTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeWidth: Dp = 4.dp,
) {
    if (progress == null) {
        CircularProgressIndicator(modifier = modifier, strokeWidth = strokeWidth)
    } else {
        CircularProgressIndicator(
            progress = { progress },
            modifier = modifier,
            strokeWidth = strokeWidth,
        )
    }
}
