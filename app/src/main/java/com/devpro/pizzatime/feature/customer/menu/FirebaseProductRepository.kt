package com.devpro.pizzatime.feature.customer.menu

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object FirebaseProductRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun loadProducts(onResult: (List<ProductUiModel>) -> Unit) {
        firestore.collection("products")
            .whereEqualTo("available", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val products = snapshot.documents.mapNotNull { mapDocument(it) }
                onResult(products)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    private fun mapDocument(doc: DocumentSnapshot): ProductUiModel? {
        val name = doc.getString("name") ?: return null
        val averageRating = doc.getDouble("averageRating")
            ?: doc.getDouble("rating")
            ?: 0.0
        val ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt().coerceAtLeast(0)
        return ProductUiModel(
            id = doc.id,
            name = name,
            description = doc.getString("description") ?: "",
            basePrice = doc.getDouble("basePrice") ?: 0.0,
            imageUrl = doc.getString("imageUrl") ?: "",
            rating = averageRating,
            averageRating = averageRating,
            ratingCount = ratingCount,
            available = doc.getBoolean("available") ?: false,
            categoryId = doc.getString("categoryId") ?: "",
            categoryName = doc.getString("categoryName") ?: "",
            sizeOptions = doc.getStringList("sizeOptions"),
            crustOptions = doc.getStringList("crustOptions"),
            toppingOptions = doc.getStringList("toppingOptions"),
        )
    }

    private fun DocumentSnapshot.getStringList(field: String): List<String> {
        return (get(field) as? List<*>)
            ?.mapNotNull { it as? String }
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinctBy { it.lowercase(Locale.US) }
            ?: emptyList()
    }
}
