package io.legado.app.feature.catalog.impl

import io.legado.app.data.entities.BookSourcePart
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.CatalogCommands
import io.legado.app.feature.catalog.api.CatalogQuery
import io.legado.app.feature.catalog.api.CatalogQueryState
import io.legado.app.feature.catalog.api.CatalogRequest
import io.legado.app.feature.catalog.api.CatalogSnapshot
import io.legado.app.feature.catalog.api.CatalogSourceSummary
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Catalog business implementation. Room is the single source of truth; deleting a source stays with
 * the app shell through [CatalogSourceRemovalHost] because it also clears runtime source state.
 */
internal class DefaultCatalogRepository(
    private val store: CatalogSourceStore,
    private val removalHost: CatalogSourceRemovalHost,
) : CatalogQuery, CatalogCommands {

    override fun observeCatalog(request: CatalogRequest) = flow {
        emit(CatalogQueryState.Loading)
        emitAll(
            combine(
                store.observeExploreGroups(),
                store.observeExploreSources(request.query, request.group),
            ) { groups, sources ->
                CatalogQueryState.Data(
                    CatalogSnapshot(groups, sources.map(BookSourcePart::toSummary))
                )
            }.catch { emit(CatalogQueryState.Failed(retryable = true)) },
        )
    }

    override suspend fun pinSource(sourceId: String): CatalogCommandResult =
        command(sourceId) { store.pinSource(it) }

    override suspend fun deleteSource(sourceId: String): CatalogCommandResult =
        command(sourceId) { removalHost.deleteSource(it.bookSourceUrl) }

    private suspend fun command(
        sourceId: String,
        block: suspend (BookSourcePart) -> Unit,
    ): CatalogCommandResult = runCatching {
        val source = store.getSource(sourceId) ?: error("书源不存在")
        block(source)
    }.fold(
        onSuccess = { CatalogCommandResult.Success },
        onFailure = { CatalogCommandResult.Failure(it.message) },
    )
}

private fun BookSourcePart.toSummary() = CatalogSourceSummary(
    id = bookSourceUrl,
    name = bookSourceName,
    group = bookSourceGroup,
    hasLogin = hasLoginUrl,
    exploreEnabled = enabledExplore && hasExploreUrl,
    responseTimeMillis = respondTime,
)
