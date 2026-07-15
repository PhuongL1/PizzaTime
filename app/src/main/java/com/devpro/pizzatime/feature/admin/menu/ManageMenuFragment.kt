package com.devpro.pizzatime.feature.admin.menu

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentManageMenuBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.admin.navigation.bindAdminTopBar
import com.devpro.pizzatime.feature.staff.navigation.openAddEditProduct
import com.devpro.pizzatime.feature.staff.navigation.openAdminDashboard
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openManageMenu
import com.devpro.pizzatime.feature.staff.navigation.openShipperDeliveryDashboard
import java.util.Locale

class ManageMenuFragment : Fragment(R.layout.fragment_manage_menu) {

    private var _binding: FragmentManageMenuBinding? = null
    private val binding: FragmentManageMenuBinding
        get() = checkNotNull(_binding) {
            "FragmentManageMenuBinding is only valid between onViewCreated and onDestroyView."
        }

    private var selectedCategory: AdminMenuCategory? = null
    private var searchQuery = ""
    private var allProducts: List<AdminMenuUiModel> = emptyList()

    private val menuAdapter = AdminMenuAdapter(
        onAvailabilityClick = { item ->
            toggleProductAvailability(item)
        },
        onEditClick = { item ->
            openAddEditProduct(productId = item.id)
        },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentManageMenuBinding.bind(view)

        setupSearch()
        setupCategoryChips()
        setupMenuList()
        setupTopBar()
        setupBottomNav()
        resetMenuFilters()
        refreshProductsFromFirestore()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            refreshProductsFromFirestore()
        }
    }

    private fun refreshProductsFromFirestore() {
        AdminMenuFirestoreRepository.loadProducts { result ->
            if (_binding == null) return@loadProducts

            result
                .onSuccess { products ->
                    allProducts = products.distinctBy { item -> item.id }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load admin products", error)
                    allProducts = emptyList()
                    showUiMessage(R.string.manage_menu_edit_product_failed, UiMessageType.ERROR)
                }
            renderMenuItems()
        }
    }

    private fun toggleProductAvailability(item: AdminMenuUiModel) {
        val newAvailable = !item.isAvailable
        AdminMenuFirestoreRepository.toggleAvailability(item.id, newAvailable) { result ->
            if (_binding == null) return@toggleAvailability

            result
                .onSuccess {
                    allProducts = allProducts.map { product ->
                        if (product.id == item.id) {
                            product.copy(isAvailable = newAvailable)
                        } else {
                            product
                        }
                    }
                    renderMenuItems()
                    showUiMessage(R.string.manage_menu_availability_updated, UiMessageType.SUCCESS)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to update product availability id=${item.id}", error)
                    showUiMessage(R.string.manage_menu_edit_product_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun setupSearch() {
        binding.edtSearchMenu.addTextChangedListener { editable ->
            searchQuery = editable?.toString().orEmpty()
            Log.d(TAG, "Manage menu search query=${searchQuery.trim()}")
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
        selectedCategory = if (selectedCategory == category) null else category
        renderMenuItems()
    }

    private fun resetMenuFilters() {
        searchQuery = ""
        selectedCategory = null
        if (binding.edtSearchMenu.text.isNotEmpty()) {
            binding.edtSearchMenu.setText("")
        }
        updateCategoryChipState()
    }

    private fun setupMenuList() = with(binding.rvMenuItems) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = menuAdapter
        isNestedScrollingEnabled = false
    }

    private fun renderMenuItems() {
        val normalizedQuery = searchQuery.trim().lowercase(Locale.US)

        val filteredItems = allProducts
            .filter { item ->
                selectedCategory == null || item.category == selectedCategory
            }
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.searchableText().contains(normalizedQuery)
            }

        menuAdapter.submitList(filteredItems)
        binding.rvMenuItems.isVisible = filteredItems.isNotEmpty()
        binding.tvEmptyMenu.isVisible = filteredItems.isEmpty()
        updateCategoryChipState()
    }

    private fun AdminMenuUiModel.searchableText(): String {
        return listOf(
            name,
            description,
            categoryId,
            getString(category.labelRes),
        ).joinToString(separator = " ")
            .lowercase(Locale.US)
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
            chip.context.getColor(
                if (isSelected) R.color.staff_nav_selected else R.color.staff_nav_unselected,
            ),
        )
    }

    private fun setupBottomNav() {
        bindAdminBottomNav(
            root = binding.staffBottomNav.root,
            selectedDestination = AdminBottomNavDestination.MENU,
            onDashboardClick = { openAdminDashboard() },
            onManageMenuClick = { openManageMenu() },
            onManagePromoCodesClick = { openShipperDeliveryDashboard() },
            onManageStaffClick = { openCustomerAccount() },
        )
    }

    private fun setupTopBar() {
        bindAdminTopBar(
            root = binding.staffTopBar.root,
            title = getString(R.string.manage_menu_title),
            onMenuClick = {
                val popped = parentFragmentManager.popBackStackImmediate()
                if (!popped) {
                    openAdminDashboard(addToBackStack = false)
                }
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "ManageMenuFragment"
    }
}
