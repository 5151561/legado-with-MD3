package io.legado.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * 一级导航目的地各自持有一条回退栈。
 *
 * 旧外壳把五个 tab 装进 `HorizontalPager`，再用一个手写的 per-page `LifecycleOwner`
 * 去模拟「非当前页不该 RESUMED」。那套做法把导航状态、页面状态与生命周期三件事
 * 缠在了一起：回退栈只有一条，tab 内部的层级只能靠 Activity 或额外的路由前缀表达。
 * 这里改成每条一级路由一条 `NavBackStack`，状态与生命周期都交给导航层。
 *
 * 采「从首页退出」的形态：起始路由的条目**恒在**条目列表里，因此无论从哪个 tab
 * 一路返回，最后都落回首页再退出应用，不会出现「返回键把用户丢在第三个 tab」。
 */
class AppNavigationState internal constructor(
    val startRoute: TopLevelRoute,
    val topLevelRoutes: List<TopLevelRoute>,
    private val selectedIdState: androidx.compose.runtime.MutableState<String>,
    internal val backStacks: Map<TopLevelRoute, NavBackStack<NavKey>>,
) {

    var selected: TopLevelRoute
        get() = topLevelRoutes.firstOrNull { it.id == selectedIdState.value } ?: startRoute
        set(value) {
            selectedIdState.value = value.id
        }

    internal val currentStack: NavBackStack<NavKey>
        get() = backStacks[selected] ?: error("没有为 ${selected.id} 建回退栈")

    /**
     * 把导航状态转成带 `SaveableStateHolder` 的 [NavEntry]。
     *
     * 每条一级路由有各自的 `SaveableStateHolder`——共用一个会让不同 tab 里
     * 同名的 `rememberSaveable` 互相覆盖。
     */
    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decorated = backStacks.mapValues { (_, stack) ->
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                entryProvider = entryProvider,
            )
        }
        return routesInUse().flatMap { decorated[it].orEmpty() }
    }

    /**
     * 当前参与渲染的一级路由。起始路由恒在第一位（「从首页退出」）。
     *
     * 不在列表里的一级路由，其回退栈与页面状态仍然保留——只是这一帧不渲染。
     */
    private fun routesInUse(): List<TopLevelRoute> =
        if (selected == startRoute) listOf(startRoute) else listOf(startRoute, selected)
}

/**
 * @param startRoute 起始的一级路由，必须也在 [topLevelRoutes] 里。用户从这里退出应用。
 */
@Composable
fun rememberAppNavigationState(
    startRoute: TopLevelRoute,
    topLevelRoutes: List<TopLevelRoute>,
): AppNavigationState {
    require(startRoute in topLevelRoutes) { "startRoute 必须在 topLevelRoutes 里" }

    // 存 id 而不是路由对象：一级路由未来可能带参数，用对象做键会在参数变化时丢掉选中态。
    val selectedId = rememberSaveable { androidx.compose.runtime.mutableStateOf(startRoute.id) }
    val backStacks = topLevelRoutes.associateWith { route -> rememberNavBackStack(route) }

    return remember(startRoute, topLevelRoutes, selectedId, backStacks) {
        AppNavigationState(
            startRoute = startRoute,
            topLevelRoutes = topLevelRoutes,
            selectedIdState = selectedId,
            backStacks = backStacks,
        )
    }
}
