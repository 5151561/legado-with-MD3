package io.legado.app.feature.catalog.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 源与规则枢纽（重设计画板 D-00）的契约。新增页，不是既有页面的改版。
 *
 * 解 N1 / L7：原先散在「我的」和设置里的九类源与规则收敛到这一页，全部走路由、
 * 全部套高级列表模板 TPL-01。每一行都带数量摘要与健康信号，因此
 * [SourceHubEntryUi.summary] 是必填而不是可选——一个不写数量的入口，
 * 用户点进去之前无从判断该不该点。
 *
 * 导入是全页唯一的写操作入口：粘贴 / 文件 / 扫码 / 内置库四个来源统一进审核面板
 * （画板 P-08，模板 TPL-02），所以这里只有一条 [SourceHubIntent.OpenImport]。
 */
enum class SourceHubEntryId {
    // 内容来源
    BookSources,
    RssSources,
    HttpTts,

    // 规则
    ReplaceRules,
    TxtTocRules,
    DictRules,
    ContentHighlight,
    TagHighlight,
    RssRules,
}

@Immutable
data class SourceHubEntryUi(
    val id: SourceHubEntryId,
    val title: String,
    /** 数量摘要 + 健康信号，如「312 个 · 启用 208 · 3 个失效」。 */
    val summary: String,
)

@Immutable
data class SourceHubGroupUi(
    val label: String,
    val entries: ImmutableList<SourceHubEntryUi>,
)

@Stable
data class SourceHubUiState(
    val loading: Boolean = false,
    /** 导入卡的副标题，写明有哪几种来源以及它们的共同去处。 */
    val importSummary: String = "",
    val groups: ImmutableList<SourceHubGroupUi> = persistentListOf(),
)

sealed interface SourceHubIntent {
    data object Back : SourceHubIntent
    data object Search : SourceHubIntent
    data object OpenImport : SourceHubIntent
    data class OpenEntry(val id: SourceHubEntryId) : SourceHubIntent
}

/** 画板 D-00 的原始数据，用于预览与对稿。 */
val SourceHubPreviewState = SourceHubUiState(
    importSummary = "粘贴 · 文件 · 扫码 · 内置库 → 统一审核",
    groups = persistentListOf(
        SourceHubGroupUi(
            label = "内容来源",
            entries = persistentListOf(
                SourceHubEntryUi(SourceHubEntryId.BookSources, "书源", "312 个 · 启用 208 · 3 个失效"),
                SourceHubEntryUi(SourceHubEntryId.RssSources, "订阅源", "8 个 · 启用 8"),
                SourceHubEntryUi(SourceHubEntryId.HttpTts, "HTTP TTS 引擎", "4 个 · 默认「云雀」"),
            ),
        ),
        SourceHubGroupUi(
            label = "规则",
            entries = persistentListOf(
                SourceHubEntryUi(SourceHubEntryId.ReplaceRules, "替换净化", "23 条 · 启用 19"),
                SourceHubEntryUi(SourceHubEntryId.TxtTocRules, "TXT 目录规则", "9 条 · 含内置 6 条"),
                SourceHubEntryUi(SourceHubEntryId.DictRules, "词典规则", "3 条"),
                // 「正文高亮 / 标签高亮」在旧 App 里名字相近、去处不明。这里命名区分，
                // 并把作用对象写进摘要——摘要不只是数量，也是这一条规则管什么的说明（解 L7）。
                SourceHubEntryUi(SourceHubEntryId.ContentHighlight, "正文高亮", "6 条 · 作用于正文文本"),
                SourceHubEntryUi(SourceHubEntryId.TagHighlight, "标签高亮", "4 条 · 作用于书架标签"),
                SourceHubEntryUi(SourceHubEntryId.RssRules, "订阅规则", "2 条 · 与「订阅源」的区别见页首说明"),
            ),
        ),
    ),
)
