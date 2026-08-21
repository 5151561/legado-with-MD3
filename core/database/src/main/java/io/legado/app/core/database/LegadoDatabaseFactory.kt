package io.legado.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** Stable database identity shared by the legacy app host and future feature impl modules. */
object LegadoDatabaseSpec {
    const val NAME = "legado.db"
    const val VERSION = 103
}

/**
 * Creates the single application database builder without obtaining a process-global Context.
 * Migration, callback and temporary main-thread compatibility policies remain explicit at the
 * app assembly boundary until the complete schema is moved into this module.
 */
fun <T : RoomDatabase> legadoDatabaseBuilder(
    context: Context,
    databaseClass: Class<T>,
): RoomDatabase.Builder<T> = Room.databaseBuilder(
    context.applicationContext,
    databaseClass,
    LegadoDatabaseSpec.NAME,
)
