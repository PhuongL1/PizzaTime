package com.devpro.pizzatime.feature.welcome

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.devpro.pizzatime.databinding.FragmentWelcomeBinding
import com.devpro.pizzatime.shared.component.BaseFragment
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHomeScreen
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen

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

}