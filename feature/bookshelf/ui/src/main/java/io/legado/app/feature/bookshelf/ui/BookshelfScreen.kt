package io.legado.app.feature.bookshelf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.legado.app.core.designsystem.component.AppConfirmDialog
import io.legado.app.core.designsystem.component.AppFeedback
import io.legado.app.core.designsystem.component.AppFeedbackState
import io.legado.app.core.designsystem.component.AppIcon
import io.legado.app.core.designsystem.component.AppIconButton
import io.legado.app.core.designsystem.component.AppListItem
import io.legado.app.core.designsystem.component.AppModalBottomSheet
import io.legado.app.core.designsystem.component.AppScaffold
import io.legado.app.core.designsystem.component.AppTopBar
import io.legado.app.core.designsystem.theme.LegadoTheme
import io.legado.app.feature.bookshelf.api.BookshelfError
import io.legado.app.feature.bookshelf.api.BookshelfSort
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookshelfRouteScreen(
    onOpenBook: (BookshelfOpenBookRequest) -> Unit,
    onOpenBookInfo: (BookshelfOpenBookRequest) -> Unit,
    onNavigateToLocalImport: () -> Unit,
    onNavigateToRemoteImport: () -> Unit,
    onNavigateToGlobalSearch: (String) -> Unit,
    onNavigateToManage: (Long) -> Unit,
    modifier: Modifier = Modifier,
    scrollToTopRequest: Long = 0L,
    onScrollToTopRequestHandled: (Long) -> Unit = {},
    viewModel: BookshelfViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOpenBook by rememberUpdatedState(onOpenBook)
    val currentOpenBookInfo by rememberUpdatedState(onOpenBookInfo)
    val currentLocalImport by rememberUpdatedState(onNavigateToLocalImport)
    val currentRemoteImport by rememberUpdatedState(onNavigateToRemoteImport)
    val currentGlobalSearch by rememberUpdatedState(onNavigateToGlobalSearch)
    val currentManage by rememberUpdatedState(onNavigateToManage)
    BookshelfEffectHost(
        effects = viewModel.effects,
        onOpenBook = { currentOpenBook(it) },
        onOpenBookInfo = { currentOpenBookInfo(it) },
        onNavigateToLocalImport = { currentLocalImport() },
        onNavigateToRemoteImport = { currentRemoteImport() },
        onNavigateToGlobalSearch = { currentGlobalSearch(it) },
        onNavigateToManage = { currentManage(it) },
    ) { snackbarHostState ->
        BookshelfScreen(
            state = state,
            onIntent = viewModel::onIntent,
            snackbarHostState = snackbarHostState,
            modifier = modifier,
            scrollToTopRequest = scrollToTopRequest,
            onScrollToTopRequestHandled = onScrollToTopRequestHandled,
        )
    }
}

@Composable
private fun BookshelfEffectHost(
    effects: Flow<BookshelfEffect>,
    onOpenBook: (BookshelfOpenBookRequest) -> Unit,
    onOpenBookInfo: (BookshelfOpenBookRequest) -> Unit,
    onNavigateToLocalImport: () -> Unit,
    onNavigateToRemoteImport: () -> Unit,
    onNavigateToGlobalSearch: (String) -> Unit,
    onNavigateToManage: (Long) -> Unit,
    content: @Composable (SnackbarHostState) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMessage by remember { mutableStateOf<BookshelfMessage?>(null) }
    val pendingMessageText = when (val message = pendingMessage) {
        null -> null
        BookshelfMessage.Success -> stringResource(R.string.bookshelf_command_success)
        is BookshelfMessage.Partial -> stringResource(
            R.string.bookshelf_command_partial,
            message.changed,
            message.failed,
        )
        is BookshelfMessage.Failure -> stringResource(
            R.string.bookshelf_command_failure,
        ) + message.error.diagnosticSuffix()
    }

    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is BookshelfEffect.OpenBook -> onOpenBook(effect.request)
                is BookshelfEffect.OpenBookInfo -> onOpenBookInfo(effect.request)
                BookshelfEffect.OpenLocalImport -> onNavigateToLocalImport()
                BookshelfEffect.OpenRemoteImport -> onNavigateToRemoteImport()
                is BookshelfEffect.OpenGlobalSearch -> onNavigateToGlobalSearch(effect.query)
                is BookshelfEffect.OpenManage -> onNavigateToManage(effect.groupId)
                is BookshelfEffect.ShowMessage -> pendingMessage = effect.message
            }
        }
    }
    LaunchedEffect(pendingMessageText) {
        pendingMessageText?.let { message ->
            snackbarHostState.showSnackbar(message)
            pendingMessage = null
        }
    }
    content(snackbarHostState)
}

