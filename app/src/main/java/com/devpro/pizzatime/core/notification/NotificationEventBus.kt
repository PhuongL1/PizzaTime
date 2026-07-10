package com.devpro.pizzatime.core.notification

import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

object NotificationEventBus {

    private val listeners = CopyOnWriteArraySet<(AppNotification) -> Unit>()

    fun observe(listener: (AppNotification) -> Unit): Closeable {
        listeners += listener
        return Closeable { listeners -= listener }
    }

    fun publish(notification: AppNotification) {
        listeners.forEach { listener ->
            listener(notification)
        }
    }
}
