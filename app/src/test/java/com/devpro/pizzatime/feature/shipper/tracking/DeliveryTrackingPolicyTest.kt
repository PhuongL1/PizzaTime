package com.devpro.pizzatime.feature.shipper.tracking

import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryTrackingPolicyTest {
    @Test
    fun `local eligibility requires shipper edition authenticated shipper session and Firebase uid`() {
        assertEquals(
            DeliveryTrackingEligibility.Eligible,
            localEligibility(),
        )

        AppEdition.entries
            .filterNot { it == AppEdition.SHIPPER }
            .forEach { edition ->
                val result = localEligibility(edition = edition)
                assertEquals(
                    DeliveryTrackingIneligibility.WRONG_EDITION,
                    (result as DeliveryTrackingEligibility.Ineligible).reason,
                )
            }
        UserRole.entries
            .filterNot { it == UserRole.SHIPPER }
            .forEach { role ->
                val result = localEligibility(sessionRole = role)
                assertEquals(
                    DeliveryTrackingIneligibility.WRONG_SESSION_ROLE,
                    (result as DeliveryTrackingEligibility.Ineligible).reason,
                )
            }
        assertEquals(
            DeliveryTrackingIneligibility.SESSION_NOT_AUTHENTICATED,
            (localEligibility(sessionLoggedIn = false) as DeliveryTrackingEligibility.Ineligible).reason,
        )
        assertEquals(
            DeliveryTrackingIneligibility.FIREBASE_USER_MISSING,
            (localEligibility(authenticatedUid = null) as DeliveryTrackingEligibility.Ineligible).reason,
        )
    }

    @Test
    fun `server eligibility requires active shipper profile matching assignment and delivering status`() {
        assertEquals(
            DeliveryTrackingEligibility.Eligible,
            serverEligibility(),
        )

        val cases = listOf(
            serverEligibility(user = activeShipperUser().copy(exists = false)) to
                DeliveryTrackingIneligibility.USER_PROFILE_MISSING,
            serverEligibility(user = activeShipperUser().copy(active = false)) to
                DeliveryTrackingIneligibility.USER_INACTIVE,
            serverEligibility(user = activeShipperUser().copy(role = "CUSTOMER")) to
                DeliveryTrackingIneligibility.WRONG_SERVER_ROLE,
            serverEligibility(order = deliveringOrder().copy(exists = false)) to
                DeliveryTrackingIneligibility.ORDER_MISSING,
            serverEligibility(order = deliveringOrder().copy(shipperId = "another-shipper")) to
                DeliveryTrackingIneligibility.ASSIGNMENT_MISMATCH,
            serverEligibility(order = deliveringOrder().copy(status = "ASSIGNED_TO_SHIPPER")) to
                DeliveryTrackingIneligibility.ORDER_NOT_DELIVERING,
            serverEligibility(order = deliveringOrder().copy(status = "DELIVERED")) to
                DeliveryTrackingIneligibility.ORDER_NOT_DELIVERING,
            serverEligibility(order = deliveringOrder().copy(status = "CANCELLED")) to
                DeliveryTrackingIneligibility.ORDER_NOT_DELIVERING,
        )

        cases.forEach { (result, reason) ->
            assertEquals(reason, (result as DeliveryTrackingEligibility.Ineligible).reason)
        }
    }

    @Test
    fun `parent observation rejects removed reassigned or non-delivering order`() {
        val statuses = listOf(
            "PENDING",
            "CONFIRMED",
            "PREPARING",
            "BAKING",
            "READY",
            "READY_FOR_DELIVERY",
            "READY_TO_DELIVER",
            "ASSIGNED_TO_SHIPPER",
            "DELIVERED",
            "CANCELLED",
        )
        statuses.forEach { status ->
            val result = DeliveryTrackingEligibilityPolicy.evaluateParentOrder(
                expectedShipperId = SHIPPER_ID,
                order = deliveringOrder().copy(status = status),
            )
            assertEquals(
                DeliveryTrackingIneligibility.ORDER_NOT_DELIVERING,
                (result as DeliveryTrackingEligibility.Ineligible).reason,
            )
        }
        val reassigned = DeliveryTrackingEligibilityPolicy.evaluateParentOrder(
            expectedShipperId = SHIPPER_ID,
            order = deliveringOrder().copy(shipperId = "another-shipper"),
        )
        assertEquals(
            DeliveryTrackingIneligibility.ASSIGNMENT_MISMATCH,
            (reassigned as DeliveryTrackingEligibility.Ineligible).reason,
        )
    }

    @Test
    fun `location validation rejects invalid coordinate timestamp accuracy and stale sample`() {
        val now = 1_000_000L
        assertNull(validSample(now, latitude = 91.0))
        assertNull(validSample(now, longitude = 181.0))
        assertNull(validSample(now, latitude = Double.NaN))
        assertNull(validSample(now, accuracy = Double.POSITIVE_INFINITY))
        assertNull(
            validSample(
                now,
                accuracy = DeliveryTrackingConfig.MAXIMUM_ACCEPTABLE_ACCURACY_METERS + 0.1,
            ),
        )
        assertNull(validSample(now, recordedAt = 0L))
        assertNull(
            validSample(
                now,
                recordedAt = now - DeliveryTrackingConfig.MAXIMUM_SAMPLE_AGE_MILLIS - 1L,
            ),
        )
        assertNull(
            validSample(
                now,
                recordedAt = now + DeliveryTrackingConfig.MAXIMUM_FUTURE_SAMPLE_SKEW_MILLIS + 1L,
            ),
        )
    }

    @Test
    fun `valid sample keeps bounded optional bearing and speed and omits invalid optionals`() {
        val now = 1_000_000L
        val valid = requireNotNull(
            validSample(now, bearing = 359.9, speed = DeliveryTrackingConfig.MAXIMUM_SPEED_METERS_PER_SECOND),
        )
        assertEquals(359.9, valid.bearingDegrees ?: Double.NaN, 0.0)
        assertEquals(
            DeliveryTrackingConfig.MAXIMUM_SPEED_METERS_PER_SECOND,
            valid.speedMetersPerSecond ?: Double.NaN,
            0.0,
        )

        val sanitized = requireNotNull(validSample(now, bearing = 360.0, speed = 100.1))
        assertNull(sanitized.bearingDegrees)
        assertNull(sanitized.speedMetersPerSecond)
    }

    @Test
    fun `throttle writes first sample and moved sample only after minimum interval`() {
        val previous = writtenSample(latitude = 10.0, longitude = 106.0, recordedAt = 1_000L, writtenAt = 1_000L)
        val moved = sample(latitude = 10.001, longitude = 106.0, recordedAt = 20_000L)

        assertTrue(DeliveryTrackingThrottlePolicy.shouldWrite(moved, null, nowMillis = 2_000L))
        assertFalse(
            DeliveryTrackingThrottlePolicy.shouldWrite(
                moved,
                previous,
                nowMillis = previous.writtenAtMillis + DeliveryTrackingConfig.MINIMUM_WRITE_INTERVAL_MILLIS - 1L,
            ),
        )
        assertTrue(
            DeliveryTrackingThrottlePolicy.shouldWrite(
                moved,
                previous,
                nowMillis = previous.writtenAtMillis + DeliveryTrackingConfig.MINIMUM_WRITE_INTERVAL_MILLIS,
            ),
        )
    }

    @Test
    fun `throttle rejects short movement but writes a current sample at maximum interval`() {
        val previous = writtenSample(latitude = 10.0, longitude = 106.0, recordedAt = 1_000L, writtenAt = 1_000L)
        val nearby = sample(latitude = 10.00001, longitude = 106.0, recordedAt = 20_000L)
        assertFalse(
            DeliveryTrackingThrottlePolicy.shouldWrite(
                nearby,
                previous,
                nowMillis = previous.writtenAtMillis + DeliveryTrackingConfig.MINIMUM_WRITE_INTERVAL_MILLIS,
            ),
        )
        assertTrue(
            DeliveryTrackingThrottlePolicy.shouldWrite(
                nearby,
                previous,
                nowMillis = previous.writtenAtMillis + DeliveryTrackingConfig.MAXIMUM_WRITE_INTERVAL_MILLIS,
            ),
        )
    }

    @Test
    fun `throttle rejects duplicate and out-of-order samples`() {
        val previous = writtenSample(latitude = 10.0, longitude = 106.0, recordedAt = 50_000L, writtenAt = 50_000L)
        val old = sample(latitude = 10.01, longitude = 106.0, recordedAt = 49_999L)
        assertFalse(
            DeliveryTrackingThrottlePolicy.shouldWrite(
                old,
                previous,
                nowMillis = previous.writtenAtMillis + DeliveryTrackingConfig.MAXIMUM_WRITE_INTERVAL_MILLIS,
            ),
        )
    }

    @Test
    fun `canonical payload contains approved fields only and omits unavailable optionals`() {
        val payload = DeliveryTrackingWritePayload(
            shipperId = SHIPPER_ID,
            sample = sample(
                latitude = 10.0,
                longitude = 106.0,
                recordedAt = 50_000L,
                bearing = null,
                speed = null,
            ),
        )
        val fields = payload.canonicalFields(
            locationValue = "location-sentinel",
            recordedAtValue = "recorded-at-sentinel",
            updatedAtValue = "server-timestamp-sentinel",
        )

        assertEquals(DeliveryTrackingWritePayload.REQUIRED_FIELDS, fields.keys)
        assertTrue(fields.keys.all { field -> field in DeliveryTrackingWritePayload.APPROVED_FIELDS })
        assertEquals(SHIPPER_ID, fields[DeliveryTrackingWritePayload.FIELD_SHIPPER_ID])
        assertEquals(
            DeliveryTrackingConfig.DELIVERING_STATUS,
            fields[DeliveryTrackingWritePayload.FIELD_ORDER_STATUS],
        )
        assertFalse(fields.containsKey(DeliveryTrackingWritePayload.FIELD_BEARING_DEGREES))
        assertFalse(fields.containsKey(DeliveryTrackingWritePayload.FIELD_SPEED_METERS_PER_SECOND))
    }

    @Test
    fun `canonical payload includes only valid optional fields`() {
        val payload = DeliveryTrackingWritePayload(
            shipperId = SHIPPER_ID,
            sample = sample(
                latitude = 10.0,
                longitude = 106.0,
                recordedAt = 50_000L,
                bearing = 45.0,
                speed = 12.0,
            ),
        )
        val fields = payload.canonicalFields("location", "recorded", "updated")

        assertEquals(DeliveryTrackingWritePayload.APPROVED_FIELDS, fields.keys)
        assertEquals(45.0, fields[DeliveryTrackingWritePayload.FIELD_BEARING_DEGREES])
        assertEquals(12.0, fields[DeliveryTrackingWritePayload.FIELD_SPEED_METERS_PER_SECOND])
        assertTrue(DeliveryTrackingPayloadPolicy.isValid(payload))
    }

    @Test
    fun `payload policy rejects invalid actor accuracy timestamp bearing and speed`() {
        val valid = DeliveryTrackingWritePayload(
            shipperId = SHIPPER_ID,
            sample = sample(10.0, 106.0, recordedAt = 50_000L),
        )
        assertTrue(DeliveryTrackingPayloadPolicy.isValid(valid))
        assertFalse(DeliveryTrackingPayloadPolicy.isValid(valid.copy(shipperId = "another/shipper")))
        assertFalse(
            DeliveryTrackingPayloadPolicy.isValid(
                valid.copy(sample = valid.sample.copy(accuracyMeters = 100.1)),
            ),
        )
        assertFalse(
            DeliveryTrackingPayloadPolicy.isValid(
                valid.copy(sample = valid.sample.copy(recordedAtMillis = 0L)),
            ),
        )
        assertFalse(
            DeliveryTrackingPayloadPolicy.isValid(
                valid.copy(sample = valid.sample.copy(bearingDegrees = 360.0)),
            ),
        )
        assertFalse(
            DeliveryTrackingPayloadPolicy.isValid(
                valid.copy(sample = valid.sample.copy(speedMetersPerSecond = 100.1)),
            ),
        )
    }

    @Test
    fun `order id policy trims input and rejects paths blanks and oversized ids`() {
        assertEquals("order-80", DeliveryTrackingOrderIdPolicy.normalize(" order-80 "))
        assertNull(DeliveryTrackingOrderIdPolicy.normalize(""))
        assertNull(DeliveryTrackingOrderIdPolicy.normalize("orders/order-80"))
        assertNull(DeliveryTrackingOrderIdPolicy.normalize("x".repeat(201)))
    }

    private fun localEligibility(
        edition: AppEdition = AppEdition.SHIPPER,
        sessionLoggedIn: Boolean = true,
        sessionRole: UserRole = UserRole.SHIPPER,
        authenticatedUid: String? = SHIPPER_ID,
    ): DeliveryTrackingEligibility {
        return DeliveryTrackingEligibilityPolicy.evaluateLocal(
            edition = edition,
            sessionLoggedIn = sessionLoggedIn,
            sessionRole = sessionRole,
            authenticatedUid = authenticatedUid,
        )
    }

    private fun serverEligibility(
        user: DeliveryTrackingUserState = activeShipperUser(),
        order: DeliveryTrackingOrderState = deliveringOrder(),
    ): DeliveryTrackingEligibility {
        return DeliveryTrackingEligibilityPolicy.evaluateServer(
            expectedShipperId = SHIPPER_ID,
            user = user,
            order = order,
        )
    }

    private fun activeShipperUser() = DeliveryTrackingUserState(
        exists = true,
        active = true,
        role = "SHIPPER",
    )

    private fun deliveringOrder() = DeliveryTrackingOrderState(
        exists = true,
        shipperId = SHIPPER_ID,
        status = DeliveryTrackingConfig.DELIVERING_STATUS,
    )

    private fun validSample(
        now: Long,
        latitude: Double = 10.0,
        longitude: Double = 106.0,
        accuracy: Double = 20.0,
        recordedAt: Long = now,
        bearing: Double? = null,
        speed: Double? = null,
    ): DeliveryTrackingLocationSample? {
        return DeliveryTrackingLocationPolicy.validate(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            recordedAtMillis = recordedAt,
            nowMillis = now,
            bearingDegrees = bearing,
            speedMetersPerSecond = speed,
        )
    }

    private fun sample(
        latitude: Double,
        longitude: Double,
        recordedAt: Long,
        bearing: Double? = null,
        speed: Double? = null,
    ) = DeliveryTrackingLocationSample(
        coordinate = requireNotNull(DeliveryCoordinate.from(latitude, longitude)),
        accuracyMeters = 20.0,
        recordedAtMillis = recordedAt,
        bearingDegrees = bearing,
        speedMetersPerSecond = speed,
    )

    private fun writtenSample(
        latitude: Double,
        longitude: Double,
        recordedAt: Long,
        writtenAt: Long,
    ) = WrittenDeliveryTrackingSample(
        sample = sample(latitude, longitude, recordedAt),
        writtenAtMillis = writtenAt,
    )

    companion object {
        private const val SHIPPER_ID = "shipper-80"
    }
}
