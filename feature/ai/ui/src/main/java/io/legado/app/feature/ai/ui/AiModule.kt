package io.legado.app.feature.ai.ui
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
val aiUiModule = module { viewModelOf(::AiViewModel) }
