package com.devpro.pizzatime.core.notification

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

object AppForegroundState : DefaultLifecycleObserver {

    var isForeground: Boolean = false
        private set

    private var initialized = false

    fun init() {
        if (initialized) {
            return
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        initialized = true
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground = false
    }
}
