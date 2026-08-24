package io.legado.app.feature.settings.impl

import io.legado.app.feature.settings.api.ProfileCommands
import io.legado.app.feature.settings.api.ProfileQuery
import java.time.LocalDate
import org.koin.dsl.module

/**
 * settings API 的唯一 Koin 绑定。app shell 加载本模块并提供各 host，
 * 不得自己绑定这些接口。
 */
val settingsImplModule = module {
    single<SettingsStore> { RoomSettingsStore(get()) }
    single<TodayProvider> { TodayProvider { LocalDate.now() } }

    single { DefaultProfileRepository(get(), get(), get(), get(), get()) }
    single<ProfileQuery> { get<DefaultProfileRepository>() }
    single<ProfileCommands> { get<DefaultProfileRepository>() }
}
