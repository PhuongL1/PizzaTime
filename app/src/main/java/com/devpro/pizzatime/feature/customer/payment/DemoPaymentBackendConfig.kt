package com.devpro.pizzatime.feature.customer.payment

import android.net.Uri
import com.devpro.pizzatime.BuildConfig
import java.util.Locale

data class DemoPaymentBackendConfigValue(
    val baseUrl: String,
    val baseUri: Uri,
    val allowDebugLocalHttp: Boolean,
)

object DemoPaymentBackendConfig {

    fun configured(): DemoPaymentBackendConfigValue? {
        return validateConfiguredBackendUrl(
            rawValue = BuildConfig.PAYMENT_BACKEND_URL,
            isDebugBuild = BuildConfig.DEBUG,
        )
    }
}

internal fun validateConfiguredBackendUrl(
    rawValue: String,
    isDebugBuild: Boolean,
): DemoPaymentBackendConfigValue? {
    val normalized = rawValue.trim().trimEnd('/')
    if (normalized.isBlank()) {
        return null
    }

    val uri = Uri.parse(normalized)
    val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
    val host = uri.host?.trim().orEmpty()
    if (host.isBlank()) {
        return null
    }
    if (scheme == "https") {
        return DemoPaymentBackendConfigValue(
            baseUrl = normalized,
            baseUri = uri,
            allowDebugLocalHttp = false,
        )
    }
    if (scheme != "http" || !isDebugBuild || !isAllowedDebugHttpHost(host)) {
        return null
    }
    return DemoPaymentBackendConfigValue(
        baseUrl = normalized,
        baseUri = uri,
        allowDebugLocalHttp = true,
    )
}

internal fun validateDemoPaymentPageUri(
    value: String,
    backendConfig: DemoPaymentBackendConfigValue,
): Uri? {
    val uri = Uri.parse(value.trim())
    val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
    val host = uri.host?.trim().orEmpty()
    if (host.isBlank() || uri.fragment != null) {
        return null
    }
    if (scheme == "https") {
        if (!sameOrigin(backendConfig.baseUri, uri)) {
            return null
        }
    } else if (scheme == "http" && backendConfig.allowDebugLocalHttp) {
        if (!sameOrigin(backendConfig.baseUri, uri) || !isAllowedDebugHttpHost(host)) {
            return null
        }
    } else {
        return null
    }
    val segments = uri.pathSegments.orEmpty()
    if (segments.size != 3 || segments[0] != "demo" || segments[1] != "pay") {
        return null
    }
    val token = segments[2]
    if (!token.matches(Regex("[A-Za-z0-9_-]{32,128}"))) {
        return null
    }
    return uri
}

internal fun sameOrigin(
    expected: Uri,
    actual: Uri,
): Boolean {
    return expected.scheme.equals(actual.scheme, ignoreCase = true) &&
        expected.host.equals(actual.host, ignoreCase = true) &&
        normalizedPort(expected) == normalizedPort(actual)
}

private fun normalizedPort(uri: Uri): Int {
    return if (uri.port != -1) {
        uri.port
    } else {
        when (uri.scheme?.lowercase(Locale.US).orEmpty()) {
            "https" -> 443
            "http" -> 80
            else -> -1
        }
    }
}

internal fun isAllowedDebugHttpHost(host: String): Boolean {
    return when (host.trim().lowercase(Locale.US)) {
        "10.0.2.2",
        "127.0.0.1",
        "localhost",
        -> true

        else -> false
    }
}
