package com.devpro.pizzatime.feature.customer.payment

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.coroutineContext

class DemoPaymentBackendRepository(
    private val idTokenProvider: DemoPaymentIdTokenProvider = FirebaseDemoPaymentIdTokenProvider(),
    private val transport: DemoPaymentHttpTransport = HttpUrlConnectionDemoPaymentTransport(),
) {

    suspend fun createPaymentSession(
        orderId: String,
        requestId: String,
    ): DemoPaymentSession {
        val backendConfig = DemoPaymentBackendConfig.configured()
            ?: throw DemoPaymentBackendException(
                DemoPaymentBackendErrorType.SERVICE_UNAVAILABLE,
                "Payment service is unavailable.",
            )
        val requestBody = buildCreatePaymentRequestJson(orderId, requestId)
        return executeCreatePayment(
            backendConfig = backendConfig,
            requestBody = requestBody,
            forceRefresh = false,
        )
    }

    private suspend fun executeCreatePayment(
        backendConfig: DemoPaymentBackendConfigValue,
        requestBody: String,
        forceRefresh: Boolean,
    ): DemoPaymentSession {
        val token = idTokenProvider.getIdToken(forceRefresh)
        val response = transport.postJson(
            url = "${backendConfig.baseUrl}/api/v1/payments/create",
            idToken = token,
            requestBody = requestBody,
        )
        if (response.statusCode == 401 && !forceRefresh) {
            return executeCreatePayment(
                backendConfig = backendConfig,
                requestBody = requestBody,
                forceRefresh = true,
            )
        }
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            throw parseCreatePaymentFailure(response.statusCode, response.responseBody)
        }
        return parseDemoPaymentSession(response.responseBody.orEmpty(), backendConfig)
    }
}

data class DemoPaymentHttpResponse(
    val statusCode: Int,
    val responseBody: String?,
)

interface DemoPaymentIdTokenProvider {
    suspend fun getIdToken(forceRefresh: Boolean): String
}

interface DemoPaymentHttpTransport {
    suspend fun postJson(
        url: String,
        idToken: String,
        requestBody: String,
    ): DemoPaymentHttpResponse
}

class FirebaseDemoPaymentIdTokenProvider : DemoPaymentIdTokenProvider {

    override suspend fun getIdToken(forceRefresh: Boolean): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw DemoPaymentBackendException(
                DemoPaymentBackendErrorType.SESSION_EXPIRED,
                "Your session has expired. Please sign in again.",
            )
        val tokenResult = suspendCancellableCoroutine<GetTokenResult> { continuation ->
            user.getIdToken(forceRefresh)
                .addOnSuccessListener { result ->
                    continuation.resume(result)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
        val token = tokenResult.token?.trim().orEmpty()
        if (token.isBlank()) {
            throw DemoPaymentBackendException(
                DemoPaymentBackendErrorType.SESSION_EXPIRED,
                "Your session has expired. Please sign in again.",
            )
        }
        return token
    }
}

class HttpUrlConnectionDemoPaymentTransport : DemoPaymentHttpTransport {

    override suspend fun postJson(
        url: String,
        idToken: String,
        requestBody: String,
    ): DemoPaymentHttpResponse = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Authorization", "Bearer $idToken")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        try {
            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            coroutineContext.ensureActive()
            val statusCode = connection.responseCode
            val inputStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
            DemoPaymentHttpResponse(
                statusCode = statusCode,
                responseBody = responseBody,
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
