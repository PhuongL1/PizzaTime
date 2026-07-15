package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.session.UserRole

internal data class NotificationFcmPayload(
    val type: NotificationType,
    val orderId: String?,
    val reviewId: String?,
    val orderStatus: String?,
    val orderCode: String?,
    val cancellationReason: String?,
    val productName: String?,
    val eventMillis: Long,
    val dedupeKey: String,
)

internal fun parseNotificationFcmPayload(
    data: Map<String, String>,
    expectedScope: NotificationScope,
): NotificationFcmPayload? {
    val applicationId = data[KEY_APPLICATION_ID].sanitized(MAX_ID_LENGTH) ?: return null
    val recipientUserId = data[KEY_RECIPIENT_USER_ID].sanitized(MAX_ID_LENGTH) ?: return null
    val recipientRole = data[KEY_RECIPIENT_ROLE].toUserRoleOrNull() ?: return null
    if (
        applicationId != expectedScope.applicationId ||
        recipientUserId != expectedScope.userId ||
        recipientRole != expectedScope.role
    ) {
        return null
    }

    val type = data[KEY_TYPE].toNotificationTypeOrNull() ?: return null
    val eventMillis = data[KEY_EVENT_MILLIS]?.toLongOrNull()?.takeIf { it > 0L } ?: return null
    val suppliedDedupeKey = data[KEY_DEDUPE_KEY].sanitized(MAX_DEDUPE_LENGTH) ?: return null

    if (type == NotificationType.ADMIN_PRODUCT_REVIEW) {
        if (expectedScope.role != UserRole.ADMIN) return null
        val reviewId = data[KEY_REVIEW_ID].sanitized(MAX_ID_LENGTH) ?: return null
        val expectedDedupeKey = canonicalReviewNotificationDedupeKey(reviewId)
        if (suppliedDedupeKey != expectedDedupeKey) return null
        return NotificationFcmPayload(
            type = type,
            orderId = data[KEY_ORDER_ID].sanitized(MAX_ID_LENGTH),
            reviewId = reviewId,
            orderStatus = null,
            orderCode = null,
            cancellationReason = null,
            productName = data[KEY_PRODUCT_NAME].sanitized(MAX_PRODUCT_NAME_LENGTH),
            eventMillis = eventMillis,
            dedupeKey = suppliedDedupeKey,
        )
    }

    val orderId = data[KEY_ORDER_ID].sanitized(MAX_ID_LENGTH) ?: return null
    val status = normalizeNotificationStatus(data[KEY_ORDER_STATUS]).takeIf { it.isNotBlank() } ?: return null
    if (notificationTypeForOrderTransition(expectedScope.role, status) != type) return null
    val expectedDedupeKey = canonicalOrderNotificationDedupeKey(
        role = expectedScope.role,
        orderId = orderId,
        status = status,
        eventMillis = eventMillis,
    )
    if (suppliedDedupeKey != expectedDedupeKey) return null

    return NotificationFcmPayload(
        type = type,
        orderId = orderId,
        reviewId = null,
        orderStatus = status,
        orderCode = data[KEY_ORDER_CODE].sanitized(MAX_ORDER_CODE_LENGTH),
        cancellationReason = data[KEY_CANCELLATION_REASON]
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.take(NotificationDefaults.MAX_REASON_LENGTH),
        productName = null,
        eventMillis = eventMillis,
        dedupeKey = suppliedDedupeKey,
    )
}

private fun String?.sanitized(maxLength: Int): String? {
    return this?.trim()?.takeIf { it.isNotBlank() && it.length <= maxLength }
}

private fun String?.toUserRoleOrNull(): UserRole? {
    return runCatching { UserRole.valueOf(this.orEmpty().trim()) }.getOrNull()
}

private fun String?.toNotificationTypeOrNull(): NotificationType? {
    return runCatching { NotificationType.valueOf(this.orEmpty().trim()) }.getOrNull()
}

internal const val FCM_KEY_APPLICATION_ID = "applicationId"
internal const val FCM_KEY_RECIPIENT_USER_ID = "recipientUserId"
internal const val FCM_KEY_RECIPIENT_ROLE = "recipientRole"
internal const val FCM_KEY_TYPE = "type"
internal const val FCM_KEY_EVENT_MILLIS = "eventMillis"
internal const val FCM_KEY_DEDUPE_KEY = "dedupeKey"
internal const val FCM_KEY_ORDER_ID = "orderId"
internal const val FCM_KEY_REVIEW_ID = "reviewId"
internal const val FCM_KEY_ORDER_STATUS = "orderStatus"

private const val KEY_APPLICATION_ID = FCM_KEY_APPLICATION_ID
private const val KEY_RECIPIENT_USER_ID = FCM_KEY_RECIPIENT_USER_ID
private const val KEY_RECIPIENT_ROLE = FCM_KEY_RECIPIENT_ROLE
private const val KEY_TYPE = FCM_KEY_TYPE
private const val KEY_EVENT_MILLIS = FCM_KEY_EVENT_MILLIS
private const val KEY_DEDUPE_KEY = FCM_KEY_DEDUPE_KEY
private const val KEY_ORDER_ID = FCM_KEY_ORDER_ID
private const val KEY_REVIEW_ID = FCM_KEY_REVIEW_ID
private const val KEY_ORDER_STATUS = FCM_KEY_ORDER_STATUS
private const val KEY_ORDER_CODE = "orderCode"
private const val KEY_CANCELLATION_REASON = "cancellationReason"
private const val KEY_PRODUCT_NAME = "productName"
private const val MAX_ID_LENGTH = 256
private const val MAX_DEDUPE_LENGTH = 512
private const val MAX_ORDER_CODE_LENGTH = 64
private const val MAX_PRODUCT_NAME_LENGTH = 100
