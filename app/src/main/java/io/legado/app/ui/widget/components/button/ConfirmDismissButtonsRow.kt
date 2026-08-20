package io.legado.app.ui.widget.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmDismissButtonsRow(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String,
    confirmText: String,
    dismissEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
    ) {
        SecondaryButton(
            onClick = onDismiss,
            modifier = Modifier.widthIn(min = 88.dp),
            enabled = dismissEnabled,
            text = dismissText
        )
        PrimaryButton(
            onClick = onConfirm,
            modifier = Modifier.widthIn(min = 88.dp),
            enabled = confirmEnabled,
            text = confirmText
        )
    }
}
