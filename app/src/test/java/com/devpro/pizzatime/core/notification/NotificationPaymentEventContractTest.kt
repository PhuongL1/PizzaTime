package com.devpro.pizzatime.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationPaymentEventContractTest {

    @Test
    fun paymentDedupeKey_isStableAndAttemptScoped() {
        val first = canonicalPaymentNotificationDedupeKey(
            orderId = "order-123",
            paymentAttemptId = "PTABCDEFGH12345678",
        )

        assertEquals(
            "payment:order-123:paid:PTABCDEFGH12345678",
            first,
        )
        assertEquals(
            first,
            canonicalPaymentNotificationDedupeKey(
                orderId = "order-123",
                paymentAttemptId = "PTABCDEFGH12345678",
            ),
        )
        assertNotEquals(
            first,
            canonicalPaymentNotificationDedupeKey(
                orderId = "order-123",
                paymentAttemptId = "PTABCDEFGH87654321",
            ),
        )
    }
}
