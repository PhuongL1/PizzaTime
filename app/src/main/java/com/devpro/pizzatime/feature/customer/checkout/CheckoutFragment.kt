package com.devpro.pizzatime.feature.customer.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCheckoutBinding
import com.devpro.pizzatime.databinding.ItemCheckoutOrderBinding
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.openOrderSuccess
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        selectedDeliveryAddress = getString(R.string.checkout_delivery_address)
        renderDeliveryDetails(
            name = getString(R.string.checkout_delivery_name),
            phone = getString(R.string.checkout_delivery_phone),
            address = selectedDeliveryAddress,
        )
        renderOrderItems(orderItems)
        renderSummary()
        setupActions()
        loadCustomerDeliveryDetails()
    }

    private fun loadCustomerDeliveryDetails() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (_binding == null) return@loadProfile
            result.onSuccess { profile ->
                val profileAddress = profile.deliveryAddress.trim()
                if (profileAddress.isNotBlank()) {
                    selectedDeliveryAddress = profileAddress
                }
                renderDeliveryDetails(
                    name = profile.fullName,
                    phone = profile.phone.ifBlank { getString(R.string.checkout_delivery_phone) },
                    address = selectedDeliveryAddress,
                )
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

        items.forEachIndexed { index, item ->
            val itemBinding = ItemCheckoutOrderBinding.inflate(
                layoutInflater,
                binding.orderItemContainer,
                false
            )

            itemBinding.imgOrderPizza.setImageResource(item.imageRes)
            itemBinding.imgOrderPizza.contentDescription = item.name
            itemBinding.tvOrderName.text = item.name
            itemBinding.tvOrderOption.text = item.optionText
            itemBinding.tvOrderPrice.text = formatMoney(item.price)

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                82.dp
            ).apply {
                if (index > 0) {
                    topMargin = 16.dp
                }
            }

            binding.orderItemContainer.addView(itemBinding.root)
        }
    }

    private fun renderSummary() {
        val subtotal = orderItems.sumOf { it.price }
        val deliveryFee = DELIVERY_FEE
        val total = subtotal - appliedDiscount + deliveryFee

        binding.tvSubtotal.text = formatMoney(subtotal)
        binding.tvDeliveryFee.text = formatMoney(deliveryFee)
        binding.promoDiscountRow.isVisible = appliedDiscount > 0
        binding.tvDiscount.text = "-${formatMoney(appliedDiscount)}"
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

        binding.btnApplyPromo.setOnClickListener {
            applyPromoCode()
        }
    }

    private fun applyPromoCode() {
        val code = binding.edtPromoCode.text?.toString()?.trim()?.uppercase(Locale.US).orEmpty()
        if (code.isBlank()) {
            Toast.makeText(requireContext(), "Enter a promo code.", Toast.LENGTH_SHORT).show()
            return
        }
        val subtotal = orderItems.sumOf { it.price }
        binding.btnApplyPromo.isEnabled = false

        FirebaseFirestore.getInstance().collection("promoCodes").document(code)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                binding.btnApplyPromo.isEnabled = true
                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Promo code not found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val active = doc.getBoolean("active") ?: false
                if (!active) {
                    Toast.makeText(requireContext(), "This promo code is inactive.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val minOrderAmount = doc.getDouble("minOrderAmount") ?: 0.0
                if (subtotal < minOrderAmount) {
                    Toast.makeText(
                        requireContext(),
                        String.format(Locale.US, "Minimum order $%.2f required.", minOrderAmount),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@addOnSuccessListener
                }
                val discountType = doc.getString("discountType") ?: "PERCENT"
                val discountValue = doc.getDouble("discountValue") ?: 0.0
                val discount = when (discountType.uppercase(Locale.US)) {
                    "PERCENT" -> (subtotal * discountValue / 100).coerceAtMost(subtotal)
                    "FIXED" -> discountValue.coerceAtMost(subtotal)
                    else -> 0.0
                }
                appliedDiscount = discount
                appliedPromoCode = code
                renderSummary()
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.btnApplyPromo.isEnabled = true
                Toast.makeText(requireContext(), "Failed to validate promo code.", Toast.LENGTH_SHORT).show()
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
                createValidatedOrder(
                    customerId = customerId,
                    customerEmail = customerEmail,
                    items = validationResult.items,
                    promoCode = validationResult.promoCode,
                    discount = validationResult.discount,
                )
            }
        }
    }

    private fun createValidatedOrder(
        customerId: String,
        customerEmail: String,
        items: List<CartItemUiModel>,
        promoCode: String,
        discount: Double,
    ) {
        FirebaseOrderRepository.createOrder(
            customerId = customerId,
            customerEmail = customerEmail,
            items = items,
            deliveryFee = DELIVERY_FEE,
            deliveryAddress = selectedDeliveryAddress,
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

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

