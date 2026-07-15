package com.devpro.pizzatime.core.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

object NotificationInboxStore {

    private lateinit var preferences: SharedPreferences
    private val cache = mutableMapOf<String, List<AppNotification>>()
    private val mutableActiveNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val authStateListener = FirebaseAuth.AuthStateListener {
        refreshForCurrentAccount()
    }
    private var authListenerRegistered = false
    private var activeScopeKey: String? = null

    val notifications: StateFlow<List<AppNotification>> = mutableActiveNotifications.asStateFlow()

    val unreadCount: Flow<Int> = notifications
        .map(::unreadNotificationCount)
        .distinctUntilChanged()

    @Synchronized
    fun init(context: Context) {
        if (!::preferences.isInitialized) {
            preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        if (!authListenerRegistered) {
            authListenerRegistered = true
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        }
        refreshForCurrentAccount()
    }

    @Synchronized
    fun refreshForCurrentAccount(): List<AppNotification> {
        if (!::preferences.isInitialized) {
            publishActiveNotifications(emptyList())
            return emptyList()
        }

        val scope = NotificationSessionResolver.currentScope()
        val key = scope?.storageKey()
        activeScopeKey = key
        if (scope == null || key == null) {
            publishActiveNotifications(emptyList())
            return emptyList()
        }

        val cached = cache[key]
        if (cached != null) {
            publishActiveNotifications(cached)
            return cached
        }

        publishActiveNotifications(emptyList())
        return loadForScope(scope).also(::publishActiveNotifications)
    }

    @Synchronized
    fun addOrUpdate(notification: AppNotification): Boolean {
        if (!::preferences.isInitialized) return false
        val scope = NotificationSessionResolver.scopeForNotification(notification) ?: return false
        val key = scope.storageKey()
        val items = loadForScope(scope).toMutableList()
        val existingIndex = items.indexOfFirst { item ->
            item.id == notification.id || item.dedupeKey == notification.dedupeKey
        }

        if (existingIndex >= 0) {
            items[existingIndex] = notification.copy(
                isRead = items[existingIndex].isRead,
            )
        } else {
            items += notification
        }

        val trimmed = items
            .sortedByDescending { it.createdAtMillis }
            .take(NotificationDefaults.MAX_INBOX_SIZE)
        cache[key] = trimmed
        saveScope(scope, trimmed)
        Log.d(TAG, "Inbox saved role=${scope.role.name} size=${trimmed.size}")
        if (activeScopeKey == key) {
            publishActiveNotifications(trimmed)
        }
        return true
    }

    @Synchronized
    fun markRead(notificationId: String): Boolean {
        val scope = activeScope() ?: return false
        return markRead(scope, notificationId)
    }

    @Synchronized
    fun markRead(
        expectedScope: NotificationScope,
        notificationId: String,
    ): Boolean {
        val scope = activeScope() ?: return false
        if (scope != expectedScope) return false
        val currentItems = loadForScope(scope)
        if (currentItems.none { notification -> notification.id == notificationId }) return false
        val updatedItems = markNotificationRead(currentItems, notificationId)
        cache[scope.storageKey()] = updatedItems
        saveScope(scope, updatedItems)
        publishActiveNotifications(updatedItems)
        return true
    }

    @Synchronized
    fun markAllRead() {
        val scope = activeScope() ?: return
        val updatedItems = markAllNotificationsRead(loadForScope(scope))
        cache[scope.storageKey()] = updatedItems
        saveScope(scope, updatedItems)
        publishActiveNotifications(updatedItems)
    }

    @Synchronized
    fun clearForSignedOutAccount() {
        activeScopeKey = null
        publishActiveNotifications(emptyList())
    }

    @Synchronized
    fun containsDedupeKey(
        expectedScope: NotificationScope,
        dedupeKey: String,
    ): Boolean {
        val scope = activeScope() ?: return false
        if (scope != expectedScope) return false
        return loadForScope(scope).any { notification -> notification.dedupeKey == dedupeKey }
    }

    @Synchronized
    fun findActiveNotification(notificationId: String): AppNotification? {
        val scope = activeScope() ?: return null
        return loadForScope(scope).firstOrNull { notification -> notification.id == notificationId }
    }

    private fun activeScope(): NotificationScope? {
        if (!::preferences.isInitialized) return null
        val scope = NotificationSessionResolver.currentScope() ?: return null
        if (scope.storageKey() != activeScopeKey) {
            refreshForCurrentAccount()
        }
        return scope
    }

    private fun loadForScope(scope: NotificationScope): List<AppNotification> {
        val key = scope.storageKey()
        cache[key]?.let { cached -> return cached }

        val raw = if (preferences.contains(key)) {
            preferences.getString(key, "").orEmpty()
        } else {
            preferences.getString(scope.legacyStorageKey(), "").orEmpty().also { legacyRaw ->
                if (legacyRaw.isNotBlank()) {
                    preferences.edit().putString(key, legacyRaw).apply()
                }
            }
        }
        val items = parsePersistedInboxOrEmpty(
            raw = raw,
            decoder = ::parseNotifications,
            onFailure = { error -> Log.w(TAG, "Persisted inbox decode failed", error) },
        )
        cache[key] = items
        return items
    }

    private fun saveScope(
        scope: NotificationScope,
        items: List<AppNotification>,
    ) {
        val payload = JSONArray().apply {
            items.forEach { item -> put(item.toJson()) }
        }
        preferences.edit()
            .putString(scope.storageKey(), payload.toString())
            .apply()
    }

    private fun parseNotifications(raw: String): List<AppNotification> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(item.toNotification())
            }
        }.sortedByDescending { it.createdAtMillis }
    }

    private fun publishActiveNotifications(notifications: List<AppNotification>) {
        mutableActiveNotifications.value = notifications.toList()
    }

    private fun NotificationScope.storageKey(): String {
        return notificationInboxStorageKey(this, AppEditionConfig.current)
    }

    private fun NotificationScope.legacyStorageKey(): String {
        return legacyNotificationInboxStorageKey(this)
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

    private const val PREFS_NAME = "pizza_time_notification_inbox"
    private const val TAG = "NotificationInboxStore"
}
