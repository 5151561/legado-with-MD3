package io.legado.app.feature.rss.impl

import io.legado.app.data.entities.RssSource
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

/**
 * RSS business implementation. Room is the single source of truth; JS evaluation and the
 * source-deletion side effects stay with the app shell through [RssSourceScriptHost] /
 * [RssSourceRemovalHost].
 */
internal class DefaultRssRepository(
    private val store: RssSourceStore,
    private val scriptHost: RssSourceScriptHost,
    private val removalHost: RssSourceRemovalHost,
) : RssQuery, RssCommands {

    override fun observeSources(request: RssRequest) = flow {
        emit(RssQueryState.Loading)
        emitAll(
            combine(
                store.observeEnabledGroups(),
                store.observeEnabledSources(request.query, request.group),
            ) { groups, sources ->
                RssQueryState.Data(RssSnapshot(groups, sources.map(RssSource::toSummary)))
            }.catch { emit(RssQueryState.Failed(retryable = true)) },
        )
    }

    override suspend fun resolveOpenTarget(sourceId: String): Result<RssOpenTarget> = runCatching {
        val source = store.getSource(sourceId) ?: error("RSS 源不存在")
        if (!source.singleUrl) {
            return@runCatching if (source.startHtml.isNullOrBlank()) {
                RssOpenTarget.Sort(source.sourceUrl)
            } else {
                RssOpenTarget.Read(source.sourceName, source.sourceUrl, startPage = true)
            }
        }
        val resolved = resolveSingleUrl(source)
        if (resolved.startsWith("http", ignoreCase = true)) {
            RssOpenTarget.Read(source.sourceName, resolved, startPage = false)
        } else {
            RssOpenTarget.External(resolved)
        }
    }

    override suspend fun pinSource(sourceId: String) = command(sourceId, store::pinSource)

    override suspend fun disableSource(sourceId: String) =
        command(sourceId) { store.setEnabled(it, enabled = false) }

    override suspend fun deleteSource(sourceId: String) =
        command(sourceId) { removalHost.deleteSource(it.sourceUrl) }

    private suspend fun resolveSingleUrl(source: RssSource): String {
        val configured = source.sortUrl
        if (configured.isNullOrBlank()) return source.sourceUrl
        val resolved = if (configured.startsWith("<js>") || configured.startsWith("@js:")) {
            val script = if (configured.startsWith("@")) {
                configured.substring(4)
            } else {
                configured.substring(4, configured.lastIndexOf("<"))
            }
            scriptHost.evaluateSourceScript(source, script)?.takeIf(String::isNotBlank) ?: configured
        } else {
            configured
        }
        return if (resolved.contains("::")) resolved.substringAfter("::") else resolved
    }

    private suspend fun command(
        sourceId: String,
        block: suspend (RssSource) -> Unit,
    ): RssCommandResult = runCatching {
        val source = store.getSource(sourceId) ?: error("RSS 源不存在")
        block(source)
    }.fold(
        onSuccess = { RssCommandResult.Success },
        onFailure = { RssCommandResult.Failure(it.message) },
    )
}

private fun RssSource.toSummary() = RssSourceSummary(
    id = sourceUrl,
    name = sourceName,
    icon = sourceIcon.takeIf(String::isNotBlank),
    group = sourceGroup,
    hasLogin = !loginUrl.isNullOrBlank(),
)
