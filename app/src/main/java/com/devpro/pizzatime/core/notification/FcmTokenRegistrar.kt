package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

@Suppress("DEPRECATION")
object FcmTokenRegistrar {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun registerCurrentToken(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                saveTokenForUser(context.applicationContext, uid, token)
            }
    }

    fun saveTokenForCurrentUser(
        context: Context,
        token: String,
    ) {
        val uid = auth.currentUser?.uid ?: return
        saveTokenForUser(context.applicationContext, uid, token)
    }

    private fun saveTokenForUser(
        context: Context,
        uid: String,
        token: String,
    ) {
        if (uid.isBlank() || token.isBlank()) {
            return
        }

        val scope = NotificationSessionResolver.currentScope() ?: return
        if (auth.currentUser?.uid != uid || scope.userId != uid) {
            return
        }
        val installationId = installationId(context)
        firestore.collection("users").document(uid)
            .collection("devices")
            .document(installationId)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "appEdition" to AppEditionConfig.current.name,
                    "applicationId" to scope.applicationId,
                    "role" to scope.role.name,
                    "installationId" to installationId,
                    "notificationsEnabled" to NotificationPermissionHelper.areNotificationsEnabled(context),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener {
                Log.d(TAG, "Token registration updated")
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Token registration failed", error)
            }
    }

    fun clearCurrentDeviceToken(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        val appContext = context.applicationContext
        firestore.collection("users").document(uid)
            .collection("devices")
            .document(installationId(appContext))
            .delete()
    }

    private fun installationId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_INSTALLATION_ID, "").orEmpty()
        if (existing.isNotBlank()) {
            return existing
        }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTALLATION_ID, generated).apply()
        return generated
    }

    private const val PREFS_NAME = "pizza_time_notification_tokens"
    private const val KEY_INSTALLATION_ID = "installation_id"
    private const val TAG = "PizzaTimeFCM"
}
