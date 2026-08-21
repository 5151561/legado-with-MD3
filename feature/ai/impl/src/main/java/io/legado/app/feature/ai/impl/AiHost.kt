package io.legado.app.feature.ai.impl

/**
 * App shell seam for the AI implementation. Setting the default model rewrites the default task
 * presets from the app-owned generation-parameter model, so that write keeps its single owner in
 * `:app` until the AI domain model itself is modularised.
 */
interface AiDefaultModelHost {
    suspend fun setDefaultModel(modelId: String)
}
