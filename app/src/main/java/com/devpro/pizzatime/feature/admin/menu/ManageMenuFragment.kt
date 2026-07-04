package com.devpro.pizzatime.feature.admin.menu

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentManageMenuBinding
import com.devpro.pizzatime.feature.admin.navigation.AdminBottomNavDestination
import com.devpro.pizzatime.feature.admin.navigation.bindAdminBottomNav
import com.devpro.pizzatime.feature.admin.navigation.bindAdminTopBar
import com.devpro.pizzatime.feature.staff.navigation.openAddEditProduct
import java.util.Locale

class ManageMenuFragment : Fragment(R.layout.fragment_manage_menu) {

    private var _binding: FragmentManageMenuBinding? = null
    private val binding: FragmentManageMenuBinding
        get() = checkNotNull(_binding) {
            "FragmentManageMenuBinding is only valid between onViewCreated and onDestroyView."
        }

    private var pendingImageUpload: PendingImageUpload? = null

    private val pickProductImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val pendingUpload = pendingImageUpload
            pendingImageUpload = null

            if (uri == null) {
                pendingUpload?.restoreButton()
                return@registerForActivityResult
            }

            if (pendingUpload == null) {
                showToast(R.string.manage_menu_upload_image_failed)
                return@registerForActivityResult
            }

            uploadProductImage(pendingUpload, uri)
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
            if (!isAdded) return@loadProducts
            allProducts = result.getOrElse { FakeAdminMenuData.getItems() }
                .distinctBy { it.id }
            renderMenuItems()
        }
    }

    private fun toggleProductAvailability(item: AdminMenuUiModel) {
        val newAvailable = !item.isAvailable
        AdminMenuFirestoreRepository.toggleAvailability(item.id, newAvailable) { result ->
            if (!isAdded) return@toggleAvailability
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
                }
                .onFailure {
                    showToast(R.string.manage_menu_edit_product_failed)
                }
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
        val imageUrlInput = createDialogInput(
            hint = getString(R.string.manage_menu_image_url_hint),
            text = item.imageUrl,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
        )
        val uploadImageButton = Button(requireContext()).apply {
            text = getString(R.string.manage_menu_upload_image)
            setOnClickListener {
                if (!CloudinaryConfig.isConfigured) {
                    showToast(R.string.manage_menu_cloudinary_not_configured)
                    return@setOnClickListener
                }

                isEnabled = false
                text = getString(R.string.manage_menu_selecting_image)
                pendingImageUpload = PendingImageUpload(
                    productId = item.id,
                    imageUrlInput = imageUrlInput,
                    uploadButton = this,
                )
                pickProductImageLauncher.launch("image/*")
            }
        }
        val deleteButton = Button(requireContext()).apply {
            text = getString(R.string.manage_menu_delete_product)
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
            addView(imageUrlInput)
            addView(uploadImageButton)
            addView(deleteButton)
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
                            imageUrl = imageUrlInput.text.toString().trim(),
                            available = availableInput.isChecked,
                            onSaved = { dismiss() },
                        )
                    }
                    deleteButton.setOnClickListener {
                        showDeleteProductConfirmation(item, this)
                    }
                }
            }
            .show()
    }

    private fun showDeleteProductConfirmation(
        item: AdminMenuUiModel,
        editDialog: AlertDialog,
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.manage_menu_delete_product_title)
            .setMessage(R.string.manage_menu_delete_product_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.manage_menu_confirm_delete_product) { _, _ ->
                deleteProduct(item, editDialog)
            }
            .show()
    }

    private fun deleteProduct(
        item: AdminMenuUiModel,
        editDialog: AlertDialog,
    ) {
        AdminMenuFirestoreRepository.deleteProduct(item.id) { result ->
            if (!isAdded) return@deleteProduct
            result
                .onSuccess {
                    allProducts = allProducts.filterNot { product -> product.id == item.id }
                    renderMenuItems()
                    if (editDialog.isShowing) {
                        editDialog.dismiss()
                    }
                    showToast(R.string.manage_menu_delete_product_saved)
                }
                .onFailure {
                    showToast(R.string.manage_menu_delete_product_failed)
                }
        }
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
            text = selectedCategory?.name ?: AdminMenuCategory.SIGNATURE.name,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
        )
        val imageUrlInput = createDialogInput(
            hint = getString(R.string.manage_menu_image_url_hint),
            text = "",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
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
            addView(imageUrlInput)
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
                            imageUrl = imageUrlInput.text.toString().trim(),
                            onSaved = { dismiss() },
                        )
                    }
                }
            }
            .show()
    }

    private fun uploadProductImage(
        pendingUpload: PendingImageUpload,
        imageUri: Uri,
    ) {
        pendingUpload.showUploading()
        CloudinaryProductImageRepository.uploadProductImage(
            context = requireContext(),
            imageUri = imageUri,
        ) { uploadResult ->
            if (!isAdded) return@uploadProductImage
            uploadResult
                .onSuccess { imageUrl ->
                    AdminMenuFirestoreRepository.updateProductImageUrl(
                        productId = pendingUpload.productId,
                        imageUrl = imageUrl,
                    ) { updateResult ->
                        if (!isAdded) return@updateProductImageUrl
                        pendingUpload.restoreButton()
                        updateResult
                            .onSuccess {
                                pendingUpload.imageUrlInput.setText(imageUrl)
                                showToast(R.string.manage_menu_upload_image_saved)
                                refreshProductsFromFirestore()
                            }
                            .onFailure {
                                showToast(R.string.manage_menu_upload_image_failed)
                            }
                    }
                }
                .onFailure {
                    pendingUpload.restoreButton()
                    showToast(R.string.manage_menu_upload_image_failed)
                }
        }
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
        imageUrl: String,
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
            imageUrl = imageUrl,
            available = available,
        ) { result ->
            if (!isAdded) return@updateProduct
            result
                .onSuccess {
                    showToast(R.string.manage_menu_edit_product_saved)
                    onSaved()
                    refreshProductsFromFirestore()
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
        imageUrl: String,
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
            imageUrl = imageUrl,
        ) { result ->
            if (!isAdded) return@createProduct
            result
                .onSuccess {
                    showToast(R.string.manage_menu_create_product_saved)
                    onSaved()
                    refreshProductsFromFirestore()
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

    private fun selectCategory(category: AdminMenuCategory) {
        selectedCategory = category
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
        val normalizedQuery = searchQuery.trim().lowercase()

        val filteredItems = allProducts
            .filter { item ->
                selectedCategory == null || item.category == selectedCategory
            }
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
        bindAdminBottomNav(
            root = binding.staffBottomNav.root,
            selectedDestination = AdminBottomNavDestination.MENU,
        )
    }

    private fun setupTopBar() {
        bindAdminTopBar(
            root = binding.staffTopBar.root,
            title = getString(R.string.manage_menu_title),
            onMenuClick = { openAddEditProduct(productId = null) },
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
        pendingImageUpload = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private val COLOR_CHIP_SELECTED = "#3A210D".toColorInt()
        private val COLOR_CHIP_UNSELECTED = "#D8C8BC".toColorInt()
    }

    private inner class PendingImageUpload(
        val productId: String,
        val imageUrlInput: EditText,
        private val uploadButton: Button,
    ) {
        fun showUploading() {
            uploadButton.isEnabled = false
            uploadButton.text = getString(R.string.manage_menu_uploading_image)
        }

        fun restoreButton() {
            uploadButton.isEnabled = true
            uploadButton.text = getString(R.string.manage_menu_upload_image)
        }
    }
}
