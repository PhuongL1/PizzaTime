package com.devpro.pizzatime.feature.staff.detail

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentStaffOrderDetailBinding
import com.devpro.pizzatime.feature.staff.dashboard.StaffOrderStatus

class StaffOrderDetailFragment : Fragment(R.layout.fragment_staff_order_detail) {

    private var _binding: FragmentStaffOrderDetailBinding? = null
    private val binding: FragmentStaffOrderDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentStaffOrderDetailBinding is only valid between onViewCreated and onDestroyView."
        }

    private lateinit var currentOrder: StaffOrderDetailUiModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStaffOrderDetailBinding.bind(view)

        val orderId = arguments?.getString(ARG_ORDER_ID).orEmpty()
        currentOrder = FakeStaffOrderDetailData.getByOrderId(orderId)

        bindOrderDetail(currentOrder)
        setupBottomNav()
        setupActions()
    }

    private fun bindOrderDetail(order: StaffOrderDetailUiModel) = with(binding) {
        tvOrderId.text = order.orderId
        tvReceivedAgo.text = getString(R.string.staff_order_detail_received, order.receivedAgo)
        tvStatus.text = mapStatusText(order.status)
        tvItemName.text = order.itemName
        tvSize.text = order.size
        tvCrust.text = order.crust
        imgPizza.setImageResource(order.imageRes)

        buildToppingsList(order.toppings)

        if (order.allergyTitle != null) {
            cardAllergy.isVisible = true
            tvAllergyTitle.text = order.allergyTitle
            tvAllergyMessage.text = order.allergyMessage.orEmpty()
        }

        if (order.customerRequest != null) {
            cardRequest.isVisible = true
            tvCustomerRequest.text = buildString {
                append('"')
                append(order.customerRequest)
                append('"')
            }
            buildTagsList(order.tags)
        }
    }

    private fun buildToppingsList(toppings: List<String>) {
        val container = binding.containerToppings
        container.removeAllViews()

        toppings.forEach { topping ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                params.bottomMargin = dpToPx(6)
                layoutParams = params
            }

            val dot = TextView(requireContext()).apply {
                text = "●"
                textSize = 8f
                setTextColor("#CF843F".toColorInt())
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                params.marginEnd = dpToPx(10)
                layoutParams = params
            }

            val label = TextView(requireContext()).apply {
                text = topping
                textSize = 14f
                setTextColor("#3A210D".toColorInt())
            }

            row.addView(dot)
            row.addView(label)
            container.addView(row)
        }
    }

    private fun buildTagsList(tags: List<String>) {
        val container = binding.containerTags
        container.removeAllViews()

        tags.forEachIndexed { index, tag ->
            val chip = TextView(requireContext()).apply {
                text = tag
                textSize = 11f
                textStyle(true)
                setTextColor("#3A210D".toColorInt())
                val backgroundRes = if (index == 0) {
                    R.drawable.bg_chip_selected_gold
                } else {
                    R.drawable.bg_chip_unselected_dark
                }
                setBackgroundResource(backgroundRes)
                if (index != 0) setTextColor("#D8C8BC".toColorInt())
                setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                if (index > 0) params.marginStart = dpToPx(8)
                layoutParams = params
            }
            container.addView(chip)
        }
    }

    private fun setupBottomNav() = with(binding.staffBottomNav) {
        navDashboard.setBackgroundResource(0)
        navDashboard.setTextColor("#D8C8BC".toColorInt())

        navKitchen.setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
        navKitchen.setTextColor("#3A210D".toColorInt())

        navDelivery.setBackgroundResource(0)
        navDelivery.setTextColor("#D8C8BC".toColorInt())

        navProfile.setBackgroundResource(0)
        navProfile.setTextColor("#D8C8BC".toColorInt())

        navDashboard.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        navKitchen.setOnClickListener {
            // Current screen, no-op.
        }

        navDelivery.setOnClickListener {
            showComingSoon(R.string.staff_nav_delivery)
        }

        navProfile.setOnClickListener {
            showComingSoon(R.string.staff_nav_profile)
        }
    }

    private fun setupActions() = with(binding) {
        btnStartPreparing.setOnClickListener {
            updateStatus(StaffOrderStatus.PREPARING)
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_order_detail_preparation_started_message),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnStartBaking.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_order_detail_baking_started_message),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnMarkReady.setOnClickListener {
            updateStatus(StaffOrderStatus.READY)
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_order_detail_ready_message),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun updateStatus(status: StaffOrderStatus) {
        FakeStaffOrderDetailData.updateStatus(currentOrder.orderId, status)
        currentOrder = currentOrder.copy(status = status)
        binding.tvStatus.text = mapStatusText(status)
    }

    private fun mapStatusText(status: StaffOrderStatus): String {
        return when (status) {
            StaffOrderStatus.PENDING -> getString(R.string.staff_order_detail_preparation_pending)
            StaffOrderStatus.CONFIRMED -> getString(R.string.staff_order_detail_order_confirmed)
            StaffOrderStatus.PREPARING -> getString(R.string.staff_order_detail_preparing)
            StaffOrderStatus.READY -> getString(R.string.staff_order_detail_ready)
        }
    }

    private fun showComingSoon(titleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_coming_soon_message, getString(titleRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun TextView.textStyle(bold: Boolean) {
        typeface = if (bold) {
            android.graphics.Typeface.DEFAULT_BOLD
        } else {
            android.graphics.Typeface.DEFAULT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ORDER_ID = "orderId"

        fun newInstance(orderId: String): StaffOrderDetailFragment {
            return StaffOrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}



