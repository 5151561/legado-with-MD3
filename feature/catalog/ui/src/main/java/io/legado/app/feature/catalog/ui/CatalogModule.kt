package io.legado.app.feature.catalog.ui

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val catalogUiModule = module {
    viewModelOf(::CatalogViewModel)
    viewModelOf(::SourceHubViewModel)

    // 三个按书取的 ViewModel 用 parametersOf(bookId) 建，键也按 bookId 分，
    // 否则同时打开两本书的详情会共用一份状态。
    viewModel { (bookId: String) -> BookDetailViewModel(bookId, get(), get(), get()) }
    viewModel { (bookId: String) -> TocManageViewModel(bookId, get(), get()) }
    viewModel { (bookId: String) -> ReaderTocViewModel(bookId, get(), get()) }
}
