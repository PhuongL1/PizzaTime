package com.devpro.pizzatime.feature.admin.menu

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentManageMenuBinding
import com.devpro.pizzatime.feature.staff.navigation.StaffBottomNavTab
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import com.devpro.pizzatime.feature.staff.navigation.setupStaffBottomNav

class ManageMenuFragment : Fragment(R.layout.fragment_manage_menu) {

    private var _binding: FragmentManageMenuBinding? = null
    private val binding: FragmentManageMenuBinding
        get() = checkNotNull(_binding) {
            "FragmentManageMenuBinding is only valid between onViewCreated and onDestroyView."
        }

    private var selectedCategory = AdminMenuCategory.SIGNATURE
    private var searchQuery = ""

    private val menuAdapter = AdminMenuAdapter(
        onAvailabilityClick = { item ->
            FakeAdminMenuData.toggleAvailability(item.id)
            renderMenuItems()
        },
        onEditClick = { item ->
            Toast.makeText(
                requireContext(),
                getString(R.string.manage_menu_edit_message, item.name),
                Toast.LENGTH_SHORT,
            ).show()
        },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentManageMenuBinding.bind(view)

        setupSearch()
        setupCategoryChips()
        setupMenuList()
        setupBottomNav()
        renderMenuItems()
    }

    private fun setupSearch() {
        binding.edtSearchMenu.addTextChangedListener { editable ->
            searchQuery = editable?.toString().orEmpty()
            renderMenuItems()
        }
    }

    private fun setupCategoryChips() = with(binding) {
        chipSignature.setOnClickListener {
            selectCategory(AdminMenuCategory.SIGNATURE)
        }

        chipClassic.setOnClickListener {
            selectCategory(AdminMenuCategory.CLASSIC)
        }

        chipVeggie.setOnClickListener {
            selectCategory(AdminMenuCategory.VEGGIE)
        }
    }

    private fun selectCategory(category: AdminMenuCategory) {
        selectedCategory = category
        renderMenuItems()
    }

    private fun setupMenuList() = with(binding.rvMenuItems) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = menuAdapter
        isNestedScrollingEnabled = false
    }

    private fun renderMenuItems() {
        val normalizedQuery = searchQuery.trim().lowercase()

        val filteredItems = FakeAdminMenuData.getItems()
            .filter { item -> item.category == selectedCategory }
            .filter { item ->
                normalizedQuery.isBlank() ||
                        item.name.lowercase().contains(normalizedQuery) ||
                        item.description.lowercase().contains(normalizedQuery)
            }

        menuAdapter.submitList(filteredItems)
        updateCategoryChipState()
    }

    private fun updateCategoryChipState() = with(binding) {
        setCategoryChipSelected(chipSignature, selectedCategory == AdminMenuCategory.SIGNATURE)
        setCategoryChipSelected(chipClassic, selectedCategory == AdminMenuCategory.CLASSIC)
        setCategoryChipSelected(chipVeggie, selectedCategory == AdminMenuCategory.VEGGIE)
    }

    private fun setCategoryChipSelected(chip: TextView, isSelected: Boolean) {
        chip.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected_gold else R.drawable.bg_chip_unselected_dark,
        )

        chip.setTextColor(
            if (isSelected) COLOR_CHIP_SELECTED else COLOR_CHIP_UNSELECTED,
        )
    }

    private fun setupBottomNav() {
        binding.staffBottomNav.setupStaffBottomNav(
            currentTab = StaffBottomNavTab.KITCHEN,
            onDashboardClick = {
                openAdminDashboard()
            },
            onDeliveryClick = {
                openShipperDeliveryDashboard()
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
        private val COLOR_CHIP_SELECTED = "#3A210D".toColorInt()
        private val COLOR_CHIP_UNSELECTED = "#D8C8BC".toColorInt()
    }
}