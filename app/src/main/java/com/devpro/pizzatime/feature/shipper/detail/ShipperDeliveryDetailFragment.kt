package com.devpro.pizzatime.feature.shipper.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentShipperDeliveryDetailBinding
import com.devpro.pizzatime.databinding.ItemShipperPaymentRowBinding
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.backToPreviousStaffScreen
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openStaffDashboard
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav

class ShipperDeliveryDetailFragment : Fragment(R.layout.fragment_shipper_delivery_detail) {

    private var _binding: FragmentShipperDeliveryDetailBinding? = null
    private val binding: FragmentShipperDeliveryDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentShipperDeliveryDetailBinding is only valid between onViewCreated and onDestroyView."
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentShipperDeliveryDetailBinding.bind(view)

        val detail = FakeShipperDeliveryDetailData.getDetail(
            arguments?.getString(ARG_ORDER_ID),
        )

        bindDetail(detail)
        setupActions(detail)
        setupBottomNav()
    }

    private fun bindDetail(detail: ShipperDeliveryDetailUiModel) = with(binding) {
        tvOrderTitle.text = getString(R.string.shipper_detail_order_title, detail.orderId.removePrefix("#"))
        tvCustomerName.text = detail.customerName
        tvDeliveryAddress.text = detail.address
        tvCourierNote.text = getString(R.string.shipper_detail_note_quote, detail.courierNote)
        tvPaymentAmount.text = detail.paymentAmount
        tvPaymentMethod.text = detail.paymentMethod

        bindPaymentItems(detail.items)
    }

    private fun bindPaymentItems(items: List<ShipperPaymentItemUiModel>) = with(binding.llPaymentItems) {
        removeAllViews()

        items.forEach { item ->
            val itemBinding = ItemShipperPaymentRowBinding.inflate(
                layoutInflater,
                this,
                false,
            )

            itemBinding.tvPaymentItemName.text = item.name
            itemBinding.tvPaymentItemPrice.text = item.price

            addView(itemBinding.root)
        }
    }

    private fun setupActions(detail: ShipperDeliveryDetailUiModel) = with(binding) {
        btnBack.setOnClickListener {
            backToPreviousStaffScreen()
        }

        btnCallCustomer.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.shipper_detail_calling_customer, detail.customerName),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnNavigate.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.shipper_message_navigation_started,
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnConfirmDelivery.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.shipper_detail_delivery_confirmed, detail.orderId),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            currentTab = StaffBottomNavTab.DELIVERY,
            onDashboardClick = {
                openStaffDashboard()
            },
            onKitchenClick = {
                openKitchenBoard()
            },
            onProfileClick = {
                showComingSoon(R.string.staff_nav_profile)
            },
        )
    }

    private fun showComingSoon(titleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_coming_soon_message, getString(titleRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ORDER_ID = "orderId"

        fun newInstance(orderId: String): ShipperDeliveryDetailFragment {
            return ShipperDeliveryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}