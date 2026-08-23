package io.legado.app.feature.catalog.api

import kotlinx.coroutines.flow.Flow

/**
 * 画板 D-00 的数据面。九类对象各自的管理页不在本网关范围内，
 * 点进去仍走各自的路由；这里只回答「每一类有多少、健康与否」。
 */
fun interface SourceHubQuery {
    /** 按 [SourceCatalogKind] 的声明顺序给出，缺一类即为该类查询失败。 */
    fun observeSourceCatalog(): Flow<List<SourceCatalogCount>>
}
