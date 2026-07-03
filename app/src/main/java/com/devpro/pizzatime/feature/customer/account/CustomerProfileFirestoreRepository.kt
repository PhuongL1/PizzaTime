package com.devpro.pizzatime.feature.customer.account

import com.devpro.pizzatime.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object CustomerProfileFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadProfile(
        uid: String,
        onResult: (Result<CustomerAccountUiModel>) -> Unit,
    ) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onResult(Result.failure(Exception("Profile not found.")))
                    return@addOnSuccessListener
                }
                onResult(Result.success(document.toCustomerAccountUiModel()))
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun updateProfile(
        uid: String,
        name: String,
        phone: String,
        deliveryAddress: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "name" to name,
                    "phone" to phone,
                    "deliveryAddress" to deliveryAddress,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    private fun DocumentSnapshot.toCustomerAccountUiModel(): CustomerAccountUiModel {
        val fallback = FakeCustomerAccountData.getCustomerAccount()
        return CustomerAccountUiModel(
            fullName = getString("name").orEmpty().ifBlank { fallback.fullName },
            tierName = fallback.tierName,
            doughPoints = fallback.doughPoints,
            email = getString("email").orEmpty().ifBlank { fallback.email },
            phone = getString("phone").orEmpty(),
            deliveryAddress = getString("deliveryAddress").orEmpty(),
            avatarUrl = getString("avatarUrl").orEmpty(),
            avatarRes = R.drawable.ic_customer_account_avatar_placeholder,
        )
    }
}
