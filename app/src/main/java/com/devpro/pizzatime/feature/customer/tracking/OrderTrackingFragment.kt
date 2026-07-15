package com.devpro.pizzatime.feature.customer.tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentOrderTrackingBinding
import com.devpro.pizzatime.databinding.ItemOrderTrackingStepBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindPizzaFlowTopBar
import com.devpro.pizzatime.feature.customer.common.navigation.updatePizzaFlowCartBadge
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.shared.location.DeliveryCoordinate
import com.devpro.pizzatime.shared.location.OrderDeliveryDestinationResolver
import com.devpro.pizzatime.shared.location.OsmdroidMapController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class OrderTrackingFragment : Fragment() {

    private var _binding: FragmentOrderTrackingBinding? = null
    private val binding: FragmentOrderTrackingBinding
        get() = checkNotNull(_binding) {
            "FragmentOrderTrackingBinding is only valid between onCreateView and onDestroyView."
        }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var orderListenerRegistration: ListenerRegistration? = null
    private var trackingListenerRegistration: ListenerRegistration? = null
    private var trackingListenerBinding: CustomerTrackingListenerBinding? = null
    private var mapController: OsmdroidMapController? = null

    private var currentOrderId: String? = null
    private var currentCustomerId: String? = null
    private var currentOrderStatus: String = STATUS_PENDING
    private var currentAssignedShipperId: String? = null
    private var destinationCoordinate: DeliveryCoordinate? = null
    private var shipperCoordinate: DeliveryCoordinate? = null
    private var trackingUpdatedAtMillis: Long? = null
    private var trackingDocumentPresent: Boolean = false
    private var cameraInitialized: Boolean = false
    private var bothLocationsFramed: Boolean = false
    private var trackingErrorShown: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrderTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTopBar()
        setupBottomNav()
        setupActions()
        setupDeliveryMap()
        updateCartBadge()

        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()
        if (orderId.isNotBlank()) {
            loadOrderFromFirestore(orderId)
        } else {
            renderFakeState()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupTopBar()
            updateCartBadge()
            mapController?.onResume()
            if (
                trackingListenerBinding != null &&
                trackingListenerBinding?.customerId != auth.currentUser?.uid
            ) {
                handleUnavailableOrder()
                return
            }
            renderTrackingUi()
        }
    }

    override fun onPause() {
        mapController?.onPause()
        super.onPause()
    }

    private fun setupTopBar() {
        bindPizzaFlowTopBar(
            root = binding.pizzaTopBar.root,
            cartItemCount = CartStore.items.sumOf { it.quantity },
            onBackClick = { parentFragmentManager.popBackStack() },
            onCartClick = { openCartScreen() },
        )
    }

    private fun setupBottomNav() {
        bindCustomerBottomNav(
            root = binding.bottomNav.root,
            selectedTab = CustomerBottomNavTab.ORDERS,
        )
    }

    private fun setupActions() {
        binding.btnProductDetail.setOnClickListener {
            showUiMessage(R.string.order_tracking_item_unavailable, UiMessageType.INFO)
        }
        binding.btnSupport.setOnClickListener {
            showUiMessage(R.string.customer_order_detail_support_message, UiMessageType.INFO)
        }
    }

    private fun setupDeliveryMap() {
        mapController = OsmdroidMapController(binding.deliveryMapView).also { controller ->
            controller.showWorld()
        }
        binding.btnCenterMap.setOnClickListener {
            renderTrackingMap(centerRequested = true)
        }
        binding.deliveryMapView.doOnLayout {
            if (_binding != null) {
                renderTrackingMap()
            }
        }
    }

    private fun loadOrderFromFirestore(orderId: String) {
        orderListenerRegistration?.remove()
        orderListenerRegistration = firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener

                val activeCustomerId = auth.currentUser?.uid.orEmpty()
                if (
                    error != null ||
                    snapshot == null ||
                    !snapshot.exists() ||
                    activeCustomerId.isBlank() ||
                    snapshot.getString(FIELD_CUSTOMER_ID) != activeCustomerId
                ) {
                    handleUnavailableOrder()
                    return@addSnapshotListener
                }

                currentOrderId = snapshot.id
                currentCustomerId = activeCustomerId
                currentOrderStatus = snapshot.getString(FIELD_STATUS) ?: STATUS_PENDING
                currentAssignedShipperId = snapshot.getString(FIELD_SHIPPER_ID)?.takeIf { it.isNotBlank() }
                destinationCoordinate = OrderDeliveryDestinationResolver.resolve(snapshot.data.orEmpty())

                val displayOrderCode = OrderCodeGenerator.displayOrderCode(
                    orderCode = snapshot.getString(FIELD_ORDER_CODE),
                    orderId = snapshot.id,
                )
                binding.tvOrderNumber.text = getString(
                    R.string.order_tracking_order_number_format,
                    displayOrderCode,
                )
                renderTrackingSteps(buildStepsFromStatus(currentOrderStatus))

                val items = snapshot.get(FIELD_ITEMS) as? List<*>
                val firstName = (items?.firstOrNull() as? Map<*, *>)?.get("name") as? String
                val total = snapshot.getDouble(FIELD_TOTAL) ?: 0.0
                val itemCount = items?.size ?: 1
                bindProduct(
                    TrackingProductUiModel(
                        name = firstName ?: getString(R.string.order_tracking_default_product_name),
                        optionText = getString(R.string.order_tracking_item_count_format, itemCount),
                        price = String.format(Locale.US, "$%.2f", total),
                        imageRes = R.drawable.img_welcome_hero,
                    ),
                )

                syncTrackingObservation()
                renderTrackingUi()
            }
    }

    private fun syncTrackingObservation() {
        val observationState = resolveObservationState()
        when (observationState) {
            CustomerTrackingObservationState.OBSERVE -> {
                val orderId = currentOrderId ?: return
                val customerId = currentCustomerId ?: return
                val nextBinding = CustomerTrackingListenerBinding(orderId, customerId)
                if (CustomerTrackingListenerPolicy.shouldReplace(trackingListenerBinding, nextBinding)) {
                    stopTrackingObservation(clearState = true)
                    trackingListenerBinding = nextBinding
                    observeTrackingCurrent(nextBinding)
                }
            }

            CustomerTrackingObservationState.WAITING_FOR_DELIVERY,
            CustomerTrackingObservationState.UNAUTHORIZED,
                -> stopTrackingObservation(clearState = true)

            CustomerTrackingObservationState.DELIVERED,
            CustomerTrackingObservationState.CANCELLED,
                -> stopTrackingObservation(clearState = false)
        }
    }

    private fun observeTrackingCurrent(bindingKey: CustomerTrackingListenerBinding) {
        trackingListenerRegistration = firestore.collection(ORDERS_COLLECTION)
            .document(bindingKey.orderId)
            .collection(TRACKING_COLLECTION)
            .document(CURRENT_TRACKING_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener
                if (trackingListenerBinding != bindingKey) return@addSnapshotListener
                if (currentOrderId != bindingKey.orderId || auth.currentUser?.uid != bindingKey.customerId) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    showTrackingTransientErrorOnce()
                    renderTrackingUi()
                    return@addSnapshotListener
                }

                trackingErrorShown = false
                if (snapshot == null || !snapshot.exists()) {
                    trackingDocumentPresent = false
                    trackingUpdatedAtMillis = null
                    shipperCoordinate = null
                    bothLocationsFramed = false
                    renderTrackingUi()
                    return@addSnapshotListener
                }

                trackingDocumentPresent = true
                val parsed = CustomerTrackingDocumentParser.parse(
                    data = snapshot.data.orEmpty(),
                    expectedShipperId = currentAssignedShipperId,
                )
                trackingUpdatedAtMillis = parsed?.updatedAtMillis
                if (parsed?.coordinate != null) {
                    shipperCoordinate = parsed.coordinate
                }
                renderTrackingUi()
            }
    }

    private fun renderTrackingUi() {
        if (_binding == null) return
        renderTrackingSummary()
        renderTrackingMap()
    }

    private fun renderTrackingSummary() {
        val observationState = resolveObservationState()
        val destination = destinationCoordinate
        val freshnessState = CustomerTrackingFreshnessPolicy.classify(
            nowMillis = System.currentTimeMillis(),
            updatedAtMillis = trackingUpdatedAtMillis,
        )
        val relativeTime = CustomerTrackingFreshnessPolicy.relativeTime(
            nowMillis = System.currentTimeMillis(),
            updatedAtMillis = trackingUpdatedAtMillis,
        )

        binding.tvDeliveryLocationState.setText(
            if (destination != null) {
                R.string.order_tracking_destination_available
            } else {
                R.string.order_tracking_destination_unavailable
            },
        )
        binding.tvTrackingMapStatus.text = when (observationState) {
            CustomerTrackingObservationState.UNAUTHORIZED -> getString(R.string.order_tracking_live_unavailable)
            CustomerTrackingObservationState.WAITING_FOR_DELIVERY -> {
                getString(R.string.order_tracking_waiting_for_delivery)
            }
            CustomerTrackingObservationState.DELIVERED -> getString(R.string.order_tracking_delivered_state)
            CustomerTrackingObservationState.CANCELLED -> getString(R.string.order_tracking_cancelled_state)
            CustomerTrackingObservationState.OBSERVE -> when {
                !trackingDocumentPresent -> getString(R.string.order_tracking_waiting_for_shipper_location)
                shipperCoordinate == null -> getString(R.string.order_tracking_location_update_delayed)
                freshnessState == CustomerTrackingFreshnessState.STALE && relativeTime != null -> {
                    getString(
                        R.string.order_tracking_location_stale,
                        formatRelativeTime(relativeTime),
                    )
                }
                freshnessState == CustomerTrackingFreshnessState.DELAYED -> {
                    getString(R.string.order_tracking_location_update_delayed)
                }
                else -> getString(R.string.order_tracking_live_available)
            }
        }
        binding.tvTrackingLastUpdated.text = if (relativeTime != null) {
            getString(
                R.string.order_tracking_last_updated,
                formatRelativeTime(relativeTime),
            )
        } else {
            getString(R.string.order_tracking_last_updated_unavailable)
        }
    }

    private fun renderTrackingMap(centerRequested: Boolean = false) {
        if (_binding == null) return

        val destination = destinationCoordinate
        val shipper = shipperCoordinate
        if (destination == null || shipper == null) {
            bothLocationsFramed = false
        }

        val presentation = CustomerTrackingMapPolicy.present(
            destination = destination,
            shipper = shipper,
            cameraInitialized = cameraInitialized,
            bothLocationsFramed = bothLocationsFramed,
            centerRequested = centerRequested,
        )
        val controller = mapController ?: return

        if (presentation.showDestinationMarker && destination != null) {
            controller.replaceMarker(
                slot = OsmdroidMapController.MarkerSlot.DESTINATION,
                coordinate = destination,
                title = getString(R.string.order_tracking_destination_marker_title),
            )
        } else {
            controller.removeMarker(OsmdroidMapController.MarkerSlot.DESTINATION)
        }

        if (presentation.showShipperMarker && shipper != null) {
            controller.replaceMarker(
                slot = OsmdroidMapController.MarkerSlot.SHIPPER,
                coordinate = shipper,
                title = getString(R.string.order_tracking_shipper_marker_title),
            )
        } else {
            controller.removeMarker(OsmdroidMapController.MarkerSlot.SHIPPER)
        }

        val formattedDistance = presentation.straightLineDistanceKm?.let { distanceKm ->
            CustomerTrackingDistanceFormatter.formatNumber(distanceKm)
        }
        binding.tvTrackingDistance.text = if (formattedDistance != null) {
            val distanceWithUnit = getString(
                R.string.order_tracking_distance_kilometers,
                formattedDistance,
            )
            getString(R.string.order_tracking_straight_line_distance, distanceWithUnit)
        } else {
            getString(R.string.order_tracking_straight_line_distance_unavailable)
        }
        binding.btnCenterMap.isEnabled = destination != null || shipper != null

        val respectUserInteraction = !centerRequested
        when (presentation.cameraAction) {
            CustomerTrackingCameraAction.CENTER_DESTINATION -> destination?.let { coordinate ->
                if (controller.center(coordinate, animate = cameraInitialized, respectUserInteraction = respectUserInteraction)) {
                    cameraInitialized = true
                }
            }

            CustomerTrackingCameraAction.CENTER_SHIPPER -> shipper?.let { coordinate ->
                if (controller.center(coordinate, animate = cameraInitialized, respectUserInteraction = respectUserInteraction)) {
                    cameraInitialized = true
                }
            }

            CustomerTrackingCameraAction.FIT_BOTH -> {
                val coordinates = listOfNotNull(destination, shipper)
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

            CustomerTrackingCameraAction.KEEP,
            CustomerTrackingCameraAction.NONE,
                -> Unit
        }
    }

    private fun renderFakeState() {
        currentOrderId = null
        currentCustomerId = null
        currentOrderStatus = STATUS_PENDING
        currentAssignedShipperId = null
        destinationCoordinate = null
        shipperCoordinate = null
        trackingUpdatedAtMillis = null
        trackingDocumentPresent = false
        renderTrackingSteps(FakeTrackingData.steps)
        bindProduct(FakeTrackingData.product)
        binding.tvOrderNumber.text = getString(R.string.tracking_order_number)
        renderTrackingUi()
    }

    private fun buildStepsFromStatus(status: String): List<TrackingStepUiModel> {
        if (status.uppercase(Locale.US) == STATUS_CANCELLED) {
            return listOf(
                TrackingStepUiModel(
                    title = getString(R.string.order_tracking_status_cancelled_title),
                    subtitle = getString(R.string.order_tracking_status_cancelled_subtitle),
                    state = TrackingStepState.CURRENT,
                ),
            )
        }

        val stepDefs = listOf(
            R.string.order_tracking_step_order_placed_title to R.string.order_tracking_step_order_placed_subtitle,
            R.string.order_tracking_step_preparing_title to R.string.order_tracking_step_preparing_subtitle,
            R.string.order_tracking_step_baking_title to R.string.order_tracking_step_baking_subtitle,
            R.string.order_tracking_step_delivery_title to R.string.order_tracking_step_delivery_subtitle,
            R.string.order_tracking_step_delivered_title to null,
        )
        val currentIndex = when (status.uppercase(Locale.US)) {
            STATUS_PENDING -> 0
            "CONFIRMED", "PREPARING" -> 1
            "BAKING", "READY", "READY_FOR_DELIVERY", "READY_TO_DELIVER" -> 2
            "ASSIGNED_TO_SHIPPER", "DELIVERING" -> 3
            "DELIVERED" -> 4
            else -> 0
        }
        return stepDefs.mapIndexed { index, (titleRes, subtitleRes) ->
            TrackingStepUiModel(
                title = getString(titleRes),
                subtitle = subtitleRes?.let(::getString).orEmpty(),
                state = when {
                    index < currentIndex -> TrackingStepState.DONE
                    index == currentIndex -> TrackingStepState.CURRENT
                    else -> TrackingStepState.PENDING
                },
            )
        }
    }

    private fun renderTrackingSteps(items: List<TrackingStepUiModel>) {
        binding.statusStepContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemOrderTrackingStepBinding.inflate(
                layoutInflater,
                binding.statusStepContainer,
                false,
            )
            itemBinding.tvStepTitle.text = item.title
            itemBinding.tvStepSubtitle.text = item.subtitle
            itemBinding.tvStepSubtitle.isVisible = item.subtitle.isNotBlank()
            itemBinding.topLine.isVisible = index != 0
            itemBinding.bottomLine.isVisible = index != items.lastIndex
            updateStepState(itemBinding, item.state)
            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                92.dp,
            )
            binding.statusStepContainer.addView(itemBinding.root)
        }
    }

    private fun updateStepState(
        itemBinding: ItemOrderTrackingStepBinding,
        state: TrackingStepState,
    ) {
        when (state) {
            TrackingStepState.DONE -> {
                itemBinding.statusCircle.setBackgroundResource(R.drawable.bg_timeline_done)
                itemBinding.tvStatusIcon.text = CHECK_MARK
                itemBinding.tvStepTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_primary),
                )
                itemBinding.tvStepSubtitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_secondary),
                )
                itemBinding.topLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pt_copper))
                itemBinding.bottomLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pt_copper))
            }

            TrackingStepState.CURRENT -> {
                itemBinding.statusCircle.setBackgroundResource(R.drawable.bg_timeline_current)
                itemBinding.tvStatusIcon.text = FIRE_ICON
                itemBinding.tvStepTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_gold),
                )
                itemBinding.tvStepSubtitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_primary),
                )
                itemBinding.topLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.pt_copper))
                itemBinding.bottomLine.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_border_warm),
                )
            }

            TrackingStepState.PENDING -> {
                itemBinding.statusCircle.setBackgroundResource(R.drawable.bg_timeline_pending)
                itemBinding.tvStatusIcon.text = ""
                itemBinding.tvStepTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_secondary),
                )
                itemBinding.tvStepSubtitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_muted),
                )
                itemBinding.topLine.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_border_warm),
                )
                itemBinding.bottomLine.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_border_warm),
                )
            }
        }
    }

    private fun bindProduct(item: TrackingProductUiModel) {
        binding.imgProduct.setImageResource(item.imageRes)
        binding.imgProduct.contentDescription = item.name
        binding.tvProductName.text = item.name
        binding.tvProductOption.text = item.optionText
        binding.tvProductPrice.text = item.price
    }

    private fun updateCartBadge() {
        updatePizzaFlowCartBadge(
            root = binding.pizzaTopBar.root,
            cartItemCount = CartStore.items.sumOf { it.quantity },
        )
    }

    private fun resolveObservationState(): CustomerTrackingObservationState {
        return CustomerTrackingObservationPolicy.resolve(
            edition = AppEditionConfig.current,
            sessionLoggedIn = FakeSessionStore.isLoggedIn,
            sessionRole = FakeSessionStore.currentRole,
            authenticatedUid = auth.currentUser?.uid,
            orderCustomerId = currentCustomerId,
            orderStatus = currentOrderStatus,
            assignedShipperId = currentAssignedShipperId,
        )
    }

    private fun stopTrackingObservation(clearState: Boolean) {
        trackingListenerRegistration?.remove()
        trackingListenerRegistration = null
        trackingListenerBinding = null
        trackingErrorShown = false
        if (clearState) {
            trackingDocumentPresent = false
            trackingUpdatedAtMillis = null
            shipperCoordinate = null
            bothLocationsFramed = false
        }
    }

    private fun handleUnavailableOrder() {
        stopTrackingObservation(clearState = true)
        AppUiMessageBus.publish(
            textRes = R.string.notification_order_unavailable,
            type = UiMessageType.ERROR,
        )
        parentFragmentManager.popBackStack()
    }

    private fun showTrackingTransientErrorOnce() {
        if (trackingErrorShown || _binding == null) {
            return
        }
        trackingErrorShown = true
        showUiMessage(R.string.order_tracking_transient_tracking_error, UiMessageType.WARNING)
    }

    private fun formatRelativeTime(relativeTime: CustomerTrackingRelativeTime): String {
        return when (relativeTime) {
            CustomerTrackingRelativeTime.JustNow -> getString(R.string.order_tracking_relative_just_now)
            is CustomerTrackingRelativeTime.MinutesAgo -> resources.getQuantityString(
                R.plurals.order_tracking_relative_minutes_ago,
                relativeTime.minutes,
                relativeTime.minutes,
            )
        }
    }

    private fun mapCameraPaddingPixels(): Int {
        return (resources.displayMetrics.density * MAP_CAMERA_PADDING_DP).toInt()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        orderListenerRegistration?.remove()
        orderListenerRegistration = null
        stopTrackingObservation(clearState = true)
        mapController?.destroy()
        mapController = null
        currentOrderId = null
        currentCustomerId = null
        currentOrderStatus = STATUS_PENDING
        currentAssignedShipperId = null
        destinationCoordinate = null
        shipperCoordinate = null
        trackingUpdatedAtMillis = null
        trackingDocumentPresent = false
        cameraInitialized = false
        bothLocationsFramed = false
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ORDER_ID = "order_id"
        private const val ORDERS_COLLECTION = "orders"
        private const val TRACKING_COLLECTION = "tracking"
        private const val CURRENT_TRACKING_DOCUMENT = "current"
        private const val FIELD_CUSTOMER_ID = "customerId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SHIPPER_ID = "shipperId"
        private const val FIELD_ORDER_CODE = "orderCode"
        private const val FIELD_ITEMS = "items"
        private const val FIELD_TOTAL = "total"
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_CANCELLED = "CANCELLED"
        private const val MAP_CAMERA_PADDING_DP = 48
        private const val CHECK_MARK = "\u2713"
        private const val FIRE_ICON = "\uD83D\uDD25"

        fun newInstance(orderId: String) = OrderTrackingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ORDER_ID, orderId)
            }
        }
    }
}
