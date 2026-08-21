package io.legado.app.data

import io.legado.app.constant.AppConst
import io.legado.app.core.database.legadoDatabaseBuilder
import io.legado.app.help.DefaultData
import splitties.init.appCtx

/** Legacy process-global access retained for unmigrated app callers and frozen by the architecture ratchet. */
val appDb by lazy {
    legadoDatabaseBuilder(appCtx, AppDatabase::class.java)
        .fallbackToDestructiveMigrationFrom(false, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        .addMigrations(*DatabaseMigrations.migrations(AppConst.androidId))
        .allowMainThreadQueries()
        .addCallback(AppDatabase.dbCallback(DefaultData.keyboardAssists))
        .build()
}
