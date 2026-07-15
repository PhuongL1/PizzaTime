package com.devpro.pizzatime.feature.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.notification.FcmTokenRegistrar
import com.devpro.pizzatime.core.notification.OrderNotificationMonitor
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.databinding.FragmentSplashBinding
import com.devpro.pizzatime.feature.auth.FirebaseAuthRepository
import com.devpro.pizzatime.feature.auth.restoreSessionAndOpenRoleHome
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.clearAppBackStack
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHomeScreen
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen
import com.devpro.pizzatime.feature.welcome.WelcomeFragment
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding: FragmentSplashBinding
        get() = checkNotNull(_binding) {
            "FragmentSplashBinding is only valid between onCreateView and onDestroyView."
        }

    private val handler = Handler(Looper.getMainLooper())

    private val routeFromSplashRunnable = Runnable { routeFromSplash() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handler.postDelayed(routeFromSplashRunnable, SPLASH_DELAY_MS)
    }

    override fun onDestroyView() {
        handler.removeCallbacks(routeFromSplashRunnable)
        _binding = null
        super.onDestroyView()
    }

    private fun routeFromSplash() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            openStartDestinationForEdition()
            return
        }

        FirebaseAuthRepository.loadCurrentUserProfile { result ->
            if (_binding == null || !isAdded) return@loadCurrentUserProfile
            result
                .onSuccess { user ->
                    if (!AppEditionConfig.isAllowedAuthRole(user.role)) {
                        signOutAndOpenStartDestination(showMismatchMessage = true)
                        return@onSuccess
                    }
                    val openedHome = restoreSessionAndOpenRoleHome(user)
                    if (!openedHome) {
                        signOutAndOpenStartDestination(showMismatchMessage = true)
                    }
                }
                .onFailure {
                    signOutAndOpenStartDestination(showMismatchMessage = false)
                }
        }
    }

    private fun signOutAndOpenStartDestination(showMismatchMessage: Boolean) {
        FcmTokenRegistrar.clearCurrentDeviceToken()
        FirebaseAuth.getInstance().signOut()
        OrderNotificationMonitor.stop()
        FakeSessionStore.logout()
        CartStore.clearForLogout()
        if (showMismatchMessage) {
            AppUiMessageBus.publish(
                textRes = R.string.app_edition_mismatch,
                type = UiMessageType.ERROR,
            )
        }
        openStartDestinationForEdition()
    }

    private fun openStartDestinationForEdition() {
        clearAppBackStack()
        when {
            AppEditionConfig.isGuestEdition -> {
                OrderNotificationMonitor.stop()
                FakeSessionStore.logout()
                CartStore.onGuestSessionStarted()
                openCustomerHomeScreen()
            }

            AppEditionConfig.current == AppEdition.CUSTOMER -> {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragmentContainer, WelcomeFragment())
                    .commitAllowingStateLoss()
            }

            else -> {
                openLoginScreen(addToBackStack = false)
            }
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1800L
    }
}
