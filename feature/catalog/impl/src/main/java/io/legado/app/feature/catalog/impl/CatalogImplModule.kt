package io.legado.app.feature.catalog.impl

import io.legado.app.feature.catalog.api.CatalogCommands
import io.legado.app.feature.catalog.api.CatalogQuery
import org.koin.dsl.module

/**
 * The only Koin binding of the catalog API. The app shell loads this module and supplies the
 * removal host; it must not bind the API interfaces itself.
 */
val catalogImplModule = module {
    single<CatalogSourceStore> { RoomCatalogSourceStore(get()) }
    single { DefaultCatalogRepository(get(), get()) }
    single<CatalogQuery> { get<DefaultCatalogRepository>() }
    single<CatalogCommands> { get<DefaultCatalogRepository>() }
}
