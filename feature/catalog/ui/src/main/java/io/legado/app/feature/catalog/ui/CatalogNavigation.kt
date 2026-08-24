package io.legado.app.feature.catalog.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.legado.app.core.navigation.AppRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * catalog 的导航目的地与它们的装配。
 *
 * 路由声明在这里而不是 `:app`：一个 feature 有哪些目的地、每个目的地怎么建 ViewModel、
 * effect 落到哪个回调，都是这个 feature 自己的事。外壳只调用 [catalogEntries]，
 * 不认识 `BookDetailViewModel`，也不认识 `BookDetailEffect` 有多少个分支。
 */

/** 源与规则枢纽（画板 D-00）。 */
@Serializable
data object SourceHubRoute : AppRoute

/** 书籍详情（画板 S-04）。[bookId] 取 `Book.bookUrl`。 */
@Serializable
data class BookDetailRoute(val bookId: String) : AppRoute

/** 目录与章节管理（画板 S-06b）。 */
@Serializable
data class TocManageRoute(val bookId: String) : AppRoute

/**
 * @param onNavigate 进入另一个已重做的目的地。
 * @param onNotRebuilt 该去处尚未重做。新外壳只装重做过的界面，因此一部分 effect
 *   暂时没有目的地；用一个显式回调表达「知道该去哪、那一块还没建」，
 *   而不是让 `when` 分支静默落空。参数是那一块的名字，由外壳决定怎么告诉用户。
 */
fun EntryProviderScope<NavKey>.catalogEntries(
    onBack: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onMessage: (String) -> Unit,
    onNotRebuilt: (String) -> Unit,
) {
    entry<SourceHubRoute> {
        val viewModel = koinViewModel<SourceHubViewModel>()
        SourceHubScreen(
            state = viewModel.uiState.collectAsStateWithLifecycle().value,
            onIntent = viewModel::onIntent,
        )
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    SourceHubEffect.NavigateBack -> onBack()
                    SourceHubEffect.OpenSearch -> onNotRebuilt("搜索")
                    SourceHubEffect.OpenImport -> onNotRebuilt("导入源")
                    is SourceHubEffect.OpenEntry -> onNotRebuilt(effect.id.label())
                }
            }
        }
    }

    entry<BookDetailRoute> { route ->
        val viewModel = koinViewModel<BookDetailViewModel>(
            key = "BookDetail:${route.bookId}",
            parameters = { parametersOf(route.bookId) },
        )
        BookDetailScreen(
            state = viewModel.uiState.collectAsStateWithLifecycle().value,
            onIntent = viewModel::onIntent,
        )
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    BookDetailEffect.NavigateBack, BookDetailEffect.CloseAfterRemoval -> onBack()
                    is BookDetailEffect.ShowMessage -> onMessage(effect.text)
                    is BookDetailEffect.OpenTocManage -> onNavigate(TocManageRoute(effect.bookId))
                    is BookDetailEffect.OpenBookDetail -> onNavigate(BookDetailRoute(effect.bookId))

                    is BookDetailEffect.OpenReader -> onNotRebuilt("阅读器")
                    is BookDetailEffect.OpenReadAloud -> onNotRebuilt("听书")
                    is BookDetailEffect.OpenChangeSource -> onNotRebuilt("换源")
                    is BookDetailEffect.OpenInsights -> onNotRebuilt("角色与洞察")
                    is BookDetailEffect.OpenGroupPicker -> onNotRebuilt("移动到书组")
                    is BookDetailEffect.OpenCoverPicker -> onNotRebuilt("换封面")
                    is BookDetailEffect.OpenInfoEditor -> onNotRebuilt("编辑书籍信息")
                    is BookDetailEffect.OpenRemarkEditor -> onNotRebuilt("备注")
                    is BookDetailEffect.OpenVariableEditor -> onNotRebuilt("变量编辑")
                    is BookDetailEffect.Share -> onNotRebuilt("分享")
                }
            }
        }
    }

    entry<TocManageRoute> { route ->
        val viewModel = koinViewModel<TocManageViewModel>(
            key = "TocManage:${route.bookId}",
            parameters = { parametersOf(route.bookId) },
        )
        var pendingDeleteCount by rememberSaveable { mutableStateOf<Int?>(null) }
        TocManageScreen(
            state = viewModel.uiState.collectAsStateWithLifecycle().value,
            onIntent = viewModel::onIntent,
        )
        pendingDeleteCount?.let { count ->
            // 二次确认由导航层弹：契约把「要确认」表达成 effect，弹什么样的框是 UI 的事。
            AlertDialog(
                onDismissRequest = { pendingDeleteCount = null },
                title = { Text("删除已选缓存") },
                text = { Text("$count 章的正文缓存会被清除，需要时可重新下载。") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDeleteCount = null
                        viewModel.confirmDeleteSelectedCache()
                    }) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteCount = null }) { Text("取消") }
                },
            )
        }
        LaunchedEffect(viewModel) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    TocManageEffect.NavigateBack -> onBack()
                    TocManageEffect.OpenSearch -> onNotRebuilt("目录内搜索")
                    is TocManageEffect.ShowMessage -> onMessage(effect.text)
                    is TocManageEffect.ConfirmDeleteCache -> pendingDeleteCount = effect.chapterCount
                }
            }
        }
    }
}

/** 枢纽九类的名字。只在「尚未重做」的提示里用到，因此留在导航层而不是契约里。 */
private fun SourceHubEntryId.label(): String = when (this) {
    SourceHubEntryId.BookSources -> "书源管理"
    SourceHubEntryId.RssSources -> "订阅源管理"
    SourceHubEntryId.HttpTts -> "朗读引擎"
    SourceHubEntryId.ReplaceRules -> "替换净化规则"
    SourceHubEntryId.TxtTocRules -> "txt 目录规则"
    SourceHubEntryId.DictRules -> "字典规则"
    SourceHubEntryId.ContentHighlight -> "正文高亮规则"
    SourceHubEntryId.TagHighlight -> "标签高亮规则"
    SourceHubEntryId.RuleSubscription -> "规则订阅"
}
