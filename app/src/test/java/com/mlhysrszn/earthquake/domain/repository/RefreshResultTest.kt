package com.mlhysrszn.earthquake.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RefreshResultTest {
    @Test
    fun `successful refresh allows an empty valid result`() {
        assertEquals(0, RefreshResult.Success(acceptedEventCount = 0).acceptedEventCount)
    }

    @Test
    fun `successful refresh rejects a negative accepted event count`() {
        assertThrows(IllegalArgumentException::class.java) {
            RefreshResult.Success(acceptedEventCount = -1)
        }
    }

    @Test
    fun `failure distinguishes expected refresh failure categories`() {
        assertEquals(
            RefreshResult.Reason.NETWORK,
            RefreshResult.Failure(RefreshResult.Reason.NETWORK).reason,
        )
        assertEquals(
            RefreshResult.Reason.INVALID_RESPONSE,
            RefreshResult.Failure(RefreshResult.Reason.INVALID_RESPONSE).reason,
        )
        assertEquals(
            RefreshResult.Reason.STORAGE,
            RefreshResult.Failure(RefreshResult.Reason.STORAGE).reason,
        )
    }
}
