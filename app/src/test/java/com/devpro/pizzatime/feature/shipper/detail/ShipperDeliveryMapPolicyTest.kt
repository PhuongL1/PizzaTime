package com.devpro.pizzatime.feature.shipper.detail

import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShipperDeliveryMapPolicyTest {

    @Test
    fun `no coordinates produces no markers and no camera movement`() {
        val presentation = ShipperDeliveryMapPolicy.present(
            destination = null,
            currentDevice = null,
            cameraInitialized = false,
            bothLocationsFramed = false,
        )

        assertFalse(presentation.showDestinationMarker)
        assertFalse(presentation.showCurrentDeviceMarker)
        assertNull(presentation.straightLineDistanceKm)
        assertEquals(ShipperDeliveryCameraAction.NONE, presentation.cameraAction)
    }

    @Test
    fun `destination only centers destination marker`() {
        val presentation = ShipperDeliveryMapPolicy.present(
            destination = coordinate(10.0, 20.0),
            currentDevice = null,
            cameraInitialized = false,
            bothLocationsFramed = false,
        )

        assertTrue(presentation.showDestinationMarker)
        assertFalse(presentation.showCurrentDeviceMarker)
        assertEquals(ShipperDeliveryCameraAction.CENTER_DESTINATION, presentation.cameraAction)
    }

    @Test
    fun `current device only centers current marker`() {
        val presentation = ShipperDeliveryMapPolicy.present(
            destination = null,
            currentDevice = coordinate(10.0, 20.0),
            cameraInitialized = false,
            bothLocationsFramed = false,
        )

        assertFalse(presentation.showDestinationMarker)
        assertTrue(presentation.showCurrentDeviceMarker)
        assertEquals(ShipperDeliveryCameraAction.CENTER_CURRENT_DEVICE, presentation.cameraAction)
    }

    @Test
    fun `first two-coordinate state fits both and computes straight-line distance`() {
        val presentation = ShipperDeliveryMapPolicy.present(
            destination = coordinate(0.0, 1.0),
            currentDevice = coordinate(0.0, 0.0),
            cameraInitialized = true,
            bothLocationsFramed = false,
        )

        assertTrue(presentation.showDestinationMarker)
        assertTrue(presentation.showCurrentDeviceMarker)
        assertEquals(111.2, presentation.straightLineDistanceKm ?: Double.NaN, 0.2)
        assertEquals(ShipperDeliveryCameraAction.FIT_BOTH, presentation.cameraAction)
    }

    @Test
    fun `later two-coordinate state preserves camera`() {
        val presentation = ShipperDeliveryMapPolicy.present(
            destination = coordinate(10.0, 20.0),
            currentDevice = coordinate(10.1, 20.1),
            cameraInitialized = true,
            bothLocationsFramed = true,
        )

        assertEquals(ShipperDeliveryCameraAction.KEEP, presentation.cameraAction)
    }

    @Test
    fun `center request fits both even after initial framing`() {
        val presentation = ShipperDeliveryMapPolicy.present(
            destination = coordinate(10.0, 20.0),
            currentDevice = coordinate(10.1, 20.1),
            cameraInitialized = true,
            bothLocationsFramed = true,
            centerRequested = true,
        )

        assertEquals(ShipperDeliveryCameraAction.FIT_BOTH, presentation.cameraAction)
    }

    @Test
    fun `distance number formatting is locale safe`() {
        assertEquals("12.3", StraightLineDistanceFormatter.formatNumber(12.34, Locale.US))
        assertEquals("12,3", StraightLineDistanceFormatter.formatNumber(12.34, Locale.GERMANY))
        assertNull(StraightLineDistanceFormatter.formatNumber(Double.NaN, Locale.US))
    }

    private fun coordinate(latitude: Double, longitude: Double): DeliveryCoordinate {
        return requireNotNull(DeliveryCoordinate.from(latitude, longitude))
    }
}
