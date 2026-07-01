package com.devpro.pizzatime.feature.customer.notifications

enum class CustomerNotificationType {
    DELIVERY,
    PROMO,
    WEATHER,
    ORDER,
    LOYALTY,
}

data class CustomerNotificationUiModel(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val type: CustomerNotificationType,
    val isUnread: Boolean,
)