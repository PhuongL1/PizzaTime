package com.devpro.pizzatime.shared.location

data class LocationSnapshot(
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
) {
    val hasValidCoordinates: Boolean
        get() = latitude.isValidLatitude() && longitude.isValidLongitude()
}

fun Double?.isValidLatitude(): Boolean = this != null && isFinite() && this in -90.0..90.0

fun Double?.isValidLongitude(): Boolean = this != null && isFinite() && this in -180.0..180.0

fun isValidCoordinate(lat: Double?, lng: Double?): Boolean =
    lat.isValidLatitude() && lng.isValidLongitude()
