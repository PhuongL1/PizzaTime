package com.devpro.pizzatime.feature.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.databinding.FragmentSplashBinding
import com.devpro.pizzatime.feature.auth.FirebaseAuthRepository
import com.devpro.pizzatime.feature.staff.navigation.clearAppBackStack
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard
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
        if (FirebaseAuth.getInstance().currentUser == null) {
            openWelcome()
            return
        }

        FirebaseAuthRepository.loadCurrentUserProfile { result ->
            if (!isAdded) return@loadCurrentUserProfile
            result
                .onSuccess { user ->
                    FakeSessionStore.login(user.role)
                    openHomeByRole(user.role)
                }
                .onFailure {
                    FirebaseAuth.getInstance().signOut()
                    FakeSessionStore.logout()
                    openWelcome()
                }
        }
    }

    private fun openHomeByRole(role: UserRole) {
        clearAppBackStack()
        when (role) {
            UserRole.GUEST,
            UserRole.CUSTOMER,
                -> openCustomerHome(addToBackStack = false)

            UserRole.STAFF -> openStaffDashboard(addToBackStack = false)
            UserRole.KITCHEN -> openKitchenBoard(addToBackStack = false)
            UserRole.SHIPPER -> openShipperDeliveryDashboard(addToBackStack = false)
            UserRole.ADMIN -> openAdminDashboard(addToBackStack = false)
        }
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
