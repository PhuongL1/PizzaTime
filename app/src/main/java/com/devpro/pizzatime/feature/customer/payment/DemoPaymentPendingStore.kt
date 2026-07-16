package com.devpro.pizzatime.feature.customer.payment

import android.content.Context
import org.json.JSONObject
import java.util.UUID

data class DemoPaymentPendingState(
    val orderId: String,
    val requestId: String,
    val paymentAttemptId: String? = null,
    val paymentReference: String? = null,
    val expiresAtIso: String? = null,
    val amountVnd: Int? = null,
)

data class DemoPaymentSuccessNavigationState(
    val orderId: String,
    val paymentAttemptId: String,
)

object DemoPaymentPendingStore {

    fun activeForUser(
        context: Context,
        userId: String,
    ): DemoPaymentPendingState? {
        val raw = prefs(context).getString(activeKey(userId), "").orEmpty()
        if (raw.isBlank()) {
            return null
        }
        return runCatching {
            val json = JSONObject(raw)
            DemoPaymentPendingState(
                orderId = json.optString("orderId").trim(),
                requestId = json.optString("requestId").trim(),
                paymentAttemptId = json.optString("paymentAttemptId").trim().ifBlank { null },
                paymentReference = json.optString("paymentReference").trim().ifBlank { null },
                expiresAtIso = json.optString("expiresAtIso").trim().ifBlank { null },
                amountVnd = json.optInt("amountVnd").takeIf { amount -> amount > 0 },
            )
        }.getOrNull()?.takeIf { state ->
            state.orderId.isNotBlank() && state.requestId.isNotBlank()
        }
    }

    fun ensureActiveState(
        context: Context,
        userId: String,
        orderId: String,
    ): DemoPaymentPendingState {
        val existing = activeForUser(context, userId)
        if (existing != null && existing.orderId == orderId) {
            return existing
        }
        val created = DemoPaymentPendingState(
            orderId = orderId,
            requestId = UUID.randomUUID().toString(),
        )
        saveActiveState(context, userId, created)
        return created
    }

    fun createNewAttemptState(
        context: Context,
        userId: String,
        orderId: String,
    ): DemoPaymentPendingState {
        val created = DemoPaymentPendingState(
            orderId = orderId,
            requestId = UUID.randomUUID().toString(),
        )
        saveActiveState(context, userId, created)
        return created
    }

    fun updateSession(
        context: Context,
        userId: String,
        orderId: String,
        session: DemoPaymentSession,
    ) {
        val current = activeForUser(context, userId)
        val updated = DemoPaymentPendingState(
            orderId = orderId,
            requestId = current?.takeIf { state -> state.orderId == orderId }?.requestId
                ?: UUID.randomUUID().toString(),
            paymentAttemptId = session.paymentAttemptId,
            paymentReference = session.paymentReference,
            expiresAtIso = formatIsoUtcDate(session.expiresAt),
            amountVnd = session.amountVnd,
        )
        saveActiveState(context, userId, updated)
    }

    fun clearActiveIfMatches(
        context: Context,
        userId: String,
        orderId: String,
    ) {
        val current = activeForUser(context, userId) ?: return
        if (current.orderId != orderId) {
            return
        }
        prefs(context).edit().remove(activeKey(userId)).apply()
    }

    fun markSuccessNavigationPending(
        context: Context,
        userId: String,
        orderId: String,
        paymentAttemptId: String,
    ): Boolean {
        val existing = successNavigationPending(context, userId)
        if (existing?.orderId == orderId && existing.paymentAttemptId == paymentAttemptId) {
            return false
        }
        val payload = JSONObject()
            .put("orderId", orderId)
            .put("paymentAttemptId", paymentAttemptId)
        prefs(context).edit()
            .putString(successKey(userId), payload.toString())
            .remove(activeKey(userId))
            .apply()
        return true
    }

    fun successNavigationPending(
        context: Context,
        userId: String,
    ): DemoPaymentSuccessNavigationState? {
        val raw = prefs(context).getString(successKey(userId), "").orEmpty()
        if (raw.isBlank()) {
            return null
        }
        return runCatching {
            val json = JSONObject(raw)
            DemoPaymentSuccessNavigationState(
                orderId = json.optString("orderId").trim(),
                paymentAttemptId = json.optString("paymentAttemptId").trim(),
            )
        }.getOrNull()?.takeIf { state ->
            state.orderId.isNotBlank() && state.paymentAttemptId.isNotBlank()
        }
    }

    fun clearSuccessNavigation(
        context: Context,
        userId: String,
        orderId: String,
        paymentAttemptId: String,
    ) {
        val current = successNavigationPending(context, userId) ?: return
        if (current.orderId != orderId || current.paymentAttemptId != paymentAttemptId) {
            return
        }
        prefs(context).edit().remove(successKey(userId)).apply()
    }

    private fun saveActiveState(
        context: Context,
        userId: String,
        state: DemoPaymentPendingState,
    ) {
        val payload = JSONObject()
            .put("orderId", state.orderId)
            .put("requestId", state.requestId)
            .put("paymentAttemptId", state.paymentAttemptId)
            .put("paymentReference", state.paymentReference)
            .put("expiresAtIso", state.expiresAtIso)
            .put("amountVnd", state.amountVnd ?: 0)
        prefs(context).edit().putString(activeKey(userId), payload.toString()).apply()
    }

    private fun activeKey(userId: String): String = "demo_payment_active_${userId.trim()}"

    private fun successKey(userId: String): String = "demo_payment_success_${userId.trim()}"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "pizza_time_demo_payment"
}
