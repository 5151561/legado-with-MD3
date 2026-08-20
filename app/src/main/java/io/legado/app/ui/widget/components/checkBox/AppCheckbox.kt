package io.legado.app.ui.widget.components.checkBox

import androidx.compose.material3.Checkbox
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    includeStateSemantics: Boolean = true
) {
    val state = if (checked) ToggleableState.On else ToggleableState.Off
    val semanticModifier = if (includeStateSemantics) {
        modifier.semantics {
            toggleableState = state
        }
    } else {
        modifier
    }

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = semanticModifier,
        enabled = enabled
    )
}

@Composable
fun AppTriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TriStateCheckbox(
        state = state,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}
