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
import com.devpro.pizzatime.feature.customer.checkout.CheckoutFragment
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
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

        binding.cartItemContainer.removeAllViews()

        val hasItems = cartItems.isNotEmpty()
        val selectedItemCount = cartItems.sumOf { it.quantity }

        binding.cartItemContainer.isVisible = hasItems
        binding.promoCodeBar.isVisible = hasItems
        binding.orderSummaryCard.isVisible = hasItems
        binding.cartBottomBar.isVisible = hasItems
        binding.emptyCartView.isVisible = !hasItems

        binding.tvCartBadge.text = selectedItemCount.toString()

        binding.tvCartSubtitle.text = if (hasItems) {
            getString(R.string.cart_items_selected_format, selectedItemCount)
        } else {
            getString(R.string.cart_empty_message)
        }

        if (!hasItems) {
            return
        }

        cartItems.forEachIndexed { index, item ->
            val itemBinding = ItemCartPizzaBinding.inflate(
                layoutInflater,
                binding.cartItemContainer,
                false
            )

            bindCartItem(itemBinding, item)

            itemBinding.btnMinus.setOnClickListener {
                CartStore.decreaseQuantity(item.cartKey)
                renderCart()
            }

            itemBinding.btnPlus.setOnClickListener {
                CartStore.increaseQuantity(item.cartKey)
                renderCart()
            }

            itemBinding.btnRemove.setOnClickListener {
                CartStore.removeItem(item.cartKey)
                renderCart()
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                142.dp
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
        val customizationText = item.customizationText()
        itemBinding.tvPizzaPrice.text = if (customizationText.isBlank()) {
            formatMoney(item.price)
        } else {
            "${formatMoney(item.price)}\n$customizationText"
        }
        itemBinding.tvQuantity.text = item.quantity.toString()
    }

    private fun CartItemUiModel.customizationText(): String {
        val parts = buildList {
            if (selectedSize.isNotBlank()) add("Size: $selectedSize")
            if (selectedCrust.isNotBlank()) add("Crust: $selectedCrust")
            if (selectedToppings.isNotEmpty()) add("Toppings: ${selectedToppings.joinToString()}")
        }
        return parts.joinToString(" • ")
    }

    private fun renderSummary(cartItems: List<CartItemUiModel>) {
        val subtotal = cartItems.sumOf { item ->
            item.price * item.quantity
        }

        val deliveryFee = FakeCartData.deliveryFee
        val discount = if (subtotal > 0) {
            FakeCartData.discount
        } else {
            0.0
        }

        val total = subtotal + deliveryFee - discount

        binding.tvSubtotal.text = formatMoney(subtotal)
        binding.tvDeliveryFee.text = formatMoney(deliveryFee)
        binding.tvDiscount.text = "-${formatMoney(discount)}"
        binding.tvTotal.text = formatMoney(total)
    }

    private fun setupActions() {
        binding.btnMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnApplyPromo.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Promo applied demo",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnProceedCheckout.setOnClickListener {
            if (FakeSessionStore.isLoggedIn) openCheckoutScreen()
            else openLoginRequiredScreen()
        }
    }

    private fun openCheckoutScreen() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, CheckoutFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun formatMoney(value: Double): String {
        return String.format(Locale.US, "\$%.2f", value)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
