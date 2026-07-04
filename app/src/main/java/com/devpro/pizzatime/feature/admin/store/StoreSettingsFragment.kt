package com.devpro.pizzatime.feature.admin.store

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentStoreSettingsBinding

class StoreSettingsFragment : Fragment(R.layout.fragment_store_settings) {

    private var _binding: FragmentStoreSettingsBinding? = null
    private val binding: FragmentStoreSettingsBinding
        get() = checkNotNull(_binding) {
            "FragmentStoreSettingsBinding is only valid between onViewCreated and onDestroyView."
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStoreSettingsBinding.bind(view)

        bindSettings(StoreSettingsUiModel())
        setupActions()
        loadSettings()
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSaveStoreSettings.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        StoreSettingsRepository.loadStoreSettings { result ->
            if (_binding == null || !isAdded) return@loadStoreSettings
            result
                .onSuccess { settings -> bindSettings(settings) }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        R.string.store_settings_load_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun bindSettings(settings: StoreSettingsUiModel) = with(binding) {
        edtStoreName.setText(settings.storeName)
        edtPickupAddress.setText(settings.pickupAddress)
        edtStorePhone.setText(settings.storePhone)
        edtOpeningHours.setText(settings.openingHours)
        switchAcceptingOrders.isChecked = settings.acceptingOrders
    }

    private fun saveSettings() = with(binding) {
        val settings = StoreSettingsUiModel(
            storeName = edtStoreName.text.toString().trim(),
            pickupAddress = edtPickupAddress.text.toString().trim(),
            storePhone = edtStorePhone.text.toString().trim(),
            openingHours = edtOpeningHours.text.toString().trim()
                .ifBlank { StoreSettingsUiModel.DEFAULT_OPENING_HOURS },
            acceptingOrders = switchAcceptingOrders.isChecked,
        )

        when {
            settings.storeName.isBlank() -> {
                showToast(R.string.store_settings_store_name_required)
                return@with
            }

            settings.pickupAddress.isBlank() -> {
                showToast(R.string.store_settings_pickup_address_required)
                return@with
            }

            settings.storePhone.isBlank() -> {
                showToast(R.string.store_settings_store_phone_required)
                return@with
            }
        }

        btnSaveStoreSettings.isEnabled = false
        StoreSettingsRepository.saveStoreSettings(settings) { result ->
            if (_binding == null || !isAdded) return@saveStoreSettings
            btnSaveStoreSettings.isEnabled = true
            result
                .onSuccess { showToast(R.string.store_settings_save_success) }
                .onFailure { showToast(R.string.store_settings_save_failed) }
        }
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
