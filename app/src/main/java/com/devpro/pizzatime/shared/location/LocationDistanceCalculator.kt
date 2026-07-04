package com.devpro.pizzatime.shared.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationDistanceCalculator {

    fun calculateDistanceKm(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
    ): Double {
        val startLatRad = Math.toRadians(startLat)
        val endLatRad = Math.toRadians(endLat)
        val deltaLatRad = Math.toRadians(endLat - startLat)
        val deltaLngRad = Math.toRadians(endLng - startLng)

        val haversine = sin(deltaLatRad / 2).pow(2.0) +
            cos(startLatRad) * cos(endLatRad) * sin(deltaLngRad / 2).pow(2.0)
        val centralAngle = 2 * asin(sqrt(haversine))

        return EARTH_RADIUS_KM * centralAngle
    }

    private const val EARTH_RADIUS_KM = 6371.0
}
