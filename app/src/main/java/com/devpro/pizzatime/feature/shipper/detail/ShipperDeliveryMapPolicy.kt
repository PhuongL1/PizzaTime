package com.devpro.pizzatime.feature.shipper.detail

import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import com.devpro.pizzatime.shared.location.LocationDistanceCalculator
import java.text.NumberFormat
import java.util.Locale

internal enum class ShipperDeliveryCameraAction {
    NONE,
    CENTER_DESTINATION,
    CENTER_CURRENT_DEVICE,
    FIT_BOTH,
    KEEP,
}

internal data class ShipperDeliveryMapPresentation(
    val showDestinationMarker: Boolean,
    val showCurrentDeviceMarker: Boolean,
    val straightLineDistanceKm: Double?,
    val cameraAction: ShipperDeliveryCameraAction,
)

internal object ShipperDeliveryMapPolicy {

    fun present(
        destination: DeliveryCoordinate?,
        currentDevice: DeliveryCoordinate?,
        cameraInitialized: Boolean,
        bothLocationsFramed: Boolean,
        centerRequested: Boolean = false,
    ): ShipperDeliveryMapPresentation {
        val cameraAction = when {
            centerRequested && destination != null && currentDevice != null ->
                ShipperDeliveryCameraAction.FIT_BOTH
            centerRequested && destination != null ->
                ShipperDeliveryCameraAction.CENTER_DESTINATION
            centerRequested && currentDevice != null ->
                ShipperDeliveryCameraAction.CENTER_CURRENT_DEVICE
            centerRequested -> ShipperDeliveryCameraAction.NONE
            destination != null && currentDevice != null && !bothLocationsFramed ->
                ShipperDeliveryCameraAction.FIT_BOTH
            cameraInitialized -> ShipperDeliveryCameraAction.KEEP
            destination != null -> ShipperDeliveryCameraAction.CENTER_DESTINATION
            currentDevice != null -> ShipperDeliveryCameraAction.CENTER_CURRENT_DEVICE
            else -> ShipperDeliveryCameraAction.NONE
        }

        return ShipperDeliveryMapPresentation(
            showDestinationMarker = destination != null,
            showCurrentDeviceMarker = currentDevice != null,
            straightLineDistanceKm = if (destination != null && currentDevice != null) {
                LocationDistanceCalculator.calculateDistanceKm(
                    startLat = currentDevice.latitude,
                    startLng = currentDevice.longitude,
                    endLat = destination.latitude,
                    endLng = destination.longitude,
                )
            } else {
                null
            },
            cameraAction = cameraAction,
        )
    }
}

internal object StraightLineDistanceFormatter {

    fun formatNumber(
        distanceKm: Double,
        locale: Locale = Locale.getDefault(),
    ): String? {
        if (!distanceKm.isFinite() || distanceKm < 0.0) {
            return null
        }
        return NumberFormat.getNumberInstance(locale).run {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
            format(distanceKm)
        }
    }
}
