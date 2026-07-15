package com.devpro.pizzatime.feature.shipper.tracking

import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DeliveryTrackingConfig {
    const val MINIMUM_WRITE_INTERVAL_MILLIS = 15_000L
    const val MINIMUM_MOVEMENT_METERS = 25.0
    const val MAXIMUM_WRITE_INTERVAL_MILLIS = 60_000L
    const val MAXIMUM_ACCEPTABLE_ACCURACY_METERS = 100.0
    const val MAXIMUM_SPEED_METERS_PER_SECOND = 100.0
    const val MAXIMUM_SAMPLE_AGE_MILLIS = 2 * 60_000L
    const val MAXIMUM_FUTURE_SAMPLE_SKEW_MILLIS = 30_000L
    const val ORDER_REVALIDATION_INTERVAL_MILLIS = 60_000L
    const val LOCATION_UPDATE_INTERVAL_MILLIS = 5_000L
    const val LOCATION_UPDATE_DISTANCE_METERS = 0f
    const val TRACKING_SCHEMA_VERSION = 1L
    const val DELIVERING_STATUS = "DELIVERING"
}

data class DeliveryTrackingUserState(
    val exists: Boolean,
    val active: Boolean,
    val role: String?,
)

data class DeliveryTrackingOrderState(
    val exists: Boolean,
    val shipperId: String?,
    val status: String?,
)

sealed interface DeliveryTrackingEligibility {
    data object Eligible : DeliveryTrackingEligibility

    data class Ineligible(val reason: DeliveryTrackingIneligibility) : DeliveryTrackingEligibility
}

enum class DeliveryTrackingIneligibility {
    WRONG_EDITION,
    SESSION_NOT_AUTHENTICATED,
    WRONG_SESSION_ROLE,
    FIREBASE_USER_MISSING,
    USER_PROFILE_MISSING,
    USER_INACTIVE,
    WRONG_SERVER_ROLE,
    ORDER_MISSING,
    ASSIGNMENT_MISMATCH,
    ORDER_NOT_DELIVERING,
}

object DeliveryTrackingEligibilityPolicy {
    fun evaluateLocal(
        edition: AppEdition,
        sessionLoggedIn: Boolean,
        sessionRole: UserRole,
        authenticatedUid: String?,
    ): DeliveryTrackingEligibility {
        return when {
            edition != AppEdition.SHIPPER -> ineligible(DeliveryTrackingIneligibility.WRONG_EDITION)
            !sessionLoggedIn -> ineligible(DeliveryTrackingIneligibility.SESSION_NOT_AUTHENTICATED)
            sessionRole != UserRole.SHIPPER -> ineligible(DeliveryTrackingIneligibility.WRONG_SESSION_ROLE)
            authenticatedUid.isNullOrBlank() -> ineligible(DeliveryTrackingIneligibility.FIREBASE_USER_MISSING)
            else -> DeliveryTrackingEligibility.Eligible
        }
    }

    fun evaluateServer(
        expectedShipperId: String,
        user: DeliveryTrackingUserState,
        order: DeliveryTrackingOrderState,
    ): DeliveryTrackingEligibility {
        return when {
            !user.exists -> ineligible(DeliveryTrackingIneligibility.USER_PROFILE_MISSING)
            !user.active -> ineligible(DeliveryTrackingIneligibility.USER_INACTIVE)
            !user.role.equals(UserRole.SHIPPER.name, ignoreCase = true) -> {
                ineligible(DeliveryTrackingIneligibility.WRONG_SERVER_ROLE)
            }
            !order.exists -> ineligible(DeliveryTrackingIneligibility.ORDER_MISSING)
            order.shipperId != expectedShipperId -> {
                ineligible(DeliveryTrackingIneligibility.ASSIGNMENT_MISMATCH)
            }
            order.status != DeliveryTrackingConfig.DELIVERING_STATUS -> {
                ineligible(DeliveryTrackingIneligibility.ORDER_NOT_DELIVERING)
            }
            else -> DeliveryTrackingEligibility.Eligible
        }
    }

