package io.legado.app.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class LegadoDatabaseSpecTest {

    @Test
    fun databaseIdentityRemainsBackupCompatible() {
        assertEquals("legado.db", LegadoDatabaseSpec.NAME)
        assertEquals(103, LegadoDatabaseSpec.VERSION)
    }
}
