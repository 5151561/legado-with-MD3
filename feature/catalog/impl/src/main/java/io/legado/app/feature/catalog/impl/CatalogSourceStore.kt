package io.legado.app.feature.catalog.impl

import io.legado.app.data.entities.BookSourcePart
import kotlinx.coroutines.flow.Flow

/**
 * Narrow persistence port over the shared Room schema, with exactly one production implementation
 * ([RoomCatalogSourceStore]). It exists so the catalog API contract can run without a device.
 */
internal interface CatalogSourceStore {
    fun observeExploreGroups(): Flow<List<String>>
    fun observeExploreSources(query: String, group: String): Flow<List<BookSourcePart>>
    suspend fun getSource(sourceId: String): BookSourcePart?
    suspend fun pinSource(source: BookSourcePart)
}
