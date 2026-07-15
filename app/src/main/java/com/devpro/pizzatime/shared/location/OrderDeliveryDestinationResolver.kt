package com.devpro.pizzatime.shared.location

import com.google.firebase.firestore.GeoPoint

/** Centralizes canonical order destination parsing and legacy read compatibility. */
object OrderDeliveryDestinationResolver {

    const val ADDRESS_FIELD = "deliveryAddress"
    const val LOCATION_FIELD = "deliveryLocation"
    const val LEGACY_LATITUDE_FIELD = "deliveryLat"
    const val LEGACY_LONGITUDE_FIELD = "deliveryLng"

    fun resolve(fields: Map<String, Any?>): DeliveryCoordinate? {
        val canonical = (fields[LOCATION_FIELD] as? GeoPoint)?.let { point ->
            DeliveryCoordinate.from(
                latitude = point.latitude,
                longitude = point.longitude,
            )
        }
        if (canonical != null) {
            return canonical
        }

        return DeliveryCoordinate.from(
            latitude = (fields[LEGACY_LATITUDE_FIELD] as? Number)?.toDouble(),
            longitude = (fields[LEGACY_LONGITUDE_FIELD] as? Number)?.toDouble(),
        )
    }

    fun canonicalFields(
        address: String,
        coordinate: DeliveryCoordinate,
    ): Map<String, Any> {
        return mapOf(
            ADDRESS_FIELD to address.trim(),
            LOCATION_FIELD to GeoPoint(coordinate.latitude, coordinate.longitude),
        )
    }
}
