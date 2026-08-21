package io.legado.app.di

import io.legado.app.feature.catalog.impl.CatalogSourceRemovalHost
import io.legado.app.help.source.SourceHelp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App shell seam required by `:feature:catalog:impl`. Source deletion keeps its existing owner in
 * [SourceHelp] because it also clears runtime source variables and the `SourceConfig` entry.
 */
class AppCatalogSourceRemovalHost : CatalogSourceRemovalHost {
    override suspend fun deleteSource(sourceId: String) = withContext(Dispatchers.IO) {
        SourceHelp.deleteBookSource(sourceId)
    }
}
