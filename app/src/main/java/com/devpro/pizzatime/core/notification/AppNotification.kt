package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.session.UserRole

data class AppNotification(
    val id: String,
    val dedupeKey: String,
    val recipientRole: UserRole,
    val recipientUserId: String?,
    val type: NotificationType,
    val title: String,
    val body: String,
    val orderId: String?,
    val reviewId: String?,
    val createdAtMillis: Long,
    val isRead: Boolean,
    val deepLinkType: NotificationDeepLink,
)

enum class NotificationType {
    CUSTOMER_PAYMENT_RECEIVED,
    CUSTOMER_ORDER_CONFIRMED,
    CUSTOMER_ORDER_STATUS_UPDATED,
    CUSTOMER_ORDER_PREPARING,
    CUSTOMER_ORDER_READY,
    CUSTOMER_DELIVERY_STARTED,
    CUSTOMER_ORDER_ARRIVED,
    CUSTOMER_ORDER_DELIVERED,
    CUSTOMER_ORDER_CANCELLED,
    STAFF_NEW_ORDER,
    KITCHEN_CONFIRMED_ORDER,
    SHIPPER_READY_ORDER,
    SHIPPER_CUSTOMER_CONFIRMED_RECEIPT,
    ADMIN_ORDER_DELIVERED,
    ADMIN_ORDER_CANCELLED,
    ADMIN_ORDER_REVIEW,
    ADMIN_PRODUCT_REVIEW,
}

enum class NotificationDeepLink {
    CUSTOMER_ORDER_TRACKING,
    CUSTOMER_ORDER_DETAIL,
    STAFF_ORDER_DETAIL,
    KITCHEN_ORDER_DETAIL,
    SHIPPER_ORDER_DETAIL,
    ADMIN_ORDER_DETAIL,
    ADMIN_REVIEW_DETAIL,
    NONE,
}

data class NotificationScope(
    val applicationId: String,
    val userId: String,
    val role: UserRole,
)

data class NotificationRoutingRequest(
    val notificationId: String,
    val applicationId: String,
    val recipientUserId: String,
    val recipientRole: UserRole,
    val deepLinkType: NotificationDeepLink,
    val orderId: String?,
    val reviewId: String?,
)

data class OrderNotificationState(
    val status: String,
    val updatedAtMillis: Long,
    val latestHistoryAtMillis: Long,
    val paymentStatus: String = "",
    val paymentAttemptId: String = "",
    val paidAtMillis: Long = 0L,
    val handoffStatus: String = "",
    val latestHandoffAtMillis: Long = 0L,
)

object NotificationDeepLinkContract {
    const val EXTRA_NOTIFICATION_DEEP_LINK = "EXTRA_NOTIFICATION_DEEP_LINK"
    const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"
    const val EXTRA_REVIEW_ID = "EXTRA_REVIEW_ID"
    const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    const val EXTRA_NOTIFICATION_APPLICATION_ID = "EXTRA_NOTIFICATION_APPLICATION_ID"
    const val EXTRA_NOTIFICATION_RECIPIENT_USER_ID = "EXTRA_NOTIFICATION_RECIPIENT_USER_ID"
    const val EXTRA_NOTIFICATION_RECIPIENT_ROLE = "EXTRA_NOTIFICATION_RECIPIENT_ROLE"
}

object NotificationDefaults {
    const val MAX_INBOX_SIZE = 200
    const val MAX_DEDUPE_KEYS = 400
    const val MAX_REASON_LENGTH = 100
}
