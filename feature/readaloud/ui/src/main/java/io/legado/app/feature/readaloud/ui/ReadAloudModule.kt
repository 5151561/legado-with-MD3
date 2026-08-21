package io.legado.app.feature.readaloud.ui
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
val readAloudUiModule = module { viewModelOf(::ReadAloudViewModel) }
