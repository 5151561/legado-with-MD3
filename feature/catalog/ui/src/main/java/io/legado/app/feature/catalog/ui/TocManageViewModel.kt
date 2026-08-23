package io.legado.app.feature.catalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterFailureReason
import io.legado.app.feature.catalog.api.ChapterSelection
import io.legado.app.feature.catalog.api.TocCacheFilter
import io.legado.app.feature.catalog.api.TocChapterSnapshot
import io.legado.app.feature.catalog.api.TocCommands
import io.legado.app.feature.catalog.api.TocQuery
import io.legado.app.feature.catalog.api.TocQueryState
import io.legado.app.feature.catalog.api.TocRequest
import io.legado.app.feature.catalog.api.TocSnapshot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TocManageViewModel(
    private val bookId: String,
    private val query: TocQuery,
    private val commands: TocCommands,
) : ViewModel() {

    private val request = MutableStateFlow(TocRequest(bookId))
    private val _uiState = MutableStateFlow(TocManageUiState(hint = RANGE_HINT))
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<TocManageEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    /** 全书章节序，用于把「起止范围」落成一段章节。过滤后的列表不能用来算范围。 */
    private var orderedChapterIds: List<String> = emptyList()

    init {
        viewModelScope.launch {
            request.flatMapLatest(query::observeToc).collect(::apply)
        }
    }

    fun onIntent(intent: TocManageIntent) {
        when (intent) {
            TocManageIntent.Close -> _effects.tryEmit(TocManageEffect.NavigateBack)
            TocManageIntent.Search -> _effects.tryEmit(TocManageEffect.OpenSearch)
            is TocManageIntent.SelectFilter -> selectFilter(intent.filter)
            is TocManageIntent.ToggleChapter -> toggle(intent.chapterId)
            is TocManageIntent.SelectRange -> selectRange(intent.fromChapterId, intent.toChapterId)
            is TocManageIntent.RetryChapter -> retry(intent.chapterId)
            TocManageIntent.SelectAll -> _uiState.update {
                it.copy(selected = it.chapters.map(TocManageChapterUi::id).toImmutableSet())
            }

            TocManageIntent.InvertSelection -> _uiState.update { state ->
                state.copy(
                    selected = state.chapters
                        .map(TocManageChapterUi::id)
                        .filterNot { it in state.selected }
                        .toImmutableSet(),
                )
            }

            TocManageIntent.DownloadSelected -> downloadSelected()
            TocManageIntent.OpenMoreMenu -> _uiState.update { it.copy(moreMenuVisible = true) }
            TocManageIntent.DismissMoreMenu -> _uiState.update { it.copy(moreMenuVisible = false) }
            TocManageIntent.DeleteSelectedCache -> deleteSelectedCache()
        }
    }

    private fun apply(state: TocQueryState) {
        val snapshot = (state as? TocQueryState.Data)?.snapshot ?: return
        orderedChapterIds = snapshot.chapters.map(TocChapterSnapshot::chapterId)
        _uiState.update { current ->
            val chapters = snapshot.chapters.map(TocChapterSnapshot::toManageUi).toImmutableList()
            val visible = chapters.map(TocManageChapterUi::id).toSet()
            current.copy(
                subtitle = CatalogFormat.join(snapshot.bookName, "${snapshot.totalChapterCount} 章"),
                filters = filters(snapshot),
                chapters = chapters,
                // 过滤后不可见的章节不该继续留在选中集合里——底部条会数出用户看不到的项。
                selected = current.selected.filter { it in visible }.toImmutableSet(),
            )
        }
    }

    private fun filters(snapshot: TocSnapshot) = persistentListOf(
        TocFilterUi(TocFilter.All, "全部", snapshot.totalChapterCount),
        TocFilterUi(TocFilter.NotCached, "未缓存", snapshot.notCachedChapterCount),
        TocFilterUi(TocFilter.Failed, "失败", snapshot.failedChapterCount),
    )

    private fun selectFilter(filter: TocFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
        request.update { it.copy(filter = filter.toApiFilter()) }
    }

    private fun toggle(chapterId: String) = _uiState.update { state ->
        val selected = if (chapterId in state.selected) {
            state.selected - chapterId
        } else {
            state.selected + chapterId
        }
        state.copy(selected = selected.toImmutableSet())
    }

    /** 起止范围按全书章节序取,与两端点击的先后无关。 */
    private fun selectRange(fromChapterId: String, toChapterId: String) {
        val from = orderedChapterIds.indexOf(fromChapterId)
        val to = orderedChapterIds.indexOf(toChapterId)
        if (from < 0 || to < 0) return
        val range = orderedChapterIds.subList(minOf(from, to), maxOf(from, to) + 1)
        _uiState.update { it.copy(selected = (it.selected + range).toImmutableSet()) }
    }

    private fun retry(chapterId: String) = command { commands.retryChapter(bookId, chapterId) }

    private fun downloadSelected() {
        val selected = _uiState.value.selected
        if (selected.isEmpty()) {
            _effects.tryEmit(TocManageEffect.ShowMessage("请先选择章节"))
            return
        }
        command(successMessage = "已加入下载队列") {
            commands.enqueueDownload(bookId, ChapterSelection.Ids(selected))
        }
    }

    private fun deleteSelectedCache() {
        _uiState.update { it.copy(moreMenuVisible = false) }
        val selected = _uiState.value.selected
        if (selected.isEmpty()) {
            _effects.tryEmit(TocManageEffect.ShowMessage("请先选择章节"))
            return
        }
        // 危险动作不与主操作并列，也不在这里直接执行——由上层弹二次确认后再回来。
        _effects.tryEmit(TocManageEffect.ConfirmDeleteCache(selected.size))
    }

    /** 二次确认通过后由上层调用。 */
    fun confirmDeleteSelectedCache() {
        val selected = _uiState.value.selected
        if (selected.isEmpty()) return
        command(successMessage = "已删除所选章节缓存") { commands.deleteCache(bookId, selected) }
    }

    private fun command(
        successMessage: String? = null,
        block: suspend () -> CatalogCommandResult,
    ) {
        viewModelScope.launch {
            when (val result = block()) {
                CatalogCommandResult.Success ->
                    successMessage?.let { _effects.emit(TocManageEffect.ShowMessage(it)) }

                is CatalogCommandResult.Failure ->
                    _effects.emit(TocManageEffect.ShowMessage(result.message ?: "操作失败"))
            }
        }
    }

    private companion object {
        const val RANGE_HINT = "支持「起止范围」快速选择：长按首章 → 点尾章"
    }
}

