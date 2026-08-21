package io.legado.app.feature.reader.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.core.designsystem.component.AppFeedback
import io.legado.app.core.designsystem.component.AppFeedbackState
import io.legado.app.core.designsystem.theme.LegadoTheme
import io.legado.app.feature.reader.api.ReaderLoadState
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReaderRouteScreen(
    onToggleMenu: () -> Unit,
    onViewportChanged: (widthPx: Int, heightPx: Int, density: Float) -> Unit,
    onFirstContentDrawn: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val fallbackMessage = stringResource(R.string.reader_command_failed)
    LaunchedEffect(state.loadState, state.contentRevision, state.pageText) {
        if (state.loadState == ReaderLoadState.Content && state.pageText.isNotBlank()) {
            onFirstContentDrawn()
        }
    }
    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReaderEffect.Message -> snackbarHostState.showSnackbar(
                    effect.text ?: fallbackMessage
                )
            }
        }
    }
    ReaderScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onToggleMenu = onToggleMenu,
        onViewportChanged = onViewportChanged,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Stateless, no-animation Phase 4 renderer. */
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onIntent: (ReaderIntent) -> Unit,
    onToggleMenu: () -> Unit,
    onViewportChanged: (widthPx: Int, heightPx: Int, density: Float) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    Surface(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size -> onViewportChanged(size.width, size.height, density) },
        color = LegadoTheme.colorScheme.background,
        contentColor = LegadoTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize()) {
            when (state.loadState) {
                ReaderLoadState.Idle,
                ReaderLoadState.Loading -> AppFeedback(
                    state = AppFeedbackState.Loading,
                    message = stringResource(R.string.reader_loading),
                    modifier = Modifier.align(Alignment.Center),
                )

                ReaderLoadState.Failure -> AppFeedback(
                    state = AppFeedbackState.Error,
                    message = stringResource(R.string.reader_error),
                    actionText = if (state.error?.retryable == true) {
                        stringResource(R.string.reader_retry)
                    } else null,
                    onAction = if (state.error?.retryable == true) {
                        { onIntent(ReaderIntent.Retry) }
                    } else null,
                    modifier = Modifier.align(Alignment.Center),
                )

                ReaderLoadState.Content -> if (state.pageText.isBlank()) {
                    AppFeedback(
                        state = AppFeedbackState.Empty,
                        message = stringResource(R.string.reader_empty),
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    ReaderPage(
                        state = state,
                        onPrevious = { onIntent(ReaderIntent.PreviousPage) },
                        onNext = { onIntent(ReaderIntent.NextPage) },
                        onToggleMenu = onToggleMenu,
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Custom reader canvas exception: neither the project Design System nor Material 3 provides a
 * paginated text surface with left/centre/right tap zones. It still consumes theme tokens and
 * exposes equivalent page actions to accessibility services.
 */
@Composable
private fun ReaderPage(
    state: ReaderUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previousLabel = stringResource(R.string.reader_previous_page)
    val nextLabel = stringResource(R.string.reader_next_page)
    val menuLabel = stringResource(R.string.reader_toggle_menu)
    val pageDescription = stringResource(
        R.string.reader_page_description,
        state.bookName.ifBlank { state.chapterTitle },
        state.chapterIndex + 1,
        state.pageIndex + 1,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = pageDescription
                onClick(label = menuLabel) {
                    onToggleMenu()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(previousLabel) {
                        if (state.canGoPrevious) onPrevious()
                        state.canGoPrevious
                    },
                    CustomAccessibilityAction(nextLabel) {
                        if (state.canGoNext) onNext()
                        state.canGoNext
                    },
                )
            }
            .pointerInput(
                state.canGoPrevious,
                state.canGoNext,
                state.contentRevision,
            ) {
                detectTapGestures { offset ->
                    when {
                        offset.x < size.width * PREVIOUS_ZONE_RATIO && state.canGoPrevious ->
                            onPrevious()
                        offset.x > size.width * NEXT_ZONE_RATIO && state.canGoNext -> onNext()
                        else -> onToggleMenu()
                    }
                }
            }
            .padding(
                horizontal = LegadoTheme.spacing.large,
                vertical = LegadoTheme.spacing.medium,
            ),
        verticalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.small),
    ) {
        Text(
            text = state.chapterTitle,
            modifier = Modifier.fillMaxWidth(),
            color = LegadoTheme.colorScheme.onSurfaceVariant,
            style = LegadoTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.pageText,
            modifier = Modifier.weight(1f),
            style = LegadoTheme.typography.bodyLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = state.bookName,
                style = LegadoTheme.typography.labelSmall,
                color = LegadoTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.reader_progress,
                    state.pageIndex + 1,
                    state.pageCount.coerceAtLeast(1),
                ),
                style = LegadoTheme.typography.labelSmall,
                color = LegadoTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val PREVIOUS_ZONE_RATIO = 0.3f
private const val NEXT_ZONE_RATIO = 0.7f
