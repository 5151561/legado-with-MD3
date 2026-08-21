package io.legado.app.feature.bookshelf.ui

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bookshelfUiModule = module {
    viewModelOf(::BookshelfViewModel)
}
