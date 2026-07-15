package com.devpro.pizzatime.shared.location

@ConsistentCopyVisibility
data class DeliveryCoordinate private constructor(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        fun from(
            latitude: Double?,
            longitude: Double?,
        ): DeliveryCoordinate? {
            if (!latitude.isValidLatitude() || !longitude.isValidLongitude()) {
                return null
            }
            return DeliveryCoordinate(
                latitude = requireNotNull(latitude),
                longitude = requireNotNull(longitude),
            )
        }
    }
}
