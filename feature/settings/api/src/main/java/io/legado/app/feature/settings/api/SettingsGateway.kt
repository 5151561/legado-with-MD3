package io.legado.app.feature.settings.api

import kotlinx.coroutines.flow.Flow

/** 「我的」（画板 P-01）的读侧。 */
fun interface ProfileQuery {
    fun observeProfile(): Flow<ProfileSnapshot>
}

/**
 * 「我的」页上能就地改的两件事。
 *
 * 其余入口一律是「去某处」，不在本页写任何设置——这是画板 P-01
 * 「同一入口不在两处出现」的另一半。
 */
interface ProfileCommands {

    suspend fun setThemeMode(mode: AppThemeMode): SettingsCommandResult

    /** 开关 Web 服务。返回 [SettingsCommandResult.Success] 表示已发出启停命令，不表示已就绪。 */
    suspend fun setWebServiceEnabled(enabled: Boolean): SettingsCommandResult
}
