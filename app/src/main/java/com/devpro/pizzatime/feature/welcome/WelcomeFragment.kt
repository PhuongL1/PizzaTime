package com.devpro.pizzatime.feature.welcome

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.devpro.pizzatime.databinding.FragmentWelcomeBinding
import com.devpro.pizzatime.shared.component.BaseFragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.auth.LoginFragment
import com.devpro.pizzatime.feature.auth.LoginRequiredFragment
import com.devpro.pizzatime.feature.customer.home.CustomerHomeFragment

class WelcomeFragment : BaseFragment<FragmentWelcomeBinding>(
    FragmentWelcomeBinding::inflate
) {

    private lateinit var viewModel: WelcomeViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[WelcomeViewModel::class.java]

        binding.btnStartOrdering.setOnClickListener {
            openCustomerHomeScreen()
        }
        binding.btnLogin.setOnClickListener {
            openLoginScreen()
        }
    }
    private fun openLoginScreen() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, LoginRequiredFragment())
            .addToBackStack(null)
            .commit()
    }
    private fun openCustomerHomeScreen() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, CustomerHomeFragment())
            .commit()
    }
}