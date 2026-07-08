package com.devpro.pizzatime.feature.customer.favorites

import com.devpro.pizzatime.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object CustomerFavoritesFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadFavoriteProductIds(
        uid: String,
        onResult: (Result<List<String>>) -> Unit,
    ) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val ids = (document.get("favoriteProductIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                onResult(Result.success(ids))
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun loadFavoriteProducts(
        uid: String,
        onResult: (Result<List<CustomerFavoriteItemUiModel>>) -> Unit,
    ) {
        loadFavoriteProductIds(uid) { idsResult ->
            idsResult
                .onSuccess { ids ->
                    if (ids.isEmpty()) {
                        onResult(Result.success(emptyList()))
                    } else {
                        loadProductsById(
                            ids = ids,
                            loadedItems = emptyList(),
                            onResult = onResult,
                        )
                    }
                }
                .onFailure { error -> onResult(Result.failure(error)) }
        }
    }

    fun addFavorite(
        uid: String,
        productId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateFavorites(
            uid = uid,
            favoriteUpdate = FieldValue.arrayUnion(productId),
            onResult = onResult,
        )
    }

    fun removeFavorite(
        uid: String,
        productId: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        updateFavorites(
            uid = uid,
            favoriteUpdate = FieldValue.arrayRemove(productId),
            onResult = onResult,
        )
    }

    fun isFavorite(
        uid: String,
        productId: String,
        onResult: (Result<Boolean>) -> Unit,
    ) {
        loadFavoriteProductIds(uid) { result ->
            result
                .onSuccess { ids -> onResult(Result.success(productId in ids)) }
                .onFailure { error -> onResult(Result.failure(error)) }
        }
    }

    private fun updateFavorites(
        uid: String,
        favoriteUpdate: FieldValue,
        onResult: (Result<Unit>) -> Unit,
    ) {
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "favoriteProductIds" to favoriteUpdate,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    private fun loadProductsById(
        ids: List<String>,
        loadedItems: List<CustomerFavoriteItemUiModel>,
        onResult: (Result<List<CustomerFavoriteItemUiModel>>) -> Unit,
    ) {
        val productId = ids.firstOrNull()
        if (productId == null) {
            onResult(Result.success(loadedItems))
            return
        }

        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                val nextItems = document.toFavoriteItem()?.let { loadedItems + it } ?: loadedItems
                loadProductsById(
                    ids = ids.drop(1),
                    loadedItems = nextItems,
                    onResult = onResult,
                )
            }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    private fun DocumentSnapshot.toFavoriteItem(): CustomerFavoriteItemUiModel? {
        val name = getString("name") ?: return null
        return CustomerFavoriteItemUiModel(
            id = id,
            name = name,
            description = getString("description") ?: "",
            price = getDouble("basePrice") ?: 0.0,
            badge = null,
            categoryId = getString("categoryId").orEmpty(),
            categoryName = getString("categoryName").orEmpty(),
            imageRes = R.drawable.img_pizza_time,
            imageUrl = getString("imageUrl").orEmpty(),
            cardType = CustomerFavoriteCardType.FEATURED,
            sizeOptions = getStringList("sizeOptions"),
            crustOptions = getStringList("crustOptions"),
            toppingOptions = getStringList("toppingOptions"),
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
