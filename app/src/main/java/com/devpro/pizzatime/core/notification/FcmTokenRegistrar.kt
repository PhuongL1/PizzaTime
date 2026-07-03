package com.devpro.pizzatime.core.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

@Suppress("DEPRECATION")
object FcmTokenRegistrar {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

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
        if (uid.isBlank() || token.isBlank()) {
            return
        }

        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "fcmTokens" to FieldValue.arrayUnion(token),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
    }
}
