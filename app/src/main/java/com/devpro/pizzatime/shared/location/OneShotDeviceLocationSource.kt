package com.devpro.pizzatime.shared.location

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper

sealed class OneShotDeviceLocationResult {
    data class Available(val coordinate: DeliveryCoordinate) : OneShotDeviceLocationResult()

    data class Unavailable(val reason: Reason) : OneShotDeviceLocationResult()

    enum class Reason {
        PERMISSION_REQUIRED,
        LOCATION_SERVICES_UNAVAILABLE,
        CURRENT_LOCATION_UNAVAILABLE,
    }
}

class OneShotDeviceLocationSource(
    private val locationManager: LocationManager,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private var activeListener: LocationListener? = null
    private var activeCallback: ((OneShotDeviceLocationResult) -> Unit)? = null
    private var timeoutRunnable: Runnable? = null

    @SuppressLint("MissingPermission")
    fun request(onResult: (OneShotDeviceLocationResult) -> Unit) {
        cancel()

        val enabledProviders = LOCATION_PROVIDERS.filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (enabledProviders.isEmpty()) {
            onResult(
                OneShotDeviceLocationResult.Unavailable(
                    OneShotDeviceLocationResult.Reason.LOCATION_SERVICES_UNAVAILABLE,
                ),
            )
            return
        }

        val nowMillis = currentTimeMillis()
        val recentLastKnown = enabledProviders
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { location -> location.toFreshCoordinate(nowMillis) != null }
            .maxByOrNull(Location::getTime)
        val lastKnownCoordinate = recentLastKnown?.toFreshCoordinate(nowMillis)
        if (lastKnownCoordinate != null) {
            onResult(OneShotDeviceLocationResult.Available(lastKnownCoordinate))
            return
        }

        activeCallback = onResult
        lateinit var listener: LocationListener
        listener = LocationListener { location ->
            if (activeListener !== listener) {
                return@LocationListener
            }
            val coordinate = location.toFreshCoordinate(currentTimeMillis())
                ?: return@LocationListener
            finish(OneShotDeviceLocationResult.Available(coordinate))
        }
        activeListener = listener

        var registeredProviderCount = 0
        var permissionFailure = false
        enabledProviders.forEach { provider ->
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    MIN_UPDATE_TIME_MS,
                    MIN_UPDATE_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper(),
                )
                registeredProviderCount += 1
            } catch (_: SecurityException) {
                permissionFailure = true
            } catch (_: IllegalArgumentException) {
                // The provider can disappear between the enabled check and registration.
            }
        }

        if (registeredProviderCount == 0) {
            finish(
                OneShotDeviceLocationResult.Unavailable(
                    if (permissionFailure) {
                        OneShotDeviceLocationResult.Reason.PERMISSION_REQUIRED
                    } else {
                        OneShotDeviceLocationResult.Reason.LOCATION_SERVICES_UNAVAILABLE
                    },
                ),
            )
            return
        }

        timeoutRunnable = Runnable {
            finish(
                OneShotDeviceLocationResult.Unavailable(
                    OneShotDeviceLocationResult.Reason.CURRENT_LOCATION_UNAVAILABLE,
                ),
            )
        }.also { runnable ->
            mainHandler.postDelayed(runnable, LOCATION_REQUEST_TIMEOUT_MS)
        }
    }

    fun cancel() {
        clearRegistration()
        activeCallback = null
    }

    private fun finish(result: OneShotDeviceLocationResult) {
        val callback = activeCallback
        clearRegistration()
        activeCallback = null
        callback?.invoke(result)
    }

    private fun clearRegistration() {
        timeoutRunnable?.let(mainHandler::removeCallbacks)
        timeoutRunnable = null
        activeListener?.let { listener ->
            runCatching { locationManager.removeUpdates(listener) }
        }
        activeListener = null
    }

    private fun Location.toFreshCoordinate(nowMillis: Long): DeliveryCoordinate? {
        if (!LocationSampleFreshnessPolicy.isFresh(
                sampleTimeMillis = time,
                nowMillis = nowMillis,
            )
        ) {
            return null
        }
        return DeliveryCoordinate.from(latitude, longitude)
    }

    companion object {
        internal const val MAX_LOCATION_SAMPLE_AGE_MS = 2 * 60 * 1000L
        internal const val MAX_FUTURE_SAMPLE_SKEW_MS = 30 * 1000L
        internal const val LOCATION_REQUEST_TIMEOUT_MS = 10 * 1000L
        internal const val MIN_UPDATE_TIME_MS = 0L
        internal const val MIN_UPDATE_DISTANCE_METERS = 0f

        private val LOCATION_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        )
    }
}

internal object LocationSampleFreshnessPolicy {
    fun isFresh(sampleTimeMillis: Long, nowMillis: Long): Boolean {
        if (sampleTimeMillis <= 0L || nowMillis <= 0L) {
            return false
        }
        val ageMillis = nowMillis - sampleTimeMillis
        return ageMillis in
            -OneShotDeviceLocationSource.MAX_FUTURE_SAMPLE_SKEW_MS..
                OneShotDeviceLocationSource.MAX_LOCATION_SAMPLE_AGE_MS
    }
}
