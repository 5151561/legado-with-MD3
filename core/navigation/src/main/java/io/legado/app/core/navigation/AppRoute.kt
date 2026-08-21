package io.legado.app.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 根导航可识别的非 sealed 标记协议。
 *
 * Feature 可以在自己的模块中实现该接口；route 的入栈、去重、深链与恢复策略仍由
 * app shell 显式注册。实现此接口本身不会让未知 route 被自动接纳。
 */
interface AppRoute : NavKey
