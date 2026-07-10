package com.devpro.pizzatime.core.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NotificationStateStore {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun hasDedupeKey(
        scope: NotificationScope,
        dedupeKey: String,
    ): Boolean {
        return readDedupeKeys(scope).contains(dedupeKey)
    }

    fun recordDedupeKey(
        scope: NotificationScope,
        dedupeKey: String,
    ) {
        val keys = readDedupeKeys(scope).toMutableList()
        keys.remove(dedupeKey)
        keys.add(0, dedupeKey)
        writeStringArray(scope.dedupeKeyKey(), keys.take(NotificationDefaults.MAX_DEDUPE_KEYS))
    }

    fun getOrderState(
        scope: NotificationScope,
        orderId: String,
    ): OrderNotificationState? {
        return readOrderStates(scope)[orderId]
    }

    fun putOrderState(
        scope: NotificationScope,
        orderId: String,
        state: OrderNotificationState,
    ) {
        val states = readOrderStates(scope).toMutableMap()
        states[orderId] = state
        writeOrderStates(scope, states)
    }

    fun putOrderStates(
        scope: NotificationScope,
        states: Map<String, OrderNotificationState>,
    ) {
        writeOrderStates(scope, states)
    }

    fun lastOrdersSyncAt(scope: NotificationScope): Long {
        return prefs().getLong(scope.ordersSyncKey(), 0L)
    }

    fun setLastOrdersSyncAt(
        scope: NotificationScope,
        value: Long,
    ) {
        prefs().edit().putLong(scope.ordersSyncKey(), value).apply()
    }

    fun lastProductReviewSyncAt(scope: NotificationScope): Long {
        return prefs().getLong(scope.productReviewSyncKey(), 0L)
    }

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
        }.getOrDefault(emptyList())
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
                        ),
                    )
                }
            }
        }.getOrDefault(emptyMap())
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

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "pizza_time_notification_state"
}
