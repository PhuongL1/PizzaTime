package com.devpro.pizzatime.feature.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.notification.OrderNotificationMonitor
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.databinding.FragmentSplashBinding
import com.devpro.pizzatime.feature.auth.FirebaseAuthRepository
import com.devpro.pizzatime.feature.auth.restoreSessionAndOpenRoleHome
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.clearAppBackStack
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
            OrderNotificationMonitor.stop()
            FakeSessionStore.logout()
            CartStore.clearForLogout()
            openWelcome()
            return
        }

        FirebaseAuthRepository.loadCurrentUserProfile { result ->
            if (!isAdded) return@loadCurrentUserProfile
            result
                .onSuccess { user ->
                    val openedHome = restoreSessionAndOpenRoleHome(user)
                    if (!openedHome) {
                        signOutAndOpenWelcome()
                    }
                }
                .onFailure {
                    signOutAndOpenWelcome()
                }
        }
    }

    private fun signOutAndOpenWelcome() {
        FirebaseAuth.getInstance().signOut()
        OrderNotificationMonitor.stop()
        FakeSessionStore.logout()
        CartStore.clearForLogout()
        openWelcome()
    }

    private fun openWelcome() {
        clearAppBackStack()
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, WelcomeFragment())
            .commitAllowingStateLoss()
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1800L
    }
}
