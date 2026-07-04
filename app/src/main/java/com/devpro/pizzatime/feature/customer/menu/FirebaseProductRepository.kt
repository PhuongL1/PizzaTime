package com.devpro.pizzatime.feature.customer.menu

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

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
        return ProductUiModel(
            id = doc.id,
            name = name,
            description = doc.getString("description") ?: "",
            basePrice = doc.getDouble("basePrice") ?: 0.0,
            imageUrl = doc.getString("imageUrl") ?: "",
            rating = doc.getDouble("rating") ?: 0.0,
            available = doc.getBoolean("available") ?: false,
            categoryId = doc.getString("categoryId") ?: "",
            categoryName = doc.getString("categoryName") ?: "",
        )
    }
}

