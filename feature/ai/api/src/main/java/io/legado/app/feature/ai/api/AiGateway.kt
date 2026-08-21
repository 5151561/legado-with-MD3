package io.legado.app.feature.ai.api

import kotlinx.coroutines.flow.Flow

data class AiProviderSummary(
    val id: String,
    val name: String,
    val protocol: String,
    val enabled: Boolean,
)

data class AiModelSummary(
    val id: String,
    val providerId: String,
    val name: String,
    val modelId: String,
    val enabled: Boolean,
    val isDefault: Boolean,
)

data class AiOverview(
    val providers: List<AiProviderSummary>,
    val models: List<AiModelSummary>,
    val presetCount: Int,
)

sealed interface AiQueryState {
    data object Loading : AiQueryState
    data class Data(val overview: AiOverview) : AiQueryState
    data class Failed(val retryable: Boolean) : AiQueryState
}

sealed interface AiCommandResult {
    data object Success : AiCommandResult
    data class Failure(val message: String?) : AiCommandResult
}

fun interface AiOverviewQuery {
    fun observeOverview(): Flow<AiQueryState>
}

fun interface AiCommands {
    suspend fun setDefaultModel(modelId: String): AiCommandResult
}
