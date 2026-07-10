package com.devpro.pizzatime.feature.customer.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.GuestSession
import com.devpro.pizzatime.databinding.FragmentCheckoutBinding
import com.devpro.pizzatime.databinding.ItemCheckoutOrderBinding
import com.devpro.pizzatime.feature.auth.PendingAuthDestinationStore
import com.devpro.pizzatime.feature.admin.store.StoreSettingsRepository
import com.devpro.pizzatime.feature.admin.store.StoreSettingsUiModel
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
import com.devpro.pizzatime.feature.staff.navigation.openOrderSuccess
import com.devpro.pizzatime.feature.staff.navigation.replaceForward
import com.devpro.pizzatime.shared.location.LocationDistanceCalculator
import com.devpro.pizzatime.shared.location.MapPickerFragment
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
    private var selectedDeliveryAddress = ""
    private var selectedDeliveryLat: Double? = null
    private var selectedDeliveryLng: Double? = null
    private var customerName = ""
    private var customerPhone = ""
    private var currentStoreSettings: StoreSettingsUiModel? = null
    private var currentDeliveryEstimate: DeliveryEstimate? = null
    private var isPlacingOrder = false

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
        loadCustomerDeliveryDetails()
        loadStoreSettings()
    }

    private fun loadCustomerDeliveryDetails() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (_binding == null) return@loadProfile
            result.onSuccess { profile ->
                val profileAddress = profile.deliveryAddress.trim()
                customerName = profile.fullName
                customerPhone = profile.phone
                selectedDeliveryLat = profile.deliveryLat
                selectedDeliveryLng = profile.deliveryLng
                if (profileAddress.isNotBlank()) {
                    selectedDeliveryAddress = profileAddress
                }
                renderDeliveryDetails(
                    name = profile.fullName,
                    phone = profile.phone.ifBlank { getString(R.string.checkout_delivery_phone) },
                    address = selectedDeliveryAddress.ifBlank { getString(R.string.common_not_provided) },
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
        binding.tvDeliveryCoordinates.text = if (
            selectedDeliveryLat.isValidLatitude() && selectedDeliveryLng.isValidLongitude()
        ) {
            getString(R.string.location_coordinates_format, selectedDeliveryLat, selectedDeliveryLng)
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
                false
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
        binding.tvDeliveryDistance.text = currentDeliveryEstimate?.let {
            formatDistance(it.distanceKm)
        } ?: getString(R.string.common_not_provided)
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

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnEditDelivery.setOnClickListener {
            openDeliveryMapPicker()
        }

        binding.paymentCreditCard.setOnClickListener {
            showToast(R.string.checkout_payment_method_coming_soon)
        }

        binding.paymentApplePay.setOnClickListener {
            showToast(R.string.checkout_payment_method_coming_soon)
        }

        binding.paymentCash.setOnClickListener {
            renderPaymentMethods()
        }

        binding.btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun placeOrder() {
        if (isPlacingOrder) {
            return
        }

        if (orderItems.isEmpty()) {
            Toast.makeText(requireContext(), "Your cart is empty.", Toast.LENGTH_SHORT).show()
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            blockCheckoutForGuest()
            return
        }
        if (selectedDeliveryAddress.trim().isBlank()) {
            showToast(R.string.checkout_delivery_address_missing)
            return
        }
        if (!selectedDeliveryLat.isValidLatitude() || !selectedDeliveryLng.isValidLongitude()) {
            showToast(R.string.checkout_delivery_location_missing)
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
                    showToast(R.string.checkout_verify_failed)
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
                showToast(R.string.checkout_items_unavailable)
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
                showToast(R.string.checkout_prices_changed)
            }

            CheckoutConsistencyResult.PromoInvalid -> {
                appliedDiscount = 0.0
                appliedPromoCode = ""
                CartStore.clearPromo()
                updateDeliveryEstimate()
                setPlaceOrderLoading(false)
                showToast(R.string.checkout_promo_invalid)
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
                            showToast(R.string.checkout_store_closed)
                        }

                        settings.storeName.isBlank() || settings.pickupAddress.isBlank() -> {
                            setPlaceOrderLoading(false)
                            showToast(R.string.checkout_store_pickup_missing)
                        }

                        !settings.pickupLat.isValidLatitude() || !settings.pickupLng.isValidLongitude() -> {
                            setPlaceOrderLoading(false)
                            showToast(R.string.checkout_store_location_missing)
                        }

                        selectedDeliveryAddress.trim().isBlank() -> {
                            setPlaceOrderLoading(false)
                            showToast(R.string.checkout_delivery_address_missing)
                        }

                        !selectedDeliveryLat.isValidLatitude() || !selectedDeliveryLng.isValidLongitude() -> {
                            setPlaceOrderLoading(false)
                            showToast(R.string.checkout_delivery_location_missing)
                        }

                        else -> {
                            val estimate = calculateDeliveryEstimate(
                                settings = settings,
                                itemsSubtotal = orderItems.sumOf { it.price },
                            )
                            if (estimate == null) {
                                setPlaceOrderLoading(false)
                                showToast(R.string.checkout_delivery_location_missing)
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
                    showToast(R.string.checkout_store_pickup_missing)
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
        val itemsSubtotal = items.sumOf { it.price * it.quantity }
        val discountAmount = discount.coerceAtLeast(0.0)
        val finalTotal = (itemsSubtotal - discountAmount).coerceAtLeast(0.0) +
            deliveryEstimate.deliveryFee
        FirebaseOrderRepository.createOrder(
            customerId = customerId,
            customerEmail = customerEmail,
            items = items,
            distanceKm = deliveryEstimate.distanceKm,
            deliveryFee = deliveryEstimate.deliveryFee,
            itemsSubtotal = itemsSubtotal,
            discountAmount = discountAmount,
            finalTotal = finalTotal,
            deliveryAddress = selectedDeliveryAddress.trim(),
            deliveryLat = selectedDeliveryLat,
            deliveryLng = selectedDeliveryLng,
            customerName = customerName.ifBlank { customerEmail },
            customerPhone = customerPhone,
            storeSettings = storeSettings,
            promoCode = promoCode,
            onResult = { result ->
                if (_binding == null) return@createOrder
                result
                    .onSuccess { orderId ->
                        CartStore.clear()
                        openOrderSuccess(orderId = orderId, addToBackStack = false)
                    }
                    .onFailure { error ->
                        setPlaceOrderLoading(false)
                        showToast(R.string.checkout_place_order_failed)
                    }
            },
        )
    }

    private fun renderPaymentMethods() = with(binding) {
        paymentCash.setBackgroundResource(R.drawable.bg_payment_selected)
        paymentCreditCard.setBackgroundResource(R.drawable.bg_payment_unselected)
        paymentApplePay.setBackgroundResource(R.drawable.bg_payment_unselected)
        paymentCash.alpha = 1.0f
        paymentCreditCard.alpha = 0.55f
        paymentApplePay.alpha = 0.55f
        paymentCreditCard.isEnabled = true
        paymentApplePay.isEnabled = true
        paymentCash.isEnabled = true
    }

    private fun setPlaceOrderLoading(loading: Boolean) {
        isPlacingOrder = loading
        binding.btnPlaceOrder.isEnabled = !loading
        binding.btnPlaceOrder.text = getString(
            if (loading) R.string.checkout_placing_order else R.string.checkout_place_order,
        )
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
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
            val address = bundle.getString(MapPickerFragment.KEY_ADDRESS).orEmpty()
            val lat = bundle.getDouble(MapPickerFragment.KEY_LAT)
            val lng = bundle.getDouble(MapPickerFragment.KEY_LNG)
            if (!lat.isValidLatitude() || !lng.isValidLongitude()) {
                showToast(R.string.checkout_delivery_location_missing)
                return@setFragmentResultListener
            }
            selectedDeliveryAddress = address
            selectedDeliveryLat = lat
            selectedDeliveryLng = lng
            renderDeliveryDetails(
                name = customerName.ifBlank { getString(R.string.checkout_delivery_name) },
                phone = customerPhone.ifBlank { getString(R.string.checkout_delivery_phone) },
                address = selectedDeliveryAddress,
            )
            renderDeliveryCoordinateStatus()
            updateDeliveryEstimate()
            saveDeliveryLocation(address, lat, lng)
        }
    }

    private fun openDeliveryMapPicker() {
        parentFragmentManager.replaceForward(
            containerId = R.id.fragmentContainer,
            fragment = MapPickerFragment.newInstance(
                mode = MapPickerFragment.MODE_CUSTOMER_DELIVERY,
                initialAddress = selectedDeliveryAddress,
                initialLat = selectedDeliveryLat,
                initialLng = selectedDeliveryLng,
            )
        )
    }

    private fun saveDeliveryLocation(address: String, lat: Double, lng: Double) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerProfileFirestoreRepository.updateDeliveryLocation(
            uid = uid,
            deliveryAddress = address,
            deliveryLat = lat,
            deliveryLng = lng,
        ) { result ->
            if (_binding == null || !isAdded) return@updateDeliveryLocation
            result.onFailure {
                showToast(R.string.checkout_delivery_location_save_failed)
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
        if (!isValidCoordinate(selectedDeliveryLat, selectedDeliveryLng)) return null

        val pickupLat = settings.pickupLat ?: return null
        val pickupLng = settings.pickupLng ?: return null
        val deliveryLat = selectedDeliveryLat ?: return null
        val deliveryLng = selectedDeliveryLng ?: return null
        val rawDistanceKm = LocationDistanceCalculator.calculateDistanceKm(
            startLat = pickupLat,
            startLng = pickupLng,
            endLat = deliveryLat,
            endLng = deliveryLng,
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TAG = "CheckoutFragment"
    }
}

private data class DeliveryEstimate(
    val distanceKm: Double,
    val deliveryFee: Double,
)

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
