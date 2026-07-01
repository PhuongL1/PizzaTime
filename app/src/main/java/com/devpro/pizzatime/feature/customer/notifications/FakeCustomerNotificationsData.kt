package com.devpro.pizzatime.feature.customer.notifications

object FakeCustomerNotificationsData {

    fun getNotifications(): List<CustomerNotificationUiModel> {
        return listOf(
            CustomerNotificationUiModel(
                id = "notification_delivery_now",
                title = "Order Out for Delivery",
                message = "Your 'Truffle & Wild Mushroom' pizza is on its way with our courier, Marco. Estimated arrival: 12:15 AM.",
                timeLabel = "JUST NOW",
                type = CustomerNotificationType.DELIVERY,
                isUnread = true,
            ),
            CustomerNotificationUiModel(
                id = "notification_midnight_special",
                title = "Midnight Special",
                message = "Unlock a complimentary glass of Chianti with any Signature Series pizza ordered before 1 AM tonight.",
                timeLabel = "2H AGO",
                type = CustomerNotificationType.PROMO,
                isUnread = false,
            ),
            CustomerNotificationUiModel(
                id = "notification_weather",
                title = "Weather Update",
                message = "Heavier rain in the downtown area may lead to 10-15 minute delays for new deliveries. We appreciate your patience.",
                timeLabel = "4H AGO",
                type = CustomerNotificationType.WEATHER,
                isUnread = true,
            ),
            CustomerNotificationUiModel(
                id = "notification_delivered",
                title = "Order Delivered",
                message = "We hope you enjoyed your Artisan Pepperoni. Don't forget to rate your experience!",
                timeLabel = "YESTERDAY",
                type = CustomerNotificationType.ORDER,
                isUnread = false,
            ),
            CustomerNotificationUiModel(
                id = "notification_loyalty",
                title = "Loyalty Milestone",
                message = "You've reached 'Gold Crust' status! Premium table reservations are now available for you.",
                timeLabel = "2D AGO",
                type = CustomerNotificationType.LOYALTY,
                isUnread = false,
            ),
        )
    }
}