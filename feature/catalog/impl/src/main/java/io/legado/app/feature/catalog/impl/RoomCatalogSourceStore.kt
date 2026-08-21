package io.legado.app.feature.catalog.impl

import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.BookSourcePart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class RoomCatalogSourceStore(private val database: AppDatabase) : CatalogSourceStore {

    private val dao get() = database.bookSourceDao

    override fun observeExploreGroups(): Flow<List<String>> = dao.flowExploreGroups()

    /** `group:` 前缀是发现页搜索框的既有语法，保持不变。 */
    override fun observeExploreSources(query: String, group: String): Flow<List<BookSourcePart>> =
        when {
            query.isNotBlank() -> if (query.startsWith("group:")) {
                dao.flowGroupExplore(query.substringAfter("group:"))
            } else {
                dao.flowExplore(query)
            }

            group.isNotBlank() -> dao.flowGroupExplore(group)
            else -> dao.flowExplore()
        }

    override suspend fun getSource(sourceId: String): BookSourcePart? =
        io { dao.getBookSourcePart(sourceId) }

    override suspend fun pinSource(source: BookSourcePart) = io {
        dao.upOrder(source.copy(customOrder = dao.minOrder - 1))
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}
