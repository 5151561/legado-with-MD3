package io.legado.app.data

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigration102To103ContractTest {

    @Test
    fun `latest migration rebuilds all read record tables`() {
        val migration = DatabaseMigrations.migrations.single {
            it.startVersion == 102 && it.endVersion == 103
        }
        val statements = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, arguments ->
            if (method.name == "execSQL") {
                statements += arguments?.firstOrNull() as String
            }
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                else -> null
            }
        } as SupportSQLiteDatabase

        migration.migrate(database)

        assertEquals(102, migration.startVersion)
        assertEquals(103, migration.endVersion)
        assertTrue(statements.any { "CREATE TABLE readRecord_migrated" in it })
        assertTrue(statements.any { it == "DROP TABLE readRecord" })
        assertTrue(statements.any { "ALTER TABLE readRecord_migrated RENAME TO readRecord" in it })
        assertTrue(statements.any { "CREATE TABLE readRecordDetail_migrated" in it })
        assertTrue(statements.any { "CREATE TABLE readRecordSession_migrated" in it })
    }
}
