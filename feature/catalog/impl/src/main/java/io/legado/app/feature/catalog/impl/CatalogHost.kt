package io.legado.app.feature.catalog.impl

/**
 * App shell seam for the catalog implementation. Deleting a book source also drops its runtime
 * source variables and its `SourceConfig` entry, so deletion keeps its existing single owner.
 */
interface CatalogSourceRemovalHost {
    suspend fun deleteSource(sourceId: String)
}
