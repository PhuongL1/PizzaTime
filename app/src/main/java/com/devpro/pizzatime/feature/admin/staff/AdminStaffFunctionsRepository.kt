package com.devpro.pizzatime.feature.admin.staff

import com.google.firebase.functions.FirebaseFunctions

object AdminStaffFunctionsRepository {

    private val functions = FirebaseFunctions.getInstance()

    fun createStaffAccount(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String,
        onResult: (Result<String>) -> Unit,
    ) {
        val data = hashMapOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "password" to password,
            "role" to role,
        )

        functions
            .getHttpsCallable("createStaffAccount")
            .call(data)
            .addOnSuccessListener { result ->
                val uid = (result.data as? Map<*, *>)?.get("uid") as? String
                if (uid.isNullOrBlank()) {
                    onResult(Result.failure(Exception("Staff account was created without a user id.")))
                } else {
                    onResult(Result.success(uid))
                }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(Exception(error.message ?: "Could not create staff account.")))
            }
    }
}
