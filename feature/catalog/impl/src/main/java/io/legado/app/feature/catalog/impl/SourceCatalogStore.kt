package io.legado.app.feature.catalog.impl

import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.SourceCatalogCounts
import kotlinx.coroutines.flow.Flow

/** 见 [CatalogSourceStore] 的说明：窄端口，唯一生产实现，让契约测试无需设备即可跑。 */
internal interface SourceCatalogStore {
    fun observeCounts(): Flow<SourceCatalogCounts>

    /** [id] 为空或查不到时发 null。 */
    fun observeHttpTtsName(id: String?): Flow<String?>
}

internal class RoomSourceCatalogStore(private val database: AppDatabase) : SourceCatalogStore {

    override fun observeCounts(): Flow<SourceCatalogCounts> = database.sourceCatalogDao.flowCounts()

    override fun observeHttpTtsName(id: String?): Flow<String?> =
        if (id.isNullOrBlank()) {
            kotlinx.coroutines.flow.flowOf(null)
        } else {
            database.sourceCatalogDao.flowHttpTtsName(id)
        }
}
