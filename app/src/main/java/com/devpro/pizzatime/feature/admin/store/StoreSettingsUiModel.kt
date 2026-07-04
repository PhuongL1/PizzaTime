package com.devpro.pizzatime.feature.admin.store

data class StoreSettingsUiModel(
    val storeName: String = DEFAULT_STORE_NAME,
    val pickupAddress: String = "",
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val storePhone: String = "",
    val openingHours: String = DEFAULT_OPENING_HOURS,
    val acceptingOrders: Boolean = true,
    val baseDeliveryFee: Double = DEFAULT_BASE_DELIVERY_FEE,
    val deliveryFeePerKm: Double = DEFAULT_DELIVERY_FEE_PER_KM,
    val freeDeliveryMinSubtotal: Double = DEFAULT_FREE_DELIVERY_MIN_SUBTOTAL,
) {
    companion object {
        const val DEFAULT_STORE_NAME = "PizzaTime"
        const val DEFAULT_OPENING_HOURS = "09:00 - 22:00"
        const val DEFAULT_BASE_DELIVERY_FEE = 15000.0
        const val DEFAULT_DELIVERY_FEE_PER_KM = 5000.0
        const val DEFAULT_FREE_DELIVERY_MIN_SUBTOTAL = 0.0
    }
}
