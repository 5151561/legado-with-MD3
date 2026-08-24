package io.legado.app.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 根导航可识别的非 sealed 标记协议。
 *
 * Feature 在自己的 `ui` 模块里声明路由并实现该接口；外壳只负责把各 feature 的
 * entry 装进同一个 `entryProvider`，不认识任何 feature 的内部结构。
 *
 * 实现此接口本身不会让未知 route 被自动接纳——没有被任何 feature 注册的 route
 * 走到 `NavDisplay` 会抛错，这是有意的：路由必须有明确的 owner。
 */
interface AppRoute : NavKey

/**
 * 一级导航目的地。每个一级目的地拥有自己的回退栈（[AppNavigationState]）。
 *
 * @property id 稳定标识。用于导航栏选中判定与跨进程恢复选中态——
 *   路由对象本身可能带参数，用它做键会在参数变化时丢掉选中态。
 */
interface TopLevelRoute : AppRoute {
    val id: String
}
