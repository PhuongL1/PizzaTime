package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class NotificationCatchUpWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        AppForegroundState.init()
        NotificationInboxStore.init(applicationContext)
        NotificationStateStore.init(applicationContext)
        NotificationDispatcher.init(applicationContext)
        PizzaTimeNotificationManager.init(applicationContext)

        val scope = NotificationSessionResolver.currentScope(applicationContext) ?: return Result.success()
        return runCatching {
            processOrders(scope)
            if (scope.role == com.devpro.pizzatime.core.session.UserRole.ADMIN) {
                processProductReviews(scope)
            }
            Log.d(TAG, "Worker success role=${scope.role.name}")
            Result.success()
        }.getOrElse { error ->
            if (error is FirebaseFirestoreException) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(TAG, "Firestore PERMISSION_DENIED role=${scope.role.name}", error)
                    return Result.success()
                }
                if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Log.w(TAG, "Retryable failure role=${scope.role.name}", error)
                    return Result.retry()
                }
            }
            Log.e(TAG, "Worker failure role=${scope.role.name}", error)
            Result.success()
        }
    }

    private fun processOrders(scope: NotificationScope) {
        val snapshot = Tasks.await(orderQuery(scope))
        val documents = snapshot.documents
        val lastSyncAt = NotificationStateStore.lastOrdersSyncAt(scope)
        if (lastSyncAt <= 0L) {
            seedOrderState(scope, documents)
            return
        }

        var newestSeen = lastSyncAt
        documents.forEach { document ->
            val updatedAt = document.getTimestamp("updatedAt").toEpochMillis()
            newestSeen = maxOf(newestSeen, updatedAt)
            val previousState = NotificationStateStore.getOrderState(scope, document.id)
            val previousHistoryAt = previousState?.latestHistoryAtMillis ?: 0L
            val historyEvents = NotificationEventFactory.historyEvents(document)
                .filter { event -> event.createdAtMillis > previousHistoryAt }

            NotificationEventFactory.createOrderNotifications(
                context = applicationContext,
                scope = scope,
                document = document,
                historyEvents = historyEvents,
            ).forEach { notification ->
                NotificationDispatcher.dispatch(notification, TAG)
            }

            NotificationStateStore.putOrderState(
                scope = scope,
                orderId = document.id,
                state = OrderNotificationState(
                    status = NotificationEventFactory.normalizeStatus(document.getString("status")),
                    updatedAtMillis = updatedAt,
                    latestHistoryAtMillis = NotificationEventFactory.latestHistoryAtMillis(document),
                ),
            )
        }
        NotificationStateStore.setLastOrdersSyncAt(scope, newestSeen)
    }

    private fun processProductReviews(scope: NotificationScope) {
        val snapshot = Tasks.await(firestore.collection("productReviews").get())
        val lastSyncAt = NotificationStateStore.lastProductReviewSyncAt(scope)
        val documents = snapshot.documents
        if (lastSyncAt <= 0L) {
            val newest = documents.maxOfOrNull { doc -> doc.getTimestamp("createdAt").toEpochMillis() } ?: 0L
            NotificationStateStore.setLastProductReviewSyncAt(scope, newest)
            return
        }

        var newestSeen = lastSyncAt
        documents.forEach { document ->
            val createdAt = document.getTimestamp("createdAt").toEpochMillis()
            newestSeen = maxOf(newestSeen, createdAt)
            if (createdAt <= lastSyncAt) {
                return@forEach
            }
            val productName = resolveProductName(document)
            val notification = runCatching {
                NotificationEventFactory.createProductReviewNotification(
                    context = applicationContext,
                    scope = scope,
                    document = document,
                    productName = productName,
                )
            }.getOrNull() ?: return@forEach
            NotificationDispatcher.dispatch(notification, TAG)
        }
        NotificationStateStore.setLastProductReviewSyncAt(scope, newestSeen)
    }

    private fun seedOrderState(
        scope: NotificationScope,
        documents: List<DocumentSnapshot>,
    ) {
        val states = documents.associate { document ->
            val updatedAt = document.getTimestamp("updatedAt").toEpochMillis()
            document.id to OrderNotificationState(
                status = NotificationEventFactory.normalizeStatus(document.getString("status")),
                updatedAtMillis = updatedAt,
                latestHistoryAtMillis = NotificationEventFactory.latestHistoryAtMillis(document),
            )
        }
        NotificationStateStore.putOrderStates(scope, states)
        val newest = documents.maxOfOrNull { document ->
            document.getTimestamp("updatedAt").toEpochMillis()
        } ?: System.currentTimeMillis()
        NotificationStateStore.setLastOrdersSyncAt(scope, newest)
    }

    private fun orderQuery(scope: NotificationScope) =
        if (scope.role == com.devpro.pizzatime.core.session.UserRole.CUSTOMER) {
            firestore.collection("orders").whereEqualTo("customerId", scope.userId).get()
        } else {
            firestore.collection("orders").get()
        }

    private fun resolveProductName(document: DocumentSnapshot): String? {
        val productId = document.getString("productId").orEmpty().trim()
        if (productId.isBlank()) {
            return null
        }
        return runCatching {
            val product = Tasks.await(firestore.collection("products").document(productId).get())
            product.getString("name")?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun com.google.firebase.Timestamp?.toEpochMillis(): Long {
        return this?.toDate()?.time ?: 0L
    }

    companion object {
        private const val TAG = "NotificationWorker"
    }
}
