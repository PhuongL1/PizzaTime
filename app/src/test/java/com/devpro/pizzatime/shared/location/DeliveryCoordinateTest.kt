package com.devpro.pizzatime.shared.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DeliveryCoordinateTest {

    @Test
    fun `valid coordinate is accepted`() {
        val coordinate = DeliveryCoordinate.from(10.5, -70.25)

        assertEquals(10.5, coordinate?.latitude ?: Double.NaN, 0.0)
        assertEquals(-70.25, coordinate?.longitude ?: Double.NaN, 0.0)
    }

    @Test
    fun `invalid latitude is rejected`() {
        assertNull(DeliveryCoordinate.from(90.1, 0.0))
        assertNull(DeliveryCoordinate.from(-90.1, 0.0))
    }

    @Test
    fun `invalid longitude is rejected`() {
        assertNull(DeliveryCoordinate.from(0.0, 180.1))
        assertNull(DeliveryCoordinate.from(0.0, -180.1))
    }

    @Test
    fun `non finite values are rejected`() {
        assertNull(DeliveryCoordinate.from(Double.NaN, 0.0))
        assertNull(DeliveryCoordinate.from(0.0, Double.POSITIVE_INFINITY))
        assertNull(DeliveryCoordinate.from(Double.NEGATIVE_INFINITY, 0.0))
    }

    @Test
    fun `missing coordinate produces no coordinate state`() {
        assertNull(DeliveryCoordinate.from(null, null))
    }

    @Test
    fun `marker replacement reuses the same conceptual slot`() {
        var created = 0
        val registry = ReplaceableMapValues<String, Any> {
            created += 1
            Any()
        }

        val first = registry.getOrCreate("selection")
        val replacement = registry.getOrCreate("selection")

        assertSame(first, replacement)
        assertEquals(1, registry.size)
        assertEquals(1, created)
    }
}
