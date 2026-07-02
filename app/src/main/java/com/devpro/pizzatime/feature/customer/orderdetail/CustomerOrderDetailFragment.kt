package com.devpro.pizzatime.feature.customer.orderdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerOrderDetailBinding
import com.devpro.pizzatime.databinding.ItemCustomerOrderDetailLineBinding
import com.devpro.pizzatime.feature.customer.orderhistory.CustomerOrderFirestoreRepository
import java.util.Locale

class CustomerOrderDetailFragment : Fragment() {

    private var _binding: FragmentCustomerOrderDetailBinding? = null
    private val binding: FragmentCustomerOrderDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerOrderDetailBinding is only valid between onCreateView and onDestroyView."
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupActions()
        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()
        loadOrder(orderId)
    }

    private fun loadOrder(orderId: String) {
        if (isFirestoreOrderId(orderId)) {
            CustomerOrderFirestoreRepository.loadOrderDetail(orderId) { result ->
                if (!isAdded) return@loadOrderDetail
                val detail = result.getOrElse {
                    FakeCustomerOrderDetailData.getOrderDetail(orderId.ifBlank { DEFAULT_ORDER_ID })
                }
                bindOrderDetail(detail)
            }
        } else {
            bindOrderDetail(
                FakeCustomerOrderDetailData.getOrderDetail(orderId.ifBlank { DEFAULT_ORDER_ID })
            )
        }
    }

    private fun isFirestoreOrderId(orderId: String): Boolean =
        orderId.isNotBlank() && !orderId.startsWith("#") && orderId.length > 8

    private fun bindOrderDetail(detail: CustomerOrderDetailUiModel) = with(binding) {
        tvStatus.text = detail.statusLabel
        tvOrderId.text = getString(R.string.customer_order_detail_order_id, detail.orderId)
        tvOrderTime.text = detail.orderTime
        ivHeroImage.setImageResource(detail.heroImageRes)
        tvHeroMessage.text = detail.heroMessage

        bindItems(detail.items)
        bindBill(detail.bill)
        bindAddress(detail)
    }

    private fun bindItems(items: List<CustomerOrderItemUiModel>) = with(binding.orderItemsContainer) {
        removeAllViews()

        items.forEach { item ->
            val itemBinding = ItemCustomerOrderDetailLineBinding.inflate(layoutInflater, this, false)

            itemBinding.tvItemName.text = getString(
                R.string.customer_order_detail_item_name,
                item.quantity,
                item.name,
            )
            itemBinding.tvItemDescription.text = item.description
            itemBinding.tvItemPrice.text = formatPrice(item.price)

            if (item.imageRes != null) {
                itemBinding.ivItemImage.setImageResource(item.imageRes)
                itemBinding.tvItemPlaceholder.isVisible = false
            } else {
                itemBinding.ivItemImage.setImageResource(0)
                itemBinding.tvItemPlaceholder.isVisible = true
            }

            addView(itemBinding.root)
        }
    }

    private fun bindBill(bill: CustomerBillUiModel) = with(binding) {
        tvSubtotalValue.text = formatPrice(bill.subtotal)
        tvDeliveryFeeValue.text = formatPrice(bill.deliveryFee)
        tvTaxesValue.text = formatPrice(bill.taxes)
        tvDiscountLabel.text = bill.discountLabel
        tvDiscountValue.text = formatSignedPrice(bill.discount)
        tvTotalAmount.text = formatPrice(bill.total)
    }

    private fun bindAddress(detail: CustomerOrderDetailUiModel) = with(binding) {
        tvDeliveredTo.text = detail.deliveryAddressTitle
        tvAddressLine1.text = detail.deliveryAddressLine1
        tvAddressLine2.text = detail.deliveryAddressLine2
    }

    private fun setupActions() = with(binding) {
        btnReorder.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.customer_order_detail_reorder_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnSupport.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.customer_order_detail_support_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun formatPrice(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    private fun formatSignedPrice(value: Double): String {
        return if (value < 0) {
            String.format(Locale.US, "-$%.2f", kotlin.math.abs(value))
        } else {
            String.format(Locale.US, "$%.2f", value)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"
        private const val DEFAULT_ORDER_ID = "PT-9821"

        fun newInstance(orderId: String): CustomerOrderDetailFragment {
            return CustomerOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}