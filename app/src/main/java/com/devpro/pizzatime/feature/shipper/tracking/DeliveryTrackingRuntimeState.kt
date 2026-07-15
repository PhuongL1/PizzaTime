package com.devpro.pizzatime.feature.shipper.tracking

import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

enum class DeliveryTrackingRuntimePhase {
    INACTIVE,
    STARTING,
    WAITING_FOR_LOCATION,
    ACTIVE,
    PERMISSION_REQUIRED,
    SERVICES_UNAVAILABLE,
}

data class DeliveryTrackingRuntimeState(
    val phase: DeliveryTrackingRuntimePhase,
    val orderId: String?,
    val lastLocationUpdateMillis: Long?,
) {
    companion object {
        val Inactive = DeliveryTrackingRuntimeState(
            phase = DeliveryTrackingRuntimePhase.INACTIVE,
            orderId = null,
            lastLocationUpdateMillis = null,
        )
    }
}

/**
 * Process-local service state. Observers must close the returned subscription with their lifecycle.
 * The store contains immutable primitives only and never retains Android Context or View objects.
 */
object DeliveryTrackingRuntimeStateStore {
    private val nextObserverId = AtomicLong(0L)
    private val lock = Any()
    private val observers = mutableMapOf<Long, (DeliveryTrackingRuntimeState) -> Unit>()
    private var state = DeliveryTrackingRuntimeState.Inactive

    fun current(): DeliveryTrackingRuntimeState = synchronized(lock) { state }

    fun observe(
        observer: (DeliveryTrackingRuntimeState) -> Unit,
    ): Closeable {
        val observerId = nextObserverId.incrementAndGet()
        val initialState = synchronized(lock) {
            observers[observerId] = observer
            state
        }
        observer(initialState)
        return Closeable {
            synchronized(lock) {
                observers.remove(observerId)
            }
        }
    }

    internal fun publish(newState: DeliveryTrackingRuntimeState) {
        val callbacks = synchronized(lock) {
            if (state == newState) {
                return
            }
            state = newState
            observers.values.toList()
        }
        callbacks.forEach { observer -> observer(newState) }
    }

    internal fun resetForTest() {
        synchronized(lock) {
            state = DeliveryTrackingRuntimeState.Inactive
            observers.clear()
        }
    }
}
