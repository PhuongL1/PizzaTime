package com.devpro.pizzatime.feature.shipper.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalMapNavigatorTest {

    @Test
    fun `valid coordinate creates Google driving navigation then generic fallback`() {
        val plans = ExternalMapNavigator.buildLaunchPlans(target())

        assertEquals(2, plans.size)
        assertEquals(ExternalMapNavigator.GOOGLE_MAPS_PACKAGE, plans[0].packageName)
        assertEquals("google.navigation:q=10.75,106.67&mode=d", plans[0].uri)
        assertNull(plans[1].packageName)
        assertEquals(
            "geo:0,0?q=10.75,106.67(12%20River%20Road)",
            plans[1].uri,
        )
    }

    @Test
    fun `coordinate label is encoded as a URI query component`() {
        val plan = ExternalMapNavigator.buildLaunchPlans(
            target(coordinateLabel = "A & B (café)"),
        )[1]

        assertEquals(
            "geo:0,0?q=10.75,106.67(A%20%26%20B%20%28caf%C3%A9%29)",
            plan.uri,
        )
    }

    @Test
    fun `missing coordinate falls back to encoded address search`() {
        val plans = ExternalMapNavigator.buildLaunchPlans(
            target(latitude = null, longitude = null, address = "  A & B Street  "),
        )

        assertEquals(listOf(ExternalMapLaunchSpec("geo:0,0?q=A%20%26%20B%20Street")), plans)
    }

    @Test
    fun `invalid coordinate without address is unavailable`() {
        val result = ExternalMapNavigator.launch(
            target(latitude = 91.0, address = "   "),
        ) { true }

        assertEquals(ExternalMapLaunchResult.DESTINATION_UNAVAILABLE, result)
    }

    @Test
    fun `missing coordinate and blank address are unavailable`() {
        val result = ExternalMapNavigator.launch(
            target(latitude = null, longitude = null, address = "   "),
        ) { true }

        assertEquals(ExternalMapLaunchResult.DESTINATION_UNAVAILABLE, result)
    }

    @Test
    fun `invalid coordinate uses honest address fallback`() {
        val plans = ExternalMapNavigator.buildLaunchPlans(
            target(latitude = Double.NaN, address = "12 River Road"),
        )

        assertEquals(listOf(ExternalMapLaunchSpec("geo:0,0?q=12%20River%20Road")), plans)
    }

    @Test
    fun `successful Google Maps launch stops before generic fallback`() {
        val attempts = mutableListOf<ExternalMapLaunchSpec>()
        val result = ExternalMapNavigator.launch(target()) { spec ->
            attempts += spec
            true
        }

        assertEquals(ExternalMapLaunchResult.GOOGLE_MAPS, result)
        assertEquals(1, attempts.size)
    }

    @Test
    fun `Google Maps launch failure falls through to generic map`() {
        val attempts = mutableListOf<ExternalMapLaunchSpec>()
        val result = ExternalMapNavigator.launch(target()) { spec ->
            attempts += spec
            spec.packageName == null
        }

        assertEquals(ExternalMapLaunchResult.GENERIC_MAP, result)
        assertEquals(2, attempts.size)
    }

    @Test
    fun `no installed handler returns no handler`() {
        val result = ExternalMapNavigator.launch(target()) { false }

        assertEquals(ExternalMapLaunchResult.NO_HANDLER, result)
    }

    @Test
    fun `navigation URI includes no unrelated private order fields`() {
        val combinedUri = ExternalMapNavigator.buildLaunchPlans(target())
            .joinToString(separator = " ") { it.uri }

        assertFalse(combinedUri.contains("customer@example.com"))
        assertFalse(combinedUri.contains("0900000000"))
        assertFalse(combinedUri.contains("Leave at door"))
        assertFalse(combinedUri.contains("orderId"))
        assertTrue(combinedUri.contains("12%20River%20Road"))
    }

    private fun target(
        latitude: Double? = 10.75,
        longitude: Double? = 106.67,
        address: String = "12 River Road",
        coordinateLabel: String = address,
    ): ExternalMapTarget {
        return ExternalMapTarget(
            latitude = latitude,
            longitude = longitude,
            address = address,
            coordinateLabel = coordinateLabel,
        )
    }
}
