package io.legado.app.feature.settings.ui

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsUiModule = module {
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ProfileViewModel)
}
