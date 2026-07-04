package com.devpro.pizzatime.feature.admin.menu

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CloudinaryProductImageRepository {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun uploadProductImage(
        context: Context,
        imageUri: Uri,
        onResult: (Result<String>) -> Unit,
    ) {
        if (!CloudinaryConfig.isConfigured) {
            onResult(Result.failure(Exception("Cloudinary is not configured.")))
            return
        }

        val appContext = context.applicationContext
        Thread {
            val result = runCatching {
                uploadMultipart(appContext, imageUri)
            }
            mainHandler.post {
                onResult(result)
            }
        }.start()
    }

    private fun uploadMultipart(
        context: Context,
        imageUri: Uri,
    ): String {
        val boundary = "PizzaTime${System.currentTimeMillis()}"
        val uploadUrl = URL(
            "https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/image/upload",
        )
        val connection = (uploadUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        val imageName = imageUri.lastPathSegment?.substringAfterLast('/') ?: "product-image"
        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"

        try {
            BufferedOutputStream(connection.outputStream).use { output ->
                output.writeText("--$boundary\r\n")
                output.writeText(
                    "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n" +
                        "${CloudinaryConfig.UPLOAD_PRESET}\r\n",
                )
                output.writeText("--$boundary\r\n")
                output.writeText(
                    "Content-Disposition: form-data; name=\"folder\"\r\n\r\n" +
                        "${CloudinaryConfig.FOLDER}\r\n",
                )
                output.writeText("--$boundary\r\n")
                output.writeText(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$imageName\"\r\n" +
                        "Content-Type: $mimeType\r\n\r\n",
                )

                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    input.copyTo(output)
                } ?: throw IllegalArgumentException("Could not open selected image.")

                output.writeText("\r\n--$boundary--\r\n")
                output.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                throw IllegalStateException(responseBody.ifBlank { "Cloudinary upload failed." })
            }

            return JSONObject(responseBody).optString("secure_url")
                .ifBlank { throw IllegalStateException("Cloudinary response did not include secure_url.") }
        } finally {
            connection.disconnect()
        }
    }

    private fun BufferedOutputStream.writeText(value: String) {
        write(value.toByteArray(Charsets.UTF_8))
    }

    private const val TIMEOUT_MS = 30_000
}
