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
            "pickupLat" to settings.pickupLat,
            "pickupLng" to settings.pickupLng,
            "storePhone" to settings.storePhone.trim(),
            "openingHours" to settings.openingHours.trim(),
            "acceptingOrders" to settings.acceptingOrders,
            "baseDeliveryFee" to settings.baseDeliveryFee,
            "deliveryFeePerKm" to settings.deliveryFeePerKm,
            "freeDeliveryMinSubtotal" to settings.freeDeliveryMinSubtotal,
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
            pickupLat = getDouble("pickupLat"),
            pickupLng = getDouble("pickupLng"),
            storePhone = getString("storePhone").orEmpty(),
            openingHours = getString("openingHours").orEmpty()
                .ifBlank { StoreSettingsUiModel.DEFAULT_OPENING_HOURS },
            acceptingOrders = getBoolean("acceptingOrders") ?: true,
            baseDeliveryFee = getDouble("baseDeliveryFee")
                ?: StoreSettingsUiModel.DEFAULT_BASE_DELIVERY_FEE,
            deliveryFeePerKm = getDouble("deliveryFeePerKm")
                ?: StoreSettingsUiModel.DEFAULT_DELIVERY_FEE_PER_KM,
            freeDeliveryMinSubtotal = getDouble("freeDeliveryMinSubtotal")
                ?: StoreSettingsUiModel.DEFAULT_FREE_DELIVERY_MIN_SUBTOTAL,
        )
    }

    private const val APP_CONFIG_COLLECTION = "appConfig"
    private const val STORE_DOCUMENT = "store"
}
