package com.devpro.pizzatime.feature.shipper.tracking

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryTrackingRuntimeStateStoreTest {
    @After
    fun tearDown() {
        DeliveryTrackingRuntimeStateStore.resetForTest()
    }

    @Test
    fun `observer receives current and later immutable states`() {
        val observed = mutableListOf<DeliveryTrackingRuntimeState>()
        val subscription = DeliveryTrackingRuntimeStateStore.observe(observed::add)
        val active = DeliveryTrackingRuntimeState(
            phase = DeliveryTrackingRuntimePhase.ACTIVE,
            orderId = "order-80",
            lastLocationUpdateMillis = 80_000L,
        )

        DeliveryTrackingRuntimeStateStore.publish(active)

        assertEquals(listOf(DeliveryTrackingRuntimeState.Inactive, active), observed)
        assertEquals(active, DeliveryTrackingRuntimeStateStore.current())
        subscription.close()
    }

    @Test
    fun `closed observer is not retained or notified`() {
        val observed = mutableListOf<DeliveryTrackingRuntimeState>()
        val subscription = DeliveryTrackingRuntimeStateStore.observe(observed::add)
        subscription.close()

        DeliveryTrackingRuntimeStateStore.publish(
            DeliveryTrackingRuntimeState(
                phase = DeliveryTrackingRuntimePhase.WAITING_FOR_LOCATION,
                orderId = "order-80",
                lastLocationUpdateMillis = null,
            ),
        )

        assertEquals(listOf(DeliveryTrackingRuntimeState.Inactive), observed)
    }

    @Test
    fun `duplicate state is not emitted twice`() {
        val observed = mutableListOf<DeliveryTrackingRuntimeState>()
        val subscription = DeliveryTrackingRuntimeStateStore.observe(observed::add)

        DeliveryTrackingRuntimeStateStore.publish(DeliveryTrackingRuntimeState.Inactive)

        assertEquals(1, observed.size)
        subscription.close()
    }
}
