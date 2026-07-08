package com.devpro.pizzatime.core.notification

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.lang.ref.WeakReference
import java.util.Locale

object OrderNotificationMonitor {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val knownStatuses = mutableMapOf<String, String>()

    private var appContext: Context? = null
    private var foregroundActivity: WeakReference<Activity>? = null
    private var listener: ListenerRegistration? = null
    private var activeRole: UserRole? = null
    private var activeUserId: String = ""
    private var initialSnapshot = true

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureChannel(context.applicationContext)
    }

    fun setForegroundActivity(activity: Activity?) {
        foregroundActivity = activity?.let { WeakReference(it) }
    }

    fun start(role: UserRole) {
        val userId = auth.currentUser?.uid.orEmpty()
        if (role == UserRole.GUEST || userId.isBlank()) {
            stop()
            return
        }

        if (listener != null && activeRole == role && activeUserId == userId) {
            return
        }

        val context = appContext ?: return
        val query = buildQuery(role, userId) ?: return
        stop()
        activeRole = role
        activeUserId = userId
        initialSnapshot = true

        Log.d(TAG, "Order notification listener start role=${role.name}")
        listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Order notification listener failed", error)
                return@addSnapshotListener
            }

            val currentSnapshot = snapshot ?: return@addSnapshotListener
            if (initialSnapshot) {
                currentSnapshot.documents.forEach { doc ->
                    knownStatuses[doc.id] = normalizeStatus(doc.getString("status"))
                }
                initialSnapshot = false
                return@addSnapshotListener
            }

            currentSnapshot.documentChanges.forEach { change ->
                val doc = change.document
                val orderId = doc.id
                val status = normalizeStatus(doc.getString("status"))
                val previousStatus = knownStatuses[orderId]
                knownStatuses[orderId] = status

                val event = resolveEvent(
                    role = role,
                    status = status,
                    previousStatus = previousStatus,
                    isAdded = change.type == DocumentChange.Type.ADDED,
                ) ?: return@forEach

                dispatch(context, orderId, event)
            }
        }
    }

    fun stop() {
        if (listener != null) {
            Log.d(TAG, "Order notification listener stop")
        }
        listener?.remove()
        listener = null
        activeRole = null
        activeUserId = ""
        knownStatuses.clear()
        initialSnapshot = true
    }

    private fun buildQuery(role: UserRole, userId: String): Query? {
        val orders = firestore.collection("orders")
        return when (role) {
            UserRole.CUSTOMER -> orders.whereEqualTo("customerId", userId)
            UserRole.STAFF -> orders.whereEqualTo("status", STATUS_PENDING)
            UserRole.KITCHEN -> orders.whereEqualTo("status", STATUS_CONFIRMED)
            UserRole.SHIPPER -> orders.whereIn("status", listOf(STATUS_READY_FOR_DELIVERY, STATUS_READY))
            UserRole.ADMIN -> orders.whereEqualTo("status", STATUS_DELIVERED)
            UserRole.GUEST -> null
        }
    }

    private fun resolveEvent(
        role: UserRole,
        status: String,
        previousStatus: String?,
        isAdded: Boolean,
    ): OrderNotificationEvent? {
        return when (role) {
            UserRole.CUSTOMER -> resolveCustomerEvent(status, previousStatus, isAdded)
            UserRole.STAFF -> if (isAdded && status == STATUS_PENDING) {
                OrderNotificationEvent(
                    title = "New order",
                    body = "A new customer order arrived.",
                    foregroundMessage = "New order received.",
                )
            } else {
                null
            }
            UserRole.KITCHEN -> if (status == STATUS_CONFIRMED && previousStatus != STATUS_CONFIRMED) {
                OrderNotificationEvent(
                    title = "Order confirmed",
                    body = "A confirmed order is ready for kitchen.",
                    foregroundMessage = "Order confirmed.",
                )
            } else {
                null
            }
            UserRole.SHIPPER -> if (status in READY_FOR_DELIVERY_STATUSES && previousStatus != status) {
                OrderNotificationEvent(
                    title = "Order ready",
                    body = "An order is ready for delivery.",
                    foregroundMessage = "Order ready.",
                )
            } else {
                null
            }
            UserRole.ADMIN -> if (status == STATUS_DELIVERED && previousStatus != STATUS_DELIVERED) {
                OrderNotificationEvent(
                    title = "Order delivered",
                    body = "An order was delivered successfully.",
                    foregroundMessage = "Order delivered.",
                )
            } else {
                null
            }
            UserRole.GUEST -> null
        }
    }

    private fun resolveCustomerEvent(
        status: String,
        previousStatus: String?,
        isAdded: Boolean,
    ): OrderNotificationEvent? {
        if (isAdded && previousStatus == null) {
            return OrderNotificationEvent(
                title = "Order placed",
                body = "Your order was created.",
                foregroundMessage = "Your order was created.",
            )
        }

        if (previousStatus == null || previousStatus == status) {
            return null
        }

        return when (status) {
            STATUS_DELIVERING -> OrderNotificationEvent(
                title = "Delivery started",
                body = "Your order is on the way.",
                foregroundMessage = "Delivery started.",
            )
            STATUS_DELIVERED -> OrderNotificationEvent(
                title = "Order delivered",
                body = "Your order has arrived.",
                foregroundMessage = "Order delivered.",
            )
            else -> OrderNotificationEvent(
                title = "Order updated",
                body = "Your order status changed.",
                foregroundMessage = "Order status updated.",
            )
        }
    }

    private fun dispatch(
        context: Context,
        orderId: String,
        event: OrderNotificationEvent,
    ) {
        Log.i(TAG, "Order notification event title=${event.title} orderId=$orderId")
        val activity = foregroundActivity?.get()
            ?.takeIf { currentActivity -> !currentActivity.isFinishing && !currentActivity.isDestroyed }
        if (activity != null) {
            Toast.makeText(activity, event.foregroundMessage, Toast.LENGTH_SHORT).show()
            return
        }

        showLocalNotification(context, orderId, event)
    }

    @SuppressLint("MissingPermission")
    private fun showLocalNotification(
        context: Context,
        orderId: String,
        event: OrderNotificationEvent,
    ) {
        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
            Log.d(TAG, "Notification permission missing; local notification skipped")
            return
        }

        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bag)
            .setContentTitle(event.title)
            .setContentText(event.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId(orderId, event.title), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    private fun notificationId(orderId: String, title: String): Int {
        return "$orderId:$title".hashCode()
    }

    private fun normalizeStatus(status: String?): String {
        return status.orEmpty().trim().uppercase(Locale.US)
    }

    private data class OrderNotificationEvent(
        val title: String,
        val body: String,
        val foregroundMessage: String,
    )

    private val READY_FOR_DELIVERY_STATUSES = setOf(STATUS_READY_FOR_DELIVERY, STATUS_READY)
    private const val CHANNEL_ID = "order_status_notifications"
    private const val TAG = "OrderNotificationMonitor"
    private const val STATUS_PENDING = "PENDING"
    private const val STATUS_CONFIRMED = "CONFIRMED"
    private const val STATUS_READY = "READY"
    private const val STATUS_READY_FOR_DELIVERY = "READY_FOR_DELIVERY"
    private const val STATUS_DELIVERING = "DELIVERING"
    private const val STATUS_DELIVERED = "DELIVERED"
}
