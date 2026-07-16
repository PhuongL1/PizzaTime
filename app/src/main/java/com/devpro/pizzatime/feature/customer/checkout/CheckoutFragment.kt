package com.devpro.pizzatime.feature.customer.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.GuestSession
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentCheckoutBinding
import com.devpro.pizzatime.databinding.ItemCheckoutOrderBinding
import com.devpro.pizzatime.feature.admin.store.StoreSettingsRepository
import com.devpro.pizzatime.feature.admin.store.StoreSettingsUiModel
import com.devpro.pizzatime.feature.auth.PendingAuthDestinationStore
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.payment.DemoPaymentBackendConfig
import com.devpro.pizzatime.feature.customer.payment.DemoPaymentPendingStore
import com.devpro.pizzatime.feature.order.PaymentMethod
import com.devpro.pizzatime.feature.staff.navigation.openDemoPayment
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
import com.devpro.pizzatime.feature.staff.navigation.openOrderSuccess
import com.devpro.pizzatime.feature.staff.navigation.replaceForward
import com.devpro.pizzatime.shared.location.DeliveryLocationSelection
import com.devpro.pizzatime.shared.location.LocationDistanceCalculator
import com.devpro.pizzatime.shared.location.MapPickerFragment
import com.devpro.pizzatime.shared.location.MapPickerResultParser
import com.devpro.pizzatime.shared.location.isValidCoordinate
import com.devpro.pizzatime.shared.location.isValidLatitude
import com.devpro.pizzatime.shared.location.isValidLongitude
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.round

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding: FragmentCheckoutBinding
        get() = checkNotNull(_binding) {
            "FragmentCheckoutBinding is only valid between onCreateView and onDestroyView."
        }

    private var orderItems = emptyList<CheckoutOrderItemUiModel>()
    private var appliedDiscount = 0.0
    private var appliedPromoCode = ""
    private var selectedDeliveryLocation = DeliveryLocationSelection.empty()
    private var hasUserSelectedDeliveryLocation = false
    private var customerName = ""
    private var customerPhone = ""
    private var currentStoreSettings: StoreSettingsUiModel? = null
    private var currentDeliveryEstimate: DeliveryEstimate? = null
    private var isPlacingOrder = false
    private var selectedPaymentMethod: PaymentMethod = PaymentMethod.COD

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (blockCheckoutForGuest()) {
            return
        }

        selectedPaymentMethod = savedInstanceState?.getString(STATE_PAYMENT_METHOD)
            ?.let(OrderPaymentMethodSelection::fromName)
            ?.paymentMethod
            ?: pendingOrderPaymentMethod()

        orderItems = CartStore.items.map { it.toCheckoutItem() }
        appliedPromoCode = CartStore.selectedPromoCode
        appliedDiscount = CartStore.promoDiscountAmount
        renderDeliveryDetails(
            name = getString(R.string.checkout_delivery_name),
            phone = getString(R.string.checkout_delivery_phone),
            address = getString(R.string.common_not_provided),
        )
        renderOrderItems(orderItems)
        renderSummary()
        setupMapPickerResult()
        setupActions()
        renderPaymentMethods()
        updatePlaceOrderLabel()
        loadCustomerDeliveryDetails()
        loadStoreSettings()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PAYMENT_METHOD, selectedPaymentMethod.name)
    }

    private fun loadCustomerDeliveryDetails() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (_binding == null) return@loadProfile
            result.onSuccess { profile ->
                customerName = profile.fullName
                customerPhone = profile.phone
                if (!hasUserSelectedDeliveryLocation) {
                    selectedDeliveryLocation = DeliveryLocationSelection.from(
                        address = profile.deliveryAddress,
                        coordinate = profile.deliveryCoordinate,
                    )
                }
                renderDeliveryDetails(
                    name = profile.fullName,
                    phone = profile.phone.ifBlank { getString(R.string.checkout_delivery_phone) },
                    address = selectedDeliveryLocation.address.ifBlank {
                        getString(R.string.common_not_provided)
                    },
                )
                renderDeliveryCoordinateStatus()
                updateDeliveryEstimate()
            }
        }
    }

    private fun loadStoreSettings() {
        StoreSettingsRepository.loadStoreSettings { result ->
            if (_binding == null) return@loadStoreSettings
            result.onSuccess { settings ->
                currentStoreSettings = settings
                updateDeliveryEstimate(settings)
            }
        }
    }

    private fun renderDeliveryDetails(
        name: String,
        phone: String,
        address: String,
    ) = with(binding) {
        tvDeliveryName.text = name
        tvDeliveryPhone.text = phone
        tvDeliveryAddress.text = address
    }

    private fun renderDeliveryCoordinateStatus() {
        val coordinate = selectedDeliveryLocation.coordinate
        binding.tvDeliveryCoordinates.text = if (coordinate != null) {
            getString(
                R.string.location_coordinates_format,
                coordinate.latitude,
                coordinate.longitude,
            )
        } else {
            getString(R.string.location_coordinates_missing)
        }
    }

    private fun renderOrderItems(items: List<CheckoutOrderItemUiModel>) {
        binding.orderItemContainer.removeAllViews()
        items.forEach { item ->
            val itemBinding = ItemCheckoutOrderBinding.inflate(
                layoutInflater,
                binding.orderItemContainer,
                false,
            )
            itemBinding.ivOrderItemImage.setImageResource(item.imageRes)
            itemBinding.ivOrderItemImage.contentDescription = item.name
            itemBinding.tvOrderItemName.text = item.name
            itemBinding.tvOrderItemMeta.text = item.optionText
            itemBinding.tvOrderItemPrice.text = formatMoney(item.price)
            binding.orderItemContainer.addView(itemBinding.root)
        }
    }

    private fun renderSummary() {
        val subtotal = orderItems.sumOf { it.price }
        val deliveryFee = currentDeliveryEstimate?.deliveryFee ?: 0.0
        val total = (subtotal - appliedDiscount).coerceAtLeast(0.0) + deliveryFee

        binding.tvSubtotal.text = formatMoney(subtotal)
        binding.rowDiscount.isVisible = appliedDiscount > 0.0
        binding.tvDiscount.text = "-${formatMoney(appliedDiscount)}"
        binding.tvDeliveryDistance.text = currentDeliveryEstimate?.distanceKm?.let(::formatDistance)
            ?: getString(R.string.common_not_provided)
        binding.tvDeliveryFee.text = formatMoney(deliveryFee)
        binding.tvTotal.text = formatMoney(total)
    }

    private fun updateDeliveryEstimate(settings: StoreSettingsUiModel? = currentStoreSettings) {
        currentDeliveryEstimate = calculateDeliveryEstimate(
            settings = settings,
            itemsSubtotal = orderItems.sumOf { it.price },
        )
        if (_binding != null) {
            renderSummary()
        }
    }

    private fun setupActions() = with(binding) {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnEditDelivery.setOnClickListener {
            openDeliveryMapPicker()
        }
        paymentCash.setOnClickListener {
            selectPaymentMethod(PaymentMethod.COD)
        }
        paymentDemo.setOnClickListener {
            if (isDemoPaymentConfigured()) {
                selectPaymentMethod(PaymentMethod.DEMO)
            }
        }
        btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        if (selectedPaymentMethod == paymentMethod) {
            return
        }
        selectedPaymentMethod = paymentMethod
        renderPaymentMethods()
        updatePlaceOrderLabel()
    }

    private fun renderPaymentMethods() = with(binding) {
        val demoConfigured = isDemoPaymentConfigured()
        val cashSelected = selectedPaymentMethod == PaymentMethod.COD
        val demoSelected = selectedPaymentMethod == PaymentMethod.DEMO && demoConfigured

        paymentCash.setBackgroundResource(
            if (cashSelected) R.drawable.bg_payment_selected else R.drawable.bg_payment_unselected,
        )
        paymentDemo.setBackgroundResource(
            if (demoSelected) R.drawable.bg_payment_selected else R.drawable.bg_payment_unselected,
        )
        paymentCash.alpha = 1.0f
        paymentDemo.alpha = if (demoConfigured) 1.0f else 0.55f
        paymentCash.isEnabled = true
        paymentDemo.isEnabled = demoConfigured
        tvDemoPaymentConfigMessage.isVisible = !demoConfigured
    }

    private fun updatePlaceOrderLabel() {
        val hasPendingPayment = activePendingDemoOrderId() != null
        if (_binding == null) {
            return
        }
        binding.btnPlaceOrder.text = getString(
            when {
                isPlacingOrder -> R.string.checkout_placing_order
                hasPendingPayment -> R.string.checkout_continue_payment
                else -> R.string.checkout_place_order
            },
        )
    }

    private fun placeOrder() {
        if (isPlacingOrder) {
            return
        }
        val pendingOrderId = activePendingDemoOrderId()
        if (pendingOrderId != null) {
            openDemoPayment(pendingOrderId)
            return
        }
        if (orderItems.isEmpty()) {
            showCheckoutMessage(R.string.cart_empty_title, UiMessageType.INFO)
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            blockCheckoutForGuest()
            return
        }
        if (selectedPaymentMethod == PaymentMethod.DEMO && !isDemoPaymentConfigured()) {
            showCheckoutMessage(R.string.checkout_demo_payment_not_configured, UiMessageType.INFO)
            return
        }
        if (selectedDeliveryLocation.address.isBlank()) {
            showCheckoutMessage(R.string.checkout_delivery_address_missing)
            return
        }
        if (selectedDeliveryLocation.coordinate == null) {
            showCheckoutMessage(R.string.checkout_delivery_location_missing)
            return
        }

        setPlaceOrderLoading(true)
        CheckoutConsistencyRepository.validateCheckout(
            items = CartStore.items,
            promoCode = appliedPromoCode,
        ) { result ->
            if (_binding == null) return@validateCheckout
            result
                .onSuccess { validationResult ->
                    handleCheckoutValidationResult(
                        validationResult = validationResult,
                        customerId = user.uid,
                        customerEmail = user.email ?: "",
                    )
                }
                .onFailure {
                    setPlaceOrderLoading(false)
                    showCheckoutMessage(R.string.checkout_verify_failed)
                }
        }
    }

    private fun handleCheckoutValidationResult(
        validationResult: CheckoutConsistencyResult,
        customerId: String,
        customerEmail: String,
    ) {
        when (validationResult) {
            CheckoutConsistencyResult.ItemsUnavailable -> {
                setPlaceOrderLoading(false)
                showCheckoutMessage(R.string.checkout_items_unavailable)
            }

            is CheckoutConsistencyResult.PriceChanged -> {
                CartStore.replaceItems(validationResult.items)
                orderItems = CartStore.items.map { it.toCheckoutItem() }
                appliedDiscount = 0.0
                appliedPromoCode = ""
                CartStore.clearPromo()
                renderOrderItems(orderItems)
                updateDeliveryEstimate()
                setPlaceOrderLoading(false)
                showCheckoutMessage(R.string.checkout_prices_changed)
            }

            CheckoutConsistencyResult.PromoInvalid -> {
                appliedDiscount = 0.0
                appliedPromoCode = ""
                CartStore.clearPromo()
                updateDeliveryEstimate()
                setPlaceOrderLoading(false)
                showCheckoutMessage(R.string.checkout_promo_invalid)
            }

            is CheckoutConsistencyResult.Valid -> {
                CartStore.replaceItems(validationResult.items)
                orderItems = CartStore.items.map { it.toCheckoutItem() }
                appliedDiscount = validationResult.discount
                appliedPromoCode = validationResult.promoCode
                CartStore.setPromo(validationResult.promoCode, validationResult.discount)
                updateDeliveryEstimate()
                loadStoreSettingsForOrder { storeSettings, deliveryEstimate ->
                    createValidatedOrder(
                        customerId = customerId,
                        customerEmail = customerEmail,
                        items = validationResult.items,
                        promoCode = validationResult.promoCode,
                        discount = validationResult.discount,
                        storeSettings = storeSettings,
                        deliveryEstimate = deliveryEstimate,
                    )
                }
            }
        }
    }

    private fun loadStoreSettingsForOrder(onValidStore: (StoreSettingsUiModel, DeliveryEstimate) -> Unit) {
        StoreSettingsRepository.loadStoreSettings { result ->
            if (_binding == null) return@loadStoreSettings
            result
                .onSuccess { settings ->
                    currentStoreSettings = settings
                    when {
                        !settings.acceptingOrders -> {
                            setPlaceOrderLoading(false)
                            showCheckoutMessage(R.string.checkout_store_closed)
                        }

                        settings.storeName.isBlank() || settings.pickupAddress.isBlank() -> {
                            setPlaceOrderLoading(false)
                            showCheckoutMessage(R.string.checkout_store_pickup_missing)
                        }

                        !settings.pickupLat.isValidLatitude() || !settings.pickupLng.isValidLongitude() -> {
                            setPlaceOrderLoading(false)
                            showCheckoutMessage(R.string.checkout_store_location_missing)
                        }

                        selectedDeliveryLocation.address.isBlank() -> {
                            setPlaceOrderLoading(false)
                            showCheckoutMessage(R.string.checkout_delivery_address_missing)
                        }

                        selectedDeliveryLocation.coordinate == null -> {
                            setPlaceOrderLoading(false)
                            showCheckoutMessage(R.string.checkout_delivery_location_missing)
                        }

                        else -> {
                            val estimate = calculateDeliveryEstimate(
                                settings = settings,
                                itemsSubtotal = orderItems.sumOf { it.price },
                            )
                            if (estimate == null) {
                                setPlaceOrderLoading(false)
                                showCheckoutMessage(R.string.checkout_delivery_location_missing)
                            } else {
                                currentDeliveryEstimate = estimate
                                renderSummary()
                                onValidStore(settings, estimate)
                            }
                        }
                    }
                }
                .onFailure {
                    setPlaceOrderLoading(false)
                    showCheckoutMessage(R.string.checkout_store_pickup_missing)
                }
        }
    }

    private fun createValidatedOrder(
        customerId: String,
        customerEmail: String,
        items: List<CartItemUiModel>,
        promoCode: String,
        discount: Double,
        storeSettings: StoreSettingsUiModel,
        deliveryEstimate: DeliveryEstimate,
    ) {
        val deliveryCoordinate = selectedDeliveryLocation.coordinate
        if (deliveryCoordinate == null) {
            setPlaceOrderLoading(false)
            showCheckoutMessage(R.string.checkout_delivery_location_missing)
            return
        }
        val itemsSubtotal = items.sumOf { it.price * it.quantity }
        val discountAmount = discount.coerceAtLeast(0.0)
        val finalTotal = (itemsSubtotal - discountAmount).coerceAtLeast(0.0) + deliveryEstimate.deliveryFee
        FirebaseOrderRepository.createOrder(
            customerId = customerId,
            customerEmail = customerEmail,
            items = items,
            distanceKm = deliveryEstimate.distanceKm,
            deliveryFee = deliveryEstimate.deliveryFee,
            itemsSubtotal = itemsSubtotal,
            discountAmount = discountAmount,
            finalTotal = finalTotal,
            deliveryAddress = selectedDeliveryLocation.address,
            deliveryCoordinate = deliveryCoordinate,
            customerName = customerName.ifBlank { customerEmail },
            customerPhone = customerPhone,
            storeSettings = storeSettings,
            paymentMethod = selectedPaymentMethod,
            promoCode = promoCode,
            onResult = { result ->
                if (_binding == null) return@createOrder
                result
                    .onSuccess { createdOrderId ->
                        when (selectedPaymentMethod) {
                            PaymentMethod.COD -> {
                                CartStore.clear()
                                openOrderSuccess(orderId = createdOrderId, addToBackStack = false)
                            }

                            PaymentMethod.DEMO -> {
                                DemoPaymentPendingStore.createNewAttemptState(
                                    context = requireContext().applicationContext,
                                    userId = customerId,
                                    orderId = createdOrderId,
                                )
                                openDemoPayment(createdOrderId)
                            }

                            else -> {
                                CartStore.clear()
                                openOrderSuccess(orderId = createdOrderId, addToBackStack = false)
                            }
                        }
                    }
                    .onFailure {
                        setPlaceOrderLoading(false)
                        showCheckoutMessage(R.string.checkout_place_order_failed)
                    }
            },
        )
    }

    private fun setPlaceOrderLoading(loading: Boolean) {
        isPlacingOrder = loading
        binding.btnPlaceOrder.isEnabled = !loading
        updatePlaceOrderLabel()
    }

    private fun showCheckoutMessage(
        messageRes: Int,
        type: UiMessageType = UiMessageType.ERROR,
    ) {
        showUiMessage(messageRes, type)
    }

    private fun blockCheckoutForGuest(): Boolean {
        if (!GuestSession.isGuest()) {
            return false
        }

        PendingAuthDestinationStore.setCheckout(requireContext())
        Log.d(TAG, "Checkout blocked because authentication missing")
        openLoginRequiredScreen(addToBackStack = false)
        return true
    }

    private fun setupMapPickerResult() {
        parentFragmentManager.setFragmentResultListener(
            MapPickerFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            if (bundle.getString(MapPickerFragment.KEY_MODE) != MapPickerFragment.MODE_CUSTOMER_DELIVERY) {
                return@setFragmentResultListener
            }
            val selection = MapPickerResultParser.parse(
                hasAddress = bundle.containsKey(MapPickerFragment.KEY_ADDRESS),
                address = bundle.getString(MapPickerFragment.KEY_ADDRESS),
                hasLatitude = bundle.containsKey(MapPickerFragment.KEY_LAT),
                latitude = if (bundle.containsKey(MapPickerFragment.KEY_LAT)) {
                    bundle.getDouble(MapPickerFragment.KEY_LAT)
                } else {
                    null
                },
                hasLongitude = bundle.containsKey(MapPickerFragment.KEY_LNG),
                longitude = if (bundle.containsKey(MapPickerFragment.KEY_LNG)) {
                    bundle.getDouble(MapPickerFragment.KEY_LNG)
                } else {
                    null
                },
            )
            if (selection == null) {
                showCheckoutMessage(R.string.checkout_delivery_location_missing)
                return@setFragmentResultListener
            }
            selectedDeliveryLocation = selection
            hasUserSelectedDeliveryLocation = true
            renderDeliveryDetails(
                name = customerName.ifBlank { getString(R.string.checkout_delivery_name) },
                phone = customerPhone.ifBlank { getString(R.string.checkout_delivery_phone) },
                address = selection.address,
            )
            renderDeliveryCoordinateStatus()
            updateDeliveryEstimate()
            saveDeliveryLocation(selection)
        }
    }

    private fun openDeliveryMapPicker() {
        parentFragmentManager.replaceForward(
            containerId = R.id.fragmentContainer,
            fragment = MapPickerFragment.newInstance(
                mode = MapPickerFragment.MODE_CUSTOMER_DELIVERY,
                initialAddress = selectedDeliveryLocation.address,
                initialLat = selectedDeliveryLocation.coordinate?.latitude,
                initialLng = selectedDeliveryLocation.coordinate?.longitude,
            ),
        )
    }

    private fun saveDeliveryLocation(selection: DeliveryLocationSelection) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerProfileFirestoreRepository.updateDeliveryLocation(
            uid = uid,
            selection = selection,
        ) { result ->
            if (_binding == null || !isAdded) return@updateDeliveryLocation
            result.onFailure {
                showCheckoutMessage(R.string.checkout_delivery_location_save_failed)
            }
        }
    }

    private fun formatMoney(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    private fun formatDistance(distanceKm: Double): String {
        return String.format(Locale.US, "%.1f km", distanceKm)
    }

    private fun calculateDeliveryEstimate(
        settings: StoreSettingsUiModel?,
        itemsSubtotal: Double,
    ): DeliveryEstimate? {
        if (settings == null) return null
        if (!isValidCoordinate(settings.pickupLat, settings.pickupLng)) return null

        val pickupLat = settings.pickupLat ?: return null
        val pickupLng = settings.pickupLng ?: return null
        val deliveryCoordinate = selectedDeliveryLocation.coordinate ?: return null
        val rawDistanceKm = LocationDistanceCalculator.calculateDistanceKm(
            startLat = pickupLat,
            startLng = pickupLng,
            endLat = deliveryCoordinate.latitude,
            endLng = deliveryCoordinate.longitude,
        )
        val roundedDistanceKm = ceil(rawDistanceKm * 10.0) / 10.0
        val deliveryFee = if (
            settings.freeDeliveryMinSubtotal > 0.0 &&
            itemsSubtotal >= settings.freeDeliveryMinSubtotal
        ) {
            0.0
        } else {
            settings.baseDeliveryFee + roundedDistanceKm * settings.deliveryFeePerKm
        }

        return DeliveryEstimate(
            distanceKm = roundedDistanceKm,
            deliveryFee = round(deliveryFee * 100.0) / 100.0,
        )
    }

    private fun isDemoPaymentConfigured(): Boolean {
        return DemoPaymentBackendConfig.configured() != null
    }

    private fun activePendingDemoOrderId(): String? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid?.trim().orEmpty()
        if (userId.isBlank()) {
            return null
        }
        return DemoPaymentPendingStore.activeForUser(
            context = requireContext().applicationContext,
            userId = userId,
        )?.orderId
    }

    private fun pendingOrderPaymentMethod(): PaymentMethod {
        return if (activePendingDemoOrderId() != null) {
            PaymentMethod.DEMO
        } else {
            PaymentMethod.COD
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        private const val TAG = "CheckoutFragment"
        private const val STATE_PAYMENT_METHOD = "state_payment_method"
    }
}

private data class DeliveryEstimate(
    val distanceKm: Double,
    val deliveryFee: Double,
)

private enum class OrderPaymentMethodSelection(val paymentMethod: PaymentMethod) {
    COD(PaymentMethod.COD),
    DEMO(PaymentMethod.DEMO);

    companion object {
        fun fromName(name: String): OrderPaymentMethodSelection {
            return entries.firstOrNull { selection -> selection.name == name } ?: COD
        }
    }
}

private fun CartItemUiModel.toCheckoutItem(): CheckoutOrderItemUiModel {
    val customizationParts = buildList {
        if (selectedSize.isNotBlank()) add(selectedSize)
        if (selectedCrust.isNotBlank()) add(selectedCrust)
        if (selectedToppings.isNotEmpty()) add(selectedToppings.joinToString())
    }
    val optionText = ("x$quantity " + customizationParts.joinToString(" • ")).trim()
    return CheckoutOrderItemUiModel(
        id = id,
        name = name,
        optionText = optionText,
        price = price * quantity,
        imageRes = imageRes,
    )
}