    fun evaluateParentOrder(
        expectedShipperId: String,
        order: DeliveryTrackingOrderState,
    ): DeliveryTrackingEligibility {
        return when {
            !order.exists -> ineligible(DeliveryTrackingIneligibility.ORDER_MISSING)
            order.shipperId != expectedShipperId -> {
                ineligible(DeliveryTrackingIneligibility.ASSIGNMENT_MISMATCH)
            }
            order.status != DeliveryTrackingConfig.DELIVERING_STATUS -> {
                ineligible(DeliveryTrackingIneligibility.ORDER_NOT_DELIVERING)
            }
            else -> DeliveryTrackingEligibility.Eligible
        }
    }

    private fun ineligible(reason: DeliveryTrackingIneligibility): DeliveryTrackingEligibility {
        return DeliveryTrackingEligibility.Ineligible(reason)
    }
}

data class DeliveryTrackingLocationSample(
    val coordinate: DeliveryCoordinate,
    val accuracyMeters: Double,
    val recordedAtMillis: Long,
    val bearingDegrees: Double?,
    val speedMetersPerSecond: Double?,
)

object DeliveryTrackingLocationPolicy {
    fun validate(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        recordedAtMillis: Long,
        nowMillis: Long,
        bearingDegrees: Double?,
        speedMetersPerSecond: Double?,
    ): DeliveryTrackingLocationSample? {
        val coordinate = DeliveryCoordinate.from(latitude, longitude) ?: return null
        if (
            !accuracyMeters.isFinite() ||
            accuracyMeters < 0.0 ||
            accuracyMeters > DeliveryTrackingConfig.MAXIMUM_ACCEPTABLE_ACCURACY_METERS
        ) {
            return null
        }
        if (recordedAtMillis <= 0L || nowMillis <= 0L) {
            return null
        }
        val ageMillis = nowMillis - recordedAtMillis
        if (
            ageMillis !in
            -DeliveryTrackingConfig.MAXIMUM_FUTURE_SAMPLE_SKEW_MILLIS..
                DeliveryTrackingConfig.MAXIMUM_SAMPLE_AGE_MILLIS
        ) {
            return null
        }

        return DeliveryTrackingLocationSample(
            coordinate = coordinate,
            accuracyMeters = accuracyMeters,
            recordedAtMillis = recordedAtMillis,
            bearingDegrees = bearingDegrees?.takeIf { value ->
                value.isFinite() && value in 0.0..<360.0
            },
            speedMetersPerSecond = speedMetersPerSecond?.takeIf { value ->
                value.isFinite() && value in 0.0..DeliveryTrackingConfig.MAXIMUM_SPEED_METERS_PER_SECOND
            },
        )
    }
}

data class WrittenDeliveryTrackingSample(
    val sample: DeliveryTrackingLocationSample,
    val writtenAtMillis: Long,
)

object DeliveryTrackingThrottlePolicy {
    fun shouldWrite(
        candidate: DeliveryTrackingLocationSample,
        previous: WrittenDeliveryTrackingSample?,
        nowMillis: Long,
    ): Boolean {
        if (previous == null) {
            return true
        }
        if (
            candidate.recordedAtMillis <= previous.sample.recordedAtMillis ||
            nowMillis <= previous.writtenAtMillis
        ) {
            return false
        }

        val elapsedMillis = nowMillis - previous.writtenAtMillis
        if (elapsedMillis >= DeliveryTrackingConfig.MAXIMUM_WRITE_INTERVAL_MILLIS) {
            return true
        }
        if (elapsedMillis < DeliveryTrackingConfig.MINIMUM_WRITE_INTERVAL_MILLIS) {
            return false
        }

        return distanceMeters(
            previous.sample.coordinate,
            candidate.coordinate,
        ) >= DeliveryTrackingConfig.MINIMUM_MOVEMENT_METERS
    }