@Composable
fun BookshelfScreen(
    state: BookshelfUiState,
    onIntent: (BookshelfIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    scrollToTopRequest: Long = 0L,
    onScrollToTopRequestHandled: (Long) -> Unit = {},
) {
    var deleteOriginal by remember(state.deleteOriginalDefault) {
        mutableStateOf(state.deleteOriginalDefault)
    }
    BackHandler(enabled = state.isSelectionMode || state.searchVisible) {
        if (state.isSelectionMode) onIntent(BookshelfIntent.ClearSelection)
        else onIntent(BookshelfIntent.ToggleSearch)
    }

    AppScaffold(
        modifier = modifier,
        topBar = {
            BookshelfTopBar(state = state, onIntent = onIntent)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.searchVisible) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onIntent(BookshelfIntent.ChangeSearchQuery(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LegadoTheme.spacing.medium,
                            vertical = LegadoTheme.spacing.small,
                        ),
                    singleLine = true,
                    label = { Text(stringResource(R.string.bookshelf_search_hint)) },
                    leadingIcon = { AppIcon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (state.searchQuery.isNotEmpty()) {
                        {
                            AppIconButton(
                                onClick = {
                                    onIntent(BookshelfIntent.ChangeSearchQuery(""))
                                }
                            ) {
                                AppIcon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.bookshelf_clear_search),
                                )
                            }
                        }
                    } else null,
                )
            }
            BookshelfGroupSelector(state = state, onIntent = onIntent)
            Box(modifier = Modifier.fillMaxSize()) {
                when (val contentState = state.contentState) {
                    BookshelfContentState.Loading -> AppFeedback(
                        state = AppFeedbackState.Loading,
                        message = stringResource(R.string.bookshelf_loading),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    BookshelfContentState.Empty -> AppFeedback(
                        state = AppFeedbackState.Empty,
                        message = stringResource(R.string.bookshelf_empty),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    is BookshelfContentState.Error -> AppFeedback(
                        state = AppFeedbackState.Error,
                        message = stringResource(R.string.bookshelf_error),
                        modifier = Modifier.align(Alignment.Center),
                        actionText = if (contentState.retryable) {
                            stringResource(R.string.bookshelf_retry)
                        } else null,
                        onAction = if (contentState.retryable) {
                            { onIntent(BookshelfIntent.RetryLoad) }
                        } else null,
                    )
                    BookshelfContentState.Content -> BookshelfBookList(
                        state = state,
                        onIntent = onIntent,
                        scrollToTopRequest = scrollToTopRequest,
                        onScrollToTopRequestHandled = onScrollToTopRequestHandled,
                    )
                }
            }
        }
    }

    AppConfirmDialog(
        show = state.pendingDeleteIds.isNotEmpty(),
        onDismissRequest = { onIntent(BookshelfIntent.DismissDelete) },
        title = stringResource(R.string.bookshelf_delete_title),
        text = stringResource(
            R.string.bookshelf_delete_message,
            state.pendingDeleteIds.size,
        ),
        confirmText = stringResource(R.string.bookshelf_confirm),
        dismissText = stringResource(R.string.bookshelf_cancel),
        onConfirm = { onIntent(BookshelfIntent.ConfirmDelete(deleteOriginal)) },
        onDismiss = { onIntent(BookshelfIntent.DismissDelete) },
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = deleteOriginal,
                    onCheckedChange = { deleteOriginal = it },
                )
                Text(stringResource(R.string.bookshelf_delete_original))
            }
        },
    )

    AppModalBottomSheet(
        show = state.showMoveSheet,
        onDismissRequest = { onIntent(BookshelfIntent.DismissMove) },
        title = stringResource(R.string.bookshelf_move_title),
    ) {
        state.groups.filter { it.isUserGroup }.forEach { group ->
            AppListItem(
                headlineContent = { Text(group.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        role = Role.Button,
                        onClick = { onIntent(BookshelfIntent.MoveSelected(group.id)) },
                    ),
            )
        }
    }
}

