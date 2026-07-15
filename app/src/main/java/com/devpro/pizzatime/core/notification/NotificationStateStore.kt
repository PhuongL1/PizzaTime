package com.devpro.pizzatime.core.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object NotificationStateStore {

    private lateinit var preferences: SharedPreferences

    @Synchronized
    fun init(context: Context) {
        if (!::preferences.isInitialized) {
            preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    @Synchronized
    fun hasDedupeKey(
        scope: NotificationScope,
        dedupeKey: String,
    ): Boolean {
        return readDedupeKeys(scope).contains(dedupeKey)
    }

    @Synchronized
    fun recordDedupeKey(
        scope: NotificationScope,
        dedupeKey: String,
    ) {
        val keys = readDedupeKeys(scope).toMutableList()
        keys.remove(dedupeKey)
        keys.add(0, dedupeKey)
        writeStringArray(scope.dedupeKeyKey(), keys.take(NotificationDefaults.MAX_DEDUPE_KEYS))
    }

    @Synchronized
    fun getOrderState(
        scope: NotificationScope,
        orderId: String,
    ): OrderNotificationState? {
        return readOrderStates(scope)[orderId]
    }

    @Synchronized
    fun putOrderState(
        scope: NotificationScope,
        orderId: String,
        state: OrderNotificationState,
    ) {
        val states = readOrderStates(scope).toMutableMap()
        states[orderId] = state
        writeOrderStates(scope, states)
    }

    @Synchronized
    fun putOrderStates(
        scope: NotificationScope,
        states: Map<String, OrderNotificationState>,
    ) {
        writeOrderStates(scope, states)
    }

    @Synchronized
    fun lastOrdersSyncAt(scope: NotificationScope): Long {
        return prefs().getLong(scope.ordersSyncKey(), 0L)
    }

    @Synchronized
    fun setLastOrdersSyncAt(
        scope: NotificationScope,
        value: Long,
    ) {
        prefs().edit().putLong(scope.ordersSyncKey(), value).apply()
    }

    @Synchronized
    fun lastProductReviewSyncAt(scope: NotificationScope): Long {
        return prefs().getLong(scope.productReviewSyncKey(), 0L)
    }

    @Synchronized
    fun setLastProductReviewSyncAt(
        scope: NotificationScope,
        value: Long,
    ) {
        prefs().edit().putLong(scope.productReviewSyncKey(), value).apply()
    }

    private fun readDedupeKeys(scope: NotificationScope): List<String> {
        val raw = prefs().getString(scope.dedupeKeyKey(), "").orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotBlank()) {
                        add(value)
                    }
                }
            }
        }.getOrElse { error ->
            Log.w(TAG, "Persisted dedupe metadata decode failed", error)
            emptyList()
        }
    }

    private fun writeStringArray(
        key: String,
        values: List<String>,
    ) {
        val payload = JSONArray().apply {
            values.forEach { value -> put(value) }
        }
        prefs().edit().putString(key, payload.toString()).apply()
    }

    private fun readOrderStates(scope: NotificationScope): Map<String, OrderNotificationState> {
        val raw = prefs().getString(scope.orderStatesKey(), "").orEmpty()
        if (raw.isBlank()) {
            return emptyMap()
        }
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val orderId = keys.next()
                    val item = json.optJSONObject(orderId) ?: continue
                    put(
                        orderId,
                        OrderNotificationState(
                            status = item.optString("status"),
                            updatedAtMillis = item.optLong("updatedAtMillis", 0L),
                            latestHistoryAtMillis = item.optLong("latestHistoryAtMillis", 0L),
                            handoffStatus = item.optString("handoffStatus"),
                            latestHandoffAtMillis = item.optLong("latestHandoffAtMillis", 0L),
                        ),
                    )
                }
            }
        }.getOrElse { error ->
            Log.w(TAG, "Persisted order state decode failed", error)
            emptyMap()
        }
    }

    private fun writeOrderStates(
        scope: NotificationScope,
        states: Map<String, OrderNotificationState>,
    ) {
        val json = JSONObject()
        states.forEach { (orderId, state) ->
            json.put(
                orderId,
                JSONObject().apply {
                    put("status", state.status)
                    put("updatedAtMillis", state.updatedAtMillis)
                    put("latestHistoryAtMillis", state.latestHistoryAtMillis)
                    put("handoffStatus", state.handoffStatus)
                    put("latestHandoffAtMillis", state.latestHandoffAtMillis)
                },
            )
        }
        prefs().edit().putString(scope.orderStatesKey(), json.toString()).apply()
    }

    private fun NotificationScope.dedupeKeyKey(): String {
        return "notification_dedupe_${applicationId}_${userId}_${role.name.lowercase()}"
    }

    private fun NotificationScope.orderStatesKey(): String {
        return "notification_order_state_${applicationId}_${userId}_${role.name.lowercase()}"
    }

    private fun NotificationScope.ordersSyncKey(): String {
        return "notification_orders_sync_${applicationId}_${userId}_${role.name.lowercase()}"
    }

    private fun NotificationScope.productReviewSyncKey(): String {
        return "notification_review_sync_${applicationId}_${userId}_${role.name.lowercase()}"
    }

    private fun prefs() = preferences

    private const val PREFS_NAME = "pizza_time_notification_state"
    private const val TAG = "NotificationStateStore"
}
