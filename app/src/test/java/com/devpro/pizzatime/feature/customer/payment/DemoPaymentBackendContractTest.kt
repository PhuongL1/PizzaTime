package com.devpro.pizzatime.feature.customer.payment

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class DemoPaymentBackendContractTest {

    private val backendConfig = checkNotNull(
        validateConfiguredBackendUrl(
            rawValue = "https://demo.example.test",
            isDebugBuild = false,
        ),
    )

    @Test
    fun createPaymentRequest_containsOnlyOrderIdAndRequestId() {
        val payload = JSONObject(
            buildCreatePaymentRequestJson(
                orderId = " order-123 ",
                requestId = " request-456 ",
            ),
        )

        assertEquals(setOf("orderId", "requestId"), payload.keys().asSequence().toSet())
        assertEquals("order-123", payload.getString("orderId"))
        assertEquals("request-456", payload.getString("requestId"))
    }

    @Test
    fun sessionParser_requiresMatchingQrPayloadAndPaymentUrl() {
        val expiresAt = "2026-07-16T10:15:30.000Z"
        val error = runCatching {
            parseDemoPaymentSession(
                responseBody = """
                    {
                      "paymentAttemptId": "PTABCDEFGH12345678",
                      "paymentReference": "PTABCDEFGH12345678",
                      "amountVnd": 123000,
                      "expiresAt": "$expiresAt",
                      "paymentPageUrl": "https://demo.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF",
                      "qrPayload": "https://other.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF"
                    }
                """.trimIndent(),
                backendConfig = backendConfig,
            )
        }.exceptionOrNull() as DemoPaymentBackendException

        assertEquals(DemoPaymentBackendErrorType.CREATE_SESSION_FAILED, error.type)
        assertEquals("QR payload does not match the payment page URL.", error.message)
    }

    @Test
    fun backendFailures_mapToSafeUserFacingCategories() {
        assertEquals(
            DemoPaymentBackendErrorType.SESSION_EXPIRED,
            parseCreatePaymentFailure(401, null).type,
        )
        assertEquals(
            DemoPaymentBackendErrorType.ORDER_NOT_PAYABLE,
            parseCreatePaymentFailure(409, """{"error":{"code":"ORDER_ALREADY_PAID"}}""").type,
        )
        assertEquals(
            DemoPaymentBackendErrorType.AMOUNT_MISMATCH,
            parseCreatePaymentFailure(409, """{"error":{"code":"ORDER_AMOUNT_MISMATCH"}}""").type,
        )
    }

    @Test
    fun utcIsoFormatter_roundTripsPersistedSessionExpiry() {
        val original = Date(1_784_198_400_123L)

        val restored = parseIsoUtcDate(formatIsoUtcDate(original))

        assertEquals(original.time, restored.time)
    }

    @Test
    fun parsedSession_acceptsCanonicalBackendResponse() {
        val session = parseDemoPaymentSession(
            responseBody = """
                {
                  "paymentAttemptId": "PTABCDEFGH12345678",
                  "paymentReference": "PTABCDEFGH12345678",
                  "amountVnd": 123000,
                  "expiresAt": "2026-07-16T10:15:30.000Z",
                  "paymentPageUrl": "https://demo.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF",
                  "qrPayload": "https://demo.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF"
                }
            """.trimIndent(),
            backendConfig = backendConfig,
        )

        assertEquals("PTABCDEFGH12345678", session.paymentAttemptId)
        assertEquals("PTABCDEFGH12345678", session.paymentReference)
        assertEquals(123000, session.amountVnd)
        assertTrue(session.paymentPageUri.toString().startsWith("https://demo.example.test/demo/pay/"))
    }
}
