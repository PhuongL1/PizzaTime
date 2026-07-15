package com.devpro.pizzatime.core.notification

internal interface NotificationDeliveryGateway {
    fun isDuplicate(scope: NotificationScope, notification: AppNotification): Boolean
    fun persist(scope: NotificationScope, notification: AppNotification): Boolean
    fun recordProcessed(scope: NotificationScope, dedupeKey: String)
    fun deliverForeground(notification: AppNotification)
    fun deliverSystem(notification: AppNotification): Boolean
}

internal enum class NotificationProcessingResult {
    REJECTED,
    DUPLICATE,
    PERSISTENCE_FAILED,
    FOREGROUND_DELIVERED,
    BACKGROUND_POSTED,
    BACKGROUND_SUPPRESSED,
}

internal class NotificationDeliveryProcessor(
    private val gateway: NotificationDeliveryGateway,
) {
    @Synchronized
    fun process(
        scope: NotificationScope,
        notification: AppNotification,
        isForeground: Boolean,
    ): NotificationProcessingResult {
        if (!notification.isValidFor(scope)) {
            return NotificationProcessingResult.REJECTED
        }
        if (gateway.isDuplicate(scope, notification)) {
            return NotificationProcessingResult.DUPLICATE
        }
        if (!gateway.persist(scope, notification)) {
            return NotificationProcessingResult.PERSISTENCE_FAILED
        }

        gateway.recordProcessed(scope, notification.dedupeKey)
        if (isForeground) {
            gateway.deliverForeground(notification)
            return NotificationProcessingResult.FOREGROUND_DELIVERED
        }

        return if (gateway.deliverSystem(notification)) {
            NotificationProcessingResult.BACKGROUND_POSTED
        } else {
            NotificationProcessingResult.BACKGROUND_SUPPRESSED
        }
    }
}

private fun AppNotification.isValidFor(scope: NotificationScope): Boolean {
    return id.isNotBlank() &&
        dedupeKey.isNotBlank() &&
        createdAtMillis > 0L &&
        recipientUserId?.trim() == scope.userId &&
        recipientRole == scope.role &&
        scope.role != com.devpro.pizzatime.core.session.UserRole.GUEST &&
        isDeepLinkAllowedForRole(scope.role, deepLinkType)
}
