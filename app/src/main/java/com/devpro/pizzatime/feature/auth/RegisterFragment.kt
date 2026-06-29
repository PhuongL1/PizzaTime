package com.devpro.pizzatime.feature.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupActions()
    }

    private fun setupActions() {
        binding.btnCreateAccount.setOnClickListener {
            handleCreateAccount()
        }

        binding.btnBackToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnGoogle.setOnClickListener {
            Toast.makeText(requireContext(), "Google sign up coming soon", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(
                    requireContext(),
                    "Register success demo",
                    Toast.LENGTH_SHORT
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