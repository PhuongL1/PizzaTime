package com.devpro.pizzatime.feature.admin.store

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object StoreSettingsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun loadStoreSettings(onResult: (Result<StoreSettingsUiModel>) -> Unit) {
        firestore.collection(APP_CONFIG_COLLECTION).document(STORE_DOCUMENT)
            .get()
            .addOnSuccessListener { document ->
                onResult(Result.success(document.toStoreSettings()))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun saveStoreSettings(
        settings: StoreSettingsUiModel,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val data = mapOf(
            "storeName" to settings.storeName.trim(),
            "pickupAddress" to settings.pickupAddress.trim(),
            "storePhone" to settings.storePhone.trim(),
            "openingHours" to settings.openingHours.trim(),
            "acceptingOrders" to settings.acceptingOrders,
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        firestore.collection(APP_CONFIG_COLLECTION).document(STORE_DOCUMENT)
            .set(data)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    private fun DocumentSnapshot.toStoreSettings(): StoreSettingsUiModel {
        if (!exists()) {
            return StoreSettingsUiModel()
        }

        return StoreSettingsUiModel(
            storeName = getString("storeName").orEmpty()
                .ifBlank { StoreSettingsUiModel.DEFAULT_STORE_NAME },
            pickupAddress = getString("pickupAddress").orEmpty(),
            storePhone = getString("storePhone").orEmpty(),
            openingHours = getString("openingHours").orEmpty()
                .ifBlank { StoreSettingsUiModel.DEFAULT_OPENING_HOURS },
            acceptingOrders = getBoolean("acceptingOrders") ?: true,
        )
    }

    private const val APP_CONFIG_COLLECTION = "appConfig"
    private const val STORE_DOCUMENT = "store"
}
