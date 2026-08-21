package io.legado.app.feature.catalog.api

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogApiContractTest {
    @Test
    fun `source identity is independent from display name`() {
        val source = CatalogSourceSummary("https://example.com", "示例", null, false, true, 10)
        assertEquals("https://example.com", source.id)
    }
}
