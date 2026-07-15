package com.devpro.pizzatime.core.notification

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.feature.auth.LoginFragment
import com.devpro.pizzatime.feature.splash.SplashFragment
import com.devpro.pizzatime.feature.staff.navigation.openCustomerNotifications
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openKitchenOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openManageOrders
import com.devpro.pizzatime.feature.staff.navigation.openOrderTracking
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDetail
import com.devpro.pizzatime.feature.staff.navigation.openStaffOrderDetail
import com.devpro.pizzatime.feature.welcome.WelcomeFragment
import org.json.JSONObject

object NotificationDeepLinkCoordinator {

    fun captureIntent(
        context: Context,
        intent: Intent?,
    ) {
        val request = intent.toRequest() ?: return
        savePendingRequest(context, request)
    }

    fun handlePendingRequest(
        context: Context,
        fragmentManager: FragmentManager,
    ): Boolean {
        if (fragmentManager.isStateSaved) return false
        val request = readPendingRequest(context) ?: return false
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer) ?: return false
        val currentScope = NotificationSessionResolver.currentScope()
        if (currentScope == null) {
            if (
                currentFragment !is SplashFragment &&
                currentFragment !is WelcomeFragment &&
                currentFragment !is LoginFragment
            ) {
                currentFragment.openLoginScreen()
            }
            return false
        }

        val notification = NotificationInboxStore.findActiveNotification(request.notificationId)
        val decision = validateNotificationRouting(request, currentScope, notification)
        if (decision != NotificationRoutingDecision.ALLOWED || notification == null) {
            clearPendingRequest(context)
            AppUiMessageBus.publish(
                R.string.notification_destination_unavailable,
                UiMessageType.WARNING,
            )
            Log.d(TAG, "Deep link rejected reason=${decision.name}")
            return false
        }

