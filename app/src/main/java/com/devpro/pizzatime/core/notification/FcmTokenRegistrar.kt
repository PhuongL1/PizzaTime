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
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun registerCurrentToken() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                saveTokenForUser(uid, token)
            }
    }

    fun saveTokenForCurrentUser(token: String) {
        val uid = auth.currentUser?.uid ?: return
        saveTokenForUser(uid, token)
    }

    fun saveTokenForUser(uid: String, token: String) {
        val context = appContext ?: return
        if (uid.isBlank() || token.isBlank()) {
            return
        }

        val role = NotificationSessionResolver.currentRole()
        val installationId = installationId(context)
        firestore.collection("users").document(uid)
            .collection("devices")
            .document(installationId)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "appEdition" to AppEditionConfig.current.name,
                    "applicationId" to context.packageName,
                    "role" to role.name,
                    "installationId" to installationId,
                    "notificationsEnabled" to NotificationPermissionHelper.areNotificationsEnabled(context),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener {
                Log.d(TAG, "Token saved uid=$uid tokenSuffix=${token.takeLast(8)}")
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Token save failed uid=$uid", error)
            }
    }

    fun clearCurrentDeviceToken() {
        val context = appContext ?: return
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("devices")
            .document(installationId(context))
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
