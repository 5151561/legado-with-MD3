package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.SourceCatalogCounts
import kotlinx.coroutines.flow.Flow

/**
 * 源与规则枢纽（画板 D-00）的跨表计数。
 *
 * 九类对象各有自己的 DAO，但枢纽要的是横跨它们的一张计数表；
 * 用子查询一次取齐，Room 会跟踪其中每一张表的失效，任一处变更都会重新发射。
 */
@Dao
interface SourceCatalogDao {

    /** 默认 HTTP TTS 引擎的名字。[id] 是偏好里存的引擎 id，与 `httpTTS.id` 同值。 */
    @Query("select name from httpTTS where cast(id as text) = :id")
    fun flowHttpTtsName(id: String): Flow<String?>

    @Query(
        """
        select
            (select count(*) from book_sources) as bookSourceTotal,
            (select count(*) from book_sources where enabled = 1) as bookSourceEnabled,
            (select count(*) from book_sources
                where bookSourceGroup like '%失效%' or bookSourceGroup like '%校验超时%'
            ) as bookSourceUnhealthy,
            (select count(*) from rssSources) as rssSourceTotal,
            (select count(*) from rssSources where enabled = 1) as rssSourceEnabled,
            (select count(*) from httpTTS) as httpTtsTotal,
            (select count(*) from replace_rules) as replaceRuleTotal,
            (select count(*) from replace_rules where isEnabled = 1) as replaceRuleEnabled,
            (select count(*) from txtTocRules) as txtTocRuleTotal,
            (select count(*) from txtTocRules where id < 0) as txtTocRuleBuiltIn,
            (select count(*) from dictRules) as dictRuleTotal,
            (select count(*) from dictRules where enabled = 1) as dictRuleEnabled,
            (select count(*) from highlightRules) as contentHighlightTotal,
            (select count(*) from highlightRules where enabled = 1) as contentHighlightEnabled,
            (select count(*) from highlight_tag_rules) as tagHighlightTotal,
            (select count(*) from highlight_tag_rules where enabled = 1) as tagHighlightEnabled,
            (select count(*) from ruleSubs) as ruleSubscriptionTotal
        """
    )
    fun flowCounts(): Flow<SourceCatalogCounts>
}
