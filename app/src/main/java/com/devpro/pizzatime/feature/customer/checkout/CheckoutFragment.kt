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
import com.devpro.pizzatime.feature.staff.navigation.openOrderSuccess
import java.util.Locale

class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val orderItems = FakeCheckoutData.orderItems

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        val deliveryFee = FakeCheckoutData.deliveryFee
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
            openOrderSuccess(orderId = DEFAULT_SUCCESS_ORDER_ID)
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
    companion object {
        private const val DEFAULT_SUCCESS_ORDER_ID = "PT-9823"
    }
}