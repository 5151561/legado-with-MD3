package io.legado.app.ui.main

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigatorBookshelfTest {

    @Test
    fun `bookshelf child route is pushed on top of home`() {
        val route = MainRouteCache(groupId = 7L)
        val backStack = mutableListOf<NavKey>(MainRouteHome)

        MainNavigator.navigateToRoute(backStack, route)

        assertEquals(listOf(MainRouteHome, route), backStack)
    }

    @Test
    fun `bookshelf child route resets unrelated stack to home`() {
        val route = MainRouteImportLocal
        val backStack = mutableListOf<NavKey>(MainRouteHome, MainRouteSettings)

        MainNavigator.navigateToRoute(backStack, route)

        assertEquals(listOf(MainRouteHome, route), backStack)
    }

    @Test
    fun `same bookshelf child route is not pushed twice`() {
        val route = MainRouteImportRemote
        val backStack = mutableListOf<NavKey>(MainRouteHome, route)

        MainNavigator.navigateToRoute(backStack, route)

        assertEquals(listOf(MainRouteHome, route), backStack)
    }

    @Test
    fun `bookshelf management route preserves group across serialization`() {
        val route = MainRouteCache(groupId = 42L)

        val encoded = Json.encodeToString(MainRouteCache.serializer(), route)
        val restored = Json.decodeFromString(MainRouteCache.serializer(), encoded)

        assertEquals(route, restored)
    }
}
