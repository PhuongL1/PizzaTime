package com.devpro.pizzatime.feature.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.notification.FcmTokenRegistrar
import com.devpro.pizzatime.core.notification.OrderNotificationMonitor
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.databinding.FragmentLoginBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.clearAppBackStack
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.feature.staff.navigation.openCheckoutScreen
import com.devpro.pizzatime.feature.staff.navigation.openForgotPassword
import com.google.firebase.auth.FirebaseAuth

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
        binding.btnCreateAccount.isVisible =
            AppEditionConfig.current == AppEdition.GUEST || AppEditionConfig.current == AppEdition.CUSTOMER

        binding.btnLogin.setOnClickListener {
            handleLogin()
        }

        binding.btnCreateAccount.setOnClickListener {
            openRegister()
        }

        binding.btnForgotPassword.setOnClickListener {
            openForgotPassword()
        }
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun observeLoginResult() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result
                .onSuccess { user ->
                    if (!AppEditionConfig.isAllowedAuthRole(user.role)) {
                        FirebaseAuth.getInstance().signOut()
                        OrderNotificationMonitor.stop()
                        FakeSessionStore.logout()
                        CartStore.clearForLogout()
                        Toast.makeText(
                            requireContext(),
                            R.string.app_edition_mismatch,
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@onSuccess
                    }

                    CartStore.onUserChanged(user.uid)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.auth_login_success_as, user.displayName),
                        Toast.LENGTH_SHORT,
                    ).show()

                    FcmTokenRegistrar.registerCurrentToken()
                    openHomeByRole(user.role)
                }
                .onFailure { error ->
                    OrderNotificationMonitor.stop()
                    FakeSessionStore.logout()
                    CartStore.clearForLogout()
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

            !Patterns.EMAIL_ADDRESS.matcher(identifier).matches() -> {
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

    private fun continueLoginNavigation(role: UserRole) {
        if (resumePendingCheckoutIfNeeded(role)) {
            return
        }
        openHomeByRole(role)
    }

    private fun resumePendingCheckoutIfNeeded(role: UserRole): Boolean {
        val pendingDestination = PendingAuthDestinationStore.consume(requireContext())
        if (pendingDestination != PendingAuthDestinationStore.DESTINATION_CHECKOUT) {
            return false
        }

        if (role != UserRole.CUSTOMER) {
            return false
        }

        FakeSessionStore.login(role)
        clearAppBackStack()
        if (CartStore.items.isEmpty()) {
            Toast.makeText(requireContext(), R.string.cart_empty_message, Toast.LENGTH_SHORT).show()
            openCartScreen(addToBackStack = false)
            return true
        }

        Log.d(TAG, "Pending checkout resumed after login")
        openCheckoutScreen(addToBackStack = false)
        return true
    }

    private fun openHomeByRole(role: UserRole) {
        FakeSessionStore.login(role)
        openRoleHome(role)
    }

    private fun openRegister() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, RegisterFragment())
            .addToBackStack(null)
            .commit()
    }



    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TAG = "LoginFragment"
    }
}
