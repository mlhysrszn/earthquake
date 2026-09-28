package com.mlhysrszn.earthquake.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferencesTest {
    @Test
    fun `defaults to disabled and a four point zero threshold`() {
        val preferences = NotificationPreferences()

        assertFalse(preferences.notificationsEnabled)
        assertTrue(preferences.magnitudeThreshold == 4.0)
    }

    @Test
    fun `accepts threshold endpoints and half magnitude steps`() {
        assertTrue(isValidMagnitudeThreshold(0.0))
        assertTrue(isValidMagnitudeThreshold(4.5))
        assertTrue(isValidMagnitudeThreshold(9.5))
        assertTrue(isValidMagnitudeThreshold(4.0))
    }

    @Test
    fun `rejects thresholds outside range off step or non-finite`() {
        listOf(-0.5, 9.6, 4.25, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertFalse("$value must be invalid", isValidMagnitudeThreshold(value))
            assertThrows(IllegalArgumentException::class.java) {
                NotificationPreferences(magnitudeThreshold = value)
            }
        }
    }
}
