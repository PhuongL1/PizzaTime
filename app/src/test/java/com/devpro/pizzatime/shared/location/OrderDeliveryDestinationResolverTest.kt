package com.devpro.pizzatime.shared.location

import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class OrderDeliveryDestinationResolverTest {

    @Test
    fun `canonical GeoPoint is mapped to delivery coordinate`() {
        val coordinate = OrderDeliveryDestinationResolver.resolve(
            mapOf(OrderDeliveryDestinationResolver.LOCATION_FIELD to GeoPoint(10.75, 106.67)),
        )

        assertEquals(10.75, coordinate?.latitude ?: Double.NaN, 0.0)
        assertEquals(106.67, coordinate?.longitude ?: Double.NaN, 0.0)
    }

    @Test
    fun `valid legacy numeric fields remain readable`() {
        val coordinate = OrderDeliveryDestinationResolver.resolve(
            mapOf(
                OrderDeliveryDestinationResolver.LEGACY_LATITUDE_FIELD to 10.75f,
                OrderDeliveryDestinationResolver.LEGACY_LONGITUDE_FIELD to 106.67,
            ),
        )

        assertEquals(10.75, coordinate?.latitude ?: Double.NaN, 0.000001)
        assertEquals(106.67, coordinate?.longitude ?: Double.NaN, 0.0)
    }

    @Test
    fun `canonical destination takes priority over legacy coordinates`() {
        val coordinate = OrderDeliveryDestinationResolver.resolve(
            mapOf(
                OrderDeliveryDestinationResolver.LOCATION_FIELD to GeoPoint(1.0, 2.0),
                OrderDeliveryDestinationResolver.LEGACY_LATITUDE_FIELD to 3.0,
                OrderDeliveryDestinationResolver.LEGACY_LONGITUDE_FIELD to 4.0,
            ),
        )

        assertEquals(1.0, coordinate?.latitude ?: Double.NaN, 0.0)
        assertEquals(2.0, coordinate?.longitude ?: Double.NaN, 0.0)
    }

    @Test
    fun `invalid or incomplete legacy coordinates are unavailable`() {
        assertNull(
            OrderDeliveryDestinationResolver.resolve(
                mapOf(
                    OrderDeliveryDestinationResolver.LEGACY_LATITUDE_FIELD to 91.0,
                    OrderDeliveryDestinationResolver.LEGACY_LONGITUDE_FIELD to 4.0,
                ),
            ),
        )
        assertNull(
            OrderDeliveryDestinationResolver.resolve(
                mapOf(OrderDeliveryDestinationResolver.LEGACY_LATITUDE_FIELD to 10.0),
            ),
        )
    }

    @Test
    fun `canonical mapper writes GeoPoint without legacy flat coordinates`() {
        val coordinate = requireNotNull(DeliveryCoordinate.from(10.75, 106.67))

        val fields = OrderDeliveryDestinationResolver.canonicalFields(
            address = "  12 River Road  ",
            coordinate = coordinate,
        )

        assertEquals("12 River Road", fields[OrderDeliveryDestinationResolver.ADDRESS_FIELD])
        assertEquals(GeoPoint(10.75, 106.67), fields[OrderDeliveryDestinationResolver.LOCATION_FIELD])
        assertFalse(fields.containsKey(OrderDeliveryDestinationResolver.LEGACY_LATITUDE_FIELD))
        assertFalse(fields.containsKey(OrderDeliveryDestinationResolver.LEGACY_LONGITUDE_FIELD))
    }
}
