package com.devpro.pizzatime.feature.auth

import android.content.Context
import android.util.Log

object PendingAuthDestinationStore {

    private const val PREFS_NAME = "pizza_time_pending_auth"
    private const val KEY_PENDING_DESTINATION = "pendingDestination"
    const val DESTINATION_CHECKOUT = "CHECKOUT"

    fun setCheckout(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_DESTINATION, DESTINATION_CHECKOUT)
            .apply()
        Log.d(TAG, "Pending checkout saved")
    }

    fun consume(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val destination = prefs.getString(KEY_PENDING_DESTINATION, "").orEmpty()
        if (destination.isNotBlank()) {
            prefs.edit().remove(KEY_PENDING_DESTINATION).apply()
        }
        return destination
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_DESTINATION)
            .apply()
    }

    private const val TAG = "PendingAuth"
}
