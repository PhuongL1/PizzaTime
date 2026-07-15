package com.devpro.pizzatime.shared.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSampleFreshnessPolicyTest {

    @Test
    fun `recent sample is fresh`() {
        val now = 1_000_000L

        assertTrue(LocationSampleFreshnessPolicy.isFresh(now - 30_000L, now))
    }

    @Test
    fun `stale and invalid samples are rejected`() {
        val now = 1_000_000L

        assertFalse(
            LocationSampleFreshnessPolicy.isFresh(
                now - OneShotDeviceLocationSource.MAX_LOCATION_SAMPLE_AGE_MS - 1L,
                now,
            ),
        )
        assertFalse(LocationSampleFreshnessPolicy.isFresh(0L, now))
    }

    @Test
    fun `implausibly future sample is rejected`() {
        val now = 1_000_000L

        assertFalse(
            LocationSampleFreshnessPolicy.isFresh(
                now + OneShotDeviceLocationSource.MAX_FUTURE_SAMPLE_SKEW_MS + 1L,
                now,
            ),
        )
    }
}
