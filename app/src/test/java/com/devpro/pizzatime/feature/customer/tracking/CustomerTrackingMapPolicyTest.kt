package com.devpro.pizzatime.feature.customer.tracking

import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerTrackingMapPolicyTest {

    @Test
    fun `no coordinates produces no markers and no camera movement`() {
        val presentation = CustomerTrackingMapPolicy.present(
            destination = null,
            shipper = null,
            cameraInitialized = false,
            bothLocationsFramed = false,
        )

        assertFalse(presentation.showDestinationMarker)
        assertFalse(presentation.showShipperMarker)
        assertNull(presentation.straightLineDistanceKm)
        assertEquals(CustomerTrackingCameraAction.NONE, presentation.cameraAction)
    }

    @Test
    fun `destination only centers destination marker`() {
        val presentation = CustomerTrackingMapPolicy.present(
            destination = coordinate(10.0, 20.0),
            shipper = null,
            cameraInitialized = false,
            bothLocationsFramed = false,
        )

        assertTrue(presentation.showDestinationMarker)
        assertFalse(presentation.showShipperMarker)
        assertEquals(CustomerTrackingCameraAction.CENTER_DESTINATION, presentation.cameraAction)
    }

    @Test
    fun `first sample fits both markers and computes straight-line distance`() {
        val presentation = CustomerTrackingMapPolicy.present(
            destination = coordinate(0.0, 1.0),
            shipper = coordinate(0.0, 0.0),
            cameraInitialized = true,
            bothLocationsFramed = false,
        )

        assertTrue(presentation.showDestinationMarker)
        assertTrue(presentation.showShipperMarker)
        assertEquals(111.2, presentation.straightLineDistanceKm ?: Double.NaN, 0.2)
        assertEquals(CustomerTrackingCameraAction.FIT_BOTH, presentation.cameraAction)
    }

    @Test
    fun `later samples preserve user camera until center is requested`() {
        val presentation = CustomerTrackingMapPolicy.present(
            destination = coordinate(10.0, 20.0),
            shipper = coordinate(10.1, 20.1),
            cameraInitialized = true,
            bothLocationsFramed = true,
        )
        val centered = CustomerTrackingMapPolicy.present(
            destination = coordinate(10.0, 20.0),
            shipper = coordinate(10.1, 20.1),
            cameraInitialized = true,
            bothLocationsFramed = true,
            centerRequested = true,
        )

        assertEquals(CustomerTrackingCameraAction.KEEP, presentation.cameraAction)
        assertEquals(CustomerTrackingCameraAction.FIT_BOTH, centered.cameraAction)
    }

    @Test
    fun `distance number formatting is locale safe`() {
        assertEquals("12.3", CustomerTrackingDistanceFormatter.formatNumber(12.34, Locale.US))
        assertEquals("12,3", CustomerTrackingDistanceFormatter.formatNumber(12.34, Locale.GERMANY))
        assertNull(CustomerTrackingDistanceFormatter.formatNumber(Double.NaN, Locale.US))
    }

    private fun coordinate(latitude: Double, longitude: Double): DeliveryCoordinate {
        return requireNotNull(DeliveryCoordinate.from(latitude, longitude))
    }
}
