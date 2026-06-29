package com.devpro.pizzatime.feature.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import androidx.lifecycle.ViewModelProvider
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.databinding.FragmentLoginBinding
import com.devpro.pizzatime.feature.customer.home.CustomerHomeFragment
import com.devpro.pizzatime.feature.staff.dashboard.StaffDashboardFragment

class LoginFragment : Fragment() {

    private lateinit var viewModel: LoginViewModel

    private var _binding: FragmentLoginBinding? = null
    private val binding: FragmentLoginBinding
        get() = checkNotNull(_binding) {
            "FragmentLoginBinding is only valid between onCreateView and onDestroyView."
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setupActions()
        observeLoginResult()
    }

    private fun setupActions() {
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }

        binding.btnCreateAccount.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnForgotPassword.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.auth_forgot_password_coming_soon),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun observeLoginResult() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result
                .onSuccess { user ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.auth_login_success_as, user.displayName),
                        Toast.LENGTH_SHORT,
                    ).show()
                    openHomeByRole(user.role)
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: getString(R.string.auth_login_failed),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun handleLogin() {
        val identifier = binding.edtIdentifier.text.toString().trim()
        val password = binding.edtPassword.text.toString()

        when {
            identifier.isBlank() -> {
                binding.edtIdentifier.error = getString(R.string.auth_identifier_required)
            }

            isLikelyEmail(identifier) && !Patterns.EMAIL_ADDRESS.matcher(identifier).matches() -> {
                binding.edtIdentifier.error = getString(R.string.auth_invalid_email)
            }

            password.isBlank() -> {
                binding.edtPassword.error = getString(R.string.auth_password_required)
            }

            else -> {
                viewModel.login(
                    identifier = identifier,
                    password = password,
                )
            }
        }
    }

    private fun openHomeByRole(role: UserRole) {
        when (role) {
            UserRole.GUEST,
            UserRole.CUSTOMER,
                -> openCustomerHome()

            UserRole.STAFF -> openStaffDashboard()

            UserRole.KITCHEN -> showRoleComingSoon(R.string.role_kitchen)

            UserRole.SHIPPER -> showRoleComingSoon(R.string.role_shipper)

            UserRole.ADMIN -> showRoleComingSoon(R.string.role_admin)
        }
    }

    private fun openCustomerHome() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, CustomerHomeFragment())
            .commit()
    }

    private fun openStaffDashboard() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, StaffDashboardFragment())
            .commit()
    }

    private fun showRoleComingSoon(roleNameRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.role_screen_coming_soon, getString(roleNameRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun isLikelyEmail(value: String): Boolean {
        return value.contains("@")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}