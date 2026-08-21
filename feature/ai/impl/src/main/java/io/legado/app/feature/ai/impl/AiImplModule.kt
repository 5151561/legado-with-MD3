package io.legado.app.feature.ai.impl

import io.legado.app.feature.ai.api.AiCommands
import io.legado.app.feature.ai.api.AiOverviewQuery
import org.koin.dsl.module

/**
 * The only Koin binding of the AI API. The app shell loads this module and supplies the default
 * model host; it must not bind the API interfaces itself.
 */
val aiImplModule = module {
    single<AiProfileStore> { RoomAiProfileStore(get()) }
    single { DefaultAiRepository(get(), get()) }
    single<AiOverviewQuery> { get<DefaultAiRepository>() }
    single<AiCommands> { get<DefaultAiRepository>() }
}