    internal fun distanceMeters(
        first: DeliveryCoordinate,
        second: DeliveryCoordinate,
    ): Double {
        val firstLatitude = Math.toRadians(first.latitude)
        val secondLatitude = Math.toRadians(second.latitude)
        val latitudeDelta = secondLatitude - firstLatitude
        val longitudeDelta = Math.toRadians(second.longitude - first.longitude)
        val haversine = sin(latitudeDelta / 2.0) * sin(latitudeDelta / 2.0) +
            cos(firstLatitude) * cos(secondLatitude) *
            sin(longitudeDelta / 2.0) * sin(longitudeDelta / 2.0)
        return EARTH_RADIUS_METERS * 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}

data class DeliveryTrackingWritePayload(
    val shipperId: String,
    val sample: DeliveryTrackingLocationSample,
) {
    fun canonicalFields(
        locationValue: Any,
        recordedAtValue: Any,
        updatedAtValue: Any,
    ): Map<String, Any> {
        return buildMap {
            put(FIELD_SHIPPER_ID, shipperId)
            put(FIELD_LOCATION, locationValue)
            put(FIELD_ACCURACY_METERS, sample.accuracyMeters)
            sample.bearingDegrees?.let { value -> put(FIELD_BEARING_DEGREES, value) }
            sample.speedMetersPerSecond?.let { value -> put(FIELD_SPEED_METERS_PER_SECOND, value) }
            put(FIELD_RECORDED_AT, recordedAtValue)
            put(FIELD_UPDATED_AT, updatedAtValue)
            put(FIELD_ORDER_STATUS, DeliveryTrackingConfig.DELIVERING_STATUS)
            put(FIELD_SCHEMA_VERSION, DeliveryTrackingConfig.TRACKING_SCHEMA_VERSION)
        }
    }

    companion object {
        const val FIELD_SHIPPER_ID = "shipperId"
        const val FIELD_LOCATION = "location"
        const val FIELD_ACCURACY_METERS = "accuracyMeters"
        const val FIELD_BEARING_DEGREES = "bearingDegrees"
        const val FIELD_SPEED_METERS_PER_SECOND = "speedMetersPerSecond"
        const val FIELD_RECORDED_AT = "recordedAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_ORDER_STATUS = "orderStatus"
        const val FIELD_SCHEMA_VERSION = "schemaVersion"

        val REQUIRED_FIELDS = setOf(
            FIELD_SHIPPER_ID,
            FIELD_LOCATION,
            FIELD_ACCURACY_METERS,
            FIELD_RECORDED_AT,
            FIELD_UPDATED_AT,
            FIELD_ORDER_STATUS,
            FIELD_SCHEMA_VERSION,
        )
        val OPTIONAL_FIELDS = setOf(
            FIELD_BEARING_DEGREES,
            FIELD_SPEED_METERS_PER_SECOND,
        )
        val APPROVED_FIELDS = REQUIRED_FIELDS + OPTIONAL_FIELDS
    }
}

object DeliveryTrackingPayloadPolicy {
    fun isValid(payload: DeliveryTrackingWritePayload): Boolean {
        val sample = payload.sample
        return DeliveryTrackingActorIdPolicy.isValid(payload.shipperId) &&
            sample.accuracyMeters.isFinite() &&
            sample.accuracyMeters in 0.0..DeliveryTrackingConfig.MAXIMUM_ACCEPTABLE_ACCURACY_METERS &&
            sample.recordedAtMillis > 0L &&
            sample.bearingDegrees?.let { value -> value.isFinite() && value in 0.0..<360.0 } != false &&
            sample.speedMetersPerSecond?.let { value ->
                value.isFinite() && value in 0.0..DeliveryTrackingConfig.MAXIMUM_SPEED_METERS_PER_SECOND
            } != false
    }
}

object DeliveryTrackingActorIdPolicy {
    private const val MAXIMUM_ACTOR_ID_LENGTH = 128

    fun isValid(actorId: String): Boolean {
        return actorId.isNotBlank() &&
            actorId == actorId.trim() &&
            actorId.length <= MAXIMUM_ACTOR_ID_LENGTH &&
            '/' !in actorId
    }
}
