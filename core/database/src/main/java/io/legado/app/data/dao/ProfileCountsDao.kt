package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.ProfileCounts
import kotlinx.coroutines.flow.Flow

/**
 * 「我的」（画板 P-01）的跨表计数。
 *
 * 与 [SourceCatalogDao] 同一手法：用子查询一次取齐，Room 会跟踪其中每一张表的失效。
 */
@Dao
interface ProfileCountsDao {

    /**
     * @param sinceDate 统计窗口的起始日期，含当天，格式 `yyyy-MM-dd`，
     *   与 `readRecordDetail.date` 同格式，因此可以直接按字符串比较。
     */
    @Query(
        """
        select
            (select count(*) from bookmarks) as bookmarkTotal,
            (select count(*) from (select distinct bookName, bookAuthor from bookmarks))
                as bookmarkBookTotal,
            (select count(*) from book_marks) as markingTotal,
            (select ifnull(sum(readTime), 0) from readRecordDetail where date >= :sinceDate)
                as windowReadMillis,
            (select count(*) from ai_chat_conversations) as aiConversationTotal
        """
    )
    fun flowCounts(sinceDate: String): Flow<ProfileCounts>
}
