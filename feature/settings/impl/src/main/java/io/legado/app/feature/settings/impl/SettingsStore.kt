package io.legado.app.feature.settings.impl

import io.legado.app.data.AppDatabase
import io.legado.app.data.entities.ProfileCounts
import io.legado.app.data.entities.SourceCatalogCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 窄端口，唯一生产实现是 Room。契约测试用假实现替换它，因此无需设备即可跑。
 * 手法与 `catalog:impl` 的各 Store 一致。
 */
internal interface SettingsStore {

    /** @param sinceDate 阅读时长统计窗口的起始日期，含当天，格式 `yyyy-MM-dd`。 */
    fun observeProfileCounts(sinceDate: String): Flow<ProfileCounts>

    /** 源与规则的计数复用枢纽（画板 D-00）那一份，不另写一套口径。 */
    fun observeSourceCounts(): Flow<SourceCatalogCounts>

    /** [id] 为空或查不到时发 null。 */
    fun observeHttpTtsName(id: String?): Flow<String?>
}

internal class RoomSettingsStore(private val database: AppDatabase) : SettingsStore {

    override fun observeProfileCounts(sinceDate: String): Flow<ProfileCounts> =
        database.profileCountsDao.flowCounts(sinceDate)

    override fun observeSourceCounts(): Flow<SourceCatalogCounts> =
        database.sourceCatalogDao.flowCounts()

    override fun observeHttpTtsName(id: String?): Flow<String?> =
        if (id.isNullOrBlank()) flowOf(null) else database.sourceCatalogDao.flowHttpTtsName(id)
}
