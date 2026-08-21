package io.legado.app.feature.catalog.api

import kotlinx.coroutines.flow.Flow

fun interface CatalogQuery {
    fun observeCatalog(request: CatalogRequest): Flow<CatalogQueryState>
}

interface CatalogCommands {
    suspend fun pinSource(sourceId: String): CatalogCommandResult
    suspend fun deleteSource(sourceId: String): CatalogCommandResult
}
