package com.devpro.pizzatime.shared.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MapPickerResultParserTest {

    @Test
    fun `complete primitive result is parsed`() {
        val result = parse(
            address = "  12 River Road ",
            latitude = 0.0,
            longitude = 0.0,
        )

        assertNotNull(result)
        assertEquals("12 River Road", result?.address)
        assertEquals(0.0, result?.coordinate?.latitude ?: Double.NaN, 0.0)
        assertEquals(0.0, result?.coordinate?.longitude ?: Double.NaN, 0.0)
    }

    @Test
    fun `missing latitude is rejected instead of becoming zero`() {
        assertNull(
            parse(
                hasLatitude = false,
                latitude = 0.0,
            ),
        )
    }

    @Test
    fun `missing longitude is rejected instead of becoming zero`() {
        assertNull(
            parse(
                hasLongitude = false,
                longitude = 0.0,
            ),
        )
    }

    @Test
    fun `missing or blank address is rejected`() {
        assertNull(parse(hasAddress = false))
        assertNull(parse(address = "   "))
    }

    @Test
    fun `invalid coordinate is rejected`() {
        assertNull(parse(latitude = 90.1))
        assertNull(parse(longitude = Double.POSITIVE_INFINITY))
    }

    private fun parse(
        hasAddress: Boolean = true,
        address: String? = "12 River Road",
        hasLatitude: Boolean = true,
        latitude: Double? = 10.75,
        hasLongitude: Boolean = true,
        longitude: Double? = 106.67,
    ): DeliveryLocationSelection? {
        return MapPickerResultParser.parse(
            hasAddress = hasAddress,
            address = address,
            hasLatitude = hasLatitude,
            latitude = latitude,
            hasLongitude = hasLongitude,
            longitude = longitude,
        )
    }
}
