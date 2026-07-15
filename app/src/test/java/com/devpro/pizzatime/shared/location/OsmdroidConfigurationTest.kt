package com.devpro.pizzatime.shared.location

import com.devpro.pizzatime.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class OsmdroidConfigurationTest {

    @Test
    fun `user agent uses variant application identity`() {
        assertEquals(
            BuildConfig.APPLICATION_ID,
            OsmdroidConfiguration.userAgent(BuildConfig.APPLICATION_ID),
        )
    }
}
