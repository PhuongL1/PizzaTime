package com.devpro.pizzatime.core.notification

import com.devpro.pizzatime.core.config.AppEdition

internal fun unreadNotificationCount(notifications: List<AppNotification>): Int {
    return notifications.count { notification -> !notification.isRead }
}

internal fun markNotificationRead(
    notifications: List<AppNotification>,
    notificationId: String,
): List<AppNotification> {
    return notifications.map { notification ->
        if (notification.id == notificationId) {
            notification.copy(isRead = true)
        } else {
            notification
        }
    }
}

internal fun markAllNotificationsRead(notifications: List<AppNotification>): List<AppNotification> {
    return notifications.map { notification -> notification.copy(isRead = true) }
}

internal fun notificationInboxStorageKey(
    scope: NotificationScope,
    edition: AppEdition,
): String {
    return "notification_inbox_${scope.applicationId}_${edition.name.lowercase()}_" +
        "${scope.userId}_${scope.role.name.lowercase()}"
}

internal fun legacyNotificationInboxStorageKey(scope: NotificationScope): String {
    return "notification_inbox_${scope.applicationId}_${scope.userId}_${scope.role.name.lowercase()}"
}

internal fun parsePersistedInboxOrEmpty(
    raw: String,
    decoder: (String) -> List<AppNotification>,
    onFailure: (Throwable) -> Unit,
): List<AppNotification> {
    if (raw.isBlank()) {
        return emptyList()
    }
    return runCatching { decoder(raw) }
        .getOrElse { error ->
            onFailure(error)
            emptyList()
        }
}
