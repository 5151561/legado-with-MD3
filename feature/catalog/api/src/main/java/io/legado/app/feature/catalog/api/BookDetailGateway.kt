package io.legado.app.feature.catalog.api

import kotlinx.coroutines.flow.Flow

interface BookDetailQuery {
    fun observeBookDetail(request: BookDetailRequest): Flow<BookDetailQueryState>

    /**
     * 移出书架前的影响面统计。含磁盘 IO（缓存体积），调用方应当在确认框弹出前后台取。
     * 书籍不存在时返回 null。
     */
    suspend fun removalImpact(bookId: String): BookRemovalImpact?
}

interface BookDetailCommands {
    suspend fun addToBookshelf(bookId: String): CatalogCommandResult

    /**
     * 移出书架。[deleteLocalFile] 是**独立的第二意图**——移出书架与删除本地文件是
     * 两种不同的对象，合并成一个开关会让用户在不知情下丢文件。
     */
    suspend fun removeFromBookshelf(
        bookId: String,
        deleteLocalFile: Boolean = false,
    ): CatalogCommandResult

    suspend fun moveToGroup(bookId: String, groupId: Long): CatalogCommandResult

    /** [coverUrl] 传 null 表示恢复书源封面。 */
    suspend fun updateCover(bookId: String, coverUrl: String?): CatalogCommandResult

    /** [remark] 传 null 或空串表示清除备注。 */
    suspend fun updateRemark(bookId: String, remark: String?): CatalogCommandResult
}
