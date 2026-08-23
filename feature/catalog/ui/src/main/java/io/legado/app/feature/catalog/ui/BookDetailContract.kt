package io.legado.app.feature.catalog.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 书籍详情（重设计画板 S-04 v2 / S-04a v2）的契约。
 *
 * 解 N4 / N5：**单一路由、单一宿主**。书架、搜索、阅读器、阅读记录、导入这五个入口
 * 全部进这一个页面、同一转场，不再有第二套详情实现。因此契约里没有「来自哪个入口」
 * 这类分支参数——入口差异只体现在导航，不体现在页面形态。
 *
 * 目录在这里只是一个入口：批量下载与章节管理属于目录管理页（画板 S-06b），
 * 阅读器内的快速跳转属于 [ReaderTocUiState]（画板 S-06a）。三者职责不重叠（解 L5）。
 *
 * 换源走统一组件（画板 S-08），这里只表达「当前源 + 有多少候选」与一条
 * [BookDetailIntent.ChangeSource]。
 */
@Immutable
data class BookDetailHeaderUi(
    val bookId: String,
    val name: String,
    /** 作者 · 分类 · 连载状态。 */
    val byline: String,
    /** 章节总数与最新一章。 */
    val chapterSummary: String,
    /** 「在书架 · 历史组」；null 表示不在书架。 */
    val shelfLabel: String? = null,
    /** 阅读进度 0f..1f；null 表示还没开始读。 */
    val progress: Float? = null,
    val progressLabel: String? = null,
)

/** 当前书源与候选数量。换源本身走画板 S-08 的统一组件。 */
@Immutable
data class BookSourceSummaryUi(
    val name: String,
    val alternativesLabel: String,
)

enum class BookDetailEntryId {
    /** 目录与章节管理 → 画板 S-06b。 */
    Catalog,

    /** 人物 · 知识 · 事件。 */
    Insights,
}

@Immutable
data class BookDetailEntryUi(
    val id: BookDetailEntryId,
    val title: String,
    val summary: String? = null,
    /** 行尾的计数文字，如「28 / 14 / 9」。 */
    val valueLabel: String? = null,
)

@Immutable
data class RelatedBookUi(
    val id: String,
    val title: String,
)

enum class BookDetailMenuAction {
    MoveToGroup,
    ChangeCover,
    EditInfo,
    Note,
    EditVariables,
    RemoveFromShelf,
    DeleteLocalFile,
}

/**
 * 更多菜单里的一项。
 *
 * [dangerous] 的项排在最后一组、与上面隔一条分隔线。
 * 「移出书架」与「删除本地文件」是**两种不同的对象**，因此分开列、各自确认，
 * 不合并成一个「删除」。
 */
@Immutable
data class BookDetailMenuItemUi(
    val action: BookDetailMenuAction,
    val title: String,
    /** 该项的适用条件，如「仅本地书可见」。 */
    val summary: String? = null,
    val dangerous: Boolean = false,
)

@Immutable
sealed interface BookDetailDialog {
    /**
     * 移出书架的确认。
     *
     * 设计规则「状态优先于成功态」要求危险操作说清对象名、影响范围与体积，
     * 所以 [impact] 是必填的整句，不是一个泛化的「此操作不可撤销」。
     *
     * [localFilePath] 非空时出现「同时删除本地文件」——它是**第二意图**，
     * 需要单独勾选，默认不选（需求文档 §18.1）。
     */
    data class RemoveFromShelf(
        val bookName: String,
        val impact: String,
        val localFilePath: String? = null,
        val deleteLocalFile: Boolean = false,
    ) : BookDetailDialog
}

@Stable
data class BookDetailUiState(
    val loading: Boolean = false,
    val header: BookDetailHeaderUi? = null,
    val source: BookSourceSummaryUi? = null,
    val intro: String = "",
    val introExpanded: Boolean = false,
    val entries: ImmutableList<BookDetailEntryUi> = persistentListOf(),
    val related: ImmutableList<RelatedBookUi> = persistentListOf(),
    /** 非空即为更多菜单已展开。菜单项随书籍类型变化，故由状态给出而非写死。 */
    val menu: ImmutableList<BookDetailMenuItemUi>? = null,
    val activeDialog: BookDetailDialog? = null,
)

