package com.devpro.pizzatime.feature.shipper.detail

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.databinding.FragmentShipperDeliveryDetailBinding
import com.devpro.pizzatime.databinding.ItemShipperPaymentRowBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.shipper.ShipperOrderFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.backToPreviousStaffScreen
import com.devpro.pizzatime.feature.staff.navigation.bindCurrentProfileAvatar
import com.devpro.pizzatime.feature.staff.navigation.bindStaffBottomNav
import com.devpro.pizzatime.feature.staff.navigation.canManageShipperScreen
import com.devpro.pizzatime.feature.staff.navigation.directionTo
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
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
        setupAvatar()
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
        orderId.matches(ORDER_CODE_KEY_REGEX) ||
            (orderId.isNotBlank() && !orderId.startsWith("#") && orderId.length > 8)

    private fun bindDetail(detail: ShipperDeliveryDetailUiModel) = with(binding) {
        tvOrderTitle.text = getString(
            R.string.shipper_detail_order_title,
            detail.displayOrderCode,
        )
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
        tvPaymentStatus.text = detail.paymentStatus.ifBlank { getString(R.string.payment_status_unpaid) }
        tvReadOnlyIndicator.isVisible = !canManageShipperScreen()
        renderActionButton(firestoreStatus)

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

        if (!canManageShipperScreen()) {
            btnCallCustomer.isVisible = false
            btnOpenPickupMap.isVisible = false
            btnNavigate.isVisible = false
            btnConfirmDelivery.isVisible = false
            btnCallCustomer.setOnClickListener(null)
            btnOpenPickupMap.setOnClickListener(null)
            btnNavigate.setOnClickListener(null)
            btnConfirmDelivery.setOnClickListener(null)
            return@with
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

        btnConfirmDelivery.setOnClickListener { handleDeliveryAction(detail) }
    }

    private fun handleDeliveryAction(detail: ShipperDeliveryDetailUiModel) {
        when (firestoreStatus) {
            "READY",
            "ASSIGNED_TO_SHIPPER",
            "READY_FOR_DELIVERY",
            "READY_TO_DELIVER",
                -> updateFirestoreStatus(detail.orderId, detail.displayOrderCode, "DELIVERING")

            "DELIVERING" -> showCompleteDeliveryDialog(detail)

            else -> Unit
        }
    }

    private fun nextActionLabel(status: String?): Int {
        return when (status) {
            "READY", "ASSIGNED_TO_SHIPPER", "READY_FOR_DELIVERY", "READY_TO_DELIVER" ->
                R.string.shipper_detail_start_delivery
            "DELIVERING" -> R.string.shipper_detail_delivered_cash_collected
            "DELIVERED" -> R.string.shipper_detail_order_completed
            "CANCELLED" -> R.string.shipper_detail_order_cancelled
            else -> R.string.shipper_detail_confirm_delivery
        }
    }

    private fun renderActionButton(status: String?) = with(binding.btnConfirmDelivery) {
        if (!canManageShipperScreen()) {
            isVisible = false
            isEnabled = false
            setOnClickListener(null)
            return@with
        }

        text = getString(nextActionLabel(status))
        isEnabled = status in setOf(
            "READY",
            "ASSIGNED_TO_SHIPPER",
            "READY_FOR_DELIVERY",
            "READY_TO_DELIVER",
            "DELIVERING",
        ) && !isUpdatingStatus
        isVisible = status != "DELIVERED" && status != "CANCELLED"
        if (status == "DELIVERED" || status == "CANCELLED") {
            isVisible = true
            isEnabled = false
        }
    }

    private fun showCompleteDeliveryDialog(detail: ShipperDeliveryDetailUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.shipper_complete_delivery_title)
            .setMessage(
                getString(
                    R.string.shipper_complete_delivery_message_with_amount,
                    detail.paymentAmount,
                ),
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.shipper_complete_delivery_confirm) { _, _ ->
                updateFirestoreStatus(detail.orderId, detail.displayOrderCode, "DELIVERED")
            }
            .show()
    }

    private fun updateFirestoreStatus(
        orderId: String,
        displayOrderCode: String,
        nextStatus: String,
    ) {
        if (isUpdatingStatus) {
            return
        }
        if (!canManageShipperScreen()) {
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
                    firestoreStatus = nextStatus
                    renderActionButton(nextStatus)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.shipper_detail_delivery_confirmed, displayOrderCode),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure { error ->
                    isUpdatingStatus = false
                    renderActionButton(firestoreStatus)
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
        if (FakeSessionStore.currentRole == UserRole.ADMIN) {
            bindAdminBottomNav(
                root = binding.staffBottomNav.root,
                selectedDestination = AdminBottomNavDestination.SHIPPER,
                onDashboardClick = { openAdminDashboard() },
                onManageMenuClick = { openManageMenu() },
                onManagePromoCodesClick = { openShipperDeliveryDashboard() },
                onManageStaffClick = { openCustomerAccount() },
            )
        } else {
            bindStaffBottomNav(
                root = binding.staffBottomNav.root,
                currentTab = StaffBottomNavTab.DELIVERY,
                onDashboardClick = {
                    openStaffDashboard(
                        addToBackStack = false,
                        direction = StaffBottomNavTab.DELIVERY.directionTo(StaffBottomNavTab.DASHBOARD),
                    )
                },
                onKitchenClick = {
                    openKitchenBoard(
                        addToBackStack = false,
                        direction = StaffBottomNavTab.DELIVERY.directionTo(StaffBottomNavTab.KITCHEN),
                    )
                },
                onProfileClick = {
                    openCustomerAccount(
                        addToBackStack = false,
                        direction = StaffBottomNavTab.DELIVERY.directionTo(StaffBottomNavTab.PROFILE),
                    )
                },
            )
        }
    }

    private fun setupAvatar() {
        bindCurrentProfileAvatar(
            initialsView = binding.tvDetailAvatar,
            imageView = binding.ivDetailAvatar,
        )
        binding.detailAvatarFrame.setOnClickListener(null)
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
        private val ORDER_CODE_KEY_REGEX = Regex("[a-z]{2}-\\d{4}")

        fun newInstance(orderId: String): ShipperDeliveryDetailFragment {
            return ShipperDeliveryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}
