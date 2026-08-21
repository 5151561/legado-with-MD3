package io.legado.app.feature.readaloud.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.core.designsystem.component.AppListItem
import io.legado.app.core.designsystem.component.AppScaffold
import io.legado.app.core.designsystem.component.AppTopBar
import io.legado.app.core.designsystem.theme.LegadoTheme
import io.legado.app.feature.readaloud.api.ReadAloudStatus
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReadAloudRouteScreen(
    onBack: () -> Unit,
    onOpenVoices: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchToClassic: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReadAloudViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReadAloudEffect.Voices -> onOpenVoices()
                ReadAloudEffect.Cache -> onOpenCache()
                ReadAloudEffect.Settings -> onOpenSettings()
                ReadAloudEffect.Classic -> onSwitchToClassic()
                is ReadAloudEffect.Message -> snackbar.showSnackbar(effect.text)
            }
        }
    }
    ReadAloudScreen(state, viewModel::onIntent, onBack, snackbar, modifier)
}

@Composable
fun ReadAloudScreen(
    state: ReadAloudUiState,
    onIntent: (ReadAloudIntent) -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = state.bookName.ifBlank { "朗读" },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(LegadoTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.medium),
        ) {
            Text(state.chapterTitle, style = LegadoTheme.typography.titleMedium)
            Text(
                text = state.currentText.ifBlank { "等待朗读内容" },
                modifier = Modifier.weight(1f),
                style = LegadoTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Slider(
                value = state.chapterPosition.coerceIn(0, state.chapterLength).toFloat(),
                onValueChange = { onIntent(ReadAloudIntent.SeekTo(it.toInt())) },
                valueRange = 0f..state.chapterLength.coerceAtLeast(1).toFloat(),
                enabled = !state.commandInFlight,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerControl(Icons.Default.SkipPrevious, "上一段") { onIntent(ReadAloudIntent.PreviousParagraph) }
                PlayerControl(Icons.Default.FastRewind, "上一章") { onIntent(ReadAloudIntent.PreviousChapter) }
                PlayerControl(
                    if (state.status == ReadAloudStatus.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (state.status == ReadAloudStatus.Playing) "暂停" else "播放",
                ) { onIntent(ReadAloudIntent.TogglePause) }
                PlayerControl(Icons.Default.FastForward, "下一章") { onIntent(ReadAloudIntent.NextChapter) }
                PlayerControl(Icons.Default.SkipNext, "下一段") { onIntent(ReadAloudIntent.NextParagraph) }
            }
            AppListItem(
                headlineContent = { Text(state.engineName.ifBlank { "系统朗读" }) },
                supportingContent = { Text(state.voiceName.ifBlank { "默认语音" }) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.small)) {
                Button(onClick = { onIntent(ReadAloudIntent.OpenVoices) }) { Text("语音") }
                Button(onClick = { onIntent(ReadAloudIntent.OpenCache) }) { Text("缓存") }
                Button(onClick = { onIntent(ReadAloudIntent.OpenSettings) }) { Text("设置") }
                Button(onClick = { onIntent(ReadAloudIntent.SwitchToClassic) }) { Text("经典") }
            }
        }
    }
}

@Composable
private fun PlayerControl(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) { Icon(imageVector, contentDescription = description) }
}
