package com.devpro.pizzatime.core.notification

internal enum class NotificationRoutingDecision {
    ALLOWED,
    WRONG_SCOPE,
    NOT_FOUND,
    MISMATCHED_EVENT,
    UNSUPPORTED_DESTINATION,
}

internal fun validateNotificationRouting(
    request: NotificationRoutingRequest,
    currentScope: NotificationScope,
    notification: AppNotification?,
): NotificationRoutingDecision {
    if (
        request.applicationId != currentScope.applicationId ||
        request.recipientUserId != currentScope.userId ||
        request.recipientRole != currentScope.role
    ) {
        return NotificationRoutingDecision.WRONG_SCOPE
    }
    val persisted = notification ?: return NotificationRoutingDecision.NOT_FOUND
    if (
        persisted.recipientUserId != currentScope.userId ||
        persisted.recipientRole != currentScope.role ||
        request.notificationId != persisted.id ||
        request.deepLinkType != persisted.deepLinkType ||
        request.orderId != persisted.orderId ||
        request.reviewId != persisted.reviewId
    ) {
        return NotificationRoutingDecision.MISMATCHED_EVENT
    }
    if (!isDeepLinkAllowedForRole(currentScope.role, persisted.deepLinkType)) {
        return NotificationRoutingDecision.UNSUPPORTED_DESTINATION
    }
    return NotificationRoutingDecision.ALLOWED
}

internal fun AppNotification.toRoutingRequest(applicationId: String): NotificationRoutingRequest? {
    val userId = recipientUserId?.trim().orEmpty().takeIf { it.isNotBlank() } ?: return null
    return NotificationRoutingRequest(
        notificationId = id,
        applicationId = applicationId,
        recipientUserId = userId,
        recipientRole = recipientRole,
        deepLinkType = deepLinkType,
        orderId = orderId,
        reviewId = reviewId,
    )
}
