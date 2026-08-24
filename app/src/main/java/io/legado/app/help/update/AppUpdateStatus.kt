package io.legado.app.help.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 启动时那一次更新检查的结果。
 *
 * 「我的」（画板 P-01）与设置主页（画板 C-01）都要显示「有没有新版本」，但两处都不该
 * 因为进了一次页面就发一次网络请求。检查仍然只在启动时做一次，结果记在这里供两处读取。
 *
 * 进程内状态，不持久化：跨进程重启后回到「没检查过」，此时 [info] 为 null，
 * 界面上表现为没有新版本角标——「不知道」与「已是最新」在界面上本来就是同一种呈现。
 */
object AppUpdateStatus {

    private val _info = MutableStateFlow<AppUpdate.UpdateInfo?>(null)

    /** null 表示没检查过、检查失败、或已是最新。 */
    val info = _info.asStateFlow()

    fun onCheckResult(updateInfo: AppUpdate.UpdateInfo?) {
        _info.value = updateInfo
    }
}