@Composable
private fun BookshelfTopBar(
    state: BookshelfUiState,
    onIntent: (BookshelfIntent) -> Unit,
) {
    if (state.isSelectionMode) {
        AppTopBar(
            title = stringResource(R.string.bookshelf_selected, state.selectedBookIds.size),
            navigationIcon = {
                AppIconButton(onClick = { onIntent(BookshelfIntent.ClearSelection) }) {
                    AppIcon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.bookshelf_cancel_selection),
                    )
                }
            },
            actions = {
                AppIconButton(onClick = { onIntent(BookshelfIntent.SelectAll) }) {
                    AppIcon(
                        Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.bookshelf_select_all),
                    )
                }
                AppIconButton(
                    onClick = { onIntent(BookshelfIntent.RequestMove) },
                    enabled = !state.commandInFlight,
                ) {
                    AppIcon(
                        Icons.AutoMirrored.Filled.DriveFileMove,
                        contentDescription = stringResource(R.string.bookshelf_move),
                    )
                }
                AppIconButton(
                    onClick = { onIntent(BookshelfIntent.RequestDelete) },
                    enabled = !state.commandInFlight,
                ) {
                    AppIcon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.bookshelf_delete),
                    )
                }
            },
        )
    } else {
        var menuExpanded by remember { mutableStateOf(false) }
        AppTopBar(
            title = stringResource(R.string.bookshelf_title),
            actions = {
                AppIconButton(onClick = { onIntent(BookshelfIntent.ToggleSearch) }) {
                    AppIcon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.bookshelf_search),
                    )
                }
                BookshelfSortMenu(state = state, onIntent = onIntent)
                AppIconButton(onClick = { onIntent(BookshelfIntent.SelectAll) }) {
                    AppIcon(
                        Icons.Default.SelectAll,
                        contentDescription = stringResource(R.string.bookshelf_enter_selection),
                    )
                }
                Box {
                    AppIconButton(onClick = { menuExpanded = true }) {
                        AppIcon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.bookshelf_more),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookshelf_global_search)) },
                            onClick = {
                                menuExpanded = false
                                onIntent(BookshelfIntent.NavigateToGlobalSearch)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookshelf_import_local)) },
                            onClick = {
                                menuExpanded = false
                                onIntent(BookshelfIntent.NavigateToLocalImport)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookshelf_import_remote)) },
                            onClick = {
                                menuExpanded = false
                                onIntent(BookshelfIntent.NavigateToRemoteImport)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookshelf_manage)) },
                            onClick = {
                                menuExpanded = false
                                onIntent(BookshelfIntent.NavigateToManage)
                            },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun BookshelfSortMenu(
    state: BookshelfUiState,
    onIntent: (BookshelfIntent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppIconButton(onClick = { expanded = true }) {
            AppIcon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.bookshelf_sort),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BookshelfSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label()) },
                    onClick = {
                        expanded = false
                        onIntent(BookshelfIntent.ChangeSort(sort))
                    },
                    trailingIcon = if (sort == state.sort) {
                        { AppIcon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (state.descending) R.string.bookshelf_sort_descending
                            else R.string.bookshelf_sort_ascending
                        )
                    )
                },
                onClick = {
                    expanded = false
                    onIntent(BookshelfIntent.ToggleSortDirection)
                },
                leadingIcon = {
                    AppIcon(
                        if (state.descending) Icons.Default.ArrowDownward
                        else Icons.Default.ArrowUpward,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun BookshelfGroupSelector(
    state: BookshelfUiState,
    onIntent: (BookshelfIntent) -> Unit,
) {
    if (state.groups.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = LegadoTheme.spacing.medium,
            vertical = LegadoTheme.spacing.extraSmall,
        ),
        horizontalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.small),
    ) {
        items(state.groups, key = { it.id }) { group ->
            FilterChip(
                selected = group.id == state.selectedGroupId,
                onClick = { onIntent(BookshelfIntent.SelectGroup(group.id)) },
                label = { Text(group.name) },
            )
        }
    }
}

