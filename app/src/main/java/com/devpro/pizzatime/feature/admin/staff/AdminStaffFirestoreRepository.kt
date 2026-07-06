package com.devpro.pizzatime.feature.admin.staff

import com.devpro.pizzatime.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

object AdminStaffFirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val STAFF_ROLES = setOf("STAFF", "KITCHEN", "SHIPPER", "ADMIN", "CUSTOMER")

    fun loadStaff(onResult: (Result<List<AdminStaffUiModel>>) -> Unit) {
        firestore.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents
                    .filter { doc ->
                        (doc.getString("role")?.uppercase(Locale.US) ?: "") in STAFF_ROLES
                    }
                    .sortedWith(
                        compareBy({ it.getString("role") ?: "" }, { it.getString("name") ?: "" }),
                    )
                    .map { it.toAdminStaffUiModel() }
                onResult(Result.success(items))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun toggleActive(
        userId: String,
        newActive: Boolean,
        onResult: (Result<Unit>) -> Unit,
    ) {
        firestore.collection("users").document(userId)
            .update(
                mapOf(
                    "active" to newActive,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    private fun DocumentSnapshot.toAdminStaffUiModel(): AdminStaffUiModel {
        val active = getBoolean("active") ?: true
        val email = getString("email") ?: ""
        return AdminStaffUiModel(
            id = id,
            name = getString("name").orEmpty().ifBlank { email },
            role = mapRole(getString("role") ?: ""),
            status = if (active) AdminStaffStatus.ACTIVE else AdminStaffStatus.INACTIVE,
            note = email,
            avatarRes = R.drawable.ic_admin_view_reports,
            isHighlighted = active,
        )
    }

    private fun mapRole(role: String): AdminStaffRole {
        return when (role.uppercase(Locale.US)) {
            "KITCHEN" -> AdminStaffRole.KITCHEN
            "SHIPPER" -> AdminStaffRole.SHIPPER
            "ADMIN" -> AdminStaffRole.ADMIN
            "CUSTOMER" -> AdminStaffRole.CUSTOMER
            else -> AdminStaffRole.STAFF
        }
    }
}

