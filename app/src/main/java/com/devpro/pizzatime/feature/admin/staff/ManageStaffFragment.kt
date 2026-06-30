package com.devpro.pizzatime.feature.admin.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentManageStaffBinding
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav

class ManageStaffFragment : Fragment() {

    private var _binding: FragmentManageStaffBinding? = null
    private val binding get() = _binding!!

    private val staffAdapter by lazy {
        AdminStaffAdapter(
            onEditClick = { showComingSoon(getString(R.string.edit_staff_format, it.name)) },
            onToggleStatusClick = { showComingSoon(getString(R.string.change_staff_status_format, it.name)) },
        )
    }

    private var selectedFilter = StaffFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManageStaffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupActions()
        setupFilters()
        setupBottomNav()
        renderStaff()
    }

    private fun setupRecyclerView() = with(binding.rvStaff) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = staffAdapter
        itemAnimator = null
    }

    private fun setupActions() = with(binding) {
        btnAddStaff.setOnClickListener {
            showComingSoon(getString(R.string.add_staff))
        }

        tvMenu.setOnClickListener {
            showComingSoon(getString(R.string.menu))
        }
    }

    private fun setupFilters() = with(binding) {
        tvChipAllStaff.setOnClickListener {
            selectedFilter = StaffFilter.ALL
            renderStaff()
        }

        tvChipKitchen.setOnClickListener {
            selectedFilter = StaffFilter.KITCHEN
            renderStaff()
        }

        tvChipShipper.setOnClickListener {
            selectedFilter = StaffFilter.SHIPPER
            renderStaff()
        }

        tvChipAdmin.setOnClickListener {
            selectedFilter = StaffFilter.ADMIN
            renderStaff()
        }
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            StaffBottomNavTab.PROFILE,
            { openAdminDashboard() },
            { openKitchenBoard() },
            { openShipperDeliveryDashboard() },
            {},
        )
    }

    private fun renderStaff() {
        val staff = FakeAdminStaffData.staff.filter { item ->
            when (selectedFilter) {
                StaffFilter.ALL -> true
                StaffFilter.KITCHEN -> item.role == AdminStaffRole.KITCHEN
                StaffFilter.SHIPPER -> item.role == AdminStaffRole.SHIPPER
                StaffFilter.ADMIN -> item.role == AdminStaffRole.ADMIN
            }
        }

        staffAdapter.submitList(staff)
        binding.rvStaff.isVisible = staff.isNotEmpty()
        binding.tvEmptyStaff.isVisible = staff.isEmpty()
        renderFilterState()
    }

    private fun renderFilterState() = with(binding) {
        tvChipAllStaff.bindChip(selectedFilter == StaffFilter.ALL)
        tvChipKitchen.bindChip(selectedFilter == StaffFilter.KITCHEN)
        tvChipShipper.bindChip(selectedFilter == StaffFilter.SHIPPER)
        tvChipAdmin.bindChip(selectedFilter == StaffFilter.ADMIN)
    }

    private fun TextView.bindChip(isSelected: Boolean) {
        setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_admin_staff_chip_selected
            } else {
                R.drawable.bg_admin_staff_chip_unselected
            },
        )

        setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) {
                    R.color.pt_gold_light
                } else {
                    R.color.pt_text_secondary
                },
            ),
        )
    }

    private fun showComingSoon(action: String) {
        Toast.makeText(
            requireContext(),
            getString(R.string.staff_action_coming_soon, action),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class StaffFilter {
        ALL,
        KITCHEN,
        SHIPPER,
        ADMIN,
    }
}