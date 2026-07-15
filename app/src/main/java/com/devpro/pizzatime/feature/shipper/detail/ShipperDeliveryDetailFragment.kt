package com.devpro.pizzatime.feature.shipper.detail

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentShipperDeliveryDetailBinding
import com.devpro.pizzatime.databinding.ItemShipperPaymentRowBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.shipper.ShipperOrderFirestoreRepository
import com.devpro.pizzatime.feature.shipper.navigation.ExternalMapLaunchResult
import com.devpro.pizzatime.feature.shipper.navigation.ExternalMapNavigator
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
import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import com.devpro.pizzatime.shared.location.OneShotDeviceLocationResult
import com.devpro.pizzatime.shared.location.OneShotDeviceLocationSource
import com.devpro.pizzatime.shared.location.OsmdroidMapController
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ShipperDeliveryDetailFragment : Fragment(R.layout.fragment_shipper_delivery_detail) {

    private var _binding: FragmentShipperDeliveryDetailBinding? = null
    private val binding: FragmentShipperDeliveryDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentShipperDeliveryDetailBinding is only valid between onViewCreated and onDestroyView."
        }

    private var firestoreStatus: String? = null
    private var isUpdatingStatus = false
    private var mapController: OsmdroidMapController? = null
    private var deviceLocationSource: OneShotDeviceLocationSource? = null
    private var destinationCoordinate: DeliveryCoordinate? = null
    private var currentDeviceCoordinate: DeliveryCoordinate? = null
    private var currentLocationUiState = CurrentLocationUiState.UNAVAILABLE
    private var cameraInitialized = false
    private var bothLocationsFramed = false
    private var hasRequestedLocationPermission = false
    private var isLocationRequestInFlight = false
    private var hasLoadedDetail = false
    private var shownLocationMessageState: CurrentLocationUiState? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (_binding == null) return@registerForActivityResult
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission()
        if (isGranted) {
            requestCurrentDeviceLocation()
        } else {
            currentDeviceCoordinate = null
            currentLocationUiState = CurrentLocationUiState.PERMISSION_REQUIRED
            renderDeliveryMap()
            showLocationMessageOnce(
                state = CurrentLocationUiState.PERMISSION_REQUIRED,
                textRes = R.string.shipper_detail_location_permission_required,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShipperDeliveryDetailBinding.bind(view)
        hasRequestedLocationPermission = hasRequestedLocationPermission ||
            savedInstanceState?.getBoolean(STATE_LOCATION_PERMISSION_REQUESTED) == true

        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()
        setupDeliveryMap()
        setupBottomNav()
        setupAvatar()
        loadOrder(orderId)
    }

    private fun loadOrder(orderId: String) {
        if (!isFirestoreOrderId(orderId)) {
            AppUiMessageBus.publish(
                R.string.notification_order_unavailable,
                UiMessageType.ERROR,
            )
            parentFragmentManager.popBackStack()
            return
        }

        ShipperOrderFirestoreRepository.loadOrderDetail(orderId) { result ->
            if (_binding == null || !isAdded) return@loadOrderDetail
            result
                .onSuccess { (detail, status) ->
                    firestoreStatus = status
                    hasLoadedDetail = true
                    bindDetail(detail)
                    setupActions(detail)
                    requestCurrentPositionIfEligible()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load shipper order", error)
                    AppUiMessageBus.publish(
                        R.string.notification_order_unavailable,
                        UiMessageType.ERROR,
                    )
                    parentFragmentManager.popBackStack()
                }
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
        destinationCoordinate = detail.deliveryCoordinate
        tvDeliveryCoordinates.text = formatCoordinates(detail.deliveryCoordinate)
        tvDeliveryFee.text = detail.deliveryFee.ifBlank { getString(R.string.common_not_provided) }
        tvCourierNote.text = getString(R.string.shipper_detail_note_quote, detail.courierNote)
        tvPaymentAmount.text = detail.paymentAmount
        tvPaymentMethod.text = detail.paymentMethod
        tvPaymentStatus.text = detail.paymentStatus.ifBlank { getString(R.string.payment_status_unpaid) }
        tvReadOnlyIndicator.isVisible = !canManageShipperScreen()
        renderActionButton(firestoreStatus)

        bindPaymentItems(detail.items)
        renderDeliveryMap()
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
            openPhoneDialer(detail.customerPhone)
        }

        btnOpenPickupMap.setOnClickListener {
            openExternalMap(
                coordinate = DeliveryCoordinate.from(detail.pickupLat, detail.pickupLng),
                label = detail.storeName,
            )
        }

        btnNavigate.setOnClickListener {
            openDeliveryNavigation(detail)
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

            else -> showUiMessage(R.string.feedback_action_failed, UiMessageType.WARNING)
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
        MaterialAlertDialogBuilder(requireContext())
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
            if (_binding == null || !isAdded) return@updateOrderStatus
            result
                .onSuccess {
                    isUpdatingStatus = false
                    firestoreStatus = nextStatus
                    renderActionButton(nextStatus)
                    showUiMessage(
                        textRes = R.string.shipper_detail_delivery_confirmed,
                        type = UiMessageType.SUCCESS,
                        args = listOf(displayOrderCode),
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to update shipper order to $nextStatus", error)
                    isUpdatingStatus = false
                    renderActionButton(firestoreStatus)
                    showUiMessage(R.string.feedback_action_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun openExternalMap(coordinate: DeliveryCoordinate?, label: String) {
        if (coordinate == null) {
            showUiMessage(R.string.location_not_available, UiMessageType.ERROR)
            return
        }
        val safeLat = coordinate.latitude
        val safeLng = coordinate.longitude
        val encodedLabel = Uri.encode(label.ifBlank { getString(R.string.app_name) })
        val uri = Uri.parse("geo:$safeLat,$safeLng?q=$safeLat,$safeLng($encodedLabel)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "No map application can handle the delivery location", error)
            showUiMessage(R.string.location_not_available, UiMessageType.ERROR)
        }
    }

    private fun openDeliveryNavigation(detail: ShipperDeliveryDetailUiModel) {
        when (
            ExternalMapNavigator.launch(
                context = requireContext(),
                coordinate = detail.deliveryCoordinate,
                address = detail.navigationAddress,
                coordinateLabel = detail.navigationAddress.ifBlank {
                    getString(R.string.shipper_detail_navigation_label)
                },
            )
        ) {
            ExternalMapLaunchResult.GOOGLE_MAPS,
            ExternalMapLaunchResult.GENERIC_MAP,
                -> Unit

            ExternalMapLaunchResult.DESTINATION_UNAVAILABLE -> showUiMessage(
                R.string.shipper_detail_navigation_destination_unavailable,
                UiMessageType.WARNING,
            )

            ExternalMapLaunchResult.NO_HANDLER -> showUiMessage(
                R.string.shipper_detail_navigation_app_unavailable,
                UiMessageType.WARNING,
            )
        }
    }

    private fun openPhoneDialer(phone: String) {
        if (phone.isBlank()) {
            showUiMessage(R.string.phone_not_available, UiMessageType.ERROR)
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "No phone application can handle the customer number", error)
            showUiMessage(R.string.phone_not_available, UiMessageType.ERROR)
        }
    }

    private fun formatCoordinates(lat: Double?, lng: Double?): String {
        return formatCoordinates(DeliveryCoordinate.from(lat, lng))
    }

    private fun formatCoordinates(coordinate: DeliveryCoordinate?): String {
        return coordinate?.let { validCoordinate ->
            getString(
                R.string.location_coordinates_format,
                validCoordinate.latitude,
                validCoordinate.longitude,
            )
        } ?: getString(R.string.location_coordinates_missing)
    }

    private fun setupDeliveryMap() {
        mapController = OsmdroidMapController(binding.deliveryMapView).also { controller ->
            controller.showWorld()
        }
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        deviceLocationSource = locationManager?.let(::OneShotDeviceLocationSource)

        binding.btnCenterMap.setOnClickListener {
            renderDeliveryMap(centerRequested = true)
        }
        binding.deliveryMapView.doOnLayout {
            if (_binding != null) {
                renderDeliveryMap()
            }
        }
    }

    private fun renderDeliveryMap(centerRequested: Boolean = false) {
        if (_binding == null) return
        val destination = destinationCoordinate
        val currentDevice = currentDeviceCoordinate
        val presentation = ShipperDeliveryMapPolicy.present(
            destination = destination,
            currentDevice = currentDevice,
            cameraInitialized = cameraInitialized,
            bothLocationsFramed = bothLocationsFramed,
            centerRequested = centerRequested,
        )
        val controller = mapController ?: return

        if (presentation.showDestinationMarker && destination != null) {
            controller.replaceMarker(
                slot = OsmdroidMapController.MarkerSlot.DESTINATION,
                coordinate = destination,
                title = getString(R.string.shipper_detail_delivery_marker_title),
            )
        } else {
            controller.removeMarker(OsmdroidMapController.MarkerSlot.DESTINATION)
        }

        if (presentation.showCurrentDeviceMarker && currentDevice != null) {
            controller.replaceMarker(
                slot = OsmdroidMapController.MarkerSlot.CURRENT_DEVICE,
                coordinate = currentDevice,
                title = getString(R.string.shipper_detail_current_marker_title),
            )
        } else {
            controller.removeMarker(OsmdroidMapController.MarkerSlot.CURRENT_DEVICE)
        }

        binding.tvDeliveryLocationState.setText(
            if (destination != null) {
                R.string.shipper_detail_delivery_location_available
            } else {
                R.string.shipper_detail_delivery_location_unavailable
            },
        )
        binding.tvCurrentLocationState.setText(currentLocationUiState.textRes)
        binding.btnCenterMap.isEnabled = destination != null || currentDevice != null

        val formattedDistance = presentation.straightLineDistanceKm?.let { distanceKm ->
            StraightLineDistanceFormatter.formatNumber(distanceKm)
        }
        binding.tvDeliveryDistance.text = if (formattedDistance != null) {
            val distanceWithUnit = getString(
                R.string.shipper_detail_distance_kilometers,
                formattedDistance,
            )
            getString(R.string.shipper_detail_straight_line_distance, distanceWithUnit)
        } else {
            getString(R.string.shipper_detail_straight_line_distance_unavailable)
        }

        val respectUserInteraction = !centerRequested
        when (presentation.cameraAction) {
            ShipperDeliveryCameraAction.CENTER_DESTINATION -> destination?.let { coordinate ->
                if (controller.center(
                        coordinate = coordinate,
                        animate = cameraInitialized,
                        respectUserInteraction = respectUserInteraction,
                    )
                ) {
                    cameraInitialized = true
                }
            }

            ShipperDeliveryCameraAction.CENTER_CURRENT_DEVICE -> currentDevice?.let { coordinate ->
                if (controller.center(
                        coordinate = coordinate,
                        animate = cameraInitialized,
                        respectUserInteraction = respectUserInteraction,
                    )
                ) {
                    cameraInitialized = true
                }
            }

            ShipperDeliveryCameraAction.FIT_BOTH -> {
                val coordinates = listOfNotNull(destination, currentDevice)
                val didFit = controller.fitCoordinates(
                    coordinates = coordinates,
                    paddingPixels = mapCameraPaddingPixels(),
                    animate = cameraInitialized,
                    respectUserInteraction = respectUserInteraction,
                )
                if (didFit) {
                    cameraInitialized = true
                    bothLocationsFramed = coordinates.size == 2
                }
            }

            ShipperDeliveryCameraAction.KEEP,
            ShipperDeliveryCameraAction.NONE,
                -> Unit
        }
    }

    private fun requestCurrentPositionIfEligible() {
        if (AppEditionConfig.current != AppEdition.SHIPPER ||
            FakeSessionStore.currentRole != UserRole.SHIPPER
        ) {
            currentDeviceCoordinate = null
            currentLocationUiState = CurrentLocationUiState.UNAVAILABLE
            renderDeliveryMap()
            return
        }
        if (hasLocationPermission()) {
            requestCurrentDeviceLocation()
            return
        }

        currentDeviceCoordinate = null
        currentLocationUiState = CurrentLocationUiState.PERMISSION_REQUIRED
        renderDeliveryMap()
        if (!hasRequestedLocationPermission &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            hasRequestedLocationPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    private fun requestCurrentDeviceLocation() {
        if (isLocationRequestInFlight || !hasLocationPermission()) {
            return
        }
        val locationSource = deviceLocationSource
        if (locationSource == null) {
            currentLocationUiState = CurrentLocationUiState.SERVICES_UNAVAILABLE
            renderDeliveryMap()
            showLocationMessageOnce(
                state = CurrentLocationUiState.SERVICES_UNAVAILABLE,
                textRes = R.string.shipper_detail_location_services_unavailable,
            )
            return
        }

        isLocationRequestInFlight = true
        currentLocationUiState = CurrentLocationUiState.LOADING
        renderDeliveryMap()
        locationSource.request { result ->
            if (_binding == null) return@request
            isLocationRequestInFlight = false
            when (result) {
                is OneShotDeviceLocationResult.Available -> {
                    currentDeviceCoordinate = result.coordinate
                    currentLocationUiState = CurrentLocationUiState.AVAILABLE
                    shownLocationMessageState = null
                }

                is OneShotDeviceLocationResult.Unavailable -> {
                    currentDeviceCoordinate = null
                    handleLocationUnavailable(result.reason)
                }
            }
            renderDeliveryMap()
        }
    }

    private fun handleLocationUnavailable(reason: OneShotDeviceLocationResult.Reason) {
        when (reason) {
            OneShotDeviceLocationResult.Reason.PERMISSION_REQUIRED -> {
                currentLocationUiState = CurrentLocationUiState.PERMISSION_REQUIRED
                showLocationMessageOnce(
                    state = CurrentLocationUiState.PERMISSION_REQUIRED,
                    textRes = R.string.shipper_detail_location_permission_required,
                )
            }

            OneShotDeviceLocationResult.Reason.LOCATION_SERVICES_UNAVAILABLE -> {
                currentLocationUiState = CurrentLocationUiState.SERVICES_UNAVAILABLE
                showLocationMessageOnce(
                    state = CurrentLocationUiState.SERVICES_UNAVAILABLE,
                    textRes = R.string.shipper_detail_location_services_unavailable,
                )
            }

            OneShotDeviceLocationResult.Reason.CURRENT_LOCATION_UNAVAILABLE -> {
                currentLocationUiState = CurrentLocationUiState.UNAVAILABLE
                showLocationMessageOnce(
                    state = CurrentLocationUiState.UNAVAILABLE,
                    textRes = R.string.shipper_detail_current_location_unavailable,
                )
            }
        }
    }

    private fun showLocationMessageOnce(
        state: CurrentLocationUiState,
        textRes: Int,
    ) {
        if (shownLocationMessageState == state || _binding == null || !isAdded) {
            return
        }
        shownLocationMessageState = state
        showUiMessage(textRes, UiMessageType.WARNING)
    }

    private fun hasLocationPermission(): Boolean {
        val safeContext = context ?: return false
        return ContextCompat.checkSelfPermission(
            safeContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                safeContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun mapCameraPaddingPixels(): Int {
        return (MAP_CAMERA_PADDING_DP * resources.displayMetrics.density).toInt()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            STATE_LOCATION_PERMISSION_REQUESTED,
            hasRequestedLocationPermission,
        )
    }

    override fun onResume() {
        super.onResume()
        mapController?.onResume()
        if (hasLoadedDetail) {
            requestCurrentPositionIfEligible()
        }
    }

    override fun onPause() {
        deviceLocationSource?.cancel()
        isLocationRequestInFlight = false
        mapController?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        deviceLocationSource?.cancel()
        deviceLocationSource = null
        mapController?.destroy()
        mapController = null
        destinationCoordinate = null
        currentDeviceCoordinate = null
        currentLocationUiState = CurrentLocationUiState.UNAVAILABLE
        cameraInitialized = false
        bothLocationsFramed = false
        isLocationRequestInFlight = false
        hasLoadedDetail = false
        shownLocationMessageState = null
        _binding = null
        super.onDestroyView()
    }

    private enum class CurrentLocationUiState(val textRes: Int) {
        UNAVAILABLE(R.string.shipper_detail_current_location_unavailable),
        LOADING(R.string.shipper_detail_current_location_loading),
        AVAILABLE(R.string.shipper_detail_current_location_available),
        PERMISSION_REQUIRED(R.string.shipper_detail_location_permission_required),
        SERVICES_UNAVAILABLE(R.string.shipper_detail_location_services_unavailable),
    }

    companion object {
        private const val ARG_ORDER_ID = "orderId"
        private const val TAG = "ShipperDeliveryDetail"
        private const val STATE_LOCATION_PERMISSION_REQUESTED = "location_permission_requested"
        private const val MAP_CAMERA_PADDING_DP = 48f
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
