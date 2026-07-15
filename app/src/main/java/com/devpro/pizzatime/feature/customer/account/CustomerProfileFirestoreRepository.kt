package com.devpro.pizzatime.feature.customer.account

import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.shared.location.DeliveryLocationSelection
import com.devpro.pizzatime.shared.location.OrderDeliveryDestinationResolver
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

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
        clearDeliveryLocation: Boolean = false,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "fullName" to name,
            "phone" to phone,
            "deliveryAddress" to deliveryAddress,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (clearDeliveryLocation) {
            updates[OrderDeliveryDestinationResolver.LOCATION_FIELD] = FieldValue.delete()
            updates[OrderDeliveryDestinationResolver.LEGACY_LATITUDE_FIELD] = FieldValue.delete()
            updates[OrderDeliveryDestinationResolver.LEGACY_LONGITUDE_FIELD] = FieldValue.delete()
        }
        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun updateDeliveryLocation(
        uid: String,
        selection: DeliveryLocationSelection,
        onResult: (Result<Unit>) -> Unit,
    ) {
        val coordinate = selection.coordinate
        if (selection.address.isBlank() || coordinate == null) {
            onResult(Result.failure(IllegalArgumentException("A delivery address and coordinate are required.")))
            return
        }
        val updates = OrderDeliveryDestinationResolver.canonicalFields(
            address = selection.address,
            coordinate = coordinate,
        ).toMutableMap()
        updates[OrderDeliveryDestinationResolver.LEGACY_LATITUDE_FIELD] = FieldValue.delete()
        updates[OrderDeliveryDestinationResolver.LEGACY_LONGITUDE_FIELD] = FieldValue.delete()
        updates["updatedAt"] = FieldValue.serverTimestamp()
        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    fun updateAvatarUrl(
        uid: String,
        avatarUrl: String,
        onResult: (Result<Unit>) -> Unit,
    ) {
        firestore.collection("users").document(uid)
            .update(
                mapOf(
                    "avatarUrl" to avatarUrl,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    private fun DocumentSnapshot.toCustomerAccountUiModel(): CustomerAccountUiModel {
        val fallback = FakeCustomerAccountData.getCustomerAccount()
        val deliveryCoordinate = OrderDeliveryDestinationResolver.resolve(data.orEmpty())
        return CustomerAccountUiModel(
            fullName = getString("fullName").orEmpty()
                .ifBlank { getString("name").orEmpty() }
                .ifBlank { fallback.fullName },
            tierName = fallback.tierName,
            doughPoints = getLong("doughPoints")?.toInt() ?: fallback.doughPoints,
            email = getString("email").orEmpty().ifBlank { fallback.email },
            phone = getString("phone").orEmpty(),
            deliveryAddress = getString("deliveryAddress").orEmpty(),
            deliveryCoordinate = deliveryCoordinate,
            avatarUrl = getString("avatarUrl").orEmpty(),
            avatarRes = R.drawable.ic_customer_account_avatar_placeholder,
            lifetimeSpendText = getDouble("lifetimeSpend")?.let(::formatCurrency)
                ?: fallback.lifetimeSpendText,
            completedOrdersText = getLong("completedOrders")?.toString()
                ?: fallback.completedOrdersText,
            role = UserRole.fromString(getString("role")) ?: fallback.role,
            active = getBoolean("active") ?: fallback.active,
        )
    }

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(value)
    }
}
