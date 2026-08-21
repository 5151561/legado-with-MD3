package io.legado.app.feature.reader.ui

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val readerUiModule = module { viewModelOf(::ReaderViewModel) }
