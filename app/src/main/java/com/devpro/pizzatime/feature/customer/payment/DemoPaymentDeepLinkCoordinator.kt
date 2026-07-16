package com.devpro.pizzatime.feature.customer.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.notification.NotificationSessionResolver
import com.devpro.pizzatime.feature.auth.LoginFragment
import com.devpro.pizzatime.feature.splash.SplashFragment
import com.devpro.pizzatime.feature.staff.navigation.openDemoPayment
import com.devpro.pizzatime.feature.welcome.WelcomeFragment
import org.json.JSONObject

object DemoPaymentDeepLinkCoordinator {

    fun captureIntent(
        context: Context,
        intent: Intent?,
    ) {
        val request = intent?.data.toRequest() ?: return
        val payload = JSONObject()
            .put("orderId", request.orderId)
            .put("paymentAttemptId", request.paymentAttemptId)
        prefs(context).edit().putString(PENDING_REQUEST_KEY, payload.toString()).apply()
    }

    fun handlePendingRequest(
        context: Context,
        fragmentManager: FragmentManager,
    ): Boolean {
        if (fragmentManager.isStateSaved || AppEditionConfig.current != AppEdition.CUSTOMER) {
            return false
        }
        val request = readPendingRequest(context) ?: return false
        val currentFragment = fragmentManager.findFragmentById(com.devpro.pizzatime.R.id.fragmentContainer)
            as? Fragment ?: return false
        val scope = NotificationSessionResolver.currentScope()
        if (scope == null || scope.role != com.devpro.pizzatime.core.session.UserRole.CUSTOMER) {
            return false
        }
        if (currentFragment is SplashFragment || currentFragment is WelcomeFragment || currentFragment is LoginFragment) {
            return false
        }
        if (isAlreadyShowing(currentFragment, request.orderId)) {
            clearPendingRequest(context)
            return true
        }
        currentFragment.openDemoPayment(request.orderId)
        clearPendingRequest(context)
        return true
    }

    fun clearPendingRequest(context: Context) {
        prefs(context).edit().remove(PENDING_REQUEST_KEY).apply()
    }

    private fun readPendingRequest(context: Context): DemoPaymentDeepLinkRequest? {
        val raw = prefs(context).getString(PENDING_REQUEST_KEY, "").orEmpty()
        if (raw.isBlank()) {
            return null
        }
        val request = runCatching {
            val json = JSONObject(raw)
            DemoPaymentDeepLinkRequest(
                orderId = json.optString("orderId").trim(),
                paymentAttemptId = json.optString("paymentAttemptId").trim(),
            )
        }.getOrNull()?.takeIf { request ->
            request.orderId.matches(ORDER_ID_REGEX) && request.paymentAttemptId.matches(PAYMENT_ATTEMPT_REGEX)
        }
        if (request == null) {
            clearPendingRequest(context)
        }
        return request
    }

    private fun isAlreadyShowing(
        currentFragment: Fragment,
        orderId: String,
    ): Boolean {
        return currentFragment.arguments?.getString("arg_order_id") == orderId &&
            currentFragment.javaClass.simpleName == "DemoPaymentFragment"
    }

    private fun Uri?.toRequest(): DemoPaymentDeepLinkRequest? {
        if (this == null) {
            return null
        }
        if (!scheme.equals("pizzatime", ignoreCase = true) || !host.equals("payment-result", ignoreCase = true)) {
            return null
        }
        val orderId = getQueryParameter("orderId")?.trim().orEmpty()
        val paymentAttemptId = getQueryParameter("paymentAttemptId")?.trim().orEmpty()
        if (!orderId.matches(ORDER_ID_REGEX) || !paymentAttemptId.matches(PAYMENT_ATTEMPT_REGEX)) {
            return null
        }
        return DemoPaymentDeepLinkRequest(
            orderId = orderId,
            paymentAttemptId = paymentAttemptId,
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private data class DemoPaymentDeepLinkRequest(
        val orderId: String,
        val paymentAttemptId: String,
    )

    private val ORDER_ID_REGEX = Regex("[A-Za-z0-9-]{4,64}")
    private val PAYMENT_ATTEMPT_REGEX = Regex("PT[A-Z0-9]{16,64}")
    private const val PREFS_NAME = "pizza_time_demo_payment_links"
    private const val PENDING_REQUEST_KEY = "pending_demo_payment_request"
}
