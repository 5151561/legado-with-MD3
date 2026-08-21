package io.legado.app.feature.ai.impl

import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.AiModelProfile
import io.legado.app.data.entities.AiProviderProfile
import io.legado.app.data.entities.AiTaskPreset
import kotlinx.coroutines.flow.Flow

internal class RoomAiProfileStore(private val database: AppDatabase) : AiProfileStore {

    private val dao get() = database.aiProfileDao

    override fun observeProviders(): Flow<List<AiProviderProfile>> = dao.observeProviders()

    override fun observeModels(): Flow<List<AiModelProfile>> = dao.observeModels()

    override fun observePresets(): Flow<List<AiTaskPreset>> = dao.observePresets()
}
