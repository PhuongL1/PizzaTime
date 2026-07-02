package com.devpro.pizzatime.feature.admin.menu

import com.devpro.pizzatime.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object AdminMenuFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadProducts(onResult: (Result<List<AdminMenuUiModel>>) -> Unit) {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents
                    .sortedWith(compareBy({ it.getString("categoryId") ?: "" }, { it.getString("name") ?: "" }))
                    .map { it.toAdminMenuUiModel() }
                onResult(Result.success(items))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun toggleAvailability(
        productId: String,
        newAvailable: Boolean,
        onResult: (Result<Unit>) -> Unit,
    ) {
        firestore.collection("products").document(productId)
            .update(
                mapOf(
                    "available" to newAvailable,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun DocumentSnapshot.toAdminMenuUiModel(): AdminMenuUiModel {
        val basePrice = getDouble("basePrice") ?: 0.0
        return AdminMenuUiModel(
            id = id,
            name = getString("name") ?: "",
            description = getString("description") ?: "",
            price = String.format(Locale.US, "$%.2f", basePrice),
            category = mapCategory(getString("categoryId") ?: ""),
            imageRes = R.drawable.img_pizza_time,
            isAvailable = getBoolean("available") ?: true,
        )
    }

    private fun mapCategory(categoryId: String): AdminMenuCategory {
        return when (categoryId.uppercase(Locale.US)) {
            "CLASSIC" -> AdminMenuCategory.CLASSIC
            "VEGGIE" -> AdminMenuCategory.VEGGIE
            else -> AdminMenuCategory.SIGNATURE
        }
    }
}

