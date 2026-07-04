package com.devpro.pizzatime.feature.shipper.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentShipperDeliveryDetailBinding
import com.devpro.pizzatime.databinding.ItemShipperPaymentRowBinding
import com.devpro.pizzatime.feature.shipper.ShipperOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.backToPreviousStaffScreen
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard
import com.devpro.pizzatime.shared.location.isValidLatitude
import com.devpro.pizzatime.shared.location.isValidLongitude
import com.google.firebase.auth.FirebaseAuth

class ShipperDeliveryDetailFragment : Fragment(R.layout.fragment_shipper_delivery_detail) {

    private var _binding: FragmentShipperDeliveryDetailBinding? = null
    private val binding: FragmentShipperDeliveryDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentShipperDeliveryDetailBinding is only valid between onViewCreated and onDestroyView."
        }

    private var firestoreStatus: String? = null
    private var isUpdatingStatus = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShipperDeliveryDetailBinding.bind(view)

        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()
        setupBottomNav()
        loadOrder(orderId)
    }

    private fun loadOrder(orderId: String) {
        if (isFirestoreOrderId(orderId)) {
            ShipperOrderFirestoreRepository.loadOrderDetail(orderId) { result ->
                if (!isAdded) return@loadOrderDetail
                result
                    .onSuccess { (detail, status) ->
                        firestoreStatus = status
                        bindDetail(detail)
                        setupActions(detail)
                    }
                    .onFailure {
                        val detail = FakeShipperDeliveryDetailData.getDetail(orderId)
                        bindDetail(detail)
                        setupActions(detail)
                    }
            }
        } else {
            val detail = FakeShipperDeliveryDetailData.getDetail(orderId.ifBlank { null })
            bindDetail(detail)
            setupActions(detail)
        }
    }

    private fun isFirestoreOrderId(orderId: String): Boolean =
        orderId.isNotBlank() && !orderId.startsWith("#") && orderId.length > 8

    private fun bindDetail(detail: ShipperDeliveryDetailUiModel) = with(binding) {
        tvOrderTitle.text = getString(R.string.shipper_detail_order_title, detail.orderId.removePrefix("#"))
        tvStoreName.text = detail.storeName
        tvPickupAddress.text = detail.pickupAddress
        tvStorePhone.text = detail.storePhone
        tvPickupCoordinates.text = formatCoordinates(detail.pickupLat, detail.pickupLng)
        tvCustomerName.text = detail.customerName
        tvCustomerPhone.text = detail.customerPhone
        tvDeliveryAddress.text = detail.address
        tvDeliveryCoordinates.text = formatCoordinates(detail.deliveryLat, detail.deliveryLng)
        tvDeliveryDistance.text = detail.distanceKm?.let { formatDistance(it) }
            ?: getString(R.string.common_not_provided)
        tvDeliveryFee.text = detail.deliveryFee.ifBlank { getString(R.string.common_not_provided) }
        tvCourierNote.text = getString(R.string.shipper_detail_note_quote, detail.courierNote)
        tvPaymentAmount.text = detail.paymentAmount
        tvPaymentMethod.text = detail.paymentMethod

        bindPaymentItems(detail.items)
    }

    private fun bindPaymentItems(items: List<ShipperPaymentItemUiModel>) = with(binding.llPaymentItems) {
        removeAllViews()

        items.forEach { item ->
            val itemBinding = ItemShipperPaymentRowBinding.inflate(
                layoutInflater,
                this,
                false,
            )

            itemBinding.tvPaymentItemName.text = item.name
            itemBinding.tvPaymentItemPrice.text = item.price

            addView(itemBinding.root)
        }
    }

    private fun setupActions(detail: ShipperDeliveryDetailUiModel) = with(binding) {
        btnBack.setOnClickListener {
            backToPreviousStaffScreen()
        }

        btnCallCustomer.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.shipper_detail_calling_customer, detail.customerName),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnOpenPickupMap.setOnClickListener {
            openExternalMap(
                lat = detail.pickupLat,
                lng = detail.pickupLng,
                label = detail.storeName,
            )
        }

        btnNavigate.setOnClickListener {
            openExternalMap(
                lat = detail.deliveryLat,
                lng = detail.deliveryLng,
                label = detail.customerName,
            )
        }

        btnConfirmDelivery.setOnClickListener {
            val nextStatus = nextFirestoreStatus(firestoreStatus)
            if (firestoreStatus != null && nextStatus != null) {
                updateFirestoreStatus(detail.orderId, nextStatus)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.shipper_detail_delivery_confirmed, detail.orderId),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun nextFirestoreStatus(status: String?): String? = when (status) {
        "READY" -> "ASSIGNED_TO_SHIPPER"
        "ASSIGNED_TO_SHIPPER" -> "DELIVERING"
        "DELIVERING" -> "DELIVERED"
        else -> null
    }

    private fun updateFirestoreStatus(orderId: String, nextStatus: String) {
        if (isUpdatingStatus) {
            return
        }

        val shipperId = FirebaseAuth.getInstance().currentUser?.uid
        isUpdatingStatus = true
        binding.btnConfirmDelivery.isEnabled = false
        ShipperOrderFirestoreRepository.updateOrderStatus(orderId, nextStatus, shipperId) { result ->
            if (!isAdded) return@updateOrderStatus
            result
                .onSuccess {
                    isUpdatingStatus = false
                    binding.btnConfirmDelivery.isEnabled = nextStatus != "DELIVERED"
                    firestoreStatus = nextStatus
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.shipper_detail_delivery_confirmed, orderId),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure { error ->
                    isUpdatingStatus = false
                    binding.btnConfirmDelivery.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to update order.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun openExternalMap(lat: Double?, lng: Double?, label: String) {
        if (!lat.isValidLatitude() || !lng.isValidLongitude()) {
            Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        val safeLat = lat ?: return
        val safeLng = lng ?: return
        val encodedLabel = Uri.encode(label.ifBlank { getString(R.string.app_name) })
        val uri = Uri.parse("geo:$safeLat,$safeLng?q=$safeLat,$safeLng($encodedLabel)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatCoordinates(lat: Double?, lng: Double?): String {
        return if (lat.isValidLatitude() && lng.isValidLongitude()) {
            getString(R.string.location_coordinates_format, lat, lng)
        } else {
            getString(R.string.location_coordinates_missing)
        }
    }

    private fun formatDistance(distanceKm: Double): String {
        return String.format(java.util.Locale.US, "%.1f km", distanceKm)
    }

    private fun setupBottomNav() {
        bindStaffBottomNav(
            root = binding.staffBottomNav.root,
            currentTab = StaffBottomNavTab.DELIVERY,
            onDashboardClick = {
                openStaffDashboard()
            },
            onKitchenClick = {
                openKitchenBoard()
            },
            onProfileClick = {
                showComingSoon(R.string.staff_nav_profile)
            },
        )
    }

    private fun showComingSoon(titleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_coming_soon_message, getString(titleRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ORDER_ID = "orderId"

        fun newInstance(orderId: String): ShipperDeliveryDetailFragment {
            return ShipperDeliveryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}
