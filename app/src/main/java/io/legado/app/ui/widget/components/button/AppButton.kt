package io.legado.app.ui.widget.components.button

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.core.designsystem.component.PrimaryButton as DesignSystemPrimaryButton
import io.legado.app.core.designsystem.component.SecondaryButton as DesignSystemSecondaryButton

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String
) {
    DesignSystemPrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text, style = LegadoTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String
) {
    DesignSystemSecondaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text, style = LegadoTheme.typography.labelLarge)
    }
}
