package com.devpro.pizzatime.feature.shipper.navigation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class ExternalMapLaunchResult {
    GOOGLE_MAPS,
    GENERIC_MAP,
    DESTINATION_UNAVAILABLE,
    NO_HANDLER,
}

internal data class ExternalMapLaunchSpec(
    val uri: String,
    val packageName: String? = null,
)

internal data class ExternalMapTarget(
    val latitude: Double?,
    val longitude: Double?,
    val address: String,
    val coordinateLabel: String,
)

/** Opens a real navigation application without retaining Context or calculating a route. */
object ExternalMapNavigator {

    fun launch(
        context: Context,
        coordinate: DeliveryCoordinate?,
        address: String,
        coordinateLabel: String,
    ): ExternalMapLaunchResult {
        val target = ExternalMapTarget(
            latitude = coordinate?.latitude,
            longitude = coordinate?.longitude,
            address = address,
            coordinateLabel = coordinateLabel,
        )
        return launch(target) { spec ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spec.uri)).apply {
                spec.packageName?.let(::setPackage)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            try {
                context.startActivity(intent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }
    }

    internal fun launch(
        target: ExternalMapTarget,
        launchAttempt: (ExternalMapLaunchSpec) -> Boolean,
    ): ExternalMapLaunchResult {
        val plans = buildLaunchPlans(target)
        if (plans.isEmpty()) {
            return ExternalMapLaunchResult.DESTINATION_UNAVAILABLE
        }

        plans.forEach { spec ->
            if (launchAttempt(spec)) {
                return if (spec.packageName == GOOGLE_MAPS_PACKAGE) {
                    ExternalMapLaunchResult.GOOGLE_MAPS
                } else {
                    ExternalMapLaunchResult.GENERIC_MAP
                }
            }
        }
        return ExternalMapLaunchResult.NO_HANDLER
    }

    internal fun buildLaunchPlans(target: ExternalMapTarget): List<ExternalMapLaunchSpec> {
        val coordinate = DeliveryCoordinate.from(target.latitude, target.longitude)
        if (coordinate != null) {
            val latitude = coordinate.latitude.toString()
            val longitude = coordinate.longitude.toString()
            val label = target.coordinateLabel.trim().ifBlank {
                target.address.trim()
            }
            val encodedLabel = encode(label)
            val genericQuery = if (encodedLabel.isBlank()) {
                "$latitude,$longitude"
            } else {
                "$latitude,$longitude($encodedLabel)"
            }
            return listOf(
                ExternalMapLaunchSpec(
                    uri = "google.navigation:q=$latitude,$longitude&mode=d",
                    packageName = GOOGLE_MAPS_PACKAGE,
                ),
                ExternalMapLaunchSpec(uri = "geo:0,0?q=$genericQuery"),
            )
        }

        val address = target.address.trim()
        if (address.isBlank()) {
            return emptyList()
        }
        return listOf(
            ExternalMapLaunchSpec(uri = "geo:0,0?q=${encode(address)}"),
        )
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
    }

    internal const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
}
