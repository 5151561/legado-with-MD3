package io.legado.app.ui.main

import io.legado.app.core.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MainNavigatorUnknownRouteTest {

    private data object UnregisteredFeatureRoute : AppRoute

    @Test
    fun `unknown feature route is rejected instead of silently ignored`() {
        val backStack = mutableListOf<androidx.navigation3.runtime.NavKey>(MainRouteHome)

        val error = assertThrows(IllegalStateException::class.java) {
            MainNavigator.navigateToRoute(backStack, UnregisteredFeatureRoute)
        }

        assertEquals(listOf(MainRouteHome), backStack)
        assert(error.message.orEmpty().contains("未注册的根导航 route"))
    }
}
