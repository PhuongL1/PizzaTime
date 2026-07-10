package com.devpro.pizzatime.core.notification

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.feature.auth.LoginFragment
import com.devpro.pizzatime.feature.splash.SplashFragment
import com.devpro.pizzatime.feature.staff.navigation.openCustomerNotifications
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen
import com.devpro.pizzatime.feature.staff.navigation.openKitchenOrderDetail
import com.devpro.pizzatime.feature.staff.navigation.openManageOrders
import com.devpro.pizzatime.feature.staff.navigation.openOrderTracking
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDetail
import com.devpro.pizzatime.feature.staff.navigation.openStaffOrderDetail
import com.devpro.pizzatime.feature.welcome.WelcomeFragment
import com.google.firebase.auth.FirebaseAuth
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
        val request = readPendingRequest(context) ?: return false
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer) ?: return false
        val currentRole = NotificationSessionResolver.currentRole()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null || currentRole == UserRole.GUEST) {
            if (currentFragment !is SplashFragment && currentFragment !is WelcomeFragment && currentFragment !is LoginFragment) {
                currentFragment.openLoginScreen()
            }
            return false
        }

        if (request.recipientRole != currentRole) {
            clearPendingRequest(context)
            Log.d(TAG, "Deep link dropped role=${currentRole.name} expected=${request.recipientRole.name}")
            return false
        }

        if (isAlreadyAtTarget(currentFragment, request)) {
            NotificationInboxStore.markRead(request.notificationId)
            clearPendingRequest(context)
            return true
        }

        val handled = when (request.deepLinkType) {
            NotificationDeepLink.CUSTOMER_ORDER_TRACKING -> {
                val orderId = request.orderId.orEmpty()
                if (orderId.isBlank()) return false
                currentFragment.openOrderTracking(orderId)
                true
            }

            NotificationDeepLink.CUSTOMER_ORDER_DETAIL -> {
                val orderId = request.orderId.orEmpty()
                if (orderId.isBlank()) return false
                currentFragment.openCustomerOrderDetail(orderId)
                true
            }

            NotificationDeepLink.STAFF_ORDER_DETAIL -> {
                val orderId = request.orderId.orEmpty()
                if (orderId.isBlank()) return false
                currentFragment.openStaffOrderDetail(orderId)
                true
            }

            NotificationDeepLink.KITCHEN_ORDER_DETAIL -> {
                val orderId = request.orderId.orEmpty()
                if (orderId.isBlank()) return false
                currentFragment.openKitchenOrderDetail(orderId)
                true
            }

            NotificationDeepLink.SHIPPER_ORDER_DETAIL -> {
                val orderId = request.orderId.orEmpty()
                if (orderId.isBlank()) return false
                currentFragment.openShipperDeliveryDetail(orderId)
                true
            }

            NotificationDeepLink.ADMIN_ORDER_DETAIL -> {
                val orderId = request.orderId.orEmpty()
                if (orderId.isBlank()) return false
                currentFragment.openStaffOrderDetail(orderId)
                true
            }

            NotificationDeepLink.ADMIN_REVIEW_DETAIL -> {
                val orderId = request.orderId.orEmpty()
                if (orderId.isNotBlank()) {
                    currentFragment.openStaffOrderDetail(orderId)
                } else {
                    currentFragment.openManageOrders()
                }
                true
            }

            NotificationDeepLink.NONE -> true
        }

        if (handled) {
            NotificationInboxStore.markRead(request.notificationId)
            clearPendingRequest(context)
            Log.d(TAG, "Deep link handled type=${request.deepLinkType.name}")
        }
        return handled
    }

    fun openInboxFromCurrentFragment(fragmentManager: FragmentManager): Boolean {
        val currentFragment = fragmentManager.findFragmentById(R.id.fragmentContainer) as? Fragment ?: return false
        currentFragment.openCustomerNotifications()
        return true
    }

    private fun isAlreadyAtTarget(
        fragment: Fragment,
        request: NotificationRoutingRequest,
    ): Boolean {
        val currentOrderId = fragment.arguments?.getString("order_id")
            ?: fragment.arguments?.getString("orderId")
        return currentOrderId == request.orderId && when (request.deepLinkType) {
            NotificationDeepLink.CUSTOMER_ORDER_TRACKING ->
                fragment.javaClass.simpleName == "OrderTrackingFragment"

            NotificationDeepLink.CUSTOMER_ORDER_DETAIL ->
                fragment.javaClass.simpleName == "CustomerOrderDetailFragment"

            NotificationDeepLink.STAFF_ORDER_DETAIL,
            NotificationDeepLink.ADMIN_ORDER_DETAIL,
            NotificationDeepLink.ADMIN_REVIEW_DETAIL,
            -> fragment.javaClass.simpleName == "StaffOrderDetailFragment"

            NotificationDeepLink.KITCHEN_ORDER_DETAIL ->
                fragment.javaClass.simpleName == "KitchenOrderDetailFragment"

            NotificationDeepLink.SHIPPER_ORDER_DETAIL ->
                fragment.javaClass.simpleName == "ShipperDeliveryDetailFragment"

            NotificationDeepLink.NONE -> true
        }
    }

    private fun Intent?.toRequest(): NotificationRoutingRequest? {
        if (this == null || !hasExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_DEEP_LINK)) {
            return null
        }
        val deepLinkType = runCatching {
            NotificationDeepLink.valueOf(
                getStringExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_DEEP_LINK).orEmpty(),
            )
        }.getOrDefault(NotificationDeepLink.NONE)
        val notificationId = getStringExtra(NotificationDeepLinkContract.EXTRA_NOTIFICATION_ID).orEmpty()
        if (notificationId.isBlank()) {
            return null
        }
        return NotificationRoutingRequest(
            notificationId = notificationId,
            recipientRole = NotificationSessionResolver.currentRole().takeIf { it != UserRole.GUEST }
                ?: defaultRoleForEdition(),
            deepLinkType = deepLinkType,
            orderId = getStringExtra(NotificationDeepLinkContract.EXTRA_ORDER_ID),
            reviewId = getStringExtra(NotificationDeepLinkContract.EXTRA_REVIEW_ID),
        )
    }

    private fun defaultRoleForEdition(): UserRole {
        return when (AppEditionConfig.current) {
            AppEdition.GUEST,
            AppEdition.CUSTOMER,
            -> UserRole.CUSTOMER

            AppEdition.STAFF -> UserRole.STAFF
            AppEdition.KITCHEN -> UserRole.KITCHEN
            AppEdition.SHIPPER -> UserRole.SHIPPER
            AppEdition.ADMIN -> UserRole.ADMIN
        }
    }

    private fun savePendingRequest(
        context: Context,
        request: NotificationRoutingRequest,
    ) {
        val payload = JSONObject().apply {
            put("notificationId", request.notificationId)
            put("recipientRole", request.recipientRole.name)
            put("deepLinkType", request.deepLinkType.name)
            put("orderId", request.orderId)
            put("reviewId", request.reviewId)
        }
        prefs(context).edit().putString(PENDING_REQUEST_KEY, payload.toString()).apply()
    }

    private fun readPendingRequest(context: Context): NotificationRoutingRequest? {
        val raw = prefs(context).getString(PENDING_REQUEST_KEY, "").orEmpty()
        if (raw.isBlank()) {
            return null
        }

        return runCatching {
            val json = JSONObject(raw)
            NotificationRoutingRequest(
                notificationId = json.optString("notificationId"),
                recipientRole = UserRole.valueOf(json.optString("recipientRole", UserRole.CUSTOMER.name)),
                deepLinkType = NotificationDeepLink.valueOf(
                    json.optString("deepLinkType", NotificationDeepLink.NONE.name),
                ),
                orderId = json.optString("orderId").trim().ifBlank { null },
                reviewId = json.optString("reviewId").trim().ifBlank { null },
            )
        }.getOrNull()
    }

    fun clearPendingRequest(context: Context) {
        prefs(context).edit().remove(PENDING_REQUEST_KEY).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "pizza_time_notification_routing"
    private const val PENDING_REQUEST_KEY = "pending_notification_request"
    private const val TAG = "NotificationDispatch"
}
