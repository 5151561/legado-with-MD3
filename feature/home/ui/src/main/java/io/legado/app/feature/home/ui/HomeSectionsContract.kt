package io.legado.app.feature.home.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 首页区块设置（重设计画板 M-01a v2）的契约。
 *
 * 四件事同页：开关、拖拽排序、恢复默认、顶部实时预览。
 *
 * **隐藏不是删除**：隐藏只影响首页呈现，数据一律保留，并且每一项都要写出该内容的
 * 其他去处（[HomeSectionUi.note]）。因此契约里没有任何删除命令。
 */
enum class HomeSectionId {
    ContinueReading,
    ReadingGoal,
    BackupReminder,
    Featured,
    RecentlyRead,
    ShelfUpdates,
}

/**
 * 一个首页区块。
 *
 * @param note 该区块的补充说明。对显示中的区块写它的深链去处，对隐藏的区块写
 *   「隐藏后还能从哪里进」——这是「隐藏不丢数据」的可见证据。
 * @param locked 不可隐藏也不可移动（「继续阅读」始终第一位）。
 */
@Immutable
data class HomeSectionUi(
    val id: HomeSectionId,
    val title: String,
    val note: String,
    val locked: Boolean = false,
)

@Stable
data class HomeSectionsUiState(
    /** 显示中的区块，顺序即首页顺序。 */
    val visible: ImmutableList<HomeSectionUi> = persistentListOf(),
    /** 已隐藏的区块，数据不丢。 */
    val hidden: ImmutableList<HomeSectionUi> = persistentListOf(),
)

sealed interface HomeSectionsIntent {
    data object Back : HomeSectionsIntent
    data object RestoreDefaults : HomeSectionsIntent
    /** 显示 ⇄ 隐藏。[HomeSectionUi.locked] 的区块不会发出这个意图。 */
    data class SetVisible(val id: HomeSectionId, val visible: Boolean) : HomeSectionsIntent
    /** 拖拽结束后的落位。索引是在「显示中」列表内的位置。 */
    data class Move(val id: HomeSectionId, val toIndex: Int) : HomeSectionsIntent
}

/** 画板 M-01a v2 的原始数据，用于预览与对稿。 */
val HomeSectionsPreviewState = HomeSectionsUiState(
    visible = persistentListOf(
        HomeSectionUi(
            HomeSectionId.ContinueReading,
            "继续阅读",
            "始终第一位 · 不可隐藏",
            locked = true,
        ),
        HomeSectionUi(HomeSectionId.ReadingGoal, "阅读目标与统计", "深链阅读统计 T-01"),
        HomeSectionUi(HomeSectionId.BackupReminder, "备份状态提醒", "仅在超期或失败时出现"),
        HomeSectionUi(HomeSectionId.Featured, "精选发现模块", "来源与模块管理 → M-02a"),
    ),
    hidden = persistentListOf(
        HomeSectionUi(HomeSectionId.RecentlyRead, "最近阅读", "隐藏后仍可从「我的 → 阅读记录」进入"),
        HomeSectionUi(HomeSectionId.ShelfUpdates, "书架更新提示", "与书架角标重复，默认关"),
    ),
)
