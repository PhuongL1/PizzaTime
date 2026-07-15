package com.devpro.pizzatime.feature.admin.product

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.product.ProductOptionDefaults
import com.devpro.pizzatime.core.ui.message.AppUiMessageBus
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentAddEditProductBinding
import com.devpro.pizzatime.feature.admin.menu.AdminMenuFirestoreRepository
import com.devpro.pizzatime.feature.admin.menu.CloudinaryConfig
import com.devpro.pizzatime.feature.admin.menu.CloudinaryProductImageRepository
import com.devpro.pizzatime.feature.admin.menu.AdminMenuUiModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class AddEditProductFragment : Fragment(R.layout.fragment_add_edit_product) {

    private var _binding: FragmentAddEditProductBinding? = null
    private val binding: FragmentAddEditProductBinding
        get() = checkNotNull(_binding) {
            "FragmentAddEditProductBinding is only valid between onViewCreated and onDestroyView."
        }

    private var productId: String? = null
    private var imageUrl = ""
    private var isAvailable = true
    private var selectedCategoryId = ProductOptionDefaults.CATEGORY_ID_PIZZA
    private val selectedSizes = linkedSetOf<String>()
    private val selectedCrusts = linkedSetOf<String>()
    private val toppingOptions = mutableListOf<String>()
    private val selectedToppings = linkedSetOf<String>()
    private var isUploadingImage = false
    private var isSavingProduct = false

    private val pickHeroImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                setImageUploading(false)
                return@registerForActivityResult
            }

            uploadHeroImage(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddEditProductBinding.bind(view)
        productId = arguments?.getString(ARG_PRODUCT_ID)?.takeIf { it.isNotBlank() }

        bindCreateDefaults()
        setupActions()

        val editProductId = productId
        if (editProductId != null) {
            loadProduct(editProductId)
        }
    }

    private fun bindCreateDefaults() {
        isAvailable = true
        imageUrl = ""
        selectedCategoryId = ProductOptionDefaults.CATEGORY_ID_PIZZA
        selectedSizes.clear()
        selectedSizes.addAll(ProductOptionDefaults.sizeOptionsFor(currentProductCategory()))
        selectedCrusts.clear()
        selectedCrusts.addAll(ProductOptionDefaults.crustOptionsFor(currentProductCategory()))
        toppingOptions.clear()
        toppingOptions.addAll(ProductOptionDefaults.toppingOptionsFor(currentProductCategory()))
        selectedToppings.clear()
        selectedToppings.addAll(toppingOptions)
        bindProductHeader(isEditMode = false)
        bindAvailability()
        renderProductOptions()
    }

    private fun loadProduct(productId: String) {
        AdminMenuFirestoreRepository.loadProduct(productId) { result ->
            if (_binding == null) return@loadProduct
            result
                .onSuccess { product ->
                    bindExistingProduct(product)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load admin productId=$productId", error)
                    AppUiMessageBus.publish(
                        R.string.manage_menu_edit_product_failed,
                        UiMessageType.ERROR,
                    )
                    parentFragmentManager.popBackStack()
                }
        }
    }

    private fun bindExistingProduct(product: AdminMenuUiModel) = with(binding) {
        etPizzaName.setText(product.name)
        etDescription.setText(product.description)
        etBasePrice.setText(formatPriceValue(product.basePrice))
        etImageUrl.setText(product.imageUrl)
        selectedCategoryId = ProductOptionDefaults.canonicalCategoryId(
            categoryId = product.categoryId,
            categoryName = product.category.name,
        ).ifBlank { ProductOptionDefaults.CATEGORY_ID_PIZZA }
        imageUrl = product.imageUrl
        isAvailable = product.isAvailable
        val productCategory = currentProductCategory()

        selectedSizes.clear()
        selectedSizes.addAll(ProductOptionDefaults.sizesOrDefault(product.sizeOptions, productCategory))
        selectedCrusts.clear()
        selectedCrusts.addAll(ProductOptionDefaults.crustsOrDefault(product.crustOptions, productCategory))
        toppingOptions.clear()
        toppingOptions.addAll(ProductOptionDefaults.sanitizeToppingOptions(product.toppingOptions, productCategory))
        selectedToppings.clear()
        selectedToppings.addAll(toppingOptions)

        bindProductHeader(isEditMode = true)
        bindAvailability()
        renderProductOptions()
    }

    private fun bindProductHeader(isEditMode: Boolean) = with(binding) {
        tvScreenTitle.text = getString(
            if (isEditMode) R.string.add_edit_product_edit_title else R.string.add_edit_product_add_title,
        )
        btnDiscardChanges.text = getString(
            if (isEditMode) R.string.manage_menu_delete_product else R.string.add_edit_product_discard_changes,
        )
        tvCategoryValue.text = getString(categoryLabelRes(currentProductCategory()))
        ivHeroImage.loadProductImage(imageUrl, R.drawable.img_pizza_time)
        if (!isEditMode) {
            etImageUrl.setText("")
        }
    }

    private fun bindAvailability(animate: Boolean = false) = with(binding) {
        availabilitySwitch.setBackgroundResource(
            if (isAvailable) {
                R.drawable.bg_add_edit_product_switch_track_on
            } else {
                R.drawable.bg_add_edit_product_switch_track_off
            },
        )

        availabilitySwitch.contentDescription = getString(
            if (isAvailable) {
                R.string.add_edit_product_available_on_description
            } else {
                R.string.add_edit_product_available_off_description
            },
        )

        val targetTranslationX = if (isAvailable) dpToPx(28).toFloat() else 0f
        availabilityThumb.animate().cancel()

        if (animate) {
            availabilityThumb.animate()
                .translationX(targetTranslationX)
                .setDuration(180L)
                .start()
        } else {
            availabilityThumb.translationX = targetTranslationX
        }
    }

    private fun renderProductOptions() {
        val productCategory = currentProductCategory()
        binding.sizeSection.isVisible = ProductOptionDefaults.supportsSizeOptions(productCategory)
        binding.crustSection.isVisible = ProductOptionDefaults.supportsCrustOptions(productCategory)
        binding.toppingsSection.isVisible = ProductOptionDefaults.supportsToppingOptions(productCategory)
        binding.tvSizeError.isVisible = binding.tvSizeError.isVisible &&
            binding.sizeSection.isVisible && selectedSizes.isEmpty()
        binding.tvCrustError.isVisible = binding.tvCrustError.isVisible &&
            binding.crustSection.isVisible && selectedCrusts.isEmpty()

        if (binding.sizeSection.isVisible) {
            renderSizeOptions(productCategory)
        }
        if (binding.crustSection.isVisible) {
            renderCrustOptions(productCategory)
        }
        if (binding.toppingsSection.isVisible) {
            renderToppings()
        }
    }

    private fun renderSizeOptions(productCategory: ProductOptionDefaults.ProductCategory) {
        val sizeViews = listOf(
            binding.tvSizeSmall,
            binding.tvSizeMedium,
            binding.tvSizeLarge,
        )
        val options = ProductOptionDefaults.sizeOptionsFor(productCategory)

        sizeViews.forEachIndexed { index, textView ->
            val option = options.getOrNull(index)
            textView.isVisible = option != null
            if (option != null) {
                textView.text = formatSizeOptionLabel(option, productCategory)
                bindChipState(textView, selectedSizes.contains(option))
                textView.setOnClickListener {
                    toggleSelection(selectedSizes, option)
                    renderSizeOptions(productCategory)
                }
            } else {
                textView.setOnClickListener(null)
            }
        }
    }

    private fun renderCrustOptions(productCategory: ProductOptionDefaults.ProductCategory) {
        val crustViews = listOf(
            CrustOptionViews(binding.tvCrustSourdoughCheck, binding.tvCrustSourdough),
            CrustOptionViews(binding.tvCrustGlutenFreeCheck, binding.tvCrustGlutenFree),
            CrustOptionViews(binding.tvCrustCharredCheck, binding.tvCrustCharred),
        )
        val options = ProductOptionDefaults.crustOptionsFor(productCategory)

        crustViews.forEachIndexed { index, views ->
            val option = options.getOrNull(index)
            views.checkView.isVisible = option != null
            views.labelView.isVisible = option != null
            if (option != null) {
                views.labelView.text = option
                bindCheckboxState(views.checkView, selectedCrusts.contains(option))
                val clickListener = View.OnClickListener {
                    toggleSelection(selectedCrusts, option)
                    renderCrustOptions(productCategory)
                }
                views.checkView.setOnClickListener(clickListener)
                views.labelView.setOnClickListener(clickListener)
            } else {
                views.checkView.setOnClickListener(null)
                views.labelView.setOnClickListener(null)
            }
        }
    }

    private fun renderToppings() = with(binding.toppingsContainer) {
        removeAllViews()

        toppingOptions.forEach { topping ->
            addView(createToppingChip(topping))
        }
    }

    private fun createToppingChip(topping: String): TextView {
        return TextView(requireContext()).apply {
            text = getString(R.string.add_edit_product_topping_chip_format, topping)
            textSize = 14f
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            bindChipState(this, selectedToppings.contains(topping))
            setOnClickListener {
                toggleSelection(selectedToppings, topping)
                renderToppings()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginEnd = dpToPx(8)
                bottomMargin = dpToPx(8)
            }
        }
    }

    private fun bindChipState(
        textView: TextView,
        selected: Boolean,
    ) {
        textView.setBackgroundResource(
            if (selected) {
                R.drawable.bg_add_edit_product_chip_selected
            } else {
                R.drawable.bg_add_edit_product_chip_unselected
            },
        )

        textView.setTextColor(
            requireContext().getColor(
                if (selected) {
                    R.color.pt_text_dark
                } else {
                    R.color.pt_text_secondary_dark_bg
                },
            ),
        )
    }

    private fun bindCheckboxState(
        checkView: TextView,
        selected: Boolean,
    ) {
        checkView.text = if (selected) {
            getString(R.string.add_edit_product_check_icon)
        } else {
            ""
        }

        checkView.setBackgroundResource(
            if (selected) {
                R.drawable.bg_add_edit_product_checkbox_checked
            } else {
                R.drawable.bg_add_edit_product_checkbox_unchecked
            },
        )
    }

    private fun setupActions() = with(binding) {
        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        heroUploadCard.setOnClickListener {
            pickHeroImage()
        }

        availabilityCard.setOnClickListener {
            toggleAvailability()
        }

        availabilitySwitch.setOnClickListener {
            toggleAvailability()
        }

        tvCategoryValue.setOnClickListener {
            showCategoryDialog()
        }

        btnAddTopping.setOnClickListener {
            showAddToppingDialog()
        }

        btnDiscardChanges.setOnClickListener {
            if (productId == null) {
                showDiscardChangesConfirmation()
            } else {
                showDeleteProductConfirmation()
            }
        }

        btnSaveProduct.setOnClickListener {
            saveProduct()
        }
    }

    private fun pickHeroImage() {
        if (isUploadingImage) return

        if (!CloudinaryConfig.isConfigured) {
            showUiMessage(R.string.manage_menu_cloudinary_not_configured, UiMessageType.ERROR)
            return
        }

        setImageUploading(true)
        pickHeroImageLauncher.launch("image/*")
    }

    private fun uploadHeroImage(imageUri: Uri) {
        CloudinaryProductImageRepository.uploadProductImage(
            context = requireContext(),
            imageUri = imageUri,
        ) { result ->
            if (_binding == null) {
                isUploadingImage = false
                return@uploadProductImage
            }

            setImageUploading(false)

            result
                .onSuccess { uploadedImageUrl ->
                    imageUrl = uploadedImageUrl
                    binding.etImageUrl.setText(uploadedImageUrl)
                    binding.ivHeroImage.loadProductImage(uploadedImageUrl, R.drawable.img_pizza_time)
                    showUiMessage(R.string.manage_menu_upload_image_saved, UiMessageType.SUCCESS)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to upload product image", error)
                    showUiMessage(R.string.manage_menu_upload_image_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun setImageUploading(uploading: Boolean) {
        isUploadingImage = uploading
        val currentBinding = _binding ?: return

        currentBinding.heroUploadCard.isEnabled = !uploading
        currentBinding.heroUploadCard.alpha = if (uploading) 0.6f else 1f
        currentBinding.btnSaveProduct.isEnabled = !uploading && !isSavingProduct
    }

    private fun showCategoryDialog() {
        val categories = listOf(
            ProductOptionDefaults.CATEGORY_ID_PIZZA to R.string.home_category_pizza,
            ProductOptionDefaults.CATEGORY_ID_DRINK to R.string.home_category_drinks,
            ProductOptionDefaults.CATEGORY_ID_COMBO to R.string.home_category_combo,
            ProductOptionDefaults.CATEGORY_ID_DESSERT to R.string.home_category_dessert,
        )
        val labels = categories.map { (_, labelRes) -> getString(labelRes) }.toTypedArray()
        val selectedIndex = categories.indexOfFirst { (categoryId, _) -> categoryId == selectedCategoryId }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_edit_product_category)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                selectedCategoryId = categories[which].first
                applyCategoryDefaultsFor(currentProductCategory())
                bindProductHeader(isEditMode = productId != null)
                renderProductOptions()
                dialog.dismiss()
            }
            .show()
    }

    private fun showAddToppingDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.add_edit_product_topping_name_hint)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_edit_product_add_new)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_edit_product_add_new, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (addTopping(input)) dismiss()
                    }
                }
            }
            .show()
    }

    private fun addTopping(input: EditText): Boolean {
        val topping = input.text.toString().trim()
        if (topping.isBlank()) {
            input.error = getString(R.string.add_edit_product_topping_required)
            return false
        }
        val duplicate = toppingOptions.any { it.equals(topping, ignoreCase = true) }
        if (duplicate) {
            input.error = getString(R.string.add_edit_product_topping_duplicate)
            return false
        }
        input.error = null
        toppingOptions.add(topping)
        selectedToppings.add(topping)
        renderToppings()
        return true
    }

    private fun saveProduct() = with(binding) {
        if (isUploadingImage || isSavingProduct) return

        val name = etPizzaName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val basePrice = etBasePrice.text.toString().trim().toDoubleOrNull()
        imageUrl = etImageUrl.text.toString().trim()
        val sizes = getSelectedSizes()
        val crusts = getSelectedCrusts()
        val toppings = getSelectedToppings()
        val productCategory = currentProductCategory()

        etPizzaName.error = if (name.isBlank()) {
            getString(R.string.manage_menu_edit_name_required)
        } else {
            null
        }
        etBasePrice.error = if (basePrice == null || basePrice <= 0.0) {
            getString(R.string.manage_menu_edit_price_required)
        } else {
            null
        }
        if (etPizzaName.error != null || etBasePrice.error != null) return
        tvSizeError.text = getString(R.string.add_edit_product_size_required)
        tvSizeError.isVisible = ProductOptionDefaults.supportsSizeOptions(productCategory) && sizes.isEmpty()
        tvCrustError.text = getString(R.string.add_edit_product_crust_required)
        tvCrustError.isVisible = ProductOptionDefaults.supportsCrustOptions(productCategory) && crusts.isEmpty()
        if (tvSizeError.isVisible || tvCrustError.isVisible) return

        val validatedBasePrice = basePrice ?: return
        setProductSaving(true)
        val existingProductId = productId
        if (existingProductId == null) {
            createProduct(
                productId = normalizeProductId(name),
                name = name,
                description = description,
                basePrice = validatedBasePrice,
                sizes = sizes,
                crusts = crusts,
                toppings = toppings,
            )
        } else {
            updateProduct(
                productId = existingProductId,
                name = name,
                description = description,
                basePrice = validatedBasePrice,
                sizes = sizes,
                crusts = crusts,
                toppings = toppings,
            )
        }
    }

    private fun createProduct(
        productId: String,
        name: String,
        description: String,
        basePrice: Double,
        sizes: List<String>,
        crusts: List<String>,
        toppings: List<String>,
    ) {
        if (productId.isBlank()) {
            setProductSaving(false)
            binding.etPizzaName.error = getString(R.string.manage_menu_create_id_required)
            return
        }

        AdminMenuFirestoreRepository.createProduct(
            productId = productId,
            name = name,
            description = description,
            basePrice = basePrice,
            categoryId = selectedCategoryId,
            imageUrl = imageUrl,
            available = isAvailable,
            sizeOptions = sizes,
            crustOptions = crusts,
            toppingOptions = toppings,
        ) { result ->
            if (_binding == null) return@createProduct
            result
                .onSuccess {
                    AppUiMessageBus.publish(
                        R.string.manage_menu_create_product_saved,
                        UiMessageType.SUCCESS,
                    )
                    parentFragmentManager.popBackStack()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to create admin productId=$productId", error)
                    setProductSaving(false)
                    showUiMessage(R.string.manage_menu_create_product_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun updateProduct(
        productId: String,
        name: String,
        description: String,
        basePrice: Double,
        sizes: List<String>,
        crusts: List<String>,
        toppings: List<String>,
    ) {
        AdminMenuFirestoreRepository.updateProduct(
            productId = productId,
            name = name,
            description = description,
            basePrice = basePrice,
            categoryId = selectedCategoryId,
            imageUrl = imageUrl,
            available = isAvailable,
            sizeOptions = sizes,
            crustOptions = crusts,
            toppingOptions = toppings,
        ) { result ->
            if (_binding == null) return@updateProduct
            result
                .onSuccess {
                    AppUiMessageBus.publish(
                        R.string.manage_menu_edit_product_saved,
                        UiMessageType.SUCCESS,
                    )
                    parentFragmentManager.popBackStack()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to update admin productId=$productId", error)
                    setProductSaving(false)
                    showUiMessage(R.string.manage_menu_edit_product_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun showDeleteProductConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.manage_menu_delete_product_title)
            .setMessage(R.string.manage_menu_delete_product_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.manage_menu_confirm_delete_product) { _, _ ->
                deleteProduct()
            }
            .show()
    }

    private fun showDiscardChangesConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_edit_product_discard_changes)
            .setMessage(R.string.add_edit_product_discard_confirmation)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_edit_product_discard_changes) { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    private fun deleteProduct() {
        val id = productId ?: return
        AdminMenuFirestoreRepository.deleteProduct(id) { result ->
            if (_binding == null) return@deleteProduct
            result
                .onSuccess {
                    AppUiMessageBus.publish(
                        R.string.manage_menu_delete_product_saved,
                        UiMessageType.SUCCESS,
                    )
                    parentFragmentManager.popBackStack()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to delete admin productId=$id", error)
                    showUiMessage(R.string.manage_menu_delete_product_failed, UiMessageType.ERROR)
                }
        }
    }

    private fun toggleAvailability() {
        isAvailable = !isAvailable
        bindAvailability(animate = true)
    }

    private fun toggleSelection(
        selectedValues: LinkedHashSet<String>,
        value: String,
    ) {
        if (selectedValues.contains(value)) {
            selectedValues.remove(value)
        } else {
            selectedValues.add(value)
        }
    }

    private fun getSelectedSizes(): List<String> {
        return ProductOptionDefaults.sanitizeSizeOptions(selectedSizes.toList(), currentProductCategory())
    }

    private fun getSelectedCrusts(): List<String> {
        return ProductOptionDefaults.sanitizeCrustOptions(selectedCrusts.toList(), currentProductCategory())
    }

    private fun getSelectedToppings(): List<String> {
        return ProductOptionDefaults.sanitizeToppingOptions(selectedToppings.toList(), currentProductCategory())
    }

    private fun normalizeProductId(rawName: String): String {
        return rawName.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    private fun setProductSaving(saving: Boolean) {
        isSavingProduct = saving
        val currentBinding = _binding ?: return
        currentBinding.btnSaveProduct.isEnabled = !saving && !isUploadingImage
    }

    private fun formatPriceValue(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun currentProductCategory(): ProductOptionDefaults.ProductCategory {
        return ProductOptionDefaults.resolveProductCategory(selectedCategoryId)
    }

    private fun applyCategoryDefaultsFor(category: ProductOptionDefaults.ProductCategory) {
        selectedSizes.retainAll(ProductOptionDefaults.sizeOptionsFor(category).toSet())
        if (ProductOptionDefaults.supportsSizeOptions(category) && selectedSizes.isEmpty()) {
            selectedSizes.addAll(ProductOptionDefaults.sizeOptionsFor(category))
        }

        selectedCrusts.retainAll(ProductOptionDefaults.crustOptionsFor(category).toSet())
        if (ProductOptionDefaults.supportsCrustOptions(category) && selectedCrusts.isEmpty()) {
            selectedCrusts.addAll(ProductOptionDefaults.crustOptionsFor(category))
        }

        val validToppings = ProductOptionDefaults.sanitizeToppingOptions(toppingOptions, category)
        toppingOptions.clear()
        if (ProductOptionDefaults.supportsToppingOptions(category)) {
            if (validToppings.isNotEmpty()) {
                toppingOptions.addAll(validToppings)
            } else {
                toppingOptions.addAll(ProductOptionDefaults.toppingOptionsFor(category))
            }
        }

        selectedToppings.retainAll(toppingOptions.toSet())
        if (ProductOptionDefaults.supportsToppingOptions(category) && selectedToppings.isEmpty()) {
            selectedToppings.addAll(toppingOptions)
        }
    }

    private fun formatSizeOptionLabel(
        size: String,
        category: ProductOptionDefaults.ProductCategory,
    ): String {
        val detail = when (category) {
            ProductOptionDefaults.ProductCategory.DRINK -> when (size) {
                "Small" -> getString(R.string.detail_size_small_ml)
                "Medium" -> getString(R.string.detail_size_medium_ml)
                "Large" -> getString(R.string.detail_size_large_ml)
                else -> ""
            }

            else -> when (size) {
                "Small" -> getString(R.string.ten_inches)
                "Medium" -> getString(R.string.twelve_inches)
                "Large" -> getString(R.string.fourteen_inches)
                else -> ""
            }
        }
        return if (detail.isBlank()) size else "$size ($detail)"
    }

    private fun categoryLabelRes(category: ProductOptionDefaults.ProductCategory): Int {
        return when (category) {
            ProductOptionDefaults.ProductCategory.PIZZA -> R.string.home_category_pizza
            ProductOptionDefaults.ProductCategory.DRINK -> R.string.home_category_drinks
            ProductOptionDefaults.ProductCategory.COMBO -> R.string.home_category_combo
            ProductOptionDefaults.ProductCategory.DESSERT -> R.string.home_category_dessert
            ProductOptionDefaults.ProductCategory.UNKNOWN -> R.string.home_category_pizza
        }
    }

    override fun onDestroyView() {
        isUploadingImage = false
        isSavingProduct = false
        super.onDestroyView()
        _binding = null
    }

    private data class CrustOptionViews(
        val checkView: TextView,
        val labelView: TextView,
    )

    companion object {
        private const val TAG = "AddEditProduct"
        private const val ARG_PRODUCT_ID = "arg_product_id"

        fun newInstance(productId: String? = null): AddEditProductFragment {
            return AddEditProductFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRODUCT_ID, productId)
                }
            }
        }
    }
}
