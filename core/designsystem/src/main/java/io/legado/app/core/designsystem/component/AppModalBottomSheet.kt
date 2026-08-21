package io.legado.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.legado.app.core.designsystem.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!show) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = LegadoTheme.colorScheme.surfaceContainer,
        contentColor = LegadoTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = LegadoTheme.colorScheme.onSurfaceVariant)
        },
        contentWindowInsets = contentWindowInsets,
        sheetGesturesEnabled = sheetGesturesEnabled,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = LegadoTheme.spacing.medium,
                    end = LegadoTheme.spacing.medium,
                    bottom = LegadoTheme.spacing.medium,
                )
                .then(modifier),
        ) {
            if (!title.isNullOrEmpty() || startAction != null || endAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = LegadoTheme.spacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    startAction?.let {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) { it() }
                    }
                    title?.let {
                        Text(
                            text = it,
                            style = LegadoTheme.typography.titleMediumEmphasized,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    endAction?.let {
                        Box(modifier = Modifier.align(Alignment.CenterEnd)) { it() }
                    }
                }
            }
            content()
        }
    }
}
