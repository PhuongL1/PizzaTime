package com.devpro.pizzatime.feature.customer.tracking

import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import com.devpro.pizzatime.shared.location.LocationDistanceCalculator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.text.NumberFormat
import java.util.Locale

internal enum class CustomerTrackingObservationState {
    UNAUTHORIZED,
    WAITING_FOR_DELIVERY,
    OBSERVE,
    DELIVERED,
    CANCELLED,
}

internal object CustomerTrackingObservationPolicy {
    fun resolve(
        edition: AppEdition,
        sessionLoggedIn: Boolean,
        sessionRole: UserRole,
        authenticatedUid: String?,
        orderCustomerId: String?,
        orderStatus: String?,
        assignedShipperId: String?,
    ): CustomerTrackingObservationState {
        if (
            edition != AppEdition.CUSTOMER ||
            !sessionLoggedIn ||
            sessionRole != UserRole.CUSTOMER ||
            authenticatedUid.isNullOrBlank() ||
            authenticatedUid != orderCustomerId
        ) {
            return CustomerTrackingObservationState.UNAUTHORIZED
        }

        return when (orderStatus) {
            "DELIVERING" -> {
                if (assignedShipperId.isNullOrBlank()) {
                    CustomerTrackingObservationState.WAITING_FOR_DELIVERY
                } else {
                    CustomerTrackingObservationState.OBSERVE
                }
            }

            "DELIVERED" -> CustomerTrackingObservationState.DELIVERED
            "CANCELLED" -> CustomerTrackingObservationState.CANCELLED
            else -> CustomerTrackingObservationState.WAITING_FOR_DELIVERY
        }
    }
}

internal data class CustomerTrackingListenerBinding(
    val orderId: String,
    val customerId: String,
)

internal object CustomerTrackingListenerPolicy {
    fun shouldReplace(
        current: CustomerTrackingListenerBinding?,
        next: CustomerTrackingListenerBinding?,
    ): Boolean = current != next
}

internal enum class CustomerTrackingFreshnessState {
    FRESH,
    STALE,
    DELAYED,
}

internal sealed interface CustomerTrackingRelativeTime {
    data object JustNow : CustomerTrackingRelativeTime

    data class MinutesAgo(val minutes: Int) : CustomerTrackingRelativeTime
}

internal object CustomerTrackingFreshnessPolicy {
    const val STALE_LOCATION_THRESHOLD_MILLIS = 5 * 60_000L
    private const val MAXIMUM_FUTURE_SKEW_MILLIS = 30_000L

    fun classify(
        nowMillis: Long,
        updatedAtMillis: Long?,
    ): CustomerTrackingFreshnessState {
        val elapsedMillis = elapsedMillis(nowMillis, updatedAtMillis) ?: return CustomerTrackingFreshnessState.DELAYED
        return if (elapsedMillis > STALE_LOCATION_THRESHOLD_MILLIS) {
            CustomerTrackingFreshnessState.STALE
        } else {
            CustomerTrackingFreshnessState.FRESH
        }
    }

    fun relativeTime(
        nowMillis: Long,
        updatedAtMillis: Long?,
    ): CustomerTrackingRelativeTime? {
        val elapsedMillis = elapsedMillis(nowMillis, updatedAtMillis) ?: return null
        if (elapsedMillis < 60_000L) {
            return CustomerTrackingRelativeTime.JustNow
        }
        val minutes = (elapsedMillis / 60_000L).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong())
        return CustomerTrackingRelativeTime.MinutesAgo(minutes.toInt())
    }

    private fun elapsedMillis(
        nowMillis: Long,
        updatedAtMillis: Long?,
    ): Long? {
        if (nowMillis <= 0L || updatedAtMillis == null || updatedAtMillis <= 0L) {
            return null
        }
        val elapsedMillis = nowMillis - updatedAtMillis
        return elapsedMillis.takeIf { it >= 0L && it <= Long.MAX_VALUE && it >= -MAXIMUM_FUTURE_SKEW_MILLIS }
    }
}

internal enum class CustomerTrackingCameraAction {
    NONE,
    CENTER_DESTINATION,
    CENTER_SHIPPER,
    FIT_BOTH,
    KEEP,
}

internal data class CustomerTrackingMapPresentation(
    val showDestinationMarker: Boolean,
    val showShipperMarker: Boolean,
    val straightLineDistanceKm: Double?,
    val cameraAction: CustomerTrackingCameraAction,
)

internal object CustomerTrackingMapPolicy {
    fun present(
        destination: DeliveryCoordinate?,
        shipper: DeliveryCoordinate?,
        cameraInitialized: Boolean,
        bothLocationsFramed: Boolean,
        centerRequested: Boolean = false,
    ): CustomerTrackingMapPresentation {
        val cameraAction = when {
            centerRequested && destination != null && shipper != null -> CustomerTrackingCameraAction.FIT_BOTH
            centerRequested && destination != null -> CustomerTrackingCameraAction.CENTER_DESTINATION
            centerRequested && shipper != null -> CustomerTrackingCameraAction.CENTER_SHIPPER
            centerRequested -> CustomerTrackingCameraAction.NONE
            destination != null && shipper != null && !bothLocationsFramed -> CustomerTrackingCameraAction.FIT_BOTH
            cameraInitialized -> CustomerTrackingCameraAction.KEEP
            destination != null -> CustomerTrackingCameraAction.CENTER_DESTINATION
            shipper != null -> CustomerTrackingCameraAction.CENTER_SHIPPER
            else -> CustomerTrackingCameraAction.NONE
        }

        return CustomerTrackingMapPresentation(
            showDestinationMarker = destination != null,
            showShipperMarker = shipper != null,
            straightLineDistanceKm = if (destination != null && shipper != null) {
                LocationDistanceCalculator.calculateDistanceKm(
                    startLat = shipper.latitude,
                    startLng = shipper.longitude,
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

internal object CustomerTrackingDistanceFormatter {
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

internal data class CustomerTrackingDocument(
    val shipperId: String,
    val coordinate: DeliveryCoordinate?,
    val updatedAtMillis: Long?,
)

internal object CustomerTrackingDocumentParser {
    fun parse(
        data: Map<String, Any?>,
        expectedShipperId: String?,
    ): CustomerTrackingDocument? {
        val shipperId = (data["shipperId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        if (!expectedShipperId.isNullOrBlank() && shipperId != expectedShipperId) {
            return null
        }

        val coordinate = (data["location"] as? GeoPoint)?.let { geoPoint ->
            DeliveryCoordinate.from(geoPoint.latitude, geoPoint.longitude)
        }
        val updatedAtMillis = (data["updatedAt"] as? Timestamp)?.toDate()?.time
        return CustomerTrackingDocument(
            shipperId = shipperId,
            coordinate = coordinate,
            updatedAtMillis = updatedAtMillis,
        )
    }
}
