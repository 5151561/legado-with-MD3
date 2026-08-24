package io.legado.app.debug

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.legado.app.core.designsystem.kit.AppEntryGroupHeader
import io.legado.app.core.designsystem.kit.AppEntryRow
import io.legado.app.core.designsystem.kit.AppNavigationBar
import io.legado.app.core.designsystem.kit.AppNavigationItem
import io.legado.app.core.designsystem.kit.AppSegmentedControl
import io.legado.app.core.designsystem.kit.AppTopAppBar
import io.legado.app.core.designsystem.kit.AppTopAppBarDefaults
import io.legado.app.core.designsystem.kit.SegmentedOption
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.designsystem.theme.ProvideAppTheme
import io.legado.app.core.designsystem.theme.ReadingPaperDefault
import io.legado.app.core.designsystem.theme.ReadingPaperNight
import io.legado.app.feature.catalog.ui.BookDetailIntent
import io.legado.app.feature.catalog.ui.BookDetailPreviewMenu
import io.legado.app.feature.catalog.ui.BookDetailPreviewRemoveDialog
import io.legado.app.feature.catalog.ui.BookDetailPreviewState
import io.legado.app.feature.catalog.ui.BookDetailScreen
import io.legado.app.feature.catalog.ui.ReaderTocPreviewState
import io.legado.app.feature.catalog.ui.ReaderTocSheet
import io.legado.app.feature.catalog.ui.SourceHubPreviewState
import io.legado.app.feature.catalog.ui.SourceHubIntent
import io.legado.app.feature.catalog.ui.SourceHubScreen
import io.legado.app.feature.catalog.ui.TocManageIntent
import io.legado.app.feature.catalog.ui.TocManagePreviewState
import io.legado.app.feature.catalog.ui.TocManageScreen
import io.legado.app.feature.home.ui.HomeDashboardPreviewState
import io.legado.app.feature.home.ui.HomeDashboardScreen
import io.legado.app.feature.home.ui.HomeSectionId
import io.legado.app.feature.home.ui.HomeSectionsIntent
import io.legado.app.feature.home.ui.HomeSectionsPreviewState
import io.legado.app.feature.home.ui.HomeSectionsScreen
import io.legado.app.feature.home.ui.HomeSectionsUiState
import io.legado.app.feature.settings.ui.ProfileIntent
import io.legado.app.feature.settings.ui.ProfilePreviewState
import io.legado.app.feature.settings.ui.ProfileScreen
import io.legado.app.feature.settings.ui.ProfileTrailing
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet

/**
 * 重设计画板的真机画廊。**仅 debug**，源码只存在于 `src/debug`，release 包里不存在。
 *
 * 存在的理由：重设计的页面按 Phase 10 的约束建在各 feature 的 `api` 上，而 `api` 尚未扩面，
 * 因此还不能接进生产导航。在那之前，这个入口让画板能在真机上按尺寸、字体缩放与深浅色走一遍——
 * 截图基线只能证明渲染没变，证明不了真机上的 inset、动态字体与触感。
 *
 * 它是自己的一个 launcher 图标，不改 `MainActivity`、`MainNavGraph` 或「我的」任何一行；
 * 线 C 把页面接进真路由后整块删除。
 *
 * 页面状态在这里就地持有：画廊要能点得动，不然验的只是一张静态图。
 */
class RedesignGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by remember { mutableStateOf<Boolean?>(null) }
            val resolvedDark = dark ?: isSystemInDarkTheme()
            var current by remember { mutableStateOf<GalleryEntry?>(null) }

            ProvideAppTheme(
                dark = resolvedDark,
                readingPalette = if (resolvedDark) ReadingPaperNight else ReadingPaperDefault,
            ) {
                val entry = current
                if (entry == null) {
                    GalleryIndex(
                        dark = resolvedDark,
                        onToggleDark = { dark = it },
                        onOpen = { current = it },
                    )
                } else {
                    BackHandler { current = null }
                    Box(Modifier.fillMaxSize()) { ArtboardHost(entry) { current = null } }
                }
            }
        }
    }
}

/** 画廊里的一项。顺序与画板墙一致。 */
private enum class GalleryEntry(val code: String, val title: String, val note: String) {
    Home("M-01 v2", "首页", "备份卡降级为状态提醒"),
    HomeSections("M-01a v2", "首页区块设置", "开关 + 拖动排序 + 实时预览"),
    Profile("P-01 v2", "我的", "四组八项"),
    SourceHub("D-00", "源与规则枢纽", "九类聚合"),
    BookDetail("S-04 v2", "书籍详情", "单一路由单宿主"),
    BookDetailMenu("S-04a v2", "详情 · 更多菜单", "危险项单列在最后一组"),
    BookDetailRemove("S-04a v2", "详情 · 移出书架确认", "第二意图默认不选"),
    ReaderToc("S-06a v2", "阅读器内目录", "只做快速跳转"),
    TocManage("S-06b v2", "目录管理页", "选择态套 TPL-03"),
}

