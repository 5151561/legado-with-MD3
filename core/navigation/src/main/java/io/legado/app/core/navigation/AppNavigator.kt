package io.legado.app.core.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 导航事件的唯一入口。页面不持有它——页面收到的永远是 `onBack` / `onNavigateToXxx`
 * 这样的回调，由外壳在装配 entry 时绑上。
 */
class AppNavigator(private val state: AppNavigationState) {

    private val _reselectEvents = MutableSharedFlow<TopLevelRoute>(extraBufferCapacity = 1)

    /** 用户再次点击已选中的一级 tab。页面据此回到顶部或清空自己的层级。 */
    val reselectEvents: Flow<TopLevelRoute> = _reselectEvents.asSharedFlow()

    /** 进入一个二级目的地，压进**当前** tab 的回退栈。 */
    fun goTo(route: AppRoute) {
        if (route is TopLevelRoute && route in state.topLevelRoutes) {
            select(route)
        } else {
            state.currentStack.add(route)
        }
    }

    /** 切一级 tab。已选中时发一次 reselect，不动回退栈。 */
    fun select(route: TopLevelRoute) {
        if (state.selected == route) {
            _reselectEvents.tryEmit(route)
        } else {
            state.selected = route
        }
    }

    /**
     * 返回。
     *
     * 已经在某个 tab 的根上时，返回先回到起始 tab；在起始 tab 的根上时不再消费，
     * 交给系统退出应用。
     */
    fun goBack() {
        val stack = state.currentStack
        if (stack.size > 1) {
            stack.removeLastOrNull()
        } else if (state.selected != state.startRoute) {
            state.selected = state.startRoute
        }
    }
}
