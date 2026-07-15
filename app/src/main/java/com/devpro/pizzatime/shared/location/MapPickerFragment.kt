package com.devpro.pizzatime.shared.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentMapPickerBinding

class MapPickerFragment : Fragment(R.layout.fragment_map_picker) {

    private var _binding: FragmentMapPickerBinding? = null
    private val binding: FragmentMapPickerBinding
        get() = checkNotNull(_binding) {
            "FragmentMapPickerBinding is only valid between onViewCreated and onDestroyView."
        }

    private var selectedCoordinate: DeliveryCoordinate? = null
    private var mapController: OsmdroidMapController? = null
    private var pendingLocationListener: LocationListener? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (_binding == null) return@registerForActivityResult
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locateCurrentPosition()
        } else {
            showLocationMessage(R.string.map_picker_permission_required, UiMessageType.WARNING)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapPickerBinding.bind(view)

        val mode = arguments?.getString(ARG_MODE).orEmpty()
        val titleRes = if (mode == MODE_STORE_PICKUP) {
            R.string.map_picker_store_title
        } else {
            R.string.map_picker_delivery_title
        }
        val initialAddress = savedInstanceState?.getString(STATE_ADDRESS)
            ?: arguments?.getString(ARG_INITIAL_ADDRESS).orEmpty()
        selectedCoordinate = if (savedInstanceState != null) {
            DeliveryCoordinate.from(
                latitude = savedInstanceState.getDoubleOrNull(STATE_LAT),
                longitude = savedInstanceState.getDoubleOrNull(STATE_LNG),
            )
        } else {
            DeliveryCoordinate.from(
                latitude = arguments?.getDouble(ARG_INITIAL_LAT),
                longitude = arguments?.getDouble(ARG_INITIAL_LNG),
            )
        }

        binding.tvMapPickerTitle.setText(titleRes)
        binding.edtMapAddress.setText(initialAddress)
        setupMap()
        setupActions()
    }

    private fun setupMap() {
        val controller = OsmdroidMapController(binding.mapView).also {
            mapController = it
        }
        val initialCoordinate = selectedCoordinate
        if (initialCoordinate != null) {
            setSelectedPoint(initialCoordinate)
            controller.center(initialCoordinate)
        } else {
            controller.showWorld()
            binding.tvSelectedCoordinate.setText(R.string.map_picker_select_point_prompt)
        }
        controller.addTapListener(::setSelectedPoint)
    }

    private fun setSelectedPoint(coordinate: DeliveryCoordinate) {
        if (_binding == null) return
        selectedCoordinate = coordinate
        mapController?.replaceMarker(
            slot = OsmdroidMapController.MarkerSlot.SELECTION,
            coordinate = coordinate,
        )
        binding.tvSelectedCoordinate.text = getString(
            R.string.map_picker_selected_coordinates,
            coordinate.latitude,
            coordinate.longitude,
        )
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnUseCurrentLocation.setOnClickListener {
            if (hasLocationPermission()) {
                locateCurrentPosition()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }

        btnUseLocation.setOnClickListener {
            val address = edtMapAddress.text.toString().trim()
            val coordinate = selectedCoordinate
            if (address.isBlank()) {
                edtMapAddress.error = getString(R.string.map_picker_address_required)
                return@setOnClickListener
            }
            edtMapAddress.error = null
            if (coordinate == null) {
                showLocationMessage(R.string.map_picker_coordinates_required, UiMessageType.ERROR)
                return@setOnClickListener
            }
            btnUseLocation.isEnabled = false

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putString(KEY_MODE, arguments?.getString(ARG_MODE).orEmpty())
                    putString(KEY_ADDRESS, address)
                    putDouble(KEY_LAT, coordinate.latitude)
                    putDouble(KEY_LNG, coordinate.longitude)
                },
            )
            parentFragmentManager.popBackStack()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun locateCurrentPosition() {
        if (!hasLocationPermission()) {
            showLocationMessage(R.string.map_picker_permission_required, UiMessageType.WARNING)
            return
        }

        val safeContext = context ?: return
        val locationManager = safeContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            showLocationMessage(R.string.map_picker_current_unavailable, UiMessageType.ERROR)
            return
        }

        val enabledProviders = LOCATION_PROVIDERS.filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        if (enabledProviders.isEmpty()) {
            showLocationMessage(R.string.map_picker_location_services_disabled, UiMessageType.WARNING)
            return
        }

        val lastKnownLocation = enabledProviders
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter(::isUsableLocation)
            .maxByOrNull { it.time }

        if (lastKnownLocation != null) {
            applyCurrentLocation(lastKnownLocation)
            return
        }

        requestSingleLocationUpdate(locationManager, enabledProviders)
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleLocationUpdate(
        locationManager: LocationManager,
        providers: List<String>,
    ) {
        clearPendingLocationRequest(locationManager)

        val listener = LocationListener { location ->
            clearPendingLocationRequest(locationManager)
            applyCurrentLocation(location)
        }
        pendingLocationListener = listener

        val requested = providers.map { provider ->
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper(),
                )
            }.isSuccess
        }.any { it }

        if (!requested) {
            clearPendingLocationRequest(locationManager)
            showLocationMessage(R.string.map_picker_current_unavailable, UiMessageType.ERROR)
            return
        }

        timeoutRunnable = Runnable {
            clearPendingLocationRequest(locationManager)
            showLocationMessage(R.string.map_picker_current_unavailable, UiMessageType.ERROR)
        }.also { runnable ->
            timeoutHandler.postDelayed(runnable, LOCATION_TIMEOUT_MS)
        }
    }

    private fun applyCurrentLocation(location: Location) {
        if (_binding == null) return
        val coordinate = DeliveryCoordinate.from(location.latitude, location.longitude)
        if (coordinate == null) {
            showLocationMessage(R.string.map_picker_current_unavailable, UiMessageType.ERROR)
            return
        }
        setSelectedPoint(coordinate)
        mapController?.center(coordinate, animate = true)
    }

    private fun isUsableLocation(location: Location): Boolean {
        val ageMillis = System.currentTimeMillis() - location.time
        return DeliveryCoordinate.from(location.latitude, location.longitude) != null &&
            location.time > 0L &&
            ageMillis in 0..MAX_LAST_KNOWN_AGE_MS
    }

    private fun clearPendingLocationRequest(
        locationManager: LocationManager? = currentLocationManager(),
    ) {
        val runnable = timeoutRunnable
        if (runnable != null) {
            timeoutHandler.removeCallbacks(runnable)
            timeoutRunnable = null
        }

        val listener = pendingLocationListener
        if (listener != null && locationManager != null) {
            runCatching { locationManager.removeUpdates(listener) }
        }
        pendingLocationListener = null
    }

    private fun currentLocationManager(): LocationManager? {
        return context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private fun showLocationMessage(
        messageRes: Int,
        type: UiMessageType,
    ) {
        if (!isAdded || _binding == null) return
        showUiMessage(messageRes, type)
    }

    private fun Bundle.getDoubleOrNull(key: String): Double? {
        return if (containsKey(key)) getDouble(key) else null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_ADDRESS, _binding?.edtMapAddress?.text?.toString().orEmpty())
        selectedCoordinate?.let { coordinate ->
            outState.putDouble(STATE_LAT, coordinate.latitude)
            outState.putDouble(STATE_LNG, coordinate.longitude)
        }
    }

    override fun onResume() {
        super.onResume()
        mapController?.onResume()
    }

    override fun onPause() {
        clearPendingLocationRequest()
        mapController?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        clearPendingLocationRequest()
        mapController?.destroy()
        mapController = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "map_picker_result"
        const val MODE_STORE_PICKUP = "STORE_PICKUP"
        const val MODE_CUSTOMER_DELIVERY = "CUSTOMER_DELIVERY"
        const val KEY_MODE = "mode"
        const val KEY_ADDRESS = "address"
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"

        private const val ARG_MODE = "arg_mode"
        private const val ARG_INITIAL_ADDRESS = "arg_initial_address"
        private const val ARG_INITIAL_LAT = "arg_initial_lat"
        private const val ARG_INITIAL_LNG = "arg_initial_lng"
        private const val STATE_ADDRESS = "state_address"
        private const val STATE_LAT = "state_lat"
        private const val STATE_LNG = "state_lng"
        private const val LOCATION_TIMEOUT_MS = 10_000L
        private const val MAX_LAST_KNOWN_AGE_MS = 2 * 60 * 1000L
        private val LOCATION_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        )

        fun newInstance(
            mode: String,
            initialAddress: String,
            initialLat: Double?,
            initialLng: Double?,
        ): MapPickerFragment {
            return MapPickerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode)
                    putString(ARG_INITIAL_ADDRESS, initialAddress)
                    putDouble(ARG_INITIAL_LAT, initialLat ?: Double.NaN)
                    putDouble(ARG_INITIAL_LNG, initialLng ?: Double.NaN)
                }
            }
        }
    }
}
