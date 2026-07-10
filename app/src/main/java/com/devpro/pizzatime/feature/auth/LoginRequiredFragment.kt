package com.devpro.pizzatime.feature.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentLoginRequiredBinding
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen

class LoginRequiredFragment : Fragment() {

    private var _binding: FragmentLoginRequiredBinding? = null
    private val binding: FragmentLoginRequiredBinding
        get() = checkNotNull(_binding) {
            "FragmentLoginRequiredBinding is only valid between onCreateView and onDestroyView."
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginRequiredBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupActions()
    }

    private fun setupActions() {
        binding.btnOpenLogin.setOnClickListener {
            openLoginScreen()
        }

        binding.btnCreateAccount.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnContinueBrowsing.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
