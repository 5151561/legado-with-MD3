package io.legado.app.feature.ai.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AiModelUi(
    val id: String,
    val providerId: String,
    val providerName: String,
    val name: String,
    val modelId: String,
    val enabled: Boolean,
    val isDefault: Boolean,
)

@Stable
data class AiUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val providerCount: Int = 0,
    val presetCount: Int = 0,
    val models: ImmutableList<AiModelUi> = persistentListOf(),
    val commandInFlight: Boolean = false,
)

sealed interface AiIntent {
    data object Retry : AiIntent
    data class SetDefaultModel(val modelId: String) : AiIntent
    data object OpenChat : AiIntent
    data object AddProvider : AiIntent
    data class EditProvider(val providerId: String) : AiIntent
    data class EditModel(val providerId: String, val modelId: String) : AiIntent
    data object OpenSummaryPrompt : AiIntent
    data object OpenPromptSettings : AiIntent
}

sealed interface AiEffect {
    data object Chat : AiEffect
    data object AddProvider : AiEffect
    data class EditProvider(val providerId: String) : AiEffect
    data class EditModel(val providerId: String, val modelId: String) : AiEffect
    data object SummaryPrompt : AiEffect
    data object PromptSettings : AiEffect
    data class Message(val text: String) : AiEffect
}
