package com.devpro.pizzatime.feature.shipper.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.devpro.pizzatime.MainActivity
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.notification.DeliveryTrackingNotificationContract
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.google.firebase.auth.FirebaseAuth

class DeliveryTrackingService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val auth: FirebaseAuth by lazy(FirebaseAuth::getInstance)
    private val repository: DeliveryTrackingFirestoreRepository by lazy {
        DeliveryTrackingFirestoreRepository()
    }
    private val locationManager: LocationManager? by lazy {
        getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private var activeOrderId: String? = null
    private var activeShipperId: String? = null
    private var lifecycleGeneration = 0L
    private var parentOrderRegistration: DeliveryTrackingRegistration? = null
    private var locationUpdatesRegistered = false
    private var writeInFlight = false
    private var newestObservedSampleTimeMillis = 0L
    private var pendingSample: DeliveryTrackingLocationSample? = null
    private var lastWrittenSample: WrittenDeliveryTrackingSample? = null
    private var preserveTerminalStateOnDestroy = false

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val expectedShipperId = activeShipperId ?: return@AuthStateListener
        if (firebaseAuth.currentUser?.uid != expectedShipperId) {
            stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
        }
    }

    private val eligibilityRevalidation = Runnable {
        val generation = lifecycleGeneration
        if (activeOrderId != null && activeShipperId != null) {
            validateServerEligibility(generation = generation, initialValidation = false)
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            acceptLocation(location)
        }

        @Deprecated("Deprecated by Android")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) {
            if (!hasEnabledLocationProvider()) {
                stopTracking(DeliveryTrackingRuntimePhase.SERVICES_UNAVAILABLE)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
            ACTION_START -> {
                val orderId = DeliveryTrackingOrderIdPolicy.normalize(
                    intent.getStringExtra(EXTRA_ORDER_ID),
                )
                if (orderId == null) {
                    stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
                } else {
                    beginTracking(orderId)
                }
            }
            else -> stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lifecycleGeneration += 1L
        clearTrackingResources()
        auth.removeAuthStateListener(authStateListener)
        if (!preserveTerminalStateOnDestroy) {
            DeliveryTrackingRuntimeStateStore.publish(DeliveryTrackingRuntimeState.Inactive)
        }
        super.onDestroy()
    }

    private fun beginTracking(orderId: String) {
        lifecycleGeneration += 1L
        clearTrackingResources()
        preserveTerminalStateOnDestroy = false

        val shipperId = auth.currentUser?.uid
        val localEligibility = DeliveryTrackingEligibilityPolicy.evaluateLocal(
            edition = AppEditionConfig.current,
            sessionLoggedIn = FakeSessionStore.isLoggedIn,
            sessionRole = FakeSessionStore.currentRole,
            authenticatedUid = shipperId,
        )
        if (localEligibility !is DeliveryTrackingEligibility.Eligible || shipperId == null) {
            stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
            return
        }
        if (!hasLocationPermission()) {
            activeOrderId = orderId
            stopTracking(DeliveryTrackingRuntimePhase.PERMISSION_REQUIRED)
            return
        }
        if (!hasEnabledLocationProvider()) {
            activeOrderId = orderId
            stopTracking(DeliveryTrackingRuntimePhase.SERVICES_UNAVAILABLE)
            return
        }

        activeOrderId = orderId
        activeShipperId = shipperId
        publishState(DeliveryTrackingRuntimePhase.STARTING)
        if (!startForegroundSafely(orderId)) {
            stopTracking(DeliveryTrackingRuntimePhase.PERMISSION_REQUIRED)
            return
        }

        validateServerEligibility(
            generation = lifecycleGeneration,
            initialValidation = true,
        )
    }

    private fun validateServerEligibility(
        generation: Long,
        initialValidation: Boolean,
    ) {
        if (!isCurrentGeneration(generation)) {
            return
        }
        val orderId = activeOrderId ?: return
        val shipperId = activeShipperId ?: return
        val localEligibility = DeliveryTrackingEligibilityPolicy.evaluateLocal(
            edition = AppEditionConfig.current,
            sessionLoggedIn = FakeSessionStore.isLoggedIn,
            sessionRole = FakeSessionStore.currentRole,
            authenticatedUid = auth.currentUser?.uid,
        )
        if (localEligibility !is DeliveryTrackingEligibility.Eligible || auth.currentUser?.uid != shipperId) {
            stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
            return
        }
        if (!hasLocationPermission()) {
            stopTracking(DeliveryTrackingRuntimePhase.PERMISSION_REQUIRED)
            return
        }
        if (!hasEnabledLocationProvider()) {
            stopTracking(DeliveryTrackingRuntimePhase.SERVICES_UNAVAILABLE)
            return
        }

        repository.validateEligibility(shipperId, orderId) { result ->
            if (!isCurrentGeneration(generation)) {
                return@validateEligibility
            }
            when (result) {
                is DeliveryTrackingRepositoryResult.Success -> {
                    if (result.value !is DeliveryTrackingEligibility.Eligible) {
                        stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
                        return@validateEligibility
                    }
                    if (initialValidation) {
                        observeParentOrder(generation, shipperId, orderId)
                        registerLocationUpdates()
                    }
                    if (isCurrentGeneration(generation)) {
                        scheduleEligibilityRevalidation()
                    }
                }
                is DeliveryTrackingRepositoryResult.Failure -> {
                    if (
                        initialValidation ||
                        result.kind != DeliveryTrackingRepositoryFailureKind.TRANSIENT
                    ) {
                        stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
                    } else {
                        scheduleEligibilityRevalidation()
                    }
                }
            }
        }
    }

    private fun observeParentOrder(
        generation: Long,
        shipperId: String,
        orderId: String,
    ) {
        parentOrderRegistration?.remove()
        parentOrderRegistration = repository.observeParentOrder(shipperId, orderId) { result ->
            if (!isCurrentGeneration(generation)) {
                return@observeParentOrder
            }
            when (result) {
                is DeliveryTrackingRepositoryResult.Success -> {
                    if (result.value !is DeliveryTrackingEligibility.Eligible) {
                        stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
                    }
                }
                is DeliveryTrackingRepositoryResult.Failure -> {
                    if (result.kind != DeliveryTrackingRepositoryFailureKind.TRANSIENT) {
                        stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationUpdates() {
        if (locationUpdatesRegistered) {
            return
        }
        val manager = locationManager
        if (manager == null) {
            stopTracking(DeliveryTrackingRuntimePhase.SERVICES_UNAVAILABLE)
            return
        }
        if (!hasLocationPermission()) {
            stopTracking(DeliveryTrackingRuntimePhase.PERMISSION_REQUIRED)
            return
        }

        var registrations = 0
        var permissionFailure = false
        enabledProviders(manager).forEach { provider ->
            try {
                manager.requestLocationUpdates(
                    provider,
                    DeliveryTrackingConfig.LOCATION_UPDATE_INTERVAL_MILLIS,
                    DeliveryTrackingConfig.LOCATION_UPDATE_DISTANCE_METERS,
                    locationListener,
                    Looper.getMainLooper(),
                )
                registrations += 1
            } catch (_: SecurityException) {
                permissionFailure = true
            } catch (_: IllegalArgumentException) {
                // A provider can disappear between the enabled check and registration.
            }
        }
        if (registrations == 0) {
            stopTracking(
                if (permissionFailure) {
                    DeliveryTrackingRuntimePhase.PERMISSION_REQUIRED
                } else {
                    DeliveryTrackingRuntimePhase.SERVICES_UNAVAILABLE
                },
            )
            return
        }
        locationUpdatesRegistered = true
        publishState(DeliveryTrackingRuntimePhase.WAITING_FOR_LOCATION)
    }

    private fun acceptLocation(location: Location) {
        if (!locationUpdatesRegistered || !hasLocationPermission()) {
            if (!hasLocationPermission()) {
                stopTracking(DeliveryTrackingRuntimePhase.PERMISSION_REQUIRED)
            }
            return
        }
        val sample = DeliveryTrackingLocationPolicy.validate(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy.toDouble() else Double.NaN,
            recordedAtMillis = location.time,
            nowMillis = System.currentTimeMillis(),
            bearingDegrees = if (location.hasBearing()) location.bearing.toDouble() else null,
            speedMetersPerSecond = if (location.hasSpeed()) location.speed.toDouble() else null,
        ) ?: return
        if (sample.recordedAtMillis <= newestObservedSampleTimeMillis) {
            return
        }
        newestObservedSampleTimeMillis = sample.recordedAtMillis
        pendingSample = sample
        drainPendingSample()
    }

    private fun drainPendingSample() {
        if (writeInFlight) {
            return
        }
        val orderId = activeOrderId ?: return
        val shipperId = activeShipperId ?: return
        val sample = pendingSample ?: return
        val nowMillis = System.currentTimeMillis()
        if (!DeliveryTrackingThrottlePolicy.shouldWrite(sample, lastWrittenSample, nowMillis)) {
            return
        }

        pendingSample = null
        writeInFlight = true
        val generation = lifecycleGeneration
        repository.writeCurrentLocation(
            orderId = orderId,
            payload = DeliveryTrackingWritePayload(shipperId = shipperId, sample = sample),
        ) { result ->
            if (!isCurrentGeneration(generation)) {
                return@writeCurrentLocation
            }
            writeInFlight = false
            when (result) {
                is DeliveryTrackingRepositoryResult.Success -> {
                    lastWrittenSample = WrittenDeliveryTrackingSample(
                        sample = sample,
                        writtenAtMillis = System.currentTimeMillis(),
                    )
                    publishState(
                        phase = DeliveryTrackingRuntimePhase.ACTIVE,
                        lastUpdateMillis = sample.recordedAtMillis,
                    )
                    drainPendingSample()
                }
                is DeliveryTrackingRepositoryResult.Failure -> {
                    if (result.kind == DeliveryTrackingRepositoryFailureKind.TRANSIENT) {
                        val queued = pendingSample
                        if (queued == null || queued.recordedAtMillis < sample.recordedAtMillis) {
                            pendingSample = sample
                        }
                    } else {
                        stopTracking(DeliveryTrackingRuntimePhase.INACTIVE)
                    }
                }
            }
        }
    }

    private fun scheduleEligibilityRevalidation() {
        mainHandler.removeCallbacks(eligibilityRevalidation)
        mainHandler.postDelayed(
            eligibilityRevalidation,
            DeliveryTrackingConfig.ORDER_REVALIDATION_INTERVAL_MILLIS,
        )
    }

    private fun stopTracking(phase: DeliveryTrackingRuntimePhase) {
        val orderId = activeOrderId
        preserveTerminalStateOnDestroy = phase != DeliveryTrackingRuntimePhase.INACTIVE
        lifecycleGeneration += 1L
        clearTrackingResources()
        DeliveryTrackingRuntimeStateStore.publish(
            if (phase == DeliveryTrackingRuntimePhase.INACTIVE) {
                DeliveryTrackingRuntimeState.Inactive
            } else {
                DeliveryTrackingRuntimeState(
                    phase = phase,
                    orderId = orderId,
                    lastLocationUpdateMillis = null,
                )
            },
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun clearTrackingResources() {
        mainHandler.removeCallbacks(eligibilityRevalidation)
        parentOrderRegistration?.remove()
        parentOrderRegistration = null
        if (locationUpdatesRegistered) {
            locationManager?.let { manager ->
                runCatching { manager.removeUpdates(locationListener) }
            }
        }
        locationUpdatesRegistered = false
        activeOrderId = null
        activeShipperId = null
        writeInFlight = false
        newestObservedSampleTimeMillis = 0L
        pendingSample = null
        lastWrittenSample = null
    }

    private fun publishState(
        phase: DeliveryTrackingRuntimePhase,
        lastUpdateMillis: Long? = DeliveryTrackingRuntimeStateStore.current().lastLocationUpdateMillis,
    ) {
        DeliveryTrackingRuntimeStateStore.publish(
            DeliveryTrackingRuntimeState(
                phase = phase,
                orderId = activeOrderId,
                lastLocationUpdateMillis = lastUpdateMillis,
            ),
        )
    }

    private fun isCurrentGeneration(generation: Long): Boolean {
        return generation == lifecycleGeneration && activeOrderId != null && activeShipperId != null
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasEnabledLocationProvider(): Boolean {
        val manager = locationManager ?: return false
        return enabledProviders(manager).isNotEmpty()
    }

    private fun enabledProviders(manager: LocationManager): List<String> {
        return LOCATION_PROVIDERS.filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
    }

    private fun startForegroundSafely(orderId: String): Boolean {
        ensureNotificationChannel()
        val notification = buildTrackingNotification(orderId)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.delivery_tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.delivery_tracking_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    private fun buildTrackingNotification(orderId: String): Notification {
        val detailIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.Builder()
                .scheme(NOTIFICATION_URI_SCHEME)
                .authority(NOTIFICATION_URI_AUTHORITY)
                .appendPath(orderId)
                .build()
            putExtra(DeliveryTrackingNotificationContract.EXTRA_ORDER_ID, orderId)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            orderId.hashCode(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val publicNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pizza)
            .setContentTitle(getString(R.string.notification_public_title))
            .setContentText(getString(R.string.notification_public_body))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pizza)
            .setContentTitle(getString(R.string.delivery_tracking_notification_title))
            .setContentText(getString(R.string.delivery_tracking_notification_body, orderId))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        const val EXTRA_ORDER_ID = "delivery_tracking_service_order_id"
        const val ACTION_START = "com.devpro.pizzatime.action.START_DELIVERY_TRACKING"
        const val ACTION_STOP = "com.devpro.pizzatime.action.STOP_DELIVERY_TRACKING"

        private const val CHANNEL_ID = "pizzatime_delivery_tracking"
        private const val NOTIFICATION_ID = 80_004
        private const val NOTIFICATION_URI_SCHEME = "pizzatime-delivery-tracking"
        private const val NOTIFICATION_URI_AUTHORITY = "order"
        private val LOCATION_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        )

        fun start(context: Context, orderId: String): Boolean {
            val normalizedOrderId = DeliveryTrackingOrderIdPolicy.normalize(orderId) ?: return false
            val appContext = context.applicationContext
            val intent = Intent(appContext, DeliveryTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ORDER_ID, normalizedOrderId)
            }
            return runCatching {
                ContextCompat.startForegroundService(appContext, intent)
                true
            }.getOrDefault(false)
        }

        fun stop(context: Context): Boolean {
            val appContext = context.applicationContext
            val intent = Intent(appContext, DeliveryTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            return runCatching {
                appContext.startService(intent)
                true
            }.getOrDefault(false)
        }
    }
}
