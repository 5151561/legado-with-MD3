package io.legado.app.di

import io.legado.app.domain.gateway.AiProfileGateway
import io.legado.app.feature.ai.impl.AiDefaultModelHost

/**
 * App shell seam required by `:feature:ai:impl`. The default-model write rewrites the default task
 * presets from the app-owned generation-parameter model, so it keeps its existing single owner.
 */
class AppAiDefaultModelHost(
    private val gateway: AiProfileGateway,
) : AiDefaultModelHost {
    override suspend fun setDefaultModel(modelId: String) {
        gateway.setDefaultModel(modelId)
    }
}
