package com.devpro.pizzatime.feature.customer.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class DemoPaymentPendingStoreTest {

    private val context = RuntimeEnvironment.getApplication().applicationContext
    private val userId = "customer-123"

    @Test
    fun ensureActiveState_reusesExistingRequestIdForSameOrder() {
        DemoPaymentPendingStore.clearActiveIfMatches(context, userId, "order-1")

        val first = DemoPaymentPendingStore.ensureActiveState(context, userId, "order-1")
        val second = DemoPaymentPendingStore.ensureActiveState(context, userId, "order-1")

        assertEquals(first.requestId, second.requestId)
    }

    @Test
    fun createNewAttemptState_replacesRequestIdForIntentionalRetry() {
        DemoPaymentPendingStore.clearActiveIfMatches(context, userId, "order-2")

        val first = DemoPaymentPendingStore.ensureActiveState(context, userId, "order-2")
        val retried = DemoPaymentPendingStore.createNewAttemptState(context, userId, "order-2")

        assertNotEquals(first.requestId, retried.requestId)
    }

    @Test
    fun updateSession_persistsIsoExpiryAndSessionFields() {
        DemoPaymentPendingStore.clearActiveIfMatches(context, userId, "order-3")
        DemoPaymentPendingStore.ensureActiveState(context, userId, "order-3")
        val session = DemoPaymentSession(
            paymentAttemptId = "PTABCDEFGH12345678",
            paymentReference = "PTABCDEFGH12345678",
            amountVnd = 123000,
            expiresAt = Date(1_784_198_400_123L),
            paymentPageUrl = "https://demo.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF",
            paymentPageUri = checkNotNull(
                validateConfiguredBackendUrl(
                    rawValue = "https://demo.example.test",
                    isDebugBuild = false,
                ),
            ).baseUri.buildUpon().appendPath("demo").appendPath("pay")
                .appendPath("abcdefghijklmnopqrstuvwxyzABCDEF").build(),
            qrPayload = "https://demo.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF",
        )

        DemoPaymentPendingStore.updateSession(context, userId, "order-3", session)

        val restored = checkNotNull(DemoPaymentPendingStore.activeForUser(context, userId))
        assertEquals("PTABCDEFGH12345678", restored.paymentAttemptId)
        assertEquals("PTABCDEFGH12345678", restored.paymentReference)
        assertEquals(123000, restored.amountVnd)
        assertTrue(restored.expiresAtIso.orEmpty().contains("T"))
        assertEquals(session.expiresAt.time, parseIsoUtcDate(restored.expiresAtIso.orEmpty()).time)
    }
}
