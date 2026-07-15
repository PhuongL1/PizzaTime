package com.devpro.pizzatime.feature.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentRegisterBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.clearAppBackStack
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.feature.staff.navigation.openCheckoutScreen
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {

    private lateinit var viewModel: RegisterViewModel

    private var _binding: FragmentRegisterBinding? = null
    private val binding: FragmentRegisterBinding
        get() = checkNotNull(_binding) {
            "FragmentRegisterBinding is only valid between onCreateView and onDestroyView."
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        setupActions()
        observeRegisterResult()
    }

    private fun setupActions() {
        binding.btnCreateAccount.setOnClickListener {
            handleCreateAccount()
        }

        binding.btnBack.setOnClickListener {
            openLoginScreen()
        }

        binding.btnGoogle.setOnClickListener {
            showUiMessage(R.string.auth_google_signup_unavailable, UiMessageType.INFO)
        }

        binding.btnFacebook.setOnClickListener {
            showUiMessage(R.string.auth_facebook_signup_unavailable, UiMessageType.INFO)
        }
    }

    private fun handleCreateAccount() {
        val fullName = binding.edtFullName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPassword.text.toString()
        val confirmPassword = binding.edtConfirmPassword.text.toString()

        when {
            fullName.isBlank() -> {
                binding.edtFullName.error = "Full name is required"
            }

            email.isBlank() -> {
                binding.edtEmail.error = "Email is required"
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.edtEmail.error = "Invalid email"
            }

            password.length < MIN_PASSWORD_LENGTH -> {
                binding.edtPassword.error = "Password must be at least 6 characters"
            }

            confirmPassword != password -> {
                binding.edtConfirmPassword.error = "Passwords do not match"
            }

            else -> {
                binding.btnCreateAccount.isEnabled = false
                viewModel.register(name = fullName, email = email, password = password)
            }
        }
    }

    private fun observeRegisterResult() {
        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            binding.btnCreateAccount.isEnabled = true
            result
                .onSuccess {
                    AppUiMessageBus.publish(R.string.auth_register_success, UiMessageType.SUCCESS)
                    if (resumePendingCheckoutAfterRegister()) {
                        return@onSuccess
                    }
                    openLoginScreen(addToBackStack = false)
                }
                .onFailure { error ->
                    Log.e(TAG, "Registration failed", error)
                    showUiMessage(R.string.auth_register_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun resumePendingCheckoutAfterRegister(): Boolean {
        val pendingDestination = PendingAuthDestinationStore.consume(requireContext())
        if (pendingDestination != PendingAuthDestinationStore.DESTINATION_CHECKOUT) {
            return false
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid?.trim().orEmpty()
        if (uid.isBlank()) {
            return false
        }

        CartStore.onUserChanged(uid)
        FakeSessionStore.login(UserRole.CUSTOMER)
        clearAppBackStack()
        if (CartStore.items.isEmpty()) {
            AppUiMessageBus.publish(R.string.cart_empty_message, UiMessageType.INFO)
            openCartScreen(addToBackStack = false)
            return true
        }

        Log.d(TAG, "Pending checkout resumed after register")
        openCheckoutScreen(addToBackStack = false)
        return true
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val TAG = "RegisterFragment"
    }
}
