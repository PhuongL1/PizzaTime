package com.devpro.pizzatime.feature.admin.menu

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

object AdminProductImageStorageRepository {

    private val storage = FirebaseStorage.getInstance()

    fun uploadProductImage(
        productId: String,
        imageUri: Uri,
        onResult: (Result<String>) -> Unit,
    ) {
        val timestamp = System.currentTimeMillis()
        val imageRef = storage.reference.child("product-images/$productId/$timestamp.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onResult(Result.success(downloadUri.toString()))
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }
}
