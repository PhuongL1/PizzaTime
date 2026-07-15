package com.devpro.pizzatime.shared.location

/**
 * Keeps a human-readable delivery address and its confirmed map coordinate together.
 *
 * Editing the address invalidates the coordinate so a point selected for an older address can
 * never be attached to the new text accidentally.
 */
@ConsistentCopyVisibility
data class DeliveryLocationSelection private constructor(
    val address: String,
    val coordinate: DeliveryCoordinate?,
) {
    val isComplete: Boolean
        get() = address.isNotBlank() && coordinate != null

    fun editAddress(address: String): DeliveryLocationSelection {
        val normalizedAddress = address.trim()
        return if (address == this.address) {
            this
        } else {
            DeliveryLocationSelection(
                address = normalizedAddress,
                coordinate = null,
            )
        }
    }

    fun selectCoordinate(coordinate: DeliveryCoordinate): DeliveryLocationSelection {
        return DeliveryLocationSelection(
            address = address,
            coordinate = coordinate,
        )
    }

    companion object {
        fun from(
            address: String,
            coordinate: DeliveryCoordinate?,
        ): DeliveryLocationSelection {
            return DeliveryLocationSelection(
                address = address.trim(),
                coordinate = coordinate,
            )
        }

        fun from(
            address: String,
            latitude: Double?,
            longitude: Double?,
        ): DeliveryLocationSelection {
            return from(
                address = address,
                coordinate = DeliveryCoordinate.from(latitude, longitude),
            )
        }

        fun empty(): DeliveryLocationSelection = DeliveryLocationSelection(
            address = "",
            coordinate = null,
        )
    }
}
