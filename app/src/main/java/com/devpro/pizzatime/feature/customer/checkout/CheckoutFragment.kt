package com.devpro.pizzatime.feature.customer.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.databinding.FragmentCheckoutBinding
import com.devpro.pizzatime.databinding.ItemCheckoutOrderBinding
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.openOrderSuccess
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private var orderItems = emptyList<CheckoutOrderItemUiModel>()

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
        renderOrderItems(orderItems)
        renderSummary()
        setupActions()
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
        val total = subtotal + deliveryFee

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
        if (orderItems.isEmpty()) {
            Toast.makeText(requireContext(), "Your cart is empty.", Toast.LENGTH_SHORT).show()
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Please log in to place an order.", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnPlaceOrder.isEnabled = false
        FirebaseOrderRepository.createOrder(
            customerId = user.uid,
            customerEmail = user.email ?: "",
            items = CartStore.items,
            deliveryFee = DELIVERY_FEE,
            onResult = { result ->
                if (_binding == null) return@createOrder
                binding.btnPlaceOrder.isEnabled = true
                result
                    .onSuccess { orderId ->
                        CartStore.clear()
                        openOrderSuccess(orderId = orderId, addToBackStack = false)
                    }
                    .onFailure { error ->
                        Toast.makeText(
                            requireContext(),
                            error.message ?: "Failed to place order.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
            },
        )
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

private fun CartItemUiModel.toCheckoutItem() = CheckoutOrderItemUiModel(
    id = id,
    name = name,
    optionText = "x$quantity",
    price = price * quantity,
    imageRes = imageRes,
)

