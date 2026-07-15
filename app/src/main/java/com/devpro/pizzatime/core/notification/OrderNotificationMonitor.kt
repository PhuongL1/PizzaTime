package com.devpro.pizzatime.core.notification

import android.content.Context
import android.util.Log
import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

object OrderNotificationMonitor {

    private val firestore = FirebaseFirestore.getInstance()

    private var appContext: Context? = null
    private var activeScope: NotificationScope? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private var ordersInitialSnapshotComplete = false
    private var reviewsInitialSnapshotComplete = false
    private var loggedMissingOrderReviewSource = false

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        AppForegroundState.init()
        NotificationInboxStore.init(applicationContext)
        NotificationStateStore.init(applicationContext)
        PizzaTimeNotificationManager.init(applicationContext)
    }

    fun start(role: UserRole) {
        val context = appContext ?: return
        val scope = NotificationSessionResolver.currentScope()
        if (role == UserRole.GUEST || scope == null || scope.role != role) {
            stop()
            return
        }

        if (activeScope == scope && listeners.isNotEmpty()) {
            return
        }

        stop()
        activeScope = scope
        ordersInitialSnapshotComplete = false
        reviewsInitialSnapshotComplete = false
        loggedMissingOrderReviewSource = false

        NotificationWorkScheduler.schedule(context, scope)
        FcmTokenRegistrar.registerCurrentToken(context)
        startOrderListener(scope)
        if (scope.role == UserRole.ADMIN) {
            startProductReviewListener(scope)
            Log.d(TAG, "Order review source not found in current schema")
            loggedMissingOrderReviewSource = true
        }
        Log.d(TAG, "Listener started role=${scope.role.name} edition=${scope.applicationId}")
    }

    fun stop() {
        listeners.forEach { registration -> registration.remove() }
        listeners.clear()
        activeScope?.let { scope ->
            appContext?.let { context ->
                NotificationWorkScheduler.cancel(context, scope)
            }
        }
        activeScope = null
        ordersInitialSnapshotComplete = false
        reviewsInitialSnapshotComplete = false
        NotificationInboxStore.clearForSignedOutAccount()
        Log.d(TAG, "Listener stopped")
    }

    private fun startOrderListener(scope: NotificationScope) {
        val query = if (scope.role == UserRole.CUSTOMER) {
            firestore.collection("orders").whereEqualTo("customerId", scope.userId)
        } else {
            firestore.collection("orders")
        }

        listeners += query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logFirestoreError("orders", scope.role, error)
                return@addSnapshotListener
            }

            val currentSnapshot = snapshot ?: return@addSnapshotListener
            if (!ordersInitialSnapshotComplete) {
                seedInitialOrderSnapshot(scope, currentSnapshot.documents)
                ordersInitialSnapshotComplete = true
                Log.d(TAG, "Initial order snapshot complete role=${scope.role.name}")
                return@addSnapshotListener
            }

            currentSnapshot.documentChanges.forEach { change ->
                if (change.type == DocumentChange.Type.REMOVED) {
                    return@forEach
                }
                processOrderDocument(scope, change.document)
            }
        }
    }

    private fun startProductReviewListener(scope: NotificationScope) {
        listeners += firestore.collection("productReviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreError("productReviews", scope.role, error)
                    return@addSnapshotListener
                }

                val currentSnapshot = snapshot ?: return@addSnapshotListener
                if (!reviewsInitialSnapshotComplete) {
                    val newest = currentSnapshot.documents.maxOfOrNull { document ->
                        notificationEpochMillis(document.get("createdAt"))
                    } ?: System.currentTimeMillis()
                    NotificationStateStore.setLastProductReviewSyncAt(scope, newest)
                    reviewsInitialSnapshotComplete = true
                    Log.d(TAG, "Initial product review snapshot complete")
                    return@addSnapshotListener
                }

                currentSnapshot.documentChanges.forEach { change ->
                    if (change.type != DocumentChange.Type.ADDED) {
                        return@forEach
                    }
                    processProductReview(scope, change.document)
                }
            }
    }

    private fun seedInitialOrderSnapshot(
        scope: NotificationScope,
        documents: List<DocumentSnapshot>,
    ) {
        val states = documents.associate { document ->
            document.id to OrderNotificationState(
                status = NotificationEventFactory.normalizeStatus(document.getString("status")),
                updatedAtMillis = notificationEpochMillis(document.get("updatedAt")),
                latestHistoryAtMillis = NotificationEventFactory.latestHistoryAtMillis(document),
                handoffStatus = NotificationEventFactory.currentHandoffStatus(document),
                latestHandoffAtMillis = NotificationEventFactory.latestHandoffAtMillis(document),
            )
        }
        NotificationStateStore.putOrderStates(scope, states)
        val newest = documents.maxOfOrNull { document ->
            notificationEpochMillis(document.get("updatedAt"))
        } ?: System.currentTimeMillis()
        NotificationStateStore.setLastOrdersSyncAt(scope, newest)
    }

    private fun processOrderDocument(
        scope: NotificationScope,
        document: DocumentSnapshot,
    ) {
        val previousState = NotificationStateStore.getOrderState(scope, document.id)
        val previousHistoryAt = previousState?.latestHistoryAtMillis ?: 0L
        val historyEvents = notificationHistoryEventsAfter(
            events = NotificationEventFactory.historyEvents(document),
            lastSeenMillis = previousHistoryAt,
        )

        NotificationEventFactory.createOrderNotifications(
            context = appContext ?: return,
            scope = scope,
            document = document,
            historyEvents = historyEvents,
        ).forEach { notification ->
            Log.d(TAG, "Event detected role=${scope.role.name} type=${notification.type.name}")
            NotificationDispatcher.dispatch(appContext ?: return, notification, TAG)
        }
        NotificationEventFactory.createHandoffNotifications(
            context = appContext ?: return,
            scope = scope,
            document = document,
            previousState = previousState,
        ).forEach { notification ->
            Log.d(TAG, "Handoff event detected role=${scope.role.name} type=${notification.type.name}")
            NotificationDispatcher.dispatch(appContext ?: return, notification, TAG)
        }

        val updatedAt = notificationEpochMillis(document.get("updatedAt"))
        NotificationStateStore.putOrderState(
            scope = scope,
            orderId = document.id,
            state = OrderNotificationState(
                status = NotificationEventFactory.normalizeStatus(document.getString("status")),
                updatedAtMillis = updatedAt,
                latestHistoryAtMillis = NotificationEventFactory.latestHistoryAtMillis(document),
                handoffStatus = NotificationEventFactory.currentHandoffStatus(document),
                latestHandoffAtMillis = NotificationEventFactory.latestHandoffAtMillis(document),
            ),
        )
        NotificationStateStore.setLastOrdersSyncAt(
            scope,
            maxOf(NotificationStateStore.lastOrdersSyncAt(scope), updatedAt),
        )
    }

    private fun processProductReview(
        scope: NotificationScope,
        document: DocumentSnapshot,
    ) {
        val createdAt = notificationEpochMillis(document.get("createdAt"))
        if (createdAt <= NotificationStateStore.lastProductReviewSyncAt(scope)) {
            return
        }
        val productId = document.getString("productId").orEmpty().trim()
        if (productId.isBlank()) {
            NotificationStateStore.setLastProductReviewSyncAt(scope, createdAt)
            return
        }

        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { productDocument ->
                val productName = productDocument.getString("name")
                val notification = runCatching {
                    NotificationEventFactory.createProductReviewNotification(
                        context = appContext ?: return@addOnSuccessListener,
                        scope = scope,
                        document = document,
                        productName = productName,
                    )
                }.getOrNull() ?: return@addOnSuccessListener
                NotificationDispatcher.dispatch(appContext ?: return@addOnSuccessListener, notification, TAG)
                NotificationStateStore.setLastProductReviewSyncAt(scope, createdAt)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Product review name lookup failed", error)
                val notification = runCatching {
                    NotificationEventFactory.createProductReviewNotification(
                        context = appContext ?: return@addOnFailureListener,
                        scope = scope,
                        document = document,
                        productName = null,
                    )
                }.getOrNull() ?: return@addOnFailureListener
                NotificationDispatcher.dispatch(appContext ?: return@addOnFailureListener, notification, TAG)
                NotificationStateStore.setLastProductReviewSyncAt(scope, createdAt)
            }
    }

    private fun logFirestoreError(
        source: String,
        role: UserRole,
        error: FirebaseFirestoreException,
    ) {
        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            Log.e(TAG, "Firestore PERMISSION_DENIED source=$source role=${role.name}", error)
        } else {
            Log.w(TAG, "Listener failed source=$source role=${role.name}", error)
        }
    }

    private const val TAG = "NotificationMonitor"
}
