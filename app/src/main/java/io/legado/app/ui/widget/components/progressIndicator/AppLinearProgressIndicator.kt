package io.legado.app.ui.widget.components.progressIndicator

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.LinearProgressIndicator

@Composable
fun AppLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier
        )
    } else {
        LinearProgressIndicator(modifier = modifier)
    }
}
