package io.legado.app.feature.rss.ui

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val rssUiModule = module { viewModelOf(::RssViewModel) }
