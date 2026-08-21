package io.legado.app.feature.catalog.ui

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val catalogUiModule = module { viewModelOf(::CatalogViewModel) }
