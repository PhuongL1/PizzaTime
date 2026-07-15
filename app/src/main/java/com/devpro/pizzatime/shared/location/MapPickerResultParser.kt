package com.devpro.pizzatime.shared.location

/** Parses the primitive Fragment Result contract without treating missing doubles as zero. */
object MapPickerResultParser {

    fun parse(
        hasAddress: Boolean,
        address: String?,
        hasLatitude: Boolean,
        latitude: Double?,
        hasLongitude: Boolean,
        longitude: Double?,
    ): DeliveryLocationSelection? {
        if (!hasAddress || !hasLatitude || !hasLongitude) {
            return null
        }

        val normalizedAddress = address?.trim().orEmpty()
        if (normalizedAddress.isBlank()) {
            return null
        }
        val coordinate = DeliveryCoordinate.from(latitude, longitude) ?: return null
        return DeliveryLocationSelection.from(
            address = normalizedAddress,
            coordinate = coordinate,
        )
    }
}
