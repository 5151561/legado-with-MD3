package io.legado.app.feature.catalog.impl

import io.legado.app.feature.catalog.api.BookDetailCommands
import io.legado.app.feature.catalog.api.BookDetailQuery
import io.legado.app.feature.catalog.api.CatalogCommands
import io.legado.app.feature.catalog.api.CatalogQuery
import io.legado.app.feature.catalog.api.SourceHubQuery
import io.legado.app.feature.catalog.api.TocCommands
import io.legado.app.feature.catalog.api.TocQuery
import org.koin.dsl.module

/**
 * The only Koin binding of the catalog API. The app shell loads this module and supplies the
 * hosts; it must not bind the API interfaces itself.
 */
val catalogImplModule = module {
    single<CatalogSourceStore> { RoomCatalogSourceStore(get()) }
    single { DefaultCatalogRepository(get(), get()) }
    single<CatalogQuery> { get<DefaultCatalogRepository>() }
    single<CatalogCommands> { get<DefaultCatalogRepository>() }

    single<SourceCatalogStore> { RoomSourceCatalogStore(get()) }
    single<SourceHubQuery> { DefaultSourceHubRepository(get(), get()) }

    single<TocStore> { RoomTocStore(get()) }
    single { DefaultTocRepository(get(), get()) }
    single<TocQuery> { get<DefaultTocRepository>() }
    single<TocCommands> { get<DefaultTocRepository>() }

    single<BookDetailStore> { RoomBookDetailStore(get()) }
    single { DefaultBookDetailRepository(get(), get(), get(), get()) }
    single<BookDetailQuery> { get<DefaultBookDetailRepository>() }
    single<BookDetailCommands> { get<DefaultBookDetailRepository>() }
}
