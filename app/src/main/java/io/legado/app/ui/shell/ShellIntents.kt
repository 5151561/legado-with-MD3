package io.legado.app.ui.shell

import android.content.Context
import android.content.Intent

/**
 * 回到应用的 Intent。
 *
 * 旧外壳有一套 `MainActivity.createXxxIntent(...)` 的深链工厂：把路由塞进 extra，
 * 由那个 1480 行的路由表分发。新外壳没有集中的路由表，也只装重做过的界面，
 * 因此**通知、快捷方式与外部调起一律只能回到外壳本身**——目标页面重做之前，
 * 深链没有落点，硬编一个假的落点比直接回首页更糟。
 *
 * 每重做一块界面，就在这里加一个指向它的具名工厂；不要恢复「路由塞 extra」那套。
 */
object ShellIntents {

    fun openApp(context: Context): Intent =
        Intent(context, ShellActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
}