private fun TocFilter.toApiFilter() = when (this) {
    TocFilter.All -> TocCacheFilter.All
    TocFilter.NotCached -> TocCacheFilter.NotCached
    TocFilter.Failed -> TocCacheFilter.Failed
}

private fun TocChapterSnapshot.toManageUi() = TocManageChapterUi(
    id = chapterId,
    title = title,
    status = cacheState.toChapterStatus(),
    note = note(),
    retryable = cacheState == ChapterCacheState.Failed,
)

/**
 * 六态折叠成画板的三档。
 *
 * 等待、下载中、暂停都还没拿到正文，因此归入「未缓存」——它们的差别写在 note 里，
 * 不占状态位。
 */
internal fun ChapterCacheState.toChapterStatus() = when (this) {
    ChapterCacheState.Cached -> TocChapterStatus.Cached
    ChapterCacheState.Failed -> TocChapterStatus.Failed
    else -> TocChapterStatus.NotCached
}

private fun TocChapterSnapshot.note(): String = when (cacheState) {
    ChapterCacheState.Cached -> CatalogFormat.join("已缓存", cachedBytes?.let(CatalogFormat::bytes))
    ChapterCacheState.NotCached -> "未缓存"
    ChapterCacheState.Waiting -> "等待下载"
    ChapterCacheState.Downloading ->
        CatalogFormat.join("下载中", downloadProgress?.let(CatalogFormat::percent))

    ChapterCacheState.Paused -> "已暂停"
    ChapterCacheState.Failed -> CatalogFormat.join("上次失败", failureReason?.label)
}

private val ChapterFailureReason.label: String?
    get() = when (this) {
        ChapterFailureReason.EmptyContent -> "正文为空"
        ChapterFailureReason.Network -> "网络错误"
        // 队列目前不携带错误类型，分不出因由时不编一个理由出来。
        ChapterFailureReason.Unknown -> null
    }
