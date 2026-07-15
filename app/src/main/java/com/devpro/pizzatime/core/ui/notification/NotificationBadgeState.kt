package com.devpro.pizzatime.core.ui.notification

internal data class NotificationBadgeState(
    val isVisible: Boolean,
    val text: String,
)

internal fun notificationBadgeState(
    count: Int,
    overflowText: String,
): NotificationBadgeState {
    return when {
        count <= 0 -> NotificationBadgeState(isVisible = false, text = "")
        count >= BADGE_OVERFLOW_COUNT -> NotificationBadgeState(isVisible = true, text = overflowText)
        else -> NotificationBadgeState(isVisible = true, text = count.toString())
    }
}

private const val BADGE_OVERFLOW_COUNT = 100
