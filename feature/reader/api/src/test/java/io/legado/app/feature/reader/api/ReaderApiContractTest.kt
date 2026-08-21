package io.legado.app.feature.reader.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderApiContractTest {
    @Test
    fun `idle snapshot has no mutable runtime payload`() {
        val snapshot = ReaderSnapshot()

        assertEquals(ReaderLoadState.Idle, snapshot.loadState)
        assertFalse(snapshot.canGoPrevious)
        assertFalse(snapshot.canGoNext)
    }

    @Test
    fun `failure distinguishes recovery`() {
        val failure = ReaderCommandResult.Failure(
            ReaderError.ContentUnavailable("offline")
        )

        assertEquals(true, (failure.error as ReaderError.ContentUnavailable).retryable)
    }
}
