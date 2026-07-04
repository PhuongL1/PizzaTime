package com.devpro.pizzatime.feature.admin.store

data class StoreSettingsUiModel(
    val storeName: String = DEFAULT_STORE_NAME,
    val pickupAddress: String = "",
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val storePhone: String = "",
    val openingHours: String = DEFAULT_OPENING_HOURS,
    val acceptingOrders: Boolean = true,
) {
    companion object {
        const val DEFAULT_STORE_NAME = "PizzaTime"
        const val DEFAULT_OPENING_HOURS = "09:00 - 22:00"
    }
}
