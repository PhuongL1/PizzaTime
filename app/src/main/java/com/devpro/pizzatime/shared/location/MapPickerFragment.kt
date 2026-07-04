package com.devpro.pizzatime.shared.location

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentMapPickerBinding
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class MapPickerFragment : Fragment(R.layout.fragment_map_picker) {

    private var _binding: FragmentMapPickerBinding? = null
    private val binding: FragmentMapPickerBinding
        get() = checkNotNull(_binding) {
            "FragmentMapPickerBinding is only valid between onViewCreated and onDestroyView."
        }

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var marker: Marker? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMapPickerBinding.bind(view)

        Configuration.getInstance().userAgentValue = requireContext().packageName

        val mode = arguments?.getString(ARG_MODE).orEmpty()
        val titleRes = if (mode == MODE_STORE_PICKUP) {
            R.string.map_picker_store_title
        } else {
            R.string.map_picker_delivery_title
        }
        val initialAddress = arguments?.getString(ARG_INITIAL_ADDRESS).orEmpty()
        selectedLat = arguments?.getDouble(ARG_INITIAL_LAT)?.takeIf { it.isValidLatitude() }
        selectedLng = arguments?.getDouble(ARG_INITIAL_LNG)?.takeIf { it.isValidLongitude() }

        binding.tvMapPickerTitle.setText(titleRes)
        binding.edtMapAddress.setText(initialAddress)
        setupMap()
        setupActions()
    }

    private fun setupMap() = with(binding.mapView) {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        minZoomLevel = MIN_ZOOM
        maxZoomLevel = MAX_ZOOM

        val startPoint = GeoPoint(
            selectedLat ?: DEFAULT_LAT,
            selectedLng ?: DEFAULT_LNG,
        )
        controller.setZoom(DEFAULT_ZOOM)
        controller.setCenter(startPoint)
        if (selectedLat.isValidLatitude() && selectedLng.isValidLongitude()) {
            setSelectedPoint(startPoint)
        } else {
            binding.tvSelectedCoordinate.text = getString(R.string.location_coordinates_missing)
        }

        overlays.add(
            MapEventsOverlay(
                object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                        setSelectedPoint(point)
                        return true
                    }

                    override fun longPressHelper(point: GeoPoint): Boolean = false
                },
            ),
        )
    }

    private fun setSelectedPoint(point: GeoPoint) {
        selectedLat = point.latitude
        selectedLng = point.longitude
        updateMarker(point)
        binding.tvSelectedCoordinate.text = getString(
            R.string.map_picker_selected_coordinates,
            point.latitude,
            point.longitude,
        )
    }

    private fun updateMarker(point: GeoPoint) = with(binding.mapView) {
        val currentMarker = marker ?: Marker(this).also { newMarker ->
            marker = newMarker
            overlays.add(newMarker)
        }
        currentMarker.position = point
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        invalidate()
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnUseLocation.setOnClickListener {
            val address = edtMapAddress.text.toString().trim()
            val lat = selectedLat
            val lng = selectedLng
            if (address.isBlank()) {
                Toast.makeText(requireContext(), R.string.map_picker_address_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!lat.isValidLatitude() || !lng.isValidLongitude()) {
                Toast.makeText(requireContext(), R.string.map_picker_coordinates_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val safeLat = lat ?: return@setOnClickListener
            val safeLng = lng ?: return@setOnClickListener

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putString(KEY_MODE, arguments?.getString(ARG_MODE).orEmpty())
                    putString(KEY_ADDRESS, address)
                    putDouble(KEY_LAT, safeLat)
                    putDouble(KEY_LNG, safeLng)
                },
            )
            parentFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapView.onDetach()
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
        private const val DEFAULT_LAT = 21.0278
        private const val DEFAULT_LNG = 105.8342
        private const val DEFAULT_ZOOM = 15.0
        private const val MIN_ZOOM = 3.0
        private const val MAX_ZOOM = 20.0

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
