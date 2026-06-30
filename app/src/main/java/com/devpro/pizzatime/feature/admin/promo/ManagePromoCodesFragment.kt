package com.devpro.pizzatime.feature.admin.promo

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
import com.devpro.pizzatime.databinding.FragmentManagePromoCodesBinding
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openKitchenBoard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav

class ManagePromoCodesFragment : Fragment() {

    private var _binding: FragmentManagePromoCodesBinding? = null
    private val binding get() = _binding!!

    private val promoAdapter by lazy {
        AdminPromoAdapter(
            onEditClick = { showActionToast(getString(R.string.edit), it) },
            onDeleteClick = { showActionToast(getString(R.string.delete), it) },
            onShareClick = { showActionToast(getString(R.string.share), it) },
            onReactivateClick = { showActionToast(getString(R.string.reactivate), it) },
        )
    }

    private var selectedFilter = PromoFilter.ACTIVE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagePromoCodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupActions()
        setupFilters()
        setupBottomNav()
        renderPromos()
    }

    private fun setupRecyclerView() = with(binding.rvPromos) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = promoAdapter
        itemAnimator = null
    }

    private fun setupActions() = with(binding) {
        btnAddPromo.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.promo_action_coming_soon, getString(R.string.add_promo)),
                Toast.LENGTH_SHORT,
            ).show()
        }

        btnMenu.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.promo_action_coming_soon, getString(R.string.menu)),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupFilters() = with(binding) {
        tvChipActive.setOnClickListener {
            selectedFilter = PromoFilter.ACTIVE
            renderPromos()
        }

        tvChipInactive.setOnClickListener {
            selectedFilter = PromoFilter.INACTIVE
            renderPromos()
        }

        tvChipScheduled.setOnClickListener {
            selectedFilter = PromoFilter.SCHEDULED
            renderPromos()
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

    private fun renderPromos() {
        val promos = when (selectedFilter) {
            PromoFilter.ACTIVE -> FakeAdminPromoData.promos.filter {
                it.status == AdminPromoStatus.ACTIVE
            }

            PromoFilter.INACTIVE -> FakeAdminPromoData.promos.filter {
                it.status == AdminPromoStatus.INACTIVE || it.status == AdminPromoStatus.EXPIRED
            }

            PromoFilter.SCHEDULED -> FakeAdminPromoData.promos.filter {
                it.status == AdminPromoStatus.SCHEDULED
            }
        }

        promoAdapter.submitList(promos)
        binding.rvPromos.isVisible = promos.isNotEmpty()
        binding.tvEmptyPromos.isVisible = promos.isEmpty()
        renderFilterState()
    }

    private fun renderFilterState() = with(binding) {
        tvChipActive.bindChip(selectedFilter == PromoFilter.ACTIVE)
        tvChipInactive.bindChip(selectedFilter == PromoFilter.INACTIVE)
        tvChipScheduled.bindChip(selectedFilter == PromoFilter.SCHEDULED)
    }

    private fun TextView.bindChip(isSelected: Boolean) {
        setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_promo_chip_selected
            } else {
                R.drawable.bg_promo_chip_unselected
            },
        )

        setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) {
                    R.color.pt_text_dark
                } else {
                    R.color.pt_text_secondary
                },
            ),
        )
    }

    private fun showActionToast(action: String, promo: AdminPromoUiModel) {
        Toast.makeText(
            requireContext(),
            getString(R.string.promo_action_coming_soon, "$action ${promo.code}"),
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class PromoFilter {
        ACTIVE,
        INACTIVE,
        SCHEDULED,
    }
}