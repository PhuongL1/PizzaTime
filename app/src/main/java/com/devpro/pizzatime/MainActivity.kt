package com.devpro.pizzatime

import android.Manifest
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.core.notification.AppForegroundState
import com.devpro.pizzatime.core.notification.FcmTokenRegistrar
import com.devpro.pizzatime.core.notification.NotificationDeepLinkCoordinator
import com.devpro.pizzatime.core.notification.NotificationInboxStore
import com.devpro.pizzatime.core.notification.NotificationPermissionHelper
import com.devpro.pizzatime.core.notification.NotificationSessionResolver
import com.devpro.pizzatime.core.notification.NotificationStateStore
import com.devpro.pizzatime.core.notification.OrderNotificationMonitor
import com.devpro.pizzatime.core.notification.PizzaTimeNotificationManager
import com.devpro.pizzatime.databinding.ActivityMainBinding
import com.devpro.pizzatime.feature.admin.dashboard.AdminDashboardFragment
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.home.CustomerHomeFragment
import com.devpro.pizzatime.feature.kitchen.board.KitchenBoardFragment
import com.devpro.pizzatime.feature.shipper.dashboard.ShipperDeliveryDashboardFragment
import com.devpro.pizzatime.feature.splash.SplashFragment
import com.devpro.pizzatime.feature.staff.dashboard.StaffDashboardFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val notificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
            FcmTokenRegistrar.registerCurrentToken()
        }

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(
            fm: FragmentManager,
            f: Fragment,
        ) {
            maybeRequestNotificationPermission(f)
            NotificationDeepLinkCoordinator.handlePendingRequest(
                context = applicationContext,
                fragmentManager = supportFragmentManager,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val systemBarColor = ContextCompat.getColor(this, R.color.pt_background)
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor
        CartStore.init(applicationContext)
        AppForegroundState.init()
        NotificationInboxStore.init(applicationContext)
        NotificationStateStore.init(applicationContext)
        FcmTokenRegistrar.init(applicationContext)
        PizzaTimeNotificationManager.init(applicationContext)
        OrderNotificationMonitor.init(applicationContext)
        NotificationDeepLinkCoordinator.captureIntent(applicationContext, intent)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SplashFragment())
                .commit()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationDeepLinkCoordinator.captureIntent(applicationContext, intent)
        NotificationDeepLinkCoordinator.handlePendingRequest(
            context = applicationContext,
            fragmentManager = supportFragmentManager,
        )
    }

    override fun onResume() {
        super.onResume()
        OrderNotificationMonitor.setForegroundActivity(this)
    }

    override fun onPause() {
        OrderNotificationMonitor.setForegroundActivity(null)
        super.onPause()
    }

    override fun onDestroy() {
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    private fun maybeRequestNotificationPermission(fragment: Fragment) {
        if (!NotificationPermissionHelper.requiresRuntimePermission()) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (!isHomeFragment(fragment)) {
            return
        }
        val scope = NotificationSessionResolver.currentScope(applicationContext) ?: return
        if (!NotificationPermissionHelper.shouldRequestNotificationPermission(this)) {
            return
        }
        if (permissionPrompted(scope.userId)) {
            return
        }

        markPermissionPrompted(scope.userId)
        AlertDialog.Builder(this)
            .setMessage(R.string.notification_permission_rationale)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .show()
    }

    private fun isHomeFragment(fragment: Fragment): Boolean {
        return fragment is CustomerHomeFragment ||
            fragment is StaffDashboardFragment ||
            fragment is KitchenBoardFragment ||
            fragment is ShipperDeliveryDashboardFragment ||
            fragment is AdminDashboardFragment
    }

    private fun permissionPrompted(userId: String): Boolean {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(promptKey(userId), false)
    }

    private fun markPermissionPrompted(userId: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(promptKey(userId), true)
            .apply()
    }

    private fun promptKey(userId: String): String {
        return "notification_permission_prompted_${packageName}_$userId"
    }

    companion object {
        private const val PREFS_NAME = "pizza_time_notification_permissions"
    }
}
