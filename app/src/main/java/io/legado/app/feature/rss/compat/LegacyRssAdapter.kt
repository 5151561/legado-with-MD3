package io.legado.app.feature.rss.compat

import io.legado.app.data.repository.RssRepository
import io.legado.app.domain.usecase.LegacyRssOpenTarget
import io.legado.app.domain.usecase.ResolveRssOpenTargetUseCase
import io.legado.app.feature.rss.api.RssCommandResult
import io.legado.app.feature.rss.api.RssCommands
import io.legado.app.feature.rss.api.RssOpenTarget
import io.legado.app.feature.rss.api.RssQuery
import io.legado.app.feature.rss.api.RssQueryState
import io.legado.app.feature.rss.api.RssRequest
import io.legado.app.feature.rss.api.RssSnapshot
import io.legado.app.feature.rss.api.RssSourceSummary
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class LegacyRssAdapter(
    private val repository: RssRepository,
    private val resolveOpenTarget: ResolveRssOpenTargetUseCase,
) : RssQuery, RssCommands {
    override fun observeSources(request: RssRequest) = flow {
        emit(RssQueryState.Loading)
        emitAll(
            combine(
                repository.getEnabledGroups(),
                repository.getEnabledSources(request.query, request.group),
            ) { groups, sources ->
                RssQueryState.Data(
                    RssSnapshot(
                        groups,
                        sources.map {
                            RssSourceSummary(
                                id = it.sourceUrl,
                                name = it.sourceName,
                                icon = it.sourceIcon.takeIf(String::isNotBlank),
                                group = it.sourceGroup,
                                hasLogin = !it.loginUrl.isNullOrBlank(),
                            )
                        },
                    )
                )
            }.catch { emit(RssQueryState.Failed(retryable = true)) },
        )
    }

    override suspend fun resolveOpenTarget(sourceId: String): Result<RssOpenTarget> = runCatching {
        val source = repository.getByKey(sourceId) ?: error("RSS 源不存在")
        when (val target = resolveOpenTarget(source)) {
            is LegacyRssOpenTarget.Sort -> RssOpenTarget.Sort(target.sourceId)
            is LegacyRssOpenTarget.Read -> RssOpenTarget.Read(
                title = target.title,
                origin = target.origin,
                startPage = target.startPage,
            )
            is LegacyRssOpenTarget.External -> RssOpenTarget.External(target.url)
        }
    }

    override suspend fun pinSource(sourceId: String) = command(sourceId) { repository.topSources(it) }
    override suspend fun disableSource(sourceId: String) = command(sourceId) { repository.disableSource(it) }
    override suspend fun deleteSource(sourceId: String) = command(sourceId) { repository.deleteSources(listOf(it)) }

    private suspend fun command(
        sourceId: String,
        block: suspend (io.legado.app.data.entities.RssSource) -> Unit,
    ): RssCommandResult = runCatching {
        val source = repository.getByKey(sourceId) ?: error("RSS 源不存在")
        block(source)
    }.fold(
        onSuccess = { RssCommandResult.Success },
        onFailure = { RssCommandResult.Failure(it.message) },
    )
}
