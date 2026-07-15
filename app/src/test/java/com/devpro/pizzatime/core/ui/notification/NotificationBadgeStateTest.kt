package com.devpro.pizzatime.core.ui.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationBadgeStateTest {

    @Test
    fun zeroAndSignedOutState_renderHidden() {
        val state = notificationBadgeState(count = 0, overflowText = "99+")

        assertFalse(state.isVisible)
        assertEquals("", state.text)
    }

    @Test
    fun one_rendersExactCount() {
        assertVisibleText(count = 1, expected = "1")
    }

    @Test
    fun ninetyNine_rendersExactCount() {
        assertVisibleText(count = 99, expected = "99")
    }

    @Test
    fun oneHundred_rendersOverflowText() {
        assertVisibleText(count = 100, expected = "99+")
    }

    private fun assertVisibleText(
        count: Int,
        expected: String,
    ) {
        val state = notificationBadgeState(count = count, overflowText = "99+")

        assertTrue(state.isVisible)
        assertEquals(expected, state.text)
    }
}
