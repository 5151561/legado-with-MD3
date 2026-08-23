package io.legado.app.feature.catalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.catalog.api.SourceCatalogCount
import io.legado.app.feature.catalog.api.SourceCatalogKind
import io.legado.app.feature.catalog.api.SourceHubQuery
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceHubViewModel(
    private val query: SourceHubQuery,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourceHubUiState(loading = true, importSummary = IMPORT_SUMMARY))
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SourceHubEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            query.observeSourceCatalog().collect { counts ->
                val byKind = counts.associateBy { it.kind }
                _uiState.update {
                    it.copy(loading = false, groups = groups(byKind))
                }
            }
        }
    }

    fun onIntent(intent: SourceHubIntent) {
        val effect = when (intent) {
            SourceHubIntent.Back -> SourceHubEffect.NavigateBack
            SourceHubIntent.Search -> SourceHubEffect.OpenSearch
            SourceHubIntent.OpenImport -> SourceHubEffect.OpenImport
            is SourceHubIntent.OpenEntry -> SourceHubEffect.OpenEntry(intent.id)
        }
        _effects.tryEmit(effect)
    }

    private fun groups(counts: Map<SourceCatalogKind, SourceCatalogCount>) = persistentListOf(
        SourceHubGroupUi(
            label = "内容来源",
            entries = listOf(
                entry(counts, SourceCatalogKind.BookSource, SourceHubEntryId.BookSources, "书源"),
                entry(counts, SourceCatalogKind.RssSource, SourceHubEntryId.RssSources, "订阅源"),
                entry(counts, SourceCatalogKind.HttpTts, SourceHubEntryId.HttpTts, "HTTP TTS 引擎"),
            ).toImmutableList(),
        ),
        SourceHubGroupUi(
            label = "规则",
            entries = listOf(
                entry(counts, SourceCatalogKind.ReplaceRule, SourceHubEntryId.ReplaceRules, "替换净化"),
                entry(counts, SourceCatalogKind.TxtTocRule, SourceHubEntryId.TxtTocRules, "TXT 目录规则"),
                entry(counts, SourceCatalogKind.DictRule, SourceHubEntryId.DictRules, "词典规则"),
                entry(counts, SourceCatalogKind.ContentHighlightRule, SourceHubEntryId.ContentHighlight, "正文高亮"),
                entry(counts, SourceCatalogKind.TagHighlightRule, SourceHubEntryId.TagHighlight, "标签高亮"),
                entry(counts, SourceCatalogKind.RuleSubscription, SourceHubEntryId.RuleSubscription, "订阅规则"),
            ).toImmutableList(),
        ),
    )

    private fun entry(
        counts: Map<SourceCatalogKind, SourceCatalogCount>,
        kind: SourceCatalogKind,
        id: SourceHubEntryId,
        title: String,
    ) = SourceHubEntryUi(id, title, summary(counts[kind], kind))

    /**
     * 摘要不只是数量，也是「这一条管什么」的说明。
     *
     * 可空的口径一律略过而不是写 0：`unhealthy` 为 null 表示这一类没有健康信号，
     * 写「0 个失效」会把「没测过」说成「都没问题」。
     */
    private fun summary(count: SourceCatalogCount?, kind: SourceCatalogKind): String {
        count ?: return ""
        val unit = if (kind.isSource) "个" else "条"
        return CatalogFormat.join(
            "${count.total} $unit",
            count.enabled?.let { "启用 $it" },
            count.unhealthy?.takeIf { it > 0 }?.let { "$it 个失效" },
            count.builtIn?.let { "含内置 $it 条" },
            count.defaultName?.let { "默认「$it」" },
            kind.scopeNote,
        )
    }

    private companion object {
        const val IMPORT_SUMMARY = "粘贴 · 文件 · 扫码 · 内置库 → 统一审核"
    }
}

private val SourceCatalogKind.isSource: Boolean
    get() = this == SourceCatalogKind.BookSource ||
        this == SourceCatalogKind.RssSource ||
        this == SourceCatalogKind.HttpTts

/**
 * 作用对象的说明。
 *
 * 「正文高亮 / 标签高亮」在旧 App 里名字相近、去处不明，「订阅规则 / 订阅源」同理；
 * 把作用对象写进摘要是画板 D-00 解这个歧义的手段，不是可有可无的装饰。
 */
private val SourceCatalogKind.scopeNote: String?
    get() = when (this) {
        SourceCatalogKind.ContentHighlightRule -> "作用于正文文本"
        SourceCatalogKind.TagHighlightRule -> "作用于书架标签"
        SourceCatalogKind.RuleSubscription -> "与「订阅源」的区别见页首说明"
        else -> null
    }
