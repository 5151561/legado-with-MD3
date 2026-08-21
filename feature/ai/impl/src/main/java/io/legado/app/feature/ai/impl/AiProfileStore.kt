package io.legado.app.feature.ai.impl

import io.legado.app.data.entities.AiModelProfile
import io.legado.app.data.entities.AiProviderProfile
import io.legado.app.data.entities.AiTaskPreset
import kotlinx.coroutines.flow.Flow

/**
 * Narrow persistence port over the shared Room schema, with exactly one production implementation
 * ([RoomAiProfileStore]). It exists so the AI overview contract can run without a device.
 */
internal interface AiProfileStore {
    fun observeProviders(): Flow<List<AiProviderProfile>>
    fun observeModels(): Flow<List<AiModelProfile>>
    fun observePresets(): Flow<List<AiTaskPreset>>
}
