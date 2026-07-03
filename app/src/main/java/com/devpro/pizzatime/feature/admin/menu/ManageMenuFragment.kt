package com.devpro.pizzatime.feature.admin.menu

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
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
import java.util.Locale

class ManageMenuFragment : Fragment(R.layout.fragment_manage_menu) {

    private var _binding: FragmentManageMenuBinding? = null
    private val binding: FragmentManageMenuBinding
        get() = checkNotNull(_binding) {
            "FragmentManageMenuBinding is only valid between onViewCreated and onDestroyView."
        }

    private var selectedCategory = AdminMenuCategory.SIGNATURE
    private var searchQuery = ""
    private var allProducts: List<AdminMenuUiModel> = FakeAdminMenuData.getItems()

    private val menuAdapter = AdminMenuAdapter(
        onAvailabilityClick = { item ->
            AdminMenuFirestoreRepository.toggleAvailability(item.id, !item.isAvailable) { result ->
                if (!isAdded) return@toggleAvailability
                if (result.isSuccess) {
                    loadFirestoreProducts()
                }
            }
        },
        onEditClick = { item ->
            showEditProductDialog(item)
        },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentManageMenuBinding.bind(view)

        setupSearch()
        setupCategoryChips()
        setupMenuList()
        setupBottomNav()
        setupActions()
        renderMenuItems()
        loadFirestoreProducts()
    }

    private fun loadFirestoreProducts() {
        AdminMenuFirestoreRepository.loadProducts { result ->
            if (!isAdded) return@loadProducts
            allProducts = result.getOrElse { FakeAdminMenuData.getItems() }
            renderMenuItems()
        }
    }

    private fun showEditProductDialog(item: AdminMenuUiModel) {
        val nameInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_name_hint),
            text = item.name,
        )
        val descriptionInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_description_hint),
            text = item.description,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        )
        val priceInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_price_hint),
            text = String.format(Locale.US, "%.2f", item.basePrice),
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        )
        val categoryInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_category_hint),
            text = item.categoryId.ifBlank { item.category.name },
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
        )
        val availableInput = CheckBox(requireContext()).apply {
            text = getString(R.string.manage_menu_edit_available)
            isChecked = item.isAvailable
        }

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.manage_menu_dialog_padding)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(nameInput)
            addView(descriptionInput)
            addView(priceInput)
            addView(categoryInput)
            addView(availableInput)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.manage_menu_edit_product_title, item.name))
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.manage_menu_edit_product_save, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        saveProductEdit(
                            productId = item.id,
                            name = nameInput.text.toString().trim(),
                            description = descriptionInput.text.toString().trim(),
                            basePriceText = priceInput.text.toString().trim(),
                            categoryId = categoryInput.text.toString().trim().uppercase(Locale.US),
                            available = availableInput.isChecked,
                            onSaved = { dismiss() },
                        )
                    }
                }
            }
            .show()
    }

    private fun showCreateProductDialog() {
        val idInput = createDialogInput(
            hint = getString(R.string.manage_menu_create_id_hint),
            text = "",
            inputType = InputType.TYPE_CLASS_TEXT,
        )
        val nameInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_name_hint),
            text = "",
        )
        val descriptionInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_description_hint),
            text = "",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        )
        val priceInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_price_hint),
            text = "",
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
        )
        val categoryInput = createDialogInput(
            hint = getString(R.string.manage_menu_edit_category_hint),
            text = selectedCategory.name,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
        )

        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.manage_menu_dialog_padding)
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(idInput)
            addView(nameInput)
            addView(descriptionInput)
            addView(priceInput)
            addView(categoryInput)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.manage_menu_create_product_title)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.manage_menu_create_product_save, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        saveProductCreate(
                            rawProductId = idInput.text.toString(),
                            name = nameInput.text.toString().trim(),
                            description = descriptionInput.text.toString().trim(),
                            basePriceText = priceInput.text.toString().trim(),
                            categoryId = categoryInput.text.toString().trim().uppercase(Locale.US),
                            onSaved = { dismiss() },
                        )
                    }
                }
            }
            .show()
    }

    private fun createDialogInput(
        hint: String,
        text: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS,
    ): EditText {
        return EditText(requireContext()).apply {
            this.hint = hint
            this.inputType = inputType
            setText(text)
            setSelectAllOnFocus(false)
        }
    }

    private fun saveProductEdit(
        productId: String,
        name: String,
        description: String,
        basePriceText: String,
        categoryId: String,
        available: Boolean,
        onSaved: () -> Unit,
    ) {
        val basePrice = basePriceText.toDoubleOrNull()
        when {
            name.isBlank() -> {
                showToast(R.string.manage_menu_edit_name_required)
                return
            }

            basePrice == null || basePrice <= 0.0 -> {
                showToast(R.string.manage_menu_edit_price_required)
                return
            }

            categoryId.isBlank() -> {
                showToast(R.string.manage_menu_edit_category_required)
                return
            }
        }

        AdminMenuFirestoreRepository.updateProduct(
            productId = productId,
            name = name,
            description = description,
            basePrice = basePrice,
            categoryId = categoryId,
            available = available,
        ) { result ->
            if (!isAdded) return@updateProduct
            result
                .onSuccess {
                    showToast(R.string.manage_menu_edit_product_saved)
                    onSaved()
                    loadFirestoreProducts()
                }
                .onFailure {
                    showToast(R.string.manage_menu_edit_product_failed)
                }
        }
    }

    private fun saveProductCreate(
        rawProductId: String,
        name: String,
        description: String,
        basePriceText: String,
        categoryId: String,
        onSaved: () -> Unit,
    ) {
        val productId = normalizeProductId(rawProductId)
        val basePrice = basePriceText.toDoubleOrNull()
        when {
            productId.isBlank() -> {
                showToast(R.string.manage_menu_create_id_required)
                return
            }

            name.isBlank() -> {
                showToast(R.string.manage_menu_edit_name_required)
                return
            }

            basePrice == null || basePrice <= 0.0 -> {
                showToast(R.string.manage_menu_edit_price_required)
                return
            }

            categoryId.isBlank() -> {
                showToast(R.string.manage_menu_edit_category_required)
                return
            }
        }

        AdminMenuFirestoreRepository.createProduct(
            productId = productId,
            name = name,
            description = description,
            basePrice = basePrice,
            categoryId = categoryId,
        ) { result ->
            if (!isAdded) return@createProduct
            result
                .onSuccess {
                    showToast(R.string.manage_menu_create_product_saved)
                    onSaved()
                    loadFirestoreProducts()
                }
                .onFailure {
                    showToast(R.string.manage_menu_create_product_failed)
                }
        }
    }

    private fun normalizeProductId(rawProductId: String): String {
        return rawProductId.trim()
            .lowercase(Locale.US)
            .replace(Regex("\\s+"), "-")
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

    private fun setupActions() = with(binding) {
        btnMenu.setOnClickListener {
            showCreateProductDialog()
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

        val filteredItems = allProducts
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

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
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
