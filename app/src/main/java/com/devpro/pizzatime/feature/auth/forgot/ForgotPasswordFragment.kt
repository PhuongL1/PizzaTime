package com.devpro.pizzatime.feature.auth.forgot

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentForgotPasswordBinding
import com.devpro.pizzatime.feature.staff.navigation.openLoginScreen

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding: FragmentForgotPasswordBinding
        get() = checkNotNull(_binding) {
            "FragmentForgotPasswordBinding is only valid between onViewCreated and onDestroyView."
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentForgotPasswordBinding.bind(view)

        setupActions()
    }

    private fun setupActions() = with(binding) {
        btnSendResetInstructions.setOnClickListener {
            sendResetInstructions()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun sendResetInstructions() {
        val account = binding.etAccount.text?.toString()?.trim() ?: ""

        if (!isValidAccount(account)) {
            binding.etAccount.error = getString(R.string.forgot_password_invalid_account)
            return
        }

        showUiMessage(
            textRes = R.string.forgot_password_sent_message,
            type = UiMessageType.SUCCESS,
            args = listOf(account),
        )
    }

    private fun isValidAccount(account: String): Boolean {
        return isValidEmail(account) || isValidPhone(account)
    }

    private fun isValidEmail(account: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(account).matches()
    }

    private fun isValidPhone(account: String): Boolean {
        val digitCount = account.count { character -> character.isDigit() }
        return digitCount >= MIN_PHONE_DIGIT_COUNT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MIN_PHONE_DIGIT_COUNT = 8
    }
}
