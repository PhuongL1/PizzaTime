package com.devpro.pizzatime.feature.welcome

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.devpro.pizzatime.core.notification.OrderNotificationMonitor
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.databinding.FragmentWelcomeBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.shared.component.BaseFragment
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHomeScreen
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen
import com.google.firebase.auth.FirebaseAuth

class WelcomeFragment : BaseFragment<FragmentWelcomeBinding>(
    FragmentWelcomeBinding::inflate
) {

    private lateinit var viewModel: WelcomeViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[WelcomeViewModel::class.java]

        binding.btnStartOrdering.setOnClickListener { continueAsGuest() }
        binding.btnContinueAsGuest.setOnClickListener { continueAsGuest() }
        binding.btnLogin.setOnClickListener {
            openLoginScreen()
        }
    }

    private fun continueAsGuest() {
        FirebaseAuth.getInstance().signOut()
        OrderNotificationMonitor.stop()
        FakeSessionStore.logout()
        CartStore.onGuestSessionStarted()
        Log.d(TAG, "Guest session entered")
        openCustomerHomeScreen()
    }

    private companion object {
        const val TAG = "WelcomeFragment"
    }
}
