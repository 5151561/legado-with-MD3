package io.legado.app.feature.rss.impl

import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.RssSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class RoomRssSourceStore(private val database: AppDatabase) : RssSourceStore {

    private val dao get() = database.rssSourceDao

    override fun observeEnabledGroups(): Flow<List<String>> = dao.flowEnabledGroups()

    override fun observeEnabledSources(query: String, group: String): Flow<List<RssSource>> =
        when {
            query.isNotEmpty() -> dao.flowEnabled(query)
            group.isNotEmpty() -> dao.flowEnabledByGroup(group)
            else -> dao.flowEnabled()
        }

    override suspend fun getSource(sourceId: String): RssSource? = io { dao.getByKey(sourceId) }

    /** Keeps the legacy "top" semantics: the pinned source takes one slot above the current min. */
    override suspend fun pinSource(source: RssSource) = io {
        dao.update(source.copy(customOrder = dao.minOrder - 1))
    }

    override suspend fun setEnabled(source: RssSource, enabled: Boolean) = io {
        dao.update(source.copy(enabled = enabled))
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}
