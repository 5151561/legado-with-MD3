package io.legado.app.feature.ai.compat

import io.legado.app.domain.gateway.AiProfileGateway
import io.legado.app.domain.model.AiTaskType
import io.legado.app.feature.ai.api.AiCommandResult
import io.legado.app.feature.ai.api.AiCommands
import io.legado.app.feature.ai.api.AiModelSummary
import io.legado.app.feature.ai.api.AiOverview
import io.legado.app.feature.ai.api.AiOverviewQuery
import io.legado.app.feature.ai.api.AiProviderSummary
import io.legado.app.feature.ai.api.AiQueryState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class LegacyAiAdapter(
    private val gateway: AiProfileGateway,
) : AiOverviewQuery, AiCommands {
    override fun observeOverview() = flow {
        emit(AiQueryState.Loading)
        emitAll(
            combine(
                gateway.observeProviders(),
                gateway.observeModels(),
                gateway.observePresets(),
            ) { providers, models, presets ->
                val defaultModelId = presets.firstOrNull {
                    it.taskType == AiTaskType.TRANSLATE_CHAPTER && it.isDefault
                }?.modelProfileId
                val providerEnabled = providers.associate { it.id to it.enabled }
                AiQueryState.Data(
                    AiOverview(
                        providers = providers.map {
                            AiProviderSummary(it.id, it.name, it.protocol, it.enabled)
                        },
                        models = models.map {
                            AiModelSummary(
                                id = it.id,
                                providerId = it.providerId,
                                name = it.displayName,
                                modelId = it.modelId,
                                enabled = it.enabled && providerEnabled[it.providerId] == true,
                                isDefault = it.id == defaultModelId,
                            )
                        },
                        presetCount = presets.size,
                    )
                )
            }.catch { emit(AiQueryState.Failed(retryable = true)) },
        )
    }

    override suspend fun setDefaultModel(modelId: String): AiCommandResult =
        runCatching { gateway.setDefaultModel(modelId) }.fold(
            onSuccess = { AiCommandResult.Success },
            onFailure = { AiCommandResult.Failure(it.message) },
        )
}
