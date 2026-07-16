package com.devpro.pizzatime.feature.customer.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DemoPaymentBackendConfigTest {

    @Test
    fun blankBackendUrl_isTreatedAsUnconfigured() {
        assertNull(validateConfiguredBackendUrl("   ", isDebugBuild = true))
    }

    @Test
    fun httpsBackendUrl_isAccepted() {
        val config = validateConfiguredBackendUrl(
            rawValue = "https://demo.example.test/",
            isDebugBuild = false,
        )

        assertNotNull(config)
        assertEquals("https://demo.example.test", config?.baseUrl)
        assertEquals(false, config?.allowDebugLocalHttp)
    }

    @Test
    fun publicHttpBackendUrl_isRejectedOutsideDebugLocalMode() {
        assertNull(
            validateConfiguredBackendUrl(
                rawValue = "http://demo.example.test",
                isDebugBuild = true,
            ),
        )
        assertNull(
            validateConfiguredBackendUrl(
                rawValue = "http://10.0.2.2:8080",
                isDebugBuild = false,
            ),
        )
    }

    @Test
    fun debugLocalHttpBackendUrl_isAcceptedForAllowedHosts() {
        val config = validateConfiguredBackendUrl(
            rawValue = "http://10.0.2.2:8080/",
            isDebugBuild = true,
        )

        assertNotNull(config)
        assertEquals("http://10.0.2.2:8080", config?.baseUrl)
        assertEquals(true, config?.allowDebugLocalHttp)
    }

    @Test
    fun paymentPageUrl_mustMatchConfiguredOriginAndPath() {
        val config = checkNotNull(
            validateConfiguredBackendUrl(
                rawValue = "https://demo.example.test",
                isDebugBuild = false,
            ),
        )

        assertNotNull(
            validateDemoPaymentPageUri(
                "https://demo.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF",
                config,
            ),
        )
        assertNull(
            validateDemoPaymentPageUri(
                "https://other.example.test/demo/pay/abcdefghijklmnopqrstuvwxyzABCDEF",
                config,
            ),
        )
        assertNull(
            validateDemoPaymentPageUri(
                "javascript:alert(1)",
                config,
            ),
        )
    }
}
