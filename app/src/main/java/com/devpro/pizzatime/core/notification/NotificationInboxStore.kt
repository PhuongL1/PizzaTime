package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log
import com.devpro.pizzatime.core.session.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

object NotificationInboxStore {

    private lateinit var appContext: Context
    private val cache = mutableMapOf<String, MutableList<AppNotification>>()
    private val observers = CopyOnWriteArraySet<(List<AppNotification>) -> Unit>()
    private var activeScopeKey: String? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun loadForCurrentAccount(): List<AppNotification> {
        val context = contextOrNull() ?: return emptyList()
        val scope = NotificationSessionResolver.currentScope(context)
        val notifications = if (scope == null) {
            emptyList()
        } else {
            loadForScope(scope)
        }
        activeScopeKey = scope?.storageKey()
        notifyObservers(notifications)
        return notifications
    }

    fun observeNotifications(listener: (List<AppNotification>) -> Unit): Closeable {
        observers += listener
        activeNotifications()?.let(listener)
        return Closeable { observers -= listener }
    }

    fun addOrUpdate(notification: AppNotification) {
        val context = contextOrNull() ?: return
        val scope = NotificationSessionResolver.scopeForNotification(context, notification) ?: return
        val key = scope.storageKey()
        val items = loadMutableForScope(scope)
        val existingIndex = items.indexOfFirst { item ->
            item.id == notification.id || item.dedupeKey == notification.dedupeKey
        }

        if (existingIndex >= 0) {
            items[existingIndex] = notification
        } else {
            items += notification
        }

        val trimmed = items
            .sortedByDescending { it.createdAtMillis }
            .take(NotificationDefaults.MAX_INBOX_SIZE)
            .toMutableList()
        cache[key] = trimmed
        saveScope(scope, trimmed)
        Log.d(TAG, "Inbox saved role=${scope.role.name} size=${trimmed.size}")
        if (activeScopeKey == key) {
            notifyObservers(trimmed)
        }
    }

    fun markRead(notificationId: String) {
        val scope = activeScope() ?: return
        val items = loadMutableForScope(scope)
        val updatedItems = items.map { item ->
            if (item.id == notificationId) {
                item.copy(isRead = true)
            } else {
                item
            }
        }
        cache[scope.storageKey()] = updatedItems.toMutableList()
        saveScope(scope, updatedItems)
        notifyObservers(updatedItems)
    }

    fun markAllRead() {
        val scope = activeScope() ?: return
        val updatedItems = loadMutableForScope(scope).map { it.copy(isRead = true) }
        cache[scope.storageKey()] = updatedItems.toMutableList()
        saveScope(scope, updatedItems)
        notifyObservers(updatedItems)
    }

    fun clearForSignedOutAccount() {
        activeScopeKey = null
        notifyObservers(emptyList())
    }

    fun unreadCount(): Int {
        return activeNotifications().orEmpty().count { notification -> !notification.isRead }
    }

    fun containsDedupeKey(dedupeKey: String): Boolean {
        return activeNotifications()
            .orEmpty()
            .any { notification -> notification.dedupeKey == dedupeKey }
    }

    private fun activeScope(): NotificationScope? {
        val context = contextOrNull() ?: return null
        val scope = NotificationSessionResolver.currentScope(context) ?: return null
        if (scope.storageKey() != activeScopeKey) {
            activeScopeKey = scope.storageKey()
        }
        return scope
    }

    private fun activeNotifications(): List<AppNotification>? {
        val scopeKey = activeScopeKey ?: return null
        return cache[scopeKey]
    }

    private fun loadForScope(scope: NotificationScope): List<AppNotification> {
        return loadMutableForScope(scope).toList()
    }

    private fun loadMutableForScope(scope: NotificationScope): MutableList<AppNotification> {
        val key = scope.storageKey()
        cache[key]?.let { cached -> return cached.toMutableList() }

        val context = contextOrNull() ?: return mutableListOf()
        val raw = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, "")
            .orEmpty()
        val items = if (raw.isBlank()) {
            mutableListOf()
        } else {
            parseNotifications(raw)
        }
        cache[key] = items
        return items.toMutableList()
    }

    private fun saveScope(
        scope: NotificationScope,
        items: List<AppNotification>,
    ) {
        val context = contextOrNull() ?: return
        val payload = JSONArray().apply {
            items.forEach { item -> put(item.toJson()) }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(scope.storageKey(), payload.toString())
            .apply()
    }

    private fun parseNotifications(raw: String): MutableList<AppNotification> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(item.toNotification())
                }
            }.sortedByDescending { it.createdAtMillis }.toMutableList()
        }.getOrDefault(mutableListOf())
    }

    private fun notifyObservers(notifications: List<AppNotification>) {
        observers.forEach { observer ->
            observer(notifications)
        }
    }

    private fun NotificationScope.storageKey(): String {
        return "notification_inbox_${applicationId}_${userId}_${role.name.lowercase()}"
    }

    private fun AppNotification.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("dedupeKey", dedupeKey)
            put("recipientRole", recipientRole.name)
            put("recipientUserId", recipientUserId)
            put("type", type.name)
            put("title", title)
            put("body", body)
            put("orderId", orderId)
            put("reviewId", reviewId)
            put("createdAtMillis", createdAtMillis)
            put("isRead", isRead)
            put("deepLinkType", deepLinkType.name)
        }
    }

    private fun JSONObject.toNotification(): AppNotification {
        return AppNotification(
            id = optString("id"),
            dedupeKey = optString("dedupeKey"),
            recipientRole = UserRole.valueOf(optString("recipientRole", UserRole.CUSTOMER.name)),
            recipientUserId = optString("recipientUserId").trim().ifBlank { null },
            type = NotificationType.valueOf(optString("type")),
            title = optString("title"),
            body = optString("body"),
            orderId = optString("orderId").trim().ifBlank { null },
            reviewId = optString("reviewId").trim().ifBlank { null },
            createdAtMillis = optLong("createdAtMillis", 0L),
            isRead = optBoolean("isRead", false),
            deepLinkType = NotificationDeepLink.valueOf(
                optString("deepLinkType", NotificationDeepLink.NONE.name),
            ),
        )
    }

    private fun contextOrNull(): Context? {
        return if (::appContext.isInitialized) appContext else null
    }

    private const val PREFS_NAME = "pizza_time_notification_inbox"
    private const val TAG = "NotificationInbox"
}
