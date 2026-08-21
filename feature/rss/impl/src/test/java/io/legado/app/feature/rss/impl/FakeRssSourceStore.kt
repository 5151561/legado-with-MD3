package io.legado.app.feature.rss.impl

import io.legado.app.data.entities.RssSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * `RssSource.equals` only compares `sourceUrl`, so a `MutableStateFlow<List<RssSource>>` would
 * conflate away every field update. Room invalidates by table, so the fake tracks a revision
 * counter instead of relying on list equality.
 */
internal class FakeRssSourceStore(sources: List<RssSource> = emptyList()) : RssSourceStore {

    var sources: List<RssSource> = sources
        set(value) {
            field = value
            revision.value += 1
        }

    private val revision = MutableStateFlow(0)
    var groupsFlow: Flow<List<String>> = MutableStateFlow(listOf("news"))
    var failOn: String? = null

    private fun gate(op: String) {
        if (failOn == op) error("boom")
    }

    override fun observeEnabledGroups(): Flow<List<String>> = groupsFlow

    override fun observeEnabledSources(query: String, group: String): Flow<List<RssSource>> =
        revision.map {
            sources.filter { source ->
                source.enabled &&
                    (query.isEmpty() || source.sourceName.contains(query, ignoreCase = true)) &&
                    (group.isEmpty() || source.sourceGroup.orEmpty().contains(group))
            }
        }

    override suspend fun getSource(sourceId: String): RssSource? =
        sources.firstOrNull { it.sourceUrl == sourceId }

    override suspend fun pinSource(source: RssSource) {
        gate("pinSource")
        val minOrder = sources.minOfOrNull { it.customOrder } ?: 0
        replace(source.copy(customOrder = minOrder - 1))
    }

    override suspend fun setEnabled(source: RssSource, enabled: Boolean) {
        gate("setEnabled")
        replace(source.copy(enabled = enabled))
    }

    private fun replace(source: RssSource) {
        sources = sources.map { if (it.sourceUrl == source.sourceUrl) source else it }
    }
}

internal class RecordingScriptHost(private val result: String? = null) : RssSourceScriptHost {
    val scripts = mutableListOf<String>()
    override suspend fun evaluateSourceScript(source: RssSource, script: String): String? {
        scripts += script
        return result
    }
}

internal class RecordingRemovalHost : RssSourceRemovalHost {
    val deleted = mutableListOf<String>()
    override suspend fun deleteSource(sourceId: String) {
        deleted += sourceId
    }
}

internal fun rssSource(
    id: String,
    name: String = id,
    group: String? = "news",
    enabled: Boolean = true,
    singleUrl: Boolean = false,
    sortUrl: String? = null,
    startHtml: String? = null,
    loginUrl: String? = null,
    icon: String = "",
    order: Int = 0,
) = RssSource(
    sourceUrl = id,
    sourceName = name,
    sourceIcon = icon,
    sourceGroup = group,
    enabled = enabled,
    singleUrl = singleUrl,
    sortUrl = sortUrl,
    startHtml = startHtml,
    loginUrl = loginUrl,
    customOrder = order,
)
