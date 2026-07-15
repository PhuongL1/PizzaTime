package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.Timestamp
import java.util.Date
import java.util.Locale

internal fun normalizeNotificationStatus(status: String?): String {
    return when (status.orEmpty().trim().uppercase(Locale.US)) {
        "READY", "READY_TO_DELIVER" -> "READY_FOR_DELIVERY"
        else -> status.orEmpty().trim().uppercase(Locale.US)
    }
}

internal fun notificationEpochMillis(value: Any?): Long {
    val rawMillis = when (value) {
        is Timestamp -> value.seconds * MILLIS_PER_SECOND + value.nanoseconds / NANOS_PER_MILLI
        is Date -> value.time
        is Number -> normalizeNumericEpoch(value.toLong())
        is String -> value.trim().toLongOrNull()?.let(::normalizeNumericEpoch) ?: 0L
        else -> 0L
    }
    return rawMillis.coerceAtLeast(0L)
}

private fun normalizeNumericEpoch(value: Long): Long {
    return if (value in 1..MAX_EPOCH_SECONDS) value * MILLIS_PER_SECOND else value
}

internal fun notificationTypeForOrderTransition(
    role: UserRole,
    status: String,
): NotificationType? {
    return when (role) {
        UserRole.CUSTOMER -> when (status) {
            "CONFIRMED" -> NotificationType.CUSTOMER_ORDER_CONFIRMED
            "PREPARING", "BAKING" -> NotificationType.CUSTOMER_ORDER_PREPARING
            "READY_FOR_DELIVERY" -> NotificationType.CUSTOMER_ORDER_READY
            "ASSIGNED_TO_SHIPPER" -> NotificationType.CUSTOMER_ORDER_STATUS_UPDATED
            "DELIVERING" -> NotificationType.CUSTOMER_DELIVERY_STARTED
            "DELIVERED" -> NotificationType.CUSTOMER_ORDER_DELIVERED
            "CANCELLED" -> NotificationType.CUSTOMER_ORDER_CANCELLED
            else -> null
        }

        UserRole.STAFF -> if (status == "PENDING") NotificationType.STAFF_NEW_ORDER else null
        UserRole.KITCHEN -> if (status == "CONFIRMED") NotificationType.KITCHEN_CONFIRMED_ORDER else null
        UserRole.SHIPPER -> if (status == "READY_FOR_DELIVERY") NotificationType.SHIPPER_READY_ORDER else null
        UserRole.ADMIN -> when (status) {
            "DELIVERED" -> NotificationType.ADMIN_ORDER_DELIVERED
            "CANCELLED" -> NotificationType.ADMIN_ORDER_CANCELLED
            else -> null
        }

        UserRole.GUEST -> null
    }
}

internal fun notificationDeepLinkForOrder(
    role: UserRole,
    status: String,
): NotificationDeepLink {
    return when (role) {
        UserRole.CUSTOMER -> when (status) {
            "DELIVERED", "CANCELLED" -> NotificationDeepLink.CUSTOMER_ORDER_DETAIL
            else -> NotificationDeepLink.CUSTOMER_ORDER_TRACKING
        }

        UserRole.STAFF -> NotificationDeepLink.STAFF_ORDER_DETAIL
        UserRole.KITCHEN -> NotificationDeepLink.KITCHEN_ORDER_DETAIL
        UserRole.SHIPPER -> NotificationDeepLink.SHIPPER_ORDER_DETAIL
        UserRole.ADMIN -> NotificationDeepLink.ADMIN_ORDER_DETAIL
        UserRole.GUEST -> NotificationDeepLink.NONE
    }
}

internal fun canonicalOrderNotificationDedupeKey(
    role: UserRole,
    orderId: String,
    status: String,
    eventMillis: Long,
): String {
    return if (role == UserRole.STAFF) {
        "staff:new-order:$orderId:$eventMillis"
    } else {
        "order:$orderId:status:$status:$eventMillis"
    }
}

internal fun canonicalReviewNotificationDedupeKey(reviewId: String): String {
    return "review:$reviewId"
}

internal fun notificationHistoryEventsAfter(
    events: List<OrderHistoryEvent>,
    lastSeenMillis: Long,
): List<OrderHistoryEvent> {
    return events.filter { event -> event.createdAtMillis > lastSeenMillis }
}

internal fun shouldSeedNotificationState(lastSyncAt: Long): Boolean {
    return lastSyncAt <= 0L
}

internal fun shouldRunNotificationWork(
    scheduledScope: NotificationScope,
    currentScope: NotificationScope?,
): Boolean {
    return currentScope != null && scheduledScope == currentScope
}

internal fun isDeepLinkAllowedForRole(
    role: UserRole,
    deepLink: NotificationDeepLink,
): Boolean {
    return when (role) {
        UserRole.CUSTOMER -> deepLink == NotificationDeepLink.CUSTOMER_ORDER_TRACKING ||
            deepLink == NotificationDeepLink.CUSTOMER_ORDER_DETAIL ||
            deepLink == NotificationDeepLink.NONE

        UserRole.STAFF -> deepLink == NotificationDeepLink.STAFF_ORDER_DETAIL ||
            deepLink == NotificationDeepLink.NONE

        UserRole.KITCHEN -> deepLink == NotificationDeepLink.KITCHEN_ORDER_DETAIL ||
            deepLink == NotificationDeepLink.NONE

        UserRole.SHIPPER -> deepLink == NotificationDeepLink.SHIPPER_ORDER_DETAIL ||
            deepLink == NotificationDeepLink.NONE

        UserRole.ADMIN -> deepLink == NotificationDeepLink.ADMIN_ORDER_DETAIL ||
            deepLink == NotificationDeepLink.ADMIN_REVIEW_DETAIL ||
            deepLink == NotificationDeepLink.NONE

        UserRole.GUEST -> false
    }
}

private const val MILLIS_PER_SECOND = 1_000L
private const val NANOS_PER_MILLI = 1_000_000
private const val MAX_EPOCH_SECONDS = 9_999_999_999L
