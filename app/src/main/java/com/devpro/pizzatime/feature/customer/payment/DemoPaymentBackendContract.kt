package com.devpro.pizzatime.feature.customer.payment

import android.net.Uri
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DemoPaymentSession(
    val paymentAttemptId: String,
    val paymentReference: String,
    val amountVnd: Int,
    val expiresAt: Date,
    val paymentPageUrl: String,
    val paymentPageUri: Uri,
    val qrPayload: String,
)

enum class DemoPaymentBackendErrorType {
    SERVICE_UNAVAILABLE,
    SESSION_EXPIRED,
    ORDER_NOT_PAYABLE,
    AMOUNT_MISMATCH,
    CREATE_SESSION_FAILED,
}

class DemoPaymentBackendException(
    val type: DemoPaymentBackendErrorType,
    override val message: String,
) : Exception(message)

internal fun buildCreatePaymentRequestJson(
    orderId: String,
    requestId: String,
): String {
    return JSONObject()
        .put("orderId", orderId.trim())
        .put("requestId", requestId.trim())
        .toString()
}

internal fun parseDemoPaymentSession(
    responseBody: String,
    backendConfig: DemoPaymentBackendConfigValue,
): DemoPaymentSession {
    val json = JSONObject(responseBody)
    val paymentAttemptId = json.optString("paymentAttemptId").trim()
    val paymentReference = json.optString("paymentReference").trim()
    val amountVnd = json.optInt("amountVnd", -1)
    val expiresAtRaw = json.optString("expiresAt").trim()
    val paymentPageUrl = json.optString("paymentPageUrl").trim()
    val qrPayload = json.optString("qrPayload").trim()

    if (!paymentAttemptId.matches(Regex("PT[A-Z0-9]{16,64}"))) {
        throw DemoPaymentBackendException(
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
            "Payment attempt id is invalid.",
        )
    }
    if (paymentReference != paymentAttemptId) {
        throw DemoPaymentBackendException(
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
            "Payment reference does not match the attempt id.",
        )
    }
    if (amountVnd <= 0) {
        throw DemoPaymentBackendException(
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
            "Payment amount is invalid.",
        )
    }

    val expiresAt = runCatching { parseIsoUtcDate(expiresAtRaw) }
        .getOrElse {
            throw DemoPaymentBackendException(
                DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
                "Payment expiration is invalid.",
            )
        }

    val paymentPageUri = validateDemoPaymentPageUri(paymentPageUrl, backendConfig)
        ?: throw DemoPaymentBackendException(
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
            "Payment page URL is invalid.",
        )
    if (qrPayload != paymentPageUrl) {
        throw DemoPaymentBackendException(
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
            "QR payload does not match the payment page URL.",
        )
    }

    return DemoPaymentSession(
        paymentAttemptId = paymentAttemptId,
        paymentReference = paymentReference,
        amountVnd = amountVnd,
        expiresAt = expiresAt,
        paymentPageUrl = paymentPageUrl,
        paymentPageUri = paymentPageUri,
        qrPayload = qrPayload,
    )
}

internal fun parseCreatePaymentFailure(
    statusCode: Int,
    responseBody: String?,
): DemoPaymentBackendException {
    if (statusCode == 401) {
        return DemoPaymentBackendException(
            DemoPaymentBackendErrorType.SESSION_EXPIRED,
            "Your session has expired. Please sign in again.",
        )
    }

    val errorCode = responseBody
        ?.takeIf { body -> body.isNotBlank() }
        ?.let(::parseBackendErrorCode)
        .orEmpty()

    return when (errorCode) {
        "ORDER_NOT_PAYABLE",
        "ORDER_NOT_FOUND",
        "ORDER_ALREADY_PAID",
        "PAYMENT_ATTEMPT_FINALIZED",
        -> DemoPaymentBackendException(
            DemoPaymentBackendErrorType.ORDER_NOT_PAYABLE,
            "This order cannot be paid.",
        )

        "ORDER_PRICING_INVALID",
        "ORDER_AMOUNT_MISMATCH",
        -> DemoPaymentBackendException(
            DemoPaymentBackendErrorType.AMOUNT_MISMATCH,
            "The payment amount could not be verified.",
        )

        else -> DemoPaymentBackendException(
            DemoPaymentBackendErrorType.CREATE_SESSION_FAILED,
            if (statusCode >= 500) {
                "Payment service is unavailable."
            } else {
                "Unable to create a payment session."
            },
        )
    }
}

private fun parseBackendErrorCode(responseBody: String): String? {
    return try {
        JSONObject(responseBody)
            .optJSONObject("error")
            ?.optString("code")
            ?.trim()
            ?.takeIf { code -> code.isNotBlank() }
    } catch (_: JSONException) {
        null
    }
}

internal fun parseIsoUtcDate(value: String): Date {
    val trimmed = value.trim()
    val parsers = isoUtcParsers()
    parsers.forEach { parser ->
        parser.parse(trimmed)?.let { parsed ->
            return parsed
        }
    }
    throw IllegalArgumentException("Invalid UTC date value.")
}

internal fun formatIsoUtcDate(value: Date): String {
    return isoUtcParsers().first().format(value)
}

private fun isoUtcParsers(): List<SimpleDateFormat> {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
    )
    return formats.map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
    }
}
