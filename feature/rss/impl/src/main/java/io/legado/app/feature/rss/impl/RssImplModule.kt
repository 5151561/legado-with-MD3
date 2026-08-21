package io.legado.app.feature.rss.impl

import io.legado.app.feature.rss.api.RssCommands
import io.legado.app.feature.rss.api.RssQuery
import org.koin.dsl.module

/**
 * The only Koin binding of the RSS API. The app shell loads this module and supplies the two host
 * seams; it must not bind the API interfaces itself.
 */
val rssImplModule = module {
    single<RssSourceStore> { RoomRssSourceStore(get()) }
    single { DefaultRssRepository(get(), get(), get()) }
    single<RssQuery> { get<DefaultRssRepository>() }
    single<RssCommands> { get<DefaultRssRepository>() }
}
