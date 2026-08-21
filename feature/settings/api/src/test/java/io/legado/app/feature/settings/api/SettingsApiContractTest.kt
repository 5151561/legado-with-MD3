package io.legado.app.feature.settings.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsApiContractTest {
    @Test
    fun `backup readiness is represented separately from progress sync`() {
        val overview = SettingsOverview(
            themeMode = "0",
            fontScale = 10,
            bitmapCacheSizeMb = 50,
            downloadThreadCount = 16,
            backupConfigured = false,
            syncBookProgress = true,
        )

        assertFalse(overview.backupConfigured)
        assertTrue(overview.syncBookProgress)
    }
}
