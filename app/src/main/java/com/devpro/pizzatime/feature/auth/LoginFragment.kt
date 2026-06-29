package com.devpro.pizzatime.feature.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupActions()
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
            Toast.makeText(requireContext(), "Forgot password coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLogin() {
        val identifier = binding.edtIdentifier.text.toString().trim()
        val password = binding.edtPassword.text.toString()

        when {
            identifier.isBlank() -> {
                binding.edtIdentifier.error = "Email, phone, or staff ID is required"
            }

            isLikelyEmail(identifier) && !Patterns.EMAIL_ADDRESS.matcher(identifier).matches() -> {
                binding.edtIdentifier.error = "Invalid email"
            }

            password.isBlank() -> {
                binding.edtPassword.error = "Password is required"
            }

            else -> {
                Toast.makeText(requireContext(), "Login success demo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isLikelyEmail(value: String): Boolean {
        return value.contains("@")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}