package io.legado.app.feature.rss.impl

import io.legado.app.data.entities.RssSource
import kotlinx.coroutines.flow.Flow

/**
 * Narrow persistence port over the shared Room schema, with exactly one production implementation
 * ([RoomRssSourceStore]). It exists so the RSS API contract can run without a device.
 */
internal interface RssSourceStore {
    fun observeEnabledGroups(): Flow<List<String>>
    fun observeEnabledSources(query: String, group: String): Flow<List<RssSource>>
    suspend fun getSource(sourceId: String): RssSource?
    suspend fun pinSource(source: RssSource)
    suspend fun setEnabled(source: RssSource, enabled: Boolean)
}
