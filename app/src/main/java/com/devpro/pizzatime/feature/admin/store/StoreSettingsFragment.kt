package com.devpro.pizzatime.feature.admin.store

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentStoreSettingsBinding
import com.devpro.pizzatime.feature.staff.navigation.replaceForward
import com.devpro.pizzatime.shared.location.MapPickerFragment
import com.devpro.pizzatime.shared.location.isValidLatitude
import com.devpro.pizzatime.shared.location.isValidLongitude

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
        setupMapPickerResult()
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

        btnPickPickupLocation.setOnClickListener {
            openPickupMapPicker()
        }
    }

    private fun setupMapPickerResult() {
        parentFragmentManager.setFragmentResultListener(
            MapPickerFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            if (bundle.getString(MapPickerFragment.KEY_MODE) != MapPickerFragment.MODE_STORE_PICKUP) {
                return@setFragmentResultListener
            }
            binding.edtPickupAddress.setText(bundle.getString(MapPickerFragment.KEY_ADDRESS).orEmpty())
            binding.tvPickupCoordinates.text = formatCoordinates(
                bundle.getDouble(MapPickerFragment.KEY_LAT),
                bundle.getDouble(MapPickerFragment.KEY_LNG),
            )
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
        tvPickupCoordinates.text = formatCoordinates(settings.pickupLat, settings.pickupLng)
        edtStorePhone.setText(settings.storePhone)
        edtOpeningHours.setText(settings.openingHours)
        switchAcceptingOrders.isChecked = settings.acceptingOrders
        edtBaseDeliveryFee.setText(formatNumber(settings.baseDeliveryFee))
        edtDeliveryFeePerKm.setText(formatNumber(settings.deliveryFeePerKm))
        edtFreeDeliveryMinSubtotal.setText(formatNumber(settings.freeDeliveryMinSubtotal))
    }

    private fun saveSettings() = with(binding) {
        val baseDeliveryFee = edtBaseDeliveryFee.text.toString().trim().toDoubleOrNull()
        val deliveryFeePerKm = edtDeliveryFeePerKm.text.toString().trim().toDoubleOrNull()
        val freeDeliveryMinSubtotal = edtFreeDeliveryMinSubtotal.text.toString().trim().toDoubleOrNull()
        val settings = StoreSettingsUiModel(
            storeName = edtStoreName.text.toString().trim(),
            pickupAddress = edtPickupAddress.text.toString().trim(),
            pickupLat = currentPickupLat(),
            pickupLng = currentPickupLng(),
            storePhone = edtStorePhone.text.toString().trim(),
            openingHours = edtOpeningHours.text.toString().trim()
                .ifBlank { StoreSettingsUiModel.DEFAULT_OPENING_HOURS },
            acceptingOrders = switchAcceptingOrders.isChecked,
            baseDeliveryFee = baseDeliveryFee ?: -1.0,
            deliveryFeePerKm = deliveryFeePerKm ?: -1.0,
            freeDeliveryMinSubtotal = freeDeliveryMinSubtotal ?: -1.0,
        )

        when {
            settings.storeName.isBlank() -> {
                showToast(R.string.store_settings_store_name_required)
                return@with
            }

            settings.acceptingOrders && settings.pickupAddress.isBlank() -> {
                showToast(R.string.store_settings_pickup_address_required)
                return@with
            }

            settings.acceptingOrders && !settings.pickupLat.isValidLatitude() -> {
                showToast(R.string.store_settings_pickup_location_required)
                return@with
            }

            settings.acceptingOrders && !settings.pickupLng.isValidLongitude() -> {
                showToast(R.string.store_settings_pickup_location_required)
                return@with
            }

            settings.acceptingOrders && settings.storePhone.isBlank() -> {
                showToast(R.string.store_settings_store_phone_required)
                return@with
            }

            settings.baseDeliveryFee < 0 ||
                settings.deliveryFeePerKm < 0 ||
                settings.freeDeliveryMinSubtotal < 0 -> {
                showToast(R.string.store_settings_delivery_fee_invalid)
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

    private fun openPickupMapPicker() = with(binding) {
        parentFragmentManager.replaceForward(
            containerId = R.id.fragmentContainer,
            fragment = MapPickerFragment.newInstance(
                mode = MapPickerFragment.MODE_STORE_PICKUP,
                initialAddress = edtPickupAddress.text.toString().trim(),
                initialLat = currentPickupLat(),
                initialLng = currentPickupLng(),
            )
        )
    }

    private fun currentPickupLat(): Double? {
        return (binding.tvPickupCoordinates.tag as? DoubleArray)
            ?.getOrNull(0)
            ?.takeIf { it in -90.0..90.0 }
    }

    private fun currentPickupLng(): Double? {
        return (binding.tvPickupCoordinates.tag as? DoubleArray)
            ?.getOrNull(1)
            ?.takeIf { it in -180.0..180.0 }
    }

    private fun formatCoordinates(lat: Double?, lng: Double?): String {
        binding.tvPickupCoordinates.tag = if (lat.isValidLatitude() && lng.isValidLongitude()) {
            doubleArrayOf(lat ?: 0.0, lng ?: 0.0)
        } else {
            null
        }
        return if (lat.isValidLatitude() && lng.isValidLongitude()) {
            getString(R.string.location_coordinates_format, lat, lng)
        } else {
            getString(R.string.location_coordinates_missing)
        }
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
