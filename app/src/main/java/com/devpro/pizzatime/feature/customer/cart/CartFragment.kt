package com.devpro.pizzatime.feature.customer.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.databinding.FragmentCartBinding
import com.devpro.pizzatime.databinding.ItemCartPizzaBinding
import com.devpro.pizzatime.feature.customer.checkout.CheckoutConsistencyRepository
import com.devpro.pizzatime.feature.customer.checkout.CheckoutFragment
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
import com.devpro.pizzatime.feature.staff.navigation.replaceForward
import java.util.Locale

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding: FragmentCartBinding
        get() = checkNotNull(_binding) {
            "FragmentCartBinding is only valid between onCreateView and onDestroyView."
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderCart()
        setupActions()
    }

    private fun renderCart() {
        val cartItems = CartStore.items
        val hasItems = cartItems.isNotEmpty()
        val selectedItemCount = cartItems.sumOf { it.quantity }

        binding.cartItemContainer.removeAllViews()
        binding.tvPromoHint.setText(CartStore.selectedPromoCode)

        binding.cartItemContainer.isVisible = hasItems
        binding.promoCodeBar.isVisible = hasItems
        binding.orderSummaryCard.isVisible = hasItems
        binding.cartBottomBar.isVisible = hasItems
        binding.emptyCartView.isVisible = !hasItems
        binding.tvCartBadge.isVisible = selectedItemCount > 0
        binding.tvCartBadge.text = if (selectedItemCount > 0) selectedItemCount.toString() else ""
        binding.tvCartSubtitle.text = if (hasItems) {
            getString(R.string.cart_items_selected_format, selectedItemCount)
        } else {
            getString(R.string.cart_empty_message)
        }

        if (!hasItems) {
            CartStore.clearPromo()
            binding.tvPromoHint.text = null
            renderSummary(emptyList())
            return
        }

        cartItems.forEachIndexed { index, item ->
            val itemBinding = ItemCartPizzaBinding.inflate(
                layoutInflater,
                binding.cartItemContainer,
                false,
            )

            bindCartItem(itemBinding, item)

            itemBinding.btnMinus.setOnClickListener {
                updateCartQuantity(item.cartKey, increase = false)
            }

            itemBinding.btnPlus.setOnClickListener {
                updateCartQuantity(item.cartKey, increase = true)
            }

            itemBinding.btnRemove.setOnClickListener {
                CartStore.removeItem(item.cartKey)
                renderCart()
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                if (index > 0) {
                    topMargin = 22.dp
                }
            }

            binding.cartItemContainer.addView(itemBinding.root)
        }

        renderSummary(cartItems)
    }

    private fun bindCartItem(
        itemBinding: ItemCartPizzaBinding,
        item: CartItemUiModel,
    ) {
        itemBinding.imgPizza.loadProductImage(item.imageUrl, item.imageRes)
        itemBinding.imgPizza.contentDescription = item.name
        itemBinding.tvPizzaName.text = item.name

        val descriptionText = item.displayDescription()
        itemBinding.tvPizzaDescription.isVisible = descriptionText.isNotBlank()
        itemBinding.tvPizzaDescription.text = descriptionText
        itemBinding.tvPizzaPrice.text = formatMoney(item.lineTotalPrice)
        itemBinding.tvQuantity.text = item.quantity.toString()
    }

    private fun updateCartQuantity(cartKey: String, increase: Boolean) {
        if (increase) {
            CartStore.increaseQuantity(cartKey)
        } else {
            CartStore.decreaseQuantity(cartKey)
        }
        renderCart()
    }

    private fun CartItemUiModel.displayDescription(): String {
        val normalizedDescription = description.compactText()
        return if (normalizedDescription.isNotBlank()) {
            normalizedDescription
        } else {
            customizationText()
        }
    }

    private fun CartItemUiModel.customizationText(): String {
        val parts = buildList {
            if (selectedSize.isNotBlank()) add("Size: $selectedSize")
            if (selectedCrust.isNotBlank()) add("Crust: $selectedCrust")
            if (selectedToppings.isNotEmpty()) add("Toppings: ${selectedToppings.joinToString()}")
        }
        return parts.joinToString(" | ")
    }

    private val CartItemUiModel.lineTotalPrice: Double
        get() = price * quantity

    private fun String.compactText(): String {
        return trim().replace(Regex("\\s+"), " ")
    }

    private fun renderSummary(cartItems: List<CartItemUiModel>) {
        val subtotal = cartItems.sumOf { item -> item.lineTotalPrice }
        val deliveryFee = if (cartItems.isEmpty()) 0.0 else FakeCartData.deliveryFee
        val discount = CartStore.promoDiscountAmount.coerceIn(0.0, subtotal)
        val total = (subtotal - discount).coerceAtLeast(0.0) + deliveryFee

        binding.tvSubtotal.text = formatMoney(subtotal)
        binding.tvDeliveryFee.text = formatMoney(deliveryFee)
        binding.tvDiscount.text = "-${formatMoney(discount)}"
        binding.tvTotal.text = formatMoney(total)
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCartIcon.setOnClickListener {
            renderCart()
        }

        binding.btnApplyPromo.setOnClickListener {
            applyPromoCode()
        }

        binding.btnProceedCheckout.setOnClickListener {
            if (FakeSessionStore.isLoggedIn) {
                openCheckoutScreen()
            } else {
                openLoginRequiredScreen()
            }
        }
    }

    private fun openCheckoutScreen() {
        parentFragmentManager.replaceForward(
            containerId = R.id.fragmentContainer,
            fragment = CheckoutFragment(),
        )
    }

    private fun applyPromoCode() {
        val promoCode = binding.tvPromoHint.text?.toString().orEmpty().trim()
        val subtotal = CartStore.items.sumOf { item -> item.lineTotalPrice }
        if (promoCode.isBlank() || subtotal <= 0.0) {
            clearInvalidPromo()
            return
        }

        CheckoutConsistencyRepository.validatePromoCode(
            promoCode = promoCode,
            subtotal = subtotal,
        ) { result ->
            if (_binding == null) return@validatePromoCode
            result
                .onSuccess { promoResult ->
                    when (promoResult) {
                        is CheckoutConsistencyRepository.PromoValidationResult.Valid -> {
                            CartStore.setPromo(
                                code = promoResult.promoCode,
                                discountAmount = promoResult.discount,
                            )
                            binding.tvPromoHint.setText(promoResult.promoCode)
                            renderSummary(CartStore.items)
                            Toast.makeText(
                                requireContext(),
                                R.string.cart_promo_applied_successfully,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }

                        is CheckoutConsistencyRepository.PromoValidationResult.Invalid -> {
                            showPromoValidationMessage(promoResult.reason)
                        }
                    }
                }
                .onFailure {
                    clearInvalidPromo()
                }
        }
    }

    private fun clearInvalidPromo() {
        CartStore.clearPromo()
        binding.tvPromoHint.text = null
        renderSummary(CartStore.items)
        Toast.makeText(
            requireContext(),
            R.string.cart_promo_code_not_valid,
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun showPromoValidationMessage(
        reason: CheckoutConsistencyRepository.PromoValidationFailureReason,
    ) {
        when (reason) {
            CheckoutConsistencyRepository.PromoValidationFailureReason.NOT_ELIGIBLE -> {
                CartStore.clearPromo()
                binding.tvPromoHint.text = null
                renderSummary(CartStore.items)
                Toast.makeText(
                    requireContext(),
                    R.string.promo_not_eligible_for_this_cart,
                    Toast.LENGTH_SHORT,
                ).show()
            }

            CheckoutConsistencyRepository.PromoValidationFailureReason.UNAVAILABLE -> {
                clearInvalidPromo()
            }
        }
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
}
