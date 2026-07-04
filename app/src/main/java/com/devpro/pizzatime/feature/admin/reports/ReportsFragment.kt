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
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = requireNotNull(_binding)

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
        setupReportFallback()
        setupActions()
        setupBottomNav()
        loadReports()
    }

    private fun setupReportFallback() = with(binding.rvBestSellers) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = bestSellerAdapter
        itemAnimator = null
        bestSellerAdapter.submitList(FakeAdminReportsData.bestSellers)
    }

    private fun loadReports() {
        AdminReportsFirestoreRepository.loadReports { result ->
            val currentBinding = _binding ?: return@loadReports
            result.onSuccess { report ->
                renderReport(currentBinding, report)
            }
        }
    }

    private fun renderReport(
        currentBinding: FragmentReportsBinding,
        report: AdminReportUiModel,
    ) = with(currentBinding) {
        tvReportRevenueValue.text = report.totalRevenue
        tvReportRevenueMeta.text = report.totalOrdersText
        tvPendingLabel.text = "IN PROGRESS"
        tvPendingValue.text = report.pendingOrdersText
        tvTotalOrders.text = report.totalOrdersText
        tvCompletedOrders.text = report.deliveredOrdersText
        tvCancelledOrders.text = report.cancelledOrdersText
        progressPendingOrders.progress = report.pendingProgress
        donutOrderHealth.setProgressPercent(report.orderHealthPercent)
        chartRevenueTrend.setValues(report.revenueTrendValues)
        bestSellerAdapter.submitList(report.bestSellers)
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
        bindAdminBottomNav(
            root = binding.staffBottomNav.root,
            selectedDestination = AdminBottomNavDestination.DASHBOARD,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
