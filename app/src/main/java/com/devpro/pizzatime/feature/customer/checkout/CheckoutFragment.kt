package com.devpro.pizzatime.feature.customer.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCheckoutBinding
import com.devpro.pizzatime.databinding.ItemCheckoutOrderBinding
import com.devpro.pizzatime.feature.admin.store.StoreSettingsRepository
import com.devpro.pizzatime.feature.admin.store.StoreSettingsUiModel
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.openOrderSuccess
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

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
    private var customerName = ""
    private var customerPhone = ""
    private var currentStoreSettings: StoreSettingsUiModel? = null
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
        orderItems = CartStore.items.map { it.toCheckoutItem() }
        renderDeliveryDetails(
            name = getString(R.string.checkout_delivery_name),
            phone = getString(R.string.checkout_delivery_phone),
            address = getString(R.string.common_not_provided),
        )
        renderOrderItems(orderItems)
        renderSummary()
        setupActions()
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
                if (profileAddress.isNotBlank()) {
                    selectedDeliveryAddress = profileAddress
                }
                renderDeliveryDetails(
                    name = profile.fullName,
                    phone = profile.phone.ifBlank { getString(R.string.checkout_delivery_phone) },
                    address = selectedDeliveryAddress.ifBlank { getString(R.string.common_not_provided) },
                )
            }
        }
    }

    private fun loadStoreSettings() {
        StoreSettingsRepository.loadStoreSettings { result ->
            if (_binding == null) return@loadStoreSettings
            result.onSuccess { settings ->
                currentStoreSettings = settings
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
        val deliveryFee = DELIVERY_FEE
        val total = subtotal - appliedDiscount + deliveryFee

        binding.tvSubtotal.text = formatMoney(subtotal)
        binding.tvDeliveryFee.text = formatMoney(deliveryFee)
        binding.tvTotal.text = formatMoney(total)
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnEditDelivery.setOnClickListener {
            Toast.makeText(requireContext(), "Edit delivery later", Toast.LENGTH_SHORT).show()
        }

        binding.paymentCreditCard.setOnClickListener {
            Toast.makeText(requireContext(), "Credit Card selected", Toast.LENGTH_SHORT).show()
        }

        binding.paymentApplePay.setOnClickListener {
            Toast.makeText(requireContext(), "Apple Pay selected", Toast.LENGTH_SHORT).show()
        }

        binding.paymentCash.setOnClickListener {
            Toast.makeText(requireContext(), "Cash on Arrival selected", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(requireContext(), "Please log in to place an order.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDeliveryAddress.trim().isBlank()) {
            showToast(R.string.checkout_delivery_address_missing)
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
                renderOrderItems(orderItems)
                renderSummary()
                setPlaceOrderLoading(false)
                showToast(R.string.checkout_prices_changed)
            }

            CheckoutConsistencyResult.PromoInvalid -> {
                appliedDiscount = 0.0
                appliedPromoCode = ""
                renderSummary()
                setPlaceOrderLoading(false)
                showToast(R.string.checkout_promo_invalid)
            }

            is CheckoutConsistencyResult.Valid -> {
                CartStore.replaceItems(validationResult.items)
                orderItems = CartStore.items.map { it.toCheckoutItem() }
                appliedDiscount = validationResult.discount
                appliedPromoCode = validationResult.promoCode
                renderSummary()
                loadStoreSettingsForOrder { storeSettings ->
                    createValidatedOrder(
                        customerId = customerId,
                        customerEmail = customerEmail,
                        items = validationResult.items,
                        promoCode = validationResult.promoCode,
                        discount = validationResult.discount,
                        storeSettings = storeSettings,
                    )
                }
            }
        }
    }

    private fun loadStoreSettingsForOrder(onValidStore: (StoreSettingsUiModel) -> Unit) {
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

                        selectedDeliveryAddress.trim().isBlank() -> {
                            setPlaceOrderLoading(false)
                            showToast(R.string.checkout_delivery_address_missing)
                        }

                        else -> onValidStore(settings)
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
    ) {
        FirebaseOrderRepository.createOrder(
            customerId = customerId,
            customerEmail = customerEmail,
            items = items,
            deliveryFee = DELIVERY_FEE,
            deliveryAddress = selectedDeliveryAddress.trim(),
            customerName = customerName.ifBlank { customerEmail },
            customerPhone = customerPhone,
            storeSettings = storeSettings,
            promoCode = promoCode,
            discount = discount,
            onResult = { result ->
                if (_binding == null) return@createOrder
                result
                    .onSuccess { orderId ->
                        CartStore.clear()
                        openOrderSuccess(orderId = orderId, addToBackStack = false)
                    }
                    .onFailure { error ->
                        setPlaceOrderLoading(false)
                        Toast.makeText(
                            requireContext(),
                            error.message ?: "Failed to place order.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
            },
        )
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

    private fun formatMoney(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
    companion object {
        private const val DELIVERY_FEE = 4.5
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
