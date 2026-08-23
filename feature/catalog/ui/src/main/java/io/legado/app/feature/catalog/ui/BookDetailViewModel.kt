package io.legado.app.feature.catalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.feature.catalog.api.BookDetailCommands
import io.legado.app.feature.catalog.api.BookDetailQuery
import io.legado.app.feature.catalog.api.BookDetailQueryState
import io.legado.app.feature.catalog.api.BookDetailRequest
import io.legado.app.feature.catalog.api.BookDetailSnapshot
import io.legado.app.feature.catalog.api.BookRemovalImpact
import io.legado.app.feature.catalog.api.CatalogCommandResult
import io.legado.app.feature.catalog.api.ChapterSelection
import io.legado.app.feature.catalog.api.TocCommands
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookId: String,
    private val query: BookDetailQuery,
    private val commands: BookDetailCommands,
    private val tocCommands: TocCommands,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState(loading = true))
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BookDetailEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    private var snapshot: BookDetailSnapshot? = null

    init {
        viewModelScope.launch {
            query.observeBookDetail(BookDetailRequest(bookId)).collect(::apply)
        }
    }

    fun onIntent(intent: BookDetailIntent) {
        when (intent) {
            BookDetailIntent.Back -> _effects.tryEmit(BookDetailEffect.NavigateBack)
            BookDetailIntent.Share -> snapshot?.let {
                _effects.tryEmit(BookDetailEffect.Share(it.bookId, it.name))
            }

            BookDetailIntent.ContinueReading -> _effects.tryEmit(BookDetailEffect.OpenReader(bookId))
            BookDetailIntent.ListenAloud -> _effects.tryEmit(BookDetailEffect.OpenReadAloud(bookId))
            BookDetailIntent.ChangeSource -> _effects.tryEmit(BookDetailEffect.OpenChangeSource(bookId))
            BookDetailIntent.Download -> download()
            BookDetailIntent.ToggleIntro -> _uiState.update { it.copy(introExpanded = !it.introExpanded) }
            BookDetailIntent.OpenMenu -> _uiState.update { it.copy(menu = menu()) }
            BookDetailIntent.DismissMenu -> _uiState.update { it.copy(menu = null) }
            is BookDetailIntent.SelectMenuAction -> selectMenuAction(intent.action)
            is BookDetailIntent.OpenEntry -> _effects.tryEmit(
                when (intent.id) {
                    BookDetailEntryId.Catalog -> BookDetailEffect.OpenTocManage(bookId)
                    BookDetailEntryId.Insights -> BookDetailEffect.OpenInsights(bookId)
                }
            )

            is BookDetailIntent.OpenRelated -> _effects.tryEmit(BookDetailEffect.OpenBookDetail(intent.bookId))
            is BookDetailIntent.SetDeleteLocalFile -> setDeleteLocalFile(intent.checked)
            BookDetailIntent.ConfirmDialog -> confirmDialog()
            BookDetailIntent.DismissDialog -> _uiState.update { it.copy(activeDialog = null) }
        }
    }

    private fun apply(state: BookDetailQueryState) = when (state) {
        BookDetailQueryState.Loading -> _uiState.update { it.copy(loading = true) }
        is BookDetailQueryState.Failed -> _uiState.update { it.copy(loading = false) }
        is BookDetailQueryState.Data -> {
            snapshot = state.snapshot
            _uiState.update { it.copy(loading = false).withSnapshot(state.snapshot) }
        }
    }

    private fun BookDetailUiState.withSnapshot(data: BookDetailSnapshot) = copy(
        header = BookDetailHeaderUi(
            bookId = data.bookId,
            name = data.name,
            byline = CatalogFormat.join(data.author, *data.kinds.toTypedArray()),
            chapterSummary = CatalogFormat.join(
                "${data.totalChapterCount} 章",
                data.latestChapterTitle?.let { "最新 $it" },
            ),
            // 不在书架就没有「在书架」这一行；分组为空时也不编一个「未分组」出来。
            shelfLabel = if (data.inBookshelf) {
                CatalogFormat.join("在书架", data.groupNames.firstOrNull()?.let { "$it 组" })
            } else {
                null
            },
            progress = data.progress,
            progressLabel = data.progress?.let(CatalogFormat::percent),
        ),
        source = data.sourceName?.let {
            BookSourceSummaryUi(
                name = it,
                alternativesLabel = if (data.alternativeSourceCount > 0) {
                    "${data.alternativeSourceCount} 个候选源可换"
                } else {
                    "暂无候选源"
                },
            )
        },
        intro = data.intro.orEmpty(),
        entries = persistentListOf(
            BookDetailEntryUi(
                id = BookDetailEntryId.Catalog,
                title = "目录与章节管理",
                summary = CatalogFormat.join(
                    "${data.totalChapterCount} 章",
                    "已缓存 ${data.cachedChapterCount}",
                    "批量下载在此",
                ),
            ),
            BookDetailEntryUi(
                id = BookDetailEntryId.Insights,
                title = "人物 · 知识 · 事件",
                valueLabel = with(data.insights) { "$characters / $knowledge / $events" },
            ),
        ),
        related = data.related.map { RelatedBookUi(it.bookId, it.title) }.toImmutableList(),
    )

    /** 菜单项随书籍类型变化：删除本地文件只对本地书有意义。 */
    private fun menu() = buildList {
        add(BookDetailMenuItemUi(BookDetailMenuAction.MoveToGroup, "移动到书组…"))
        add(BookDetailMenuItemUi(BookDetailMenuAction.ChangeCover, "换封面"))
        add(BookDetailMenuItemUi(BookDetailMenuAction.EditInfo, "编辑书籍信息"))
        add(BookDetailMenuItemUi(BookDetailMenuAction.Note, "备注"))
        if (snapshot?.isLocal == false) {
            add(BookDetailMenuItemUi(BookDetailMenuAction.EditVariables, "变量编辑"))
        }
        add(BookDetailMenuItemUi(BookDetailMenuAction.RemoveFromShelf, "移出书架", dangerous = true))
        if (snapshot?.isLocal == true) {
            add(
                BookDetailMenuItemUi(
                    BookDetailMenuAction.DeleteLocalFile,
                    "删除本地文件",
                    summary = "仅本地书可见",
                    dangerous = true,
                )
            )
        }
    }.toImmutableList()

    private fun selectMenuAction(action: BookDetailMenuAction) {
        _uiState.update { it.copy(menu = null) }
        when (action) {
            BookDetailMenuAction.MoveToGroup -> _effects.tryEmit(BookDetailEffect.OpenGroupPicker(bookId))
            BookDetailMenuAction.ChangeCover -> _effects.tryEmit(BookDetailEffect.OpenCoverPicker(bookId))
            BookDetailMenuAction.EditInfo -> _effects.tryEmit(BookDetailEffect.OpenInfoEditor(bookId))
            BookDetailMenuAction.Note -> _effects.tryEmit(BookDetailEffect.OpenRemarkEditor(bookId, null))
            BookDetailMenuAction.EditVariables -> _effects.tryEmit(BookDetailEffect.OpenVariableEditor(bookId))
            BookDetailMenuAction.RemoveFromShelf,
            BookDetailMenuAction.DeleteLocalFile,
                -> openRemovalDialog()
        }
    }

    /**
     * 移出书架与删除本地文件走同一个确认框。
     *
     * 它们是两个对象、两次勾选，但影响面要在同一句话里说清——分成两个框会让用户
     * 在第一个框里就以为文件已经删了。
     */
    private fun openRemovalDialog() {
        viewModelScope.launch {
            val impact = query.removalImpact(bookId) ?: return@launch
            _uiState.update {
                it.copy(
                    activeDialog = BookDetailDialog.RemoveFromShelf(
                        bookName = impact.bookName,
                        impact = impactSentence(impact),
                        localFilePath = impact.localFilePath,
                    )
                )
            }
        }
    }

    /**
     * 影响面写成整句，取不到的数就不写那一句。
     *
     * 「已缓存 214 章（18.2 MB）」在统计不到体积时退成「已缓存 214 章」，
     * 而不是「已缓存 214 章（0 MB）」。
     */
    private fun impactSentence(impact: BookRemovalImpact): String {
        val lost = CatalogFormat.enumerate(
            impact.progress?.let { "阅读进度（${CatalogFormat.percent(it)}）" },
            impact.bookmarkCount.takeIf { it > 0 }?.let { "书签 $it 条" },
            impact.noteCount.takeIf { it > 0 }?.let { "笔记 $it 条" },
        )
        val first = if (lost.isEmpty()) "" else "${lost}会一并删除。"
        val cache = impact.cachedChapterCount.takeIf { it > 0 }?.let { chapters ->
            val size = impact.cachedBytes?.let { "（${CatalogFormat.bytes(it)}）" }.orEmpty()
            "已缓存的 $chapters 章正文${size}也会清除。"
        }.orEmpty()
        return (first + cache).ifEmpty { "该书没有可丢失的阅读数据。" }
    }

    private fun setDeleteLocalFile(checked: Boolean) {
        _uiState.update { state ->
            val dialog = state.activeDialog as? BookDetailDialog.RemoveFromShelf ?: return@update state
            state.copy(activeDialog = dialog.copy(deleteLocalFile = checked))
        }
    }

    private fun confirmDialog() {
        val dialog = _uiState.value.activeDialog as? BookDetailDialog.RemoveFromShelf ?: return
        _uiState.update { it.copy(activeDialog = null) }
        viewModelScope.launch {
            val result = commands.removeFromBookshelf(bookId, dialog.deleteLocalFile)
            when (result) {
                CatalogCommandResult.Success -> _effects.emit(BookDetailEffect.CloseAfterRemoval)
                is CatalogCommandResult.Failure ->
                    _effects.emit(BookDetailEffect.ShowMessage(result.message ?: "移出书架失败"))
            }
        }
    }

    /**
     * 详情页的「下载」是一键下载全部未缓存章节。
     *
     * 它没有目录在手，因此发的是 [ChapterSelection.AllMissing] 而不是一个章节集合；
     * 挑着下载是目录管理页（画板 S-06b）的事。
     */
    private fun download() {
        viewModelScope.launch {
            val cached = snapshot?.cachedChapterCount ?: 0
            val total = snapshot?.totalChapterCount ?: 0
            if (total > 0 && cached >= total) {
                _effects.emit(BookDetailEffect.ShowMessage("已全部缓存"))
                return@launch
            }
            val result = tocCommands.enqueueDownload(bookId, ChapterSelection.AllMissing)
            val message = when (result) {
                CatalogCommandResult.Success -> "已加入下载队列"
                is CatalogCommandResult.Failure -> result.message ?: "加入下载队列失败"
            }
            _effects.emit(BookDetailEffect.ShowMessage(message))
        }
    }
}
