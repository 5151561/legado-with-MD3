package io.legado.app.feature.catalog.compat

import io.legado.app.data.repository.ExploreRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Phase 3 compatibility seam over the existing book-source SSOT and repository commands. */
class LegacyCatalogAdapter(
    private val repository: ExploreRepository,
) : CatalogQuery, CatalogCommands {
    override fun observeCatalog(request: CatalogRequest) = flow {
        emit(CatalogQueryState.Loading)
        emitAll(
            combine(
                repository.getExploreGroups(),
                repository.getExploreSources(request.query, request.group),
            ) { groups, sources ->
                CatalogQueryState.Data(
                    CatalogSnapshot(
                        groups = groups,
                        sources = sources.map { source ->
                            CatalogSourceSummary(
                                id = source.bookSourceUrl,
                                name = source.bookSourceName,
                                group = source.bookSourceGroup,
                                hasLogin = source.hasLoginUrl,
                                exploreEnabled = source.enabledExplore && source.hasExploreUrl,
                                responseTimeMillis = source.respondTime,
                            )
                        },
                    )
                )
            }.catch { emit(CatalogQueryState.Failed(retryable = true)) },
        )
    }

    override suspend fun pinSource(sourceId: String): CatalogCommandResult = command {
        val source = repository.getExploreSources("", "").first()
            .firstOrNull { it.bookSourceUrl == sourceId }
            ?: error("书源不存在")
        repository.topSource(source)
    }

    override suspend fun deleteSource(sourceId: String): CatalogCommandResult = command {
        repository.deleteSource(sourceId)
    }

    private suspend fun command(block: suspend () -> Unit): CatalogCommandResult =
        runCatching { block() }.fold(
            onSuccess = { CatalogCommandResult.Success },
            onFailure = { CatalogCommandResult.Failure(it.message) },
        )
}
