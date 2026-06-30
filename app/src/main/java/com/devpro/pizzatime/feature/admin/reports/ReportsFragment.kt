package com.devpro.pizzatime.feature.admin.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentReportsBinding
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val bestSellerAdapter = BestSellerAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupBestSellers()
        setupActions()
        setupBottomNav()
    }

    private fun setupBestSellers() = with(binding.rvBestSellers) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = bestSellerAdapter
        itemAnimator = null
        bestSellerAdapter.submitList(FakeAdminReportsData.bestSellers)
    }

    private fun setupActions() {
        binding.btnMenu.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_action_coming_soon, getString(R.string.menu)),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            StaffBottomNavTab.DASHBOARD,
            {},
            { openKitchenBoard() },
            { openShipperDeliveryDashboard() },
            {},
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}