@Composable
private fun GalleryIndex(
    dark: Boolean,
    onToggleDark: (Boolean) -> Unit,
    onOpen: (GalleryEntry) -> Unit,
) {
    val c = AppTheme.colorScheme
    val dimens = AppTheme.dimens
    Column(
        Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        AppTopAppBar(
            title = "重设计画板",
            subtitle = "P0 v2 · 仅 debug",
            titleStyle = AppTopAppBarDefaults.displayTitleStyle,
        )
        AppSegmentedControl(
            options = persistentListOf(
                SegmentedOption("light", "日光"),
                SegmentedOption("dark", "夜墨"),
            ),
            selectedId = if (dark) "dark" else "light",
            onSelect = { onToggleDark(it == "dark") },
            modifier = Modifier.padding(horizontal = dimens.spaceContent),
        )
        LazyColumn(
            contentPadding = PaddingValues(
                start = dimens.spaceContent,
                end = dimens.spaceContent,
                top = dimens.spaceM,
                bottom = dimens.spaceGroup +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item("header") { AppEntryGroupHeader("画板") }
            items(GalleryEntry.entries, key = { it.name }) { entry ->
                AppEntryRow(
                    title = entry.title,
                    summary = "${entry.code} · ${entry.note}",
                    onClick = { onOpen(entry) },
                )
            }
        }
    }
}

/**
 * 单块画板的宿主。状态就地持有——画廊要能点得动。
 *
 * @param onBack 返回索引。系统返回键由调用方的 `BackHandler` 接管。
 */
@Composable
private fun ArtboardHost(entry: GalleryEntry, onBack: () -> Unit) {
    when (entry) {
        GalleryEntry.Home -> HomeDashboardScreen(
            state = HomeDashboardPreviewState,
            onIntent = {},
            bottomBar = { GalleryNavigationBar(selectedId = "home") },
        )

        GalleryEntry.HomeSections -> {
            var state by remember { mutableStateOf(HomeSectionsPreviewState) }
            HomeSectionsScreen(
                state = state,
                onIntent = { intent ->
                    when (intent) {
                        is HomeSectionsIntent.Back -> onBack()
                        is HomeSectionsIntent.RestoreDefaults ->
                            state = HomeSectionsPreviewState

                        is HomeSectionsIntent.SetVisible -> state = state.toggleVisible(intent.id)
                        is HomeSectionsIntent.Move -> state = state.move(intent.id, intent.toIndex)
                    }
                },
            )
        }

        GalleryEntry.Profile -> {
            var state by remember { mutableStateOf(ProfilePreviewState) }
            // 「我的」不再有 bottomBar 槽位——导航栏归外壳，画廊在这里自己摆一条。
            Column(Modifier.fillMaxSize()) {
                ProfileScreen(
                    modifier = Modifier.weight(1f),
                    state = state,
                    onIntent = { intent ->
                        when (intent) {
                            is ProfileIntent.SelectThemeMode ->
                                state = state.copy(themeMode = intent.mode)

                            is ProfileIntent.SetToggle -> state = state.copy(
                                groups = state.groups.map { group ->
                                    group.copy(
                                        entries = group.entries.map { row ->
                                            if (row.id == intent.id) {
                                                row.copy(trailing = ProfileTrailing.Toggle(intent.checked))
                                            } else {
                                                row
                                            }
                                        }.toImmutableList(),
                                    )
                                }.toImmutableList(),
                            )

                            is ProfileIntent.OpenEntry -> Unit
                        }
                    },
                )
                GalleryNavigationBar(selectedId = "me")
            }
        }

        GalleryEntry.SourceHub -> SourceHubScreen(
            state = SourceHubPreviewState,
            onIntent = { if (it is SourceHubIntent.Back) onBack() },
        )

        GalleryEntry.BookDetail,
        GalleryEntry.BookDetailMenu,
        GalleryEntry.BookDetailRemove,
        -> {
            var state by remember {
                mutableStateOf(
                    when (entry) {
                        GalleryEntry.BookDetailMenu ->
                            BookDetailPreviewState.copy(menu = BookDetailPreviewMenu)

                        GalleryEntry.BookDetailRemove ->
                            BookDetailPreviewState.copy(activeDialog = BookDetailPreviewRemoveDialog)

                        else -> BookDetailPreviewState
                    },
                )
            }
            BookDetailScreen(
                state = state,
                onIntent = { intent ->
                    when (intent) {
                        is BookDetailIntent.Back -> onBack()
                        is BookDetailIntent.OpenMenu ->
                            state = state.copy(menu = BookDetailPreviewMenu)

                        is BookDetailIntent.DismissMenu -> state = state.copy(menu = null)
                        is BookDetailIntent.SelectMenuAction -> state = state.copy(
                            menu = null,
                            activeDialog = BookDetailPreviewRemoveDialog,
                        )

                        is BookDetailIntent.DismissDialog,
                        is BookDetailIntent.ConfirmDialog,
                        -> state = state.copy(activeDialog = null)

                        is BookDetailIntent.SetDeleteLocalFile -> state = state.copy(
                            activeDialog = BookDetailPreviewRemoveDialog.copy(
                                deleteLocalFile = intent.checked,
                            ),
                        )

                        is BookDetailIntent.ToggleIntro ->
                            state = state.copy(introExpanded = !state.introExpanded)

                        else -> Unit
                    }
                },
            )
        }

        GalleryEntry.ReaderToc -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.reading.paper),
            verticalArrangement = Arrangement.Bottom,
        ) {
            ReaderTocSheet(state = ReaderTocPreviewState, onIntent = {})
        }

        GalleryEntry.TocManage -> {
            var state by remember { mutableStateOf(TocManagePreviewState) }
            TocManageScreen(
                state = state,
                onIntent = { intent ->
                    when (intent) {
                        is TocManageIntent.Close -> onBack()
                        is TocManageIntent.SelectFilter ->
                            state = state.copy(activeFilter = intent.filter)

                        is TocManageIntent.ToggleChapter -> {
                            val next = state.selected.toMutableSet()
                            if (!next.add(intent.chapterId)) next.remove(intent.chapterId)
                            state = state.copy(selected = next.toPersistentSet())
                        }

                        is TocManageIntent.SelectAll ->
                            state = state.copy(
                                selected = state.chapters.map { it.id }.toPersistentSet(),
                            )

                        is TocManageIntent.InvertSelection ->
                            state = state.copy(
                                selected = state.chapters
                                    .map { it.id }
                                    .filterNot { it in state.selected }
                                    .toPersistentSet(),
                            )

                        else -> Unit
                    }
                },
            )
        }
    }
}