@Composable
private fun BookshelfBookList(
    state: BookshelfUiState,
    onIntent: (BookshelfIntent) -> Unit,
    scrollToTopRequest: Long,
    onScrollToTopRequestHandled: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest == 0L) return@LaunchedEffect
        if (state.books.isNotEmpty()) listState.animateScrollToItem(0)
        onScrollToTopRequestHandled(scrollToTopRequest)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = LegadoTheme.spacing.large,
        ),
    ) {
        items(
            items = state.books,
            key = { it.id },
            contentType = { "bookshelf-book" },
        ) { book ->
            BookshelfBookRow(
                book = book,
                selected = book.id in state.selectedBookIds,
                selectionMode = state.isSelectionMode,
                onClick = { onIntent(BookshelfIntent.OpenBook(book.id)) },
                onLongClick = { onIntent(BookshelfIntent.OpenBookInfo(book.id)) },
                onToggleSelection = { onIntent(BookshelfIntent.ToggleSelection(book.id)) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookshelfBookRow(
    book: BookshelfBookUi,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clickLabel = stringResource(R.string.bookshelf_open_book, book.name)
    val longClickLabel = stringResource(R.string.bookshelf_select_book, book.name)
    AppListItem(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .combinedClickable(
                role = Role.Button,
                onClickLabel = clickLabel,
                onLongClickLabel = longClickLabel,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        leadingContent = {
            Surface(
                modifier = Modifier
                    .size(width = 56.dp, height = 76.dp)
                    .clip(RoundedCornerShape(LegadoTheme.spacing.extraSmall)),
                shape = RectangleShape,
                color = LegadoTheme.colorScheme.surfaceContainerHigh,
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        headlineContent = {
            Text(
                text = book.name,
                style = LegadoTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(LegadoTheme.spacing.extraSmall)) {
                Text(
                    text = book.author.ifBlank {
                        stringResource(R.string.bookshelf_unknown_author)
                    },
                    style = LegadoTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                book.chapterTitle?.takeIf(String::isNotBlank)?.let {
                    Text(
                        text = it,
                        style = LegadoTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { book.readingProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                    )
                    Spacer(Modifier.width(LegadoTheme.spacing.small))
                    Text(
                        text = stringResource(
                            R.string.bookshelf_progress,
                            (book.readingProgress * 100).toInt(),
                        ),
                        style = LegadoTheme.typography.labelSmall,
                    )
                }
                if (book.unreadChapterCount > 0) {
                    Text(
                        text = stringResource(R.string.bookshelf_unread, book.unreadChapterCount),
                        style = LegadoTheme.typography.labelSmall,
                        color = LegadoTheme.colorScheme.primary,
                    )
                }
            }
        },
        trailingContent = if (selectionMode) {
            {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    enabled = true,
                )
            }
        } else null,
    )
}

@Composable
private fun BookshelfSort.label(): String = stringResource(
    when (this) {
        BookshelfSort.RecentReading -> R.string.bookshelf_sort_recent
        BookshelfSort.LatestChapter -> R.string.bookshelf_sort_latest
        BookshelfSort.BookName -> R.string.bookshelf_sort_name
        BookshelfSort.Manual -> R.string.bookshelf_sort_manual
        BookshelfSort.LastActivity -> R.string.bookshelf_sort_activity
        BookshelfSort.Author -> R.string.bookshelf_sort_author
    }
)

private fun BookshelfError.diagnosticSuffix(): String = diagnostic
    ?.takeIf(String::isNotBlank)
    ?.let { "\n$it" }
    .orEmpty()