sealed interface BookDetailIntent {
    data object Back : BookDetailIntent
    data object Share : BookDetailIntent
    data object OpenMenu : BookDetailIntent
    data object DismissMenu : BookDetailIntent
    data class SelectMenuAction(val action: BookDetailMenuAction) : BookDetailIntent
    data object ContinueReading : BookDetailIntent
    data object ListenAloud : BookDetailIntent
    data object Download : BookDetailIntent
    data object ChangeSource : BookDetailIntent
    data object ToggleIntro : BookDetailIntent
    data class OpenEntry(val id: BookDetailEntryId) : BookDetailIntent
    data class OpenRelated(val bookId: String) : BookDetailIntent
    /** 确认框里的第二意图勾选。 */
    data class SetDeleteLocalFile(val checked: Boolean) : BookDetailIntent
    data object ConfirmDialog : BookDetailIntent
    data object DismissDialog : BookDetailIntent
}

sealed interface BookDetailEffect {
    data object NavigateBack : BookDetailEffect
    data class ShowMessage(val text: String) : BookDetailEffect
    data class Share(val bookId: String, val bookName: String) : BookDetailEffect

    /** 继续阅读。章号由阅读器自己从书上读，这里不传。 */
    data class OpenReader(val bookId: String) : BookDetailEffect

    /** 听书归 readaloud，详情页只把书交出去。 */
    data class OpenReadAloud(val bookId: String) : BookDetailEffect

    /** 换源走画板 S-08 的统一组件。 */
    data class OpenChangeSource(val bookId: String) : BookDetailEffect
    data class OpenTocManage(val bookId: String) : BookDetailEffect
    data class OpenInsights(val bookId: String) : BookDetailEffect
    data class OpenBookDetail(val bookId: String) : BookDetailEffect
    data class OpenGroupPicker(val bookId: String) : BookDetailEffect
    data class OpenCoverPicker(val bookId: String) : BookDetailEffect
    data class OpenInfoEditor(val bookId: String) : BookDetailEffect
    data class OpenRemarkEditor(val bookId: String, val remark: String?) : BookDetailEffect
    data class OpenVariableEditor(val bookId: String) : BookDetailEffect

    /** 书已不在书架，页面该退出了。 */
    data object CloseAfterRemoval : BookDetailEffect
}

/** 画板 S-04 v2 的原始数据，用于预览与对稿。 */
val BookDetailPreviewState = BookDetailUiState(
    header = BookDetailHeaderUi(
        bookId = "xuelochangan",
        name = "雪落长安",
        byline = "柳仲卿 · 历史 · 连载中",
        chapterSummary = "386 章 · 最新 第 386 章 归途",
        shelfLabel = "在书架 · 历史组",
        progress = 0.62f,
        progressLabel = "62%",
    ),
    source = BookSourceSummaryUi(name = "墨韵书屋", alternativesLabel = "12 个候选源可换"),
    intro = "元和三年，长安大雪。卖炭翁的独轮车碾过朱雀大街的车辙，与二十年前那场雪重合。" +
        "一桩旧案、两代人的沉默，在这个冬天被重新翻开……",
    entries = persistentListOf(
        BookDetailEntryUi(
            BookDetailEntryId.Catalog,
            "目录与章节管理",
            summary = "386 章 · 已缓存 214 · 批量下载在此",
        ),
        BookDetailEntryUi(BookDetailEntryId.Insights, "人物 · 知识 · 事件", valueLabel = "28 / 14 / 9"),
    ),
    related = persistentListOf(
        RelatedBookUi("1", "长安十二时辰"),
        RelatedBookUi("2", "长安古意"),
        RelatedBookUi("3", "夜航船"),
    ),
)

/** 画板 S-04a v2 的更多菜单。 */
val BookDetailPreviewMenu = persistentListOf(
    BookDetailMenuItemUi(BookDetailMenuAction.MoveToGroup, "移动到书组…"),
    BookDetailMenuItemUi(BookDetailMenuAction.ChangeCover, "换封面"),
    BookDetailMenuItemUi(BookDetailMenuAction.EditInfo, "编辑书籍信息"),
    BookDetailMenuItemUi(BookDetailMenuAction.Note, "备注"),
    BookDetailMenuItemUi(BookDetailMenuAction.EditVariables, "变量编辑"),
    BookDetailMenuItemUi(BookDetailMenuAction.RemoveFromShelf, "移出书架", dangerous = true),
    BookDetailMenuItemUi(
        BookDetailMenuAction.DeleteLocalFile,
        "删除本地文件",
        summary = "仅本地书可见",
        dangerous = true,
    ),
)

/** 画板 S-04a v2 的移出书架确认。 */
val BookDetailPreviewRemoveDialog = BookDetailDialog.RemoveFromShelf(
    bookName = "雪落长安",
    impact = "阅读进度（62%）、书签 14 条与笔记会一并删除。已缓存的 214 章正文（18.2 MB）也会清除。",
    localFilePath = "/Books/雪落长安.epub",
)
