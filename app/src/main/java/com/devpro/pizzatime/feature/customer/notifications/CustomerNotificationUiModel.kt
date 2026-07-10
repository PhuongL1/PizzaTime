package com.devpro.pizzatime.feature.customer.notifications

import com.devpro.pizzatime.core.notification.NotificationDeepLink

data class CustomerNotificationUiModel(
    val id: String,
    val title: String,
    val body: String,
    val timestampLabel: String,
    val isUnread: Boolean,
    val orderId: String?,
    val reviewId: String?,
    val deepLinkType: NotificationDeepLink,
    val iconRes: Int,
    val iconBackgroundRes: Int,
    val iconTintRes: Int,
)
