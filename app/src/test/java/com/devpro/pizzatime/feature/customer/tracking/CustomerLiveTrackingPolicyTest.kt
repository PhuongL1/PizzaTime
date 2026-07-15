package com.devpro.pizzatime.feature.customer.tracking

import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerLiveTrackingPolicyTest {

    @Test
    fun `owning customer may observe a delivering order with an assigned shipper`() {
        assertEquals(
            CustomerTrackingObservationState.OBSERVE,
            observationState(),
        )
    }

    @Test
    fun `other customer guest and wrong role are denied by observation scope`() {
        assertEquals(
            CustomerTrackingObservationState.UNAUTHORIZED,
            observationState(authenticatedUid = "customer-2"),
        )
        assertEquals(
            CustomerTrackingObservationState.UNAUTHORIZED,
            observationState(edition = AppEdition.GUEST, sessionRole = UserRole.GUEST),
        )
        assertEquals(
            CustomerTrackingObservationState.UNAUTHORIZED,
            observationState(sessionRole = UserRole.SHIPPER),
        )
    }

    @Test
    fun `non-delivering orders stay idle and delivered or cancelled orders stop observation`() {
        assertEquals(
            CustomerTrackingObservationState.WAITING_FOR_DELIVERY,
            observationState(orderStatus = "ASSIGNED_TO_SHIPPER"),
        )
        assertEquals(
            CustomerTrackingObservationState.WAITING_FOR_DELIVERY,
            observationState(orderStatus = "DELIVERING", assignedShipperId = null),
        )
        assertEquals(
            CustomerTrackingObservationState.DELIVERED,
            observationState(orderStatus = "DELIVERED"),
        )
        assertEquals(
            CustomerTrackingObservationState.CANCELLED,
            observationState(orderStatus = "CANCELLED"),
        )
    }

    @Test
    fun `account switch and view destruction require listener replacement`() {
        val current = CustomerTrackingListenerBinding(
            orderId = "order-80",
            customerId = "customer-1",
        )
        val switchedAccount = CustomerTrackingListenerBinding(
            orderId = "order-80",
            customerId = "customer-2",
        )

        assertEquals(
            true,
            CustomerTrackingListenerPolicy.shouldReplace(current, switchedAccount),
        )
        assertEquals(
            true,
            CustomerTrackingListenerPolicy.shouldReplace(current, null),
        )
        assertEquals(
            false,
            CustomerTrackingListenerPolicy.shouldReplace(current, current),
        )
    }

    @Test
    fun `freshness distinguishes just-now stale and delayed updates`() {
        val now = 10 * 60_000L

        assertEquals(
            CustomerTrackingFreshnessState.FRESH,
            CustomerTrackingFreshnessPolicy.classify(now, now - 30_000L),
        )
        assertEquals(
            CustomerTrackingRelativeTime.JustNow,
            CustomerTrackingFreshnessPolicy.relativeTime(now, now - 30_000L),
        )
        assertEquals(
            CustomerTrackingFreshnessState.STALE,
            CustomerTrackingFreshnessPolicy.classify(
                now,
                now - CustomerTrackingFreshnessPolicy.STALE_LOCATION_THRESHOLD_MILLIS - 1L,
            ),
        )
        assertEquals(
            CustomerTrackingRelativeTime.MinutesAgo(5),
            CustomerTrackingFreshnessPolicy.relativeTime(now, now - 5 * 60_000L),
        )
        assertEquals(
            CustomerTrackingFreshnessState.DELAYED,
            CustomerTrackingFreshnessPolicy.classify(now, null),
        )
    }

    @Test
    fun `invalid tracking payloads are ignored while valid shipper snapshots are parsed`() {
        val valid = CustomerTrackingDocumentParser.parse(
            data = mapOf(
                "shipperId" to "shipper-1",
                "location" to GeoPoint(10.0, 106.0),
                "updatedAt" to Timestamp(Date(60_000L)),
            ),
            expectedShipperId = "shipper-1",
        )
        assertNotNull(valid)
        assertEquals(10.0, valid?.coordinate?.latitude ?: Double.NaN, 0.0)
        assertEquals(106.0, valid?.coordinate?.longitude ?: Double.NaN, 0.0)

        val invalidCoordinate = CustomerTrackingDocumentParser.parse(
            data = mapOf(
                "shipperId" to "shipper-1",
                "location" to "bad-location",
                "updatedAt" to Timestamp(Date(60_000L)),
            ),
            expectedShipperId = "shipper-1",
        )
        assertNotNull(invalidCoordinate)
        assertNull(invalidCoordinate?.coordinate)

        assertNull(
            CustomerTrackingDocumentParser.parse(
                data = mapOf(
                    "shipperId" to "shipper-2",
                    "location" to GeoPoint(10.0, 106.0),
                    "updatedAt" to Timestamp(Date(60_000L)),
                ),
                expectedShipperId = "shipper-1",
            ),
        )
    }

    private fun observationState(
        edition: AppEdition = AppEdition.CUSTOMER,
        sessionLoggedIn: Boolean = true,
        sessionRole: UserRole = UserRole.CUSTOMER,
        authenticatedUid: String? = "customer-1",
        orderCustomerId: String? = "customer-1",
        orderStatus: String? = "DELIVERING",
        assignedShipperId: String? = "shipper-1",
    ): CustomerTrackingObservationState {
        return CustomerTrackingObservationPolicy.resolve(
            edition = edition,
            sessionLoggedIn = sessionLoggedIn,
            sessionRole = sessionRole,
            authenticatedUid = authenticatedUid,
            orderCustomerId = orderCustomerId,
            orderStatus = orderStatus,
            assignedShipperId = assignedShipperId,
        )
    }
}
