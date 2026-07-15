package com.devpro.pizzatime.shared.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryLocationSelectionTest {

    @Test
    fun `selection keeps normalized address and confirmed coordinate together`() {
        val coordinate = requireNotNull(DeliveryCoordinate.from(10.75, 106.67))

        val selection = DeliveryLocationSelection.from("  12 River Road  ", coordinate)

        assertEquals("12 River Road", selection.address)
        assertSame(coordinate, selection.coordinate)
        assertTrue(selection.isComplete)
    }

    @Test
    fun `editing address invalidates coordinate selected for previous address`() {
        val coordinate = requireNotNull(DeliveryCoordinate.from(10.75, 106.67))
        val selection = DeliveryLocationSelection.from("12 River Road", coordinate)

        val edited = selection.editAddress("34 Market Street")

        assertEquals("34 Market Street", edited.address)
        assertNull(edited.coordinate)
        assertFalse(edited.isComplete)
    }

    @Test
    fun `any address edit including whitespace invalidates coordinate`() {
        val coordinate = requireNotNull(DeliveryCoordinate.from(10.75, 106.67))
        val selection = DeliveryLocationSelection.from("12 River Road", coordinate)

        assertNull(selection.editAddress(" 12 River Road ").coordinate)
        assertSame(selection, selection.editAddress("12 River Road"))
    }

    @Test
    fun `invalid primitive coordinate becomes unavailable`() {
        val selection = DeliveryLocationSelection.from(
            address = "12 River Road",
            latitude = Double.NaN,
            longitude = 106.67,
        )

        assertNull(selection.coordinate)
        assertFalse(selection.isComplete)
    }
}
