package com.devpro.pizzatime.feature.kitchen.board

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav
import com.devpro.pizzatime.databinding.FragmentKitchenBoardBinding
import com.devpro.pizzatime.feature.staff.navigation.backToPreviousStaffScreen
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard

class KitchenBoardFragment : Fragment() {

    private var _binding: FragmentKitchenBoardBinding? = null
    private val binding get() = requireNotNull(_binding) {
        "Binding is only valid between onCreateView and onDestroyView"
    }

    private val adapter = KitchenOrderAdapter(
        onPrimaryActionClick = { order ->
            handleOrderAction(order)
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentKitchenBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupOrders()
        setupBottomNav()
    }

    private fun setupOrders() = with(binding.rvKitchenOrders) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@KitchenBoardFragment.adapter
        this@KitchenBoardFragment.adapter.submitList(FakeKitchenBoardData.getOrders())
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            currentTab = StaffBottomNavTab.KITCHEN,
            onDashboardClick = {
                backToPreviousStaffScreen()
            },
            onDeliveryClick = {
                openShipperDeliveryDashboard()
            },
            onProfileClick = {
                showComingSoon(R.string.staff_nav_profile)
            },
        )
    }

    private fun handleOrderAction(order: KitchenOrderUiModel) {
        val messageRes = when (order.status) {
            KitchenOrderStatus.WAITING -> R.string.kitchen_message_baking_started
            KitchenOrderStatus.PREPARING -> R.string.kitchen_message_progress_updated
            KitchenOrderStatus.READY -> R.string.kitchen_message_order_handed_over
            KitchenOrderStatus.NEW -> R.string.kitchen_message_order_accepted
        }

        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun showComingSoon(titleRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(R.string.coming_soon_format, getString(titleRes)),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}