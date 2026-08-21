package io.legado.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.legado.app.core.designsystem.theme.LegadoTheme

@Composable
fun AppConfirmDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    confirmText: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = LegadoTheme.colorScheme.surfaceContainer,
        iconContentColor = LegadoTheme.colorScheme.primary,
        titleContentColor = LegadoTheme.colorScheme.onSurface,
        textContentColor = LegadoTheme.colorScheme.onSurfaceVariant,
        title = title?.let { { Text(text = it) } },
        text = if (text != null || content != null) {
            {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    text?.let { Text(text = it) }
                    content?.invoke()
                }
            }
        } else {
            null
        },
        confirmButton = {
            if (onConfirm != null) {
                PrimaryButton(onClick = onConfirm) {
                    Text(text = confirmText, style = LegadoTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = {
            if (dismissText != null && onDismiss != null) {
                SecondaryButton(onClick = onDismiss) {
                    Text(text = dismissText, style = LegadoTheme.typography.labelLarge)
                }
            }
        },
    )
}