        return routeNotification(
            context = context,
            currentFragment = currentFragment,
            currentScope = currentScope,
            request = request,
            clearPending = true,
        )
    }

    fun openInboxNotification(
        context: Context,
        fragmentManager: FragmentManager,
        notificationId: String,
    ): Boolean {
        if (fragmentManager.isStateSaved) return false
        val currentScope = NotificationSessionResolver.currentScope() ?: return false
        val notification = NotificationInboxStore.findActiveNotification(notificationId) ?: return false
        val request = notification.toRoutingRequest(currentScope.applicationId) ?: return false
        val decision = validateNotificationRouting(request, currentScope, notification)
        if (decision != NotificationRoutingDecision.ALLOWED) return false
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer) ?: return false
        return routeNotification(
            context = context,
            currentFragment = currentFragment,
            currentScope = currentScope,
            request = request,
            clearPending = false,
        )
    }

    fun openInboxFromCurrentFragment(fragmentManager: FragmentManager): Boolean {
        if (fragmentManager.isStateSaved) return false
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer) as? Fragment ?: return false
        currentFragment.openCustomerNotifications()
        return true
    }

    private fun routeNotification(
        context: Context,
        currentFragment: Fragment,
        currentScope: NotificationScope,
        request: NotificationRoutingRequest,
        clearPending: Boolean,
    ): Boolean {
        if (isAlreadyAtTarget(currentFragment, request)) {
            return completeRouting(context, currentScope, request.notificationId, clearPending)
        }

        val handled = when (request.deepLinkType) {
            NotificationDeepLink.CUSTOMER_ORDER_TRACKING -> request.orderId.routeOrder(
                onAvailable = currentFragment::openOrderTracking,
            )

            NotificationDeepLink.CUSTOMER_ORDER_DETAIL -> request.orderId.routeOrder { orderId ->
                currentFragment.openCustomerOrderDetail(
                    orderId = orderId,
                    isNotificationDestination = true,
                )
            }

            NotificationDeepLink.STAFF_ORDER_DETAIL -> request.orderId.routeOrder(
                onAvailable = currentFragment::openStaffOrderDetail,
            )

            NotificationDeepLink.KITCHEN_ORDER_DETAIL -> request.orderId.routeOrder(
                onAvailable = currentFragment::openKitchenOrderDetail,
            )

            NotificationDeepLink.SHIPPER_ORDER_DETAIL -> request.orderId.routeOrder(
                onAvailable = currentFragment::openShipperDeliveryDetail,
            )

            NotificationDeepLink.ADMIN_ORDER_DETAIL -> {
                currentFragment.openManageOrders()
                true
            }

            NotificationDeepLink.ADMIN_REVIEW_DETAIL -> {
                currentFragment.openManageMenu()
                true
            }

            NotificationDeepLink.NONE -> true
        }

        if (!handled) {
            AppUiMessageBus.publish(R.string.notification_order_unavailable, UiMessageType.ERROR)
        }
        completeRouting(context, currentScope, request.notificationId, clearPending)
        Log.d(TAG, "Deep link consumed type=${request.deepLinkType.name} routed=$handled")
        return true
    }

    private fun String?.routeOrder(onAvailable: (String) -> Unit): Boolean {
        val orderId = this.orEmpty().trim()
        if (orderId.isBlank()) return false
        onAvailable(orderId)
        return true
    }

    private fun completeRouting(
        context: Context,
        scope: NotificationScope,
        notificationId: String,
        clearPending: Boolean,
    ): Boolean {
        val markedRead = NotificationInboxStore.markRead(scope, notificationId)
        if (clearPending) {
            clearPendingRequest(context)
        }
        return markedRead
    }

    private fun isAlreadyAtTarget(
        fragment: Fragment,
        request: NotificationRoutingRequest,
    ): Boolean {
        val currentOrderId = fragment.arguments?.getString("order_id")
            ?: fragment.arguments?.getString("orderId")
        return when (request.deepLinkType) {
            NotificationDeepLink.CUSTOMER_ORDER_TRACKING ->
                currentOrderId == request.orderId && fragment.javaClass.simpleName == "OrderTrackingFragment"

            NotificationDeepLink.CUSTOMER_ORDER_DETAIL ->
                currentOrderId == request.orderId && fragment.javaClass.simpleName == "CustomerOrderDetailFragment"

            NotificationDeepLink.STAFF_ORDER_DETAIL ->
                currentOrderId == request.orderId && fragment.javaClass.simpleName == "StaffOrderDetailFragment"

            NotificationDeepLink.KITCHEN_ORDER_DETAIL ->
                currentOrderId == request.orderId && fragment.javaClass.simpleName == "KitchenOrderDetailFragment"

            NotificationDeepLink.SHIPPER_ORDER_DETAIL ->
                currentOrderId == request.orderId && fragment.javaClass.simpleName == "ShipperDeliveryDetailFragment"

            NotificationDeepLink.ADMIN_ORDER_DETAIL ->
                fragment.javaClass.simpleName == "ManageOrdersFragment"

            NotificationDeepLink.ADMIN_REVIEW_DETAIL ->
                fragment.javaClass.simpleName == "ManageMenuFragment"

            NotificationDeepLink.NONE -> true
        }
    }

    private fun Intent?.toRequest(): NotificationRoutingRequest? {
        if (this == null || !hasExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_DEEP_LINK)) return null
        val notificationId = getStringExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_ID)
            .sanitizedRoutingValue() ?: return null
        val applicationId = getStringExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_APPLICATION_ID)
            .sanitizedRoutingValue() ?: return null
        val recipientUserId = getStringExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_RECIPIENT_USER_ID)
            .sanitizedRoutingValue() ?: return null
        val recipientRole = runCatching {
            com.devpro.pizzatime.core.session.UserRole.valueOf(
                getStringExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_RECIPIENT_ROLE).orEmpty(),
            )
        }.getOrNull() ?: return null
        val deepLinkType = runCatching {
            NotificationDeepLink.valueOf(
                getStringExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_DEEP_LINK).orEmpty(),
            )
        }.getOrNull() ?: return null
        return NotificationRoutingRequest(
            notificationId = notificationId,
            applicationId = applicationId,
            recipientUserId = recipientUserId,
            recipientRole = recipientRole,
            deepLinkType = deepLinkType,
            orderId = getStringExtra(NotificationDeepLinkContract.EXTRA_ORDER_ID).sanitizedRoutingValue(),
            reviewId = getStringExtra(NotificationDeepLinkContract.EXTRA_REVIEW_ID).sanitizedRoutingValue(),
        )
    }

    private fun savePendingRequest(
        context: Context,
        request: NotificationRoutingRequest,
    ) {
        val payload = JSONObject().apply {
            put("notificationId", request.notificationId)
            put("applicationId", request.applicationId)
            put("recipientUserId", request.recipientUserId)
            put("recipientRole", request.recipientRole.name)
            put("deepLinkType", request.deepLinkType.name)
            put("orderId", request.orderId)
            put("reviewId", request.reviewId)
        }
        prefs(context).edit().putString(PENDING_REQUEST_KEY, payload.toString()).apply()
    }

    private fun readPendingRequest(context: Context): NotificationRoutingRequest? {
        val raw = prefs(context).getString(PENDING_REQUEST_KEY, "").orEmpty()
        if (raw.isBlank()) return null
        val request = runCatching {
            val json = JSONObject(raw)
            NotificationRoutingRequest(
                notificationId = json.optString("notificationId").sanitizedRoutingValue() ?: return@runCatching null,
                applicationId = json.optString("applicationId").sanitizedRoutingValue() ?: return@runCatching null,
                recipientUserId = json.optString("recipientUserId").sanitizedRoutingValue() ?: return@runCatching null,
                recipientRole = com.devpro.pizzatime.core.session.UserRole.valueOf(json.optString("recipientRole")),
                deepLinkType = NotificationDeepLink.valueOf(json.optString("deepLinkType")),
                orderId = json.optString("orderId").sanitizedRoutingValue(),
                reviewId = json.optString("reviewId").sanitizedRoutingValue(),
            )
        }.getOrNull()
        if (request == null) {
            clearPendingRequest(context)
        }
        return request
    }

    fun clearPendingRequest(context: Context) {
        prefs(context).edit().remove(PENDING_REQUEST_KEY).apply()
    }

    private fun String?.sanitizedRoutingValue(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() && it.length <= MAX_ROUTING_VALUE_LENGTH }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "pizza_time_notification_routing"
    private const val PENDING_REQUEST_KEY = "pending_notification_request"
    private const val MAX_ROUTING_VALUE_LENGTH = 512
    private const val TAG = "NotificationDeepLink"
}
