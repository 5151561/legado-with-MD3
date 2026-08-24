package io.legado.app.core.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锁住换骨架时定下的四条导航行为。它们此前只写在 KDoc 里：
 * 「从首页退出」的形态、二级栈先于 tab 出栈、一级路由不入栈、重复选中发 reselect。
 */
class AppNavigatorTest {

    private data object HomeRoute : TopLevelRoute {
        override val id: String get() = "home"
    }

    private data object ProfileRoute : TopLevelRoute {
        override val id: String get() = "profile"
    }

    private data class DetailRoute(val value: String) : AppRoute

    private fun navigationState(vararg routes: TopLevelRoute): AppNavigationState {
        val start = routes.first()
        return AppNavigationState(
            startRoute = start,
            topLevelRoutes = routes.toList(),
            selectedIdState = mutableStateOf(start.id),
            backStacks = routes.associateWith { route -> NavBackStack<NavKey>(route) },
        )
    }

    @Test
    fun `在首页根上返回不消费`() {
        val state = navigationState(HomeRoute, ProfileRoute)
        val navigator = AppNavigator(state)

        navigator.goBack()

        assertEquals(HomeRoute, state.selected)
        assertTrue(state.isAtRoot)
    }

    @Test
    fun `在别的 tab 的根上返回先落回首页`() {
        val state = navigationState(HomeRoute, ProfileRoute)
        val navigator = AppNavigator(state)
        navigator.select(ProfileRoute)

        navigator.goBack()

        assertEquals(HomeRoute, state.selected)
    }

    @Test
    fun `返回先出二级栈再切回首页`() {
        val state = navigationState(HomeRoute, ProfileRoute)
        val navigator = AppNavigator(state)
        navigator.select(ProfileRoute)
        navigator.goTo(DetailRoute("a"))
        assertFalse(state.isAtRoot)

        navigator.goBack()

        assertEquals(ProfileRoute, state.selected)
        assertTrue(state.isAtRoot)

        navigator.goBack()

        assertEquals(HomeRoute, state.selected)
    }

    @Test
    fun `goTo 一级路由走切换而不是入栈`() {
        val state = navigationState(HomeRoute, ProfileRoute)
        val navigator = AppNavigator(state)

        navigator.goTo(ProfileRoute)

        assertEquals(ProfileRoute, state.selected)
        assertTrue(state.isAtRoot)
    }

    @Test
    fun `二级目的地压进当前 tab 的栈`() {
        val state = navigationState(HomeRoute, ProfileRoute)
        val navigator = AppNavigator(state)
        navigator.select(ProfileRoute)

        navigator.goTo(DetailRoute("a"))

        assertFalse(state.isAtRoot)
        // 只动当前 tab：切回首页仍在根上。
        navigator.select(HomeRoute)
        assertTrue(state.isAtRoot)
    }

    @Test
    fun `重复选中发 reselect 且不动回退栈`() = runTest {
        val state = navigationState(HomeRoute, ProfileRoute)
        val navigator = AppNavigator(state)
        navigator.select(ProfileRoute)
        navigator.goTo(DetailRoute("a"))

        val events = mutableListOf<TopLevelRoute>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            navigator.reselectEvents.collect { events += it }
        }

        navigator.select(ProfileRoute)
        runCurrent()

        assertEquals(listOf(ProfileRoute), events)
        assertFalse(state.isAtRoot)
    }
}
