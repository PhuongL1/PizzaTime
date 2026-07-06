package com.devpro.pizzatime.feature.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.devpro.pizzatime.databinding.FragmentRegisterBinding
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen

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
            Toast.makeText(requireContext(), "Google sign up coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnFacebook.setOnClickListener {
            Toast.makeText(requireContext(), "Facebook sign up coming soon", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(
                        requireContext(),
                        "Account created successfully!",
                        Toast.LENGTH_SHORT,
                    ).show()
                    openLoginScreen(addToBackStack = false)
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Registration failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
