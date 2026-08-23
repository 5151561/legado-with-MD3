package io.legado.app.feature.catalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.ChapterCacheState
import io.legado.app.feature.catalog.api.ChapterSelection
import io.legado.app.feature.catalog.api.TocChapterSnapshot
import io.legado.app.feature.catalog.api.TocCommands
import io.legado.app.feature.catalog.api.TocQuery
import io.legado.app.feature.catalog.api.TocQueryState
import io.legado.app.feature.catalog.api.TocRequest
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 阅读器内目录（画板 S-06a）。
 *
 * 只做跳转与单章操作——没有多选、没有全选、没有批量命令，那些属于目录管理页。
 */
class ReaderTocViewModel(
    private val bookId: String,
    private val query: TocQuery,
    private val commands: TocCommands,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderTocUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ReaderTocEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    private var reversed = false

    init {
        viewModelScope.launch {
            query.observeToc(TocRequest(bookId)).collect(::apply)
        }
    }

    fun onIntent(intent: ReaderTocIntent) {
        when (intent) {
            is ReaderTocIntent.JumpTo -> _effects.tryEmit(ReaderTocEffect.JumpToChapter(intent.chapterId))
            ReaderTocIntent.BackToCurrent -> backToCurrent()
            ReaderTocIntent.Search -> _effects.tryEmit(ReaderTocEffect.OpenSearch)
            ReaderTocIntent.ToggleOrder -> command { commands.setReversed(bookId, !reversed) }
            is ReaderTocIntent.OpenChapterMenu -> _uiState.update { it.copy(chapterMenuFor = intent.chapterId) }
            ReaderTocIntent.DismissChapterMenu -> _uiState.update { it.copy(chapterMenuFor = null) }
            is ReaderTocIntent.SelectChapterAction -> chapterAction(intent.chapterId, intent.action)
            ReaderTocIntent.OpenTocManage -> _effects.tryEmit(ReaderTocEffect.OpenTocManage(bookId))
        }
    }

    private fun apply(state: TocQueryState) {
        val snapshot = (state as? TocQueryState.Data)?.snapshot ?: return
        reversed = snapshot.reversed
        _uiState.update {
            it.copy(
                summary = CatalogFormat.join(
                    "${snapshot.totalChapterCount} 章",
                    "已缓存 ${snapshot.cachedChapterCount}",
                ),
                chapters = snapshot.chapters
                    .map { chapter -> chapter.toReaderUi(snapshot.currentChapterProgress) }
                    .toImmutableList(),
            )
        }
    }

    /** 回到当前章由面板自己滚动；没有当前章（还没开始读）时什么都不做。 */
    private fun backToCurrent() {
        val current = _uiState.value.chapters.firstOrNull { it.isCurrent } ?: return
        _effects.tryEmit(ReaderTocEffect.JumpToChapter(current.id))
    }

    private fun chapterAction(chapterId: String, action: TocChapterAction) {
        _uiState.update { it.copy(chapterMenuFor = null) }
        when (action) {
            TocChapterAction.RefreshChapter -> command("已重新下载本章") {
                commands.retryChapter(bookId, chapterId)
            }

            TocChapterAction.DownloadChapter -> command("已加入下载队列") {
                commands.enqueueDownload(bookId, ChapterSelection.Ids(setOf(chapterId)))
            }

            TocChapterAction.ChangeChapterSource ->
                _effects.tryEmit(ReaderTocEffect.OpenChangeSource(bookId, chapterId))
        }
    }

    private fun command(successMessage: String? = null, block: suspend () -> CatalogCommandResult) {
        viewModelScope.launch {
            when (val result = block()) {
                CatalogCommandResult.Success ->
                    successMessage?.let { _effects.emit(ReaderTocEffect.ShowMessage(it)) }

                is CatalogCommandResult.Failure ->
                    _effects.emit(ReaderTocEffect.ShowMessage(result.message ?: "操作失败"))
            }
        }
    }
}

private fun TocChapterSnapshot.toReaderUi(currentProgress: Float?) = TocChapterUi(
    id = chapterId,
    title = title,
    status = cacheState.toChapterStatus(),
    note = readerNote(),
    isCurrent = isCurrent,
    isRead = isRead,
    progress = currentProgress.takeIf { isCurrent },
    progressLabel = currentProgress?.takeIf { isCurrent }?.let(CatalogFormat::percent),
)

/**
 * 状态说明只在需要给出路时才写。
 *
 * 已缓存不需要解释；未缓存要说明点开会联网，失败要说明还能换源——
 * 只画一个图标不算给出路（设计规则「状态优先于成功态」）。
 */
private fun TocChapterSnapshot.readerNote(): String? = when (cacheState) {
    ChapterCacheState.Cached -> null
    ChapterCacheState.Failed -> "上次加载失败 · 可换源"
    ChapterCacheState.Downloading -> "下载中"
    ChapterCacheState.Waiting -> "等待下载"
    ChapterCacheState.Paused -> "下载已暂停"
    ChapterCacheState.NotCached -> "未缓存 · 点开即联网加载"
}
