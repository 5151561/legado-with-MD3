package io.legado.app.feature.readaloud.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadAloudApiContractTest {
    @Test fun `idle state has safe progress denominator`() {
        assertEquals(1, ReadAloudSnapshot().chapterLength)
    }
}
