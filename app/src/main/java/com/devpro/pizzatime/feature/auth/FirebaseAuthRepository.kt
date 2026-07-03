package com.devpro.pizzatime.feature.auth

import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun login(email: String, password: String, onResult: (Result<AuthUserUiModel>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    auth.signOut()
                    onResult(Result.failure(Exception("User profile not found.")))
                    return@addOnSuccessListener
                }
                fetchUserProfile(uid, email, onResult)
            }
            .addOnFailureListener {
                onResult(Result.failure(Exception("Invalid email or password.")))
            }
    }

    fun loadCurrentUserProfile(onResult: (Result<AuthUserUiModel>) -> Unit) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(Result.failure(Exception("No authenticated user.")))
            return
        }

        fetchUserProfile(
            uid = uid,
            email = currentUser.email.orEmpty(),
            onResult = onResult,
        )
    }

    private fun fetchUserProfile(
        uid: String,
        email: String,
        onResult: (Result<AuthUserUiModel>) -> Unit,
    ) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    auth.signOut()
                    onResult(Result.failure(Exception("User profile not found.")))
                    return@addOnSuccessListener
                }

                val active = doc.getBoolean("active") ?: false
                if (!active) {
                    auth.signOut()
                    onResult(Result.failure(Exception("Your account is disabled.")))
                    return@addOnSuccessListener
                }

                val role = UserRole.fromString(doc.getString("role"))
                if (role == null) {
                    auth.signOut()
                    onResult(Result.failure(Exception("Your account role is not supported.")))
                    return@addOnSuccessListener
                }

                val displayName = doc.getString("name") ?: email
                onResult(
                    Result.success(
                        AuthUserUiModel(
                            uid = uid,
                            identifier = email,
                            displayName = displayName,
                            role = role,
                        ),
                    ),
                )
            }
            .addOnFailureListener {
                auth.signOut()
                onResult(Result.failure(Exception("User profile not found.")))
            }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    onResult(Result.failure(Exception("Registration failed. Please try again.")))
                    return@addOnSuccessListener
                }
                createUserDocument(uid, name, email, onResult)
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(Exception(e.message ?: "Registration failed.")))
            }
    }

    private fun createUserDocument(
        uid: String,
        name: String,
        email: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val userDoc = hashMapOf(
            "id" to uid,
            "name" to name,
            "email" to email,
            "phone" to "",
            "role" to "CUSTOMER",
            "active" to true,
            "avatarUrl" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("users").document(uid)
            .set(userDoc)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener {
                onResult(Result.failure(Exception("Registration failed. Please try again.")))
            }
    }
}

