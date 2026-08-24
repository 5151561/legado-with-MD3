package io.legado.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowWidthSizeClass
import io.legado.app.core.designsystem.kit.AppNavigationBar
import io.legado.app.core.designsystem.kit.AppNavigationItem
import io.legado.app.core.designsystem.kit.AppNavigationRail
import io.legado.app.core.designsystem.theme.AppTheme
import io.legado.app.core.navigation.AppNavigator
import io.legado.app.core.navigation.TopLevelRoute
import io.legado.app.core.navigation.rememberAppNavigationState
import io.legado.app.feature.catalog.ui.SourceHubRoute
import io.legado.app.feature.catalog.ui.catalogEntries
import io.legado.app.feature.settings.ui.ProfileRoute
import io.legado.app.feature.settings.ui.settingsEntries
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

/**
 * 应用外壳。
 *
 * 相对旧外壳的四处结构变化：
 *
 * 1. **每个一级 tab 一条回退栈**（`:core:navigation` 的 `AppNavigationState`）。
 *    旧外壳把五个 tab 装进 `HorizontalPager`，再用一个手写的 per-page `LifecycleOwner`
 *    模拟「非当前页不该 RESUMED」；现在状态与生命周期都归导航层。
 * 2. **没有集中的路由表**。每个 feature 在自己的 `ui` 模块里注册 entry
 *    （`catalogEntries` / `settingsEntries`），外壳只把它们装进同一个 `entryProvider`。
 * 3. **只有一套主题**。外壳内不出现 `LegadoTheme`，一律走 `:core:designsystem`。
 * 4. **一级导航随宽度换形态**：紧凑宽度用底栏，中等与展开宽度用侧栏
 *    （画板 X-01 平板三栏 / X-02 折叠屏双栏）。
 *
 * 一级导航栏（底栏与侧栏）由外壳自己摆，都在 `NavDisplay` 之外：**页面不接收导航栏槽位**。
 * 底栏显不显示由导航状态回答（`isAtRoot`，即当前 tab 停在自己的根上），不由「装配 entry
 * 时有没有传槽位」回答——后者会让外壳的布局出现在每个 feature 的公开签名里。
 *
 * 尚未重做的一级页面走 [ShellPlaceholderScreen]，清单集中在 [ShellPlaceholderRoutes]。
 */
@Composable
fun ShellScreen(modifier: Modifier = Modifier) {
    val state = rememberAppNavigationState(
        startRoute = HomeTabRoute,
        topLevelRoutes = ShellTopLevelRoutes,
    )
    val navigator = remember(state) { AppNavigator(state) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 装配只做一次：entryProvider 每次重组重建会让 rememberDecoratedNavEntries 跟着重建条目。
    val entryProvider = remember(navigator, scope, snackbarHostState) {
        val onNotRebuilt: (String) -> Unit = { name ->
            scope.launch { snackbarHostState.showSnackbar("$name 还没重做") }
        }
        val onMessage: (String) -> Unit = { text ->
            scope.launch { snackbarHostState.showSnackbar(text) }
        }
        entryProvider {
            settingsEntries(
                onNotRebuilt = onNotRebuilt,
                onOpenSourceHub = { navigator.goTo(SourceHubRoute) },
            )
            catalogEntries(
                onBack = navigator::goBack,
                onNavigate = navigator::goTo,
                onMessage = onMessage,
                onNotRebuilt = onNotRebuilt,
            )
            ShellPlaceholderRoutes.forEach { (route, info) ->
                entry(route) { ShellPlaceholderScreen(info = info) }
            }
        }
    }

    val compact = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass ==
        WindowWidthSizeClass.COMPACT
    // 侧栏形态下一级导航不占底部；二级目的地上底栏收起。
    val showBottomBar = compact && state.isAtRoot

    Row(modifier = modifier.fillMaxSize()) {
        if (!compact) {
            ShellNavigationRail(
                selectedId = state.selected.id,
                onSelect = { id -> navigator.select(routeOf(id)) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
            )
        }
        Column(Modifier.weight(1f)) {
            Box(Modifier.weight(1f)) {
                NavDisplay(
                    entries = state.toDecoratedEntries(entryProvider),
                    onBack = { navigator.goBack() },
                    modifier = Modifier.fillMaxSize(),
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    // 底栏在场时由底栏给手势条让位，snackbar 再让一次会浮空。
                    modifier = Modifier.align(Alignment.BottomCenter).let {
                        if (showBottomBar) it else it.windowInsetsPadding(WindowInsets.navigationBars)
                    },
                )
            }
            if (showBottomBar) {
                ShellNavigationBar(
                    selectedId = state.selected.id,
                    onSelect = { id -> navigator.select(routeOf(id)) },
                )
            }
        }
    }
}

private fun routeOf(id: String): TopLevelRoute =
    ShellTopLevelRoutes.first { it.id == id }

/** 导航栏的项。角标暂无来源——rss 未读数随订阅重做时接上。 */
private val ShellNavigationItems = ShellTopLevelRoutes
    .map { route -> AppNavigationItem(id = route.id, label = route.label()) }
    .toImmutableList()

private fun TopLevelRoute.label(): String = when (this) {
    HomeTabRoute -> "首页"
    BookshelfTabRoute -> "书架"
    ExploreTabRoute -> "发现"
    RssTabRoute -> "订阅"
    ProfileRoute -> "我的"
    else -> id
}

private fun TopLevelRoute.icon(): ImageVector = when (this) {
    HomeTabRoute -> Icons.Filled.Home
    BookshelfTabRoute -> Icons.AutoMirrored.Outlined.MenuBook
    ExploreTabRoute -> Icons.Outlined.Explore
    RssTabRoute -> Icons.Outlined.RssFeed
    else -> Icons.Outlined.Person
}

@Composable
private fun ShellNavigationBar(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppNavigationBar(
        items = ShellNavigationItems,
        selectedId = selectedId,
        onSelect = onSelect,
        // 手势条让位归宿主施加，见 AppNavigationBar 的说明。
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { item, selected -> ShellNavigationIcon(item, selected) }
}

@Composable
private fun ShellNavigationRail(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppNavigationRail(
        items = ShellNavigationItems,
        selectedId = selectedId,
        onSelect = onSelect,
        modifier = modifier,
    ) { item, selected -> ShellNavigationIcon(item, selected) }
}

@Composable
private fun ShellNavigationIcon(item: AppNavigationItem, selected: Boolean) {
    val c = AppTheme.colorScheme
    Icon(
        imageVector = routeOf(item.id).icon(),
        contentDescription = null,
        tint = if (selected) c.onSecondaryContainer else c.onSurfaceVariant,
        modifier = Modifier.size(AppTheme.dimens.iconLarge),
    )
}
