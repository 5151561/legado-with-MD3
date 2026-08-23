package io.legado.app.feature.catalog.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 阅读器内目录（重设计画板 S-06a v2）的契约。
 *
 * 解 L5：目录的职责一分为二。**这里只做快速跳转 + 单章操作**，
 * 批量下载与章节管理属于目录管理页（画板 S-06b，见 [TocManageUiState]）。
 * 因此本契约里没有多选、没有全选、没有批量命令——旧稿的 4-Tab Sheet 与
 * `TocActivity` 随之取消。
 *
 * 面板画在阅读面上，用纸色而不是 App 的 surface 色阶（见 `ReadingPalette`）：
 * 从正文滑出来的东西不应该换一套材质。
 */
enum class TocChapterStatus {
    /** 已缓存，可离线打开。 */
    Cached,

    /** 未缓存，点开即联网加载。 */
    NotCached,

    /** 上次加载失败。 */
    Failed,
}

/**
 * 目录中的一章。
 *
 * @param note 状态的一句话说明。设计规则「状态优先于成功态」：未缓存要说明点开会联网，
 *   失败要说明还能换源——只画一个图标不算给出路。已缓存无需说明，故可空。
 * @param progress 仅当前章有；0f..1f。
 */
@Immutable
data class TocChapterUi(
    val id: String,
    val title: String,
    val status: TocChapterStatus,
    val note: String? = null,
    val isCurrent: Boolean = false,
    val isRead: Boolean = false,
    val progress: Float? = null,
    val progressLabel: String? = null,
)

@Stable
data class ReaderTocUiState(
    /** 形如「386 章 · 已缓存 214」。 */
    val summary: String = "",
    val chapters: ImmutableList<TocChapterUi> = persistentListOf(),
    /** 展开了单章菜单的那一章；null 表示没有展开。 */
    val chapterMenuFor: String? = null,
)

/** 单章操作。长按或行尾 more 展开，只作用于这一章。 */
enum class TocChapterAction {
    RefreshChapter,
    ChangeChapterSource,
    DownloadChapter,
}

sealed interface ReaderTocIntent {
    data class JumpTo(val chapterId: String) : ReaderTocIntent
    data object BackToCurrent : ReaderTocIntent
    data object Search : ReaderTocIntent
    data object ToggleOrder : ReaderTocIntent
    data class OpenChapterMenu(val chapterId: String) : ReaderTocIntent
    data object DismissChapterMenu : ReaderTocIntent
    data class SelectChapterAction(
        val chapterId: String,
        val action: TocChapterAction,
    ) : ReaderTocIntent

    /** 跳去目录管理页（画板 S-06b）。这是本面板唯一通向批量操作的出口。 */
    data object OpenTocManage : ReaderTocIntent
}

/** 画板 S-06a v2 的原始数据，用于预览与对稿。 */
val ReaderTocPreviewState = ReaderTocUiState(
    summary = "386 章 · 已缓存 214",
    chapters = persistentListOf(
        TocChapterUi("45", "第四十五章 断碑", TocChapterStatus.Cached, isRead = true),
        TocChapterUi("46", "第四十六章 雪夜的信", TocChapterStatus.Cached, isRead = true),
        TocChapterUi(
            "47",
            "第四十七章 城南旧雪",
            TocChapterStatus.Cached,
            isCurrent = true,
            progress = 0.62f,
            progressLabel = "62%",
        ),
        TocChapterUi("48", "第四十八章 旧案", TocChapterStatus.Cached),
        TocChapterUi(
            "49",
            "第四十九章 长夜",
            TocChapterStatus.NotCached,
            note = "未缓存 · 点开即联网加载",
        ),
        TocChapterUi(
            "50",
            "第五十章 归途",
            TocChapterStatus.Failed,
            note = "上次加载失败 · 可换源",
        ),
    ),
)
