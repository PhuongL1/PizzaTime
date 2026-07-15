package com.devpro.pizzatime.feature.shipper.tracking

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import java.util.Date

sealed interface DeliveryTrackingRepositoryResult<out T> {
    data class Success<T>(val value: T) : DeliveryTrackingRepositoryResult<T>

    data class Failure(
        val kind: DeliveryTrackingRepositoryFailureKind,
        val cause: Throwable,
    ) : DeliveryTrackingRepositoryResult<Nothing>
}

enum class DeliveryTrackingRepositoryFailureKind {
    TRANSIENT,
    PERMANENT_AUTHORIZATION,
    PERMANENT_INVALID_DATA,
}

fun interface DeliveryTrackingRegistration {
    fun remove()
}

class DeliveryTrackingFirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun validateEligibility(
        shipperId: String,
        orderId: String,
        onResult: (DeliveryTrackingRepositoryResult<DeliveryTrackingEligibility>) -> Unit,
    ) {
        if (!DeliveryTrackingActorIdPolicy.isValid(shipperId) || !DeliveryTrackingOrderIdPolicy.isValid(orderId)) {
            onResult(invalidDataFailure())
            return
        }

        firestore.collection(USERS_COLLECTION)
            .document(shipperId)
            .get(Source.SERVER)
            .addOnSuccessListener { userDocument ->
                val user = userDocument.toTrackingUserState()
                if (!user.exists || !user.active || !user.role.equals(SHIPPER_ROLE, ignoreCase = true)) {
                    onResult(
                        DeliveryTrackingRepositoryResult.Success(
                            DeliveryTrackingEligibilityPolicy.evaluateServer(
                                expectedShipperId = shipperId,
                                user = user,
                                order = DeliveryTrackingOrderState(
                                    exists = false,
                                    shipperId = null,
                                    status = null,
                                ),
                            ),
                        ),
                    )
                    return@addOnSuccessListener
                }

                firestore.collection(ORDERS_COLLECTION)
                    .document(orderId)
                    .get(Source.SERVER)
                    .addOnSuccessListener { orderDocument ->
                        onResult(
                            DeliveryTrackingRepositoryResult.Success(
                                DeliveryTrackingEligibilityPolicy.evaluateServer(
                                    expectedShipperId = shipperId,
                                    user = user,
                                    order = orderDocument.toTrackingOrderState(),
                                ),
                            ),
                        )
                    }
                    .addOnFailureListener { error -> onResult(error.toRepositoryFailure()) }
            }
            .addOnFailureListener { error -> onResult(error.toRepositoryFailure()) }
    }

    fun observeParentOrder(
        shipperId: String,
        orderId: String,
        onResult: (DeliveryTrackingRepositoryResult<DeliveryTrackingEligibility>) -> Unit,
    ): DeliveryTrackingRegistration {
        if (!DeliveryTrackingActorIdPolicy.isValid(shipperId) || !DeliveryTrackingOrderIdPolicy.isValid(orderId)) {
            onResult(invalidDataFailure())
            return DeliveryTrackingRegistration { }
        }

        val registration: ListenerRegistration = firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(error.toRepositoryFailure())
                    return@addSnapshotListener
                }
                val order = snapshot?.toTrackingOrderState()
                    ?: DeliveryTrackingOrderState(false, null, null)
                onResult(
                    DeliveryTrackingRepositoryResult.Success(
                        DeliveryTrackingEligibilityPolicy.evaluateParentOrder(
                            expectedShipperId = shipperId,
                            order = order,
                        ),
                    ),
                )
            }
        return DeliveryTrackingRegistration(registration::remove)
    }

    fun writeCurrentLocation(
        orderId: String,
        payload: DeliveryTrackingWritePayload,
        onResult: (DeliveryTrackingRepositoryResult<Unit>) -> Unit,
    ) {
        if (
            !DeliveryTrackingOrderIdPolicy.isValid(orderId) ||
            !DeliveryTrackingPayloadPolicy.isValid(payload)
        ) {
            onResult(invalidDataFailure())
            return
        }

        val fields = payload.canonicalFields(
            locationValue = GeoPoint(
                payload.sample.coordinate.latitude,
                payload.sample.coordinate.longitude,
            ),
            recordedAtValue = Timestamp(Date(payload.sample.recordedAtMillis)),
            updatedAtValue = FieldValue.serverTimestamp(),
        )
        if (fields.keys.any { field -> field !in DeliveryTrackingWritePayload.APPROVED_FIELDS }) {
            onResult(invalidDataFailure())
            return
        }

        firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .collection(TRACKING_COLLECTION)
            .document(CURRENT_DOCUMENT)
            .set(fields)
            .addOnSuccessListener {
                onResult(DeliveryTrackingRepositoryResult.Success(Unit))
            }
            .addOnFailureListener { error -> onResult(error.toRepositoryFailure()) }
    }

    private fun DocumentSnapshot.toTrackingUserState(): DeliveryTrackingUserState {
        return DeliveryTrackingUserState(
            exists = exists(),
            active = getBoolean(FIELD_ACTIVE) ?: false,
            role = getString(FIELD_ROLE),
        )
    }

    private fun DocumentSnapshot.toTrackingOrderState(): DeliveryTrackingOrderState {
        return DeliveryTrackingOrderState(
            exists = exists(),
            shipperId = getString(FIELD_SHIPPER_ID),
            status = getString(FIELD_STATUS),
        )
    }

    private fun Throwable.toRepositoryFailure(): DeliveryTrackingRepositoryResult.Failure {
        val code = (this as? FirebaseFirestoreException)?.code
        val kind = when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED,
            -> DeliveryTrackingRepositoryFailureKind.PERMANENT_AUTHORIZATION

            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> {
                DeliveryTrackingRepositoryFailureKind.PERMANENT_INVALID_DATA
            }

            else -> DeliveryTrackingRepositoryFailureKind.TRANSIENT
        }
        return DeliveryTrackingRepositoryResult.Failure(kind = kind, cause = this)
    }

    private fun invalidDataFailure(): DeliveryTrackingRepositoryResult.Failure {
        return DeliveryTrackingRepositoryResult.Failure(
            kind = DeliveryTrackingRepositoryFailureKind.PERMANENT_INVALID_DATA,
            cause = IllegalArgumentException("Invalid delivery tracking request."),
        )
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val ORDERS_COLLECTION = "orders"
        private const val TRACKING_COLLECTION = "tracking"
        private const val CURRENT_DOCUMENT = "current"
        private const val FIELD_ACTIVE = "active"
        private const val FIELD_ROLE = "role"
        private const val FIELD_SHIPPER_ID = "shipperId"
        private const val FIELD_STATUS = "status"
        private const val SHIPPER_ROLE = "SHIPPER"
    }
}

object DeliveryTrackingOrderIdPolicy {
    private const val MAXIMUM_ORDER_ID_LENGTH = 200

    fun normalize(rawOrderId: String?): String? {
        val normalized = rawOrderId?.trim().orEmpty()
        return normalized.takeIf { value ->
            value.isNotBlank() &&
                value.length <= MAXIMUM_ORDER_ID_LENGTH &&
                '/' !in value
        }
    }

    fun isValid(orderId: String): Boolean = normalize(orderId) == orderId
}
