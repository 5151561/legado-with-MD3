package io.legado.app.feature.bookshelf.impl

import io.legado.app.feature.bookshelf.api.BookshelfCommands
import io.legado.app.feature.bookshelf.api.BookshelfGroupCommands
import io.legado.app.feature.bookshelf.api.BookshelfPreferencesGateway
import io.legado.app.feature.bookshelf.api.BookshelfQuery
import org.koin.dsl.module

/**
 * The only Koin binding of the bookshelf API. The app shell loads this module and supplies the two
 * host seams; it must not bind the API interfaces itself.
 */
val bookshelfImplModule = module {
    single<BookshelfStore> { RoomBookshelfStore(get()) }
    single { DefaultBookshelfRepository(get(), get(), get()) }
    single<BookshelfQuery> { get<DefaultBookshelfRepository>() }
    single<BookshelfPreferencesGateway> { get<DefaultBookshelfRepository>() }
    single<BookshelfCommands> { get<DefaultBookshelfRepository>() }
    single<BookshelfGroupCommands> { get<DefaultBookshelfRepository>() }
}