/** 画廊里的一级导航栏。真正的栏体归 App 外壳，这里只为让 M-01 / P-01 摆满整屏。 */
@Composable
private fun GalleryNavigationBar(selectedId: String) {
    val c = AppTheme.colorScheme
    AppNavigationBar(
        items = persistentListOf(
            AppNavigationItem("home", "首页"),
            AppNavigationItem("shelf", "书架"),
            AppNavigationItem("explore", "发现"),
            AppNavigationItem("rss", "订阅", badgeCount = 12),
            AppNavigationItem("me", "我的"),
        ),
        selectedId = selectedId,
        onSelect = {},
    ) { item, selected ->
        Icon(
            imageVector = when (item.id) {
                "home" -> Icons.Filled.Home
                "shelf" -> Icons.AutoMirrored.Outlined.MenuBook
                "explore" -> Icons.Outlined.Explore
                "rss" -> Icons.Outlined.RssFeed
                else -> Icons.Outlined.Person
            },
            contentDescription = null,
            tint = if (selected) c.onSecondaryContainer else c.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.dimens.iconLarge),
        )
    }
}

/** 显示 ⇄ 隐藏。隐藏项落到「已隐藏」列表尾部——它没有位置概念，位置只属于显示中的那一段。 */
private fun HomeSectionsUiState.toggleVisible(id: HomeSectionId): HomeSectionsUiState {
    visible.firstOrNull { it.id == id }?.let { section ->
        return copy(
            visible = visible.filterNot { it.id == id }.toImmutableList(),
            hidden = (hidden + section).toImmutableList(),
        )
    }
    val section = hidden.firstOrNull { it.id == id } ?: return this
    return copy(
        visible = (visible + section).toImmutableList(),
        hidden = hidden.filterNot { it.id == id }.toImmutableList(),
    )
}

/** 拖拽落位。锁定项恒在第一位，因此目标索引下限是 1。 */
private fun HomeSectionsUiState.move(id: HomeSectionId, toIndex: Int): HomeSectionsUiState {
    val from = visible.indexOfFirst { it.id == id }
    if (from < 0) return this
    val lockedCount = visible.count { it.locked }
    val target = toIndex.coerceIn(lockedCount, visible.lastIndex)
    if (target == from) return this
    val next = visible.toMutableList()
    next.add(target, next.removeAt(from))
    return copy(visible = next.toImmutableList())
}
