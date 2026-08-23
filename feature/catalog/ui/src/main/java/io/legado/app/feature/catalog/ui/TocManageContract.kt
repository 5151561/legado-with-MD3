package io.legado.app.feature.catalog.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * 目录管理页（重设计画板 S-06b v2）的契约。
 *
 * 目录职责拆分的另一半（解 L5）：这里做批量，阅读器内目录（画板 S-06a）只做跳转。
 *
 * 选择态套模板 TPL-03：底部一条统一的浮动条，**主操作按对象固定**——这一页的对象是
 * 章节，所以主操作恒为「下载」。删除缓存这类危险动作收在「更多」里并带二次确认，
 * 不与主操作并列。
 */
enum class TocFilter {
    All,
    NotCached,
    Failed,
}

@Immutable
data class TocFilterUi(
    val id: TocFilter,
    val label: String,
    val count: Int,
)

@Immutable
data class TocManageChapterUi(
    val id: String,
    val title: String,
    val status: TocChapterStatus,
    /** 状态说明：「未缓存」「已缓存 · 12 KB」「上次失败 · 正文为空」。 */
    val note: String,
    /** 失败项的就地重试；其余为 false。 */
    val retryable: Boolean = false,
)

@Stable
data class TocManageUiState(
    /** 书名 · 章节总数，写在标题下方。 */
    val subtitle: String = "",
    val filters: ImmutableList<TocFilterUi> = persistentListOf(),
    val activeFilter: TocFilter = TocFilter.All,
    val chapters: ImmutableList<TocManageChapterUi> = persistentListOf(),
    val selected: ImmutableSet<String> = persistentSetOf(),
    /** 列表底部的操作提示。范围选择不是隐藏功能，要写出来。 */
    val hint: String = "",
    val moreMenuVisible: Boolean = false,
)

sealed interface TocManageIntent {
    data object Close : TocManageIntent
    data object Search : TocManageIntent
    data class SelectFilter(val filter: TocFilter) : TocManageIntent
    data class ToggleChapter(val chapterId: String) : TocManageIntent
    /** 长按首章 → 点尾章的范围选择落位。 */
    data class SelectRange(val fromChapterId: String, val toChapterId: String) : TocManageIntent
    data class RetryChapter(val chapterId: String) : TocManageIntent
    data object SelectAll : TocManageIntent
    data object InvertSelection : TocManageIntent
    /** 主操作。对象是章节，因此恒为下载。 */
    data object DownloadSelected : TocManageIntent
    data object OpenMoreMenu : TocManageIntent
    data object DismissMoreMenu : TocManageIntent
    /** 「更多」里的删除缓存。执行前由上层弹二次确认。 */
    data object DeleteSelectedCache : TocManageIntent
}

sealed interface TocManageEffect {
    data object NavigateBack : TocManageEffect
    data object OpenSearch : TocManageEffect
    data class ShowMessage(val text: String) : TocManageEffect

    /** 删除缓存的二次确认由上层弹，确认后回 [TocManageIntent.DeleteSelectedCache]。 */
    data class ConfirmDeleteCache(val chapterCount: Int) : TocManageEffect
}

/** 画板 S-06b v2 的原始数据，用于预览与对稿。 */
val TocManagePreviewState = TocManageUiState(
    subtitle = "雪落长安 · 386 章",
    filters = persistentListOf(
        TocFilterUi(TocFilter.All, "全部", 386),
        TocFilterUi(TocFilter.NotCached, "未缓存", 172),
        TocFilterUi(TocFilter.Failed, "失败", 2),
    ),
    chapters = persistentListOf(
        TocManageChapterUi("51", "第五十一章 雪停", TocChapterStatus.NotCached, "未缓存"),
        TocManageChapterUi("52", "第五十二章 火盆", TocChapterStatus.NotCached, "未缓存"),
        TocManageChapterUi(
            "53",
            "第五十三章 旧友",
            TocChapterStatus.Failed,
            "上次失败 · 正文为空",
            retryable = true,
        ),
        TocManageChapterUi("54", "第五十四章 长街", TocChapterStatus.Cached, "已缓存 · 12 KB"),
        TocManageChapterUi("55", "第五十五章 春信", TocChapterStatus.NotCached, "未缓存"),
        TocManageChapterUi("56", "第五十六章 归人", TocChapterStatus.NotCached, "未缓存"),
    ),
    selected = persistentSetOf("51", "52", "53"),
    hint = "支持「起止范围」快速选择：长按首章 → 点尾章",
)
