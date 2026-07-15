package com.devpro.pizzatime.core.notification

import android.annotation.SuppressLint
import com.devpro.pizzatime.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@Suppress("DEPRECATION")
class PizzaTimeFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenRegistrar.init(applicationContext)
        FcmTokenRegistrar.saveTokenForCurrentUser(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        FcmTokenRegistrar.init(applicationContext)
        NotificationInboxStore.init(applicationContext)
        NotificationStateStore.init(applicationContext)
        NotificationDispatcher.init(applicationContext)
        PizzaTimeNotificationManager.init(applicationContext)
        val scope = NotificationSessionResolver.currentScope() ?: return
        val notification = message.toNotification(scope) ?: return
        NotificationDispatcher.dispatch(notification, TAG)
    }

    @SuppressLint("DiscouragedApi")
    private fun RemoteMessage.toNotification(scope: NotificationScope): AppNotification? {
        val type = runCatching {
            NotificationType.valueOf(data["type"].orEmpty())
        }.getOrNull() ?: return null
        val title = data["title"] ?: notification?.title ?: return null
        val body = data["body"] ?: notification?.body ?: return null
        val dedupeKey = data["dedupeKey"].orEmpty().ifBlank {
            "remote:${type.name}:${data["orderId"].orEmpty()}:${data["reviewId"].orEmpty()}:${sentTime}"
        }
        return AppNotification(
            id = data["notificationId"].orEmpty().ifBlank { dedupeKey },
            dedupeKey = dedupeKey,
            recipientRole = scope.role,
            recipientUserId = FirebaseAuth.getInstance().currentUser?.uid,
            type = type,
            title = title,
            body = body,
            orderId = data["orderId"]?.trim()?.ifBlank { null },
            reviewId = data["reviewId"]?.trim()?.ifBlank { null },
            createdAtMillis = if (sentTime > 0L) sentTime else System.currentTimeMillis(),
            isRead = false,
            deepLinkType = runCatching {
                NotificationDeepLink.valueOf(
                    data["deepLinkType"].orEmpty().ifBlank { defaultDeepLinkFor(type).name },
                )
            }.getOrDefault(defaultDeepLinkFor(type)),
        )
    }

    private fun defaultDeepLinkFor(type: NotificationType): NotificationDeepLink {
        return when (type) {
            NotificationType.CUSTOMER_ORDER_DELIVERED,
            NotificationType.CUSTOMER_ORDER_CANCELLED,
            -> NotificationDeepLink.CUSTOMER_ORDER_DETAIL

            NotificationType.CUSTOMER_ORDER_CONFIRMED,
            NotificationType.CUSTOMER_ORDER_STATUS_UPDATED,
            NotificationType.CUSTOMER_ORDER_PREPARING,
            NotificationType.CUSTOMER_ORDER_READY,
            NotificationType.CUSTOMER_DELIVERY_STARTED,
            -> NotificationDeepLink.CUSTOMER_ORDER_TRACKING

            NotificationType.STAFF_NEW_ORDER -> NotificationDeepLink.STAFF_ORDER_DETAIL
            NotificationType.KITCHEN_CONFIRMED_ORDER -> NotificationDeepLink.KITCHEN_ORDER_DETAIL
            NotificationType.SHIPPER_READY_ORDER -> NotificationDeepLink.SHIPPER_ORDER_DETAIL
            NotificationType.ADMIN_ORDER_DELIVERED,
            NotificationType.ADMIN_ORDER_CANCELLED,
            -> NotificationDeepLink.ADMIN_ORDER_DETAIL

            NotificationType.ADMIN_ORDER_REVIEW,
            NotificationType.ADMIN_PRODUCT_REVIEW,
            -> NotificationDeepLink.ADMIN_REVIEW_DETAIL
        }
    }

    companion object {
        private const val TAG = "PizzaTimeFCM"
    }
}
