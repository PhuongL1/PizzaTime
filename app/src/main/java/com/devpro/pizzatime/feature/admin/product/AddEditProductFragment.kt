package com.devpro.pizzatime.feature.admin.product

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.product.ProductOptionDefaults
import com.devpro.pizzatime.databinding.FragmentAddEditProductBinding
import com.devpro.pizzatime.feature.admin.menu.AdminMenuCategory
import com.devpro.pizzatime.feature.admin.menu.AdminMenuFirestoreRepository
import com.devpro.pizzatime.feature.admin.menu.CloudinaryConfig
import com.devpro.pizzatime.feature.admin.menu.CloudinaryProductImageRepository
import com.devpro.pizzatime.feature.admin.menu.AdminMenuUiModel
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
    private var selectedCategory = AdminMenuCategory.SIGNATURE
    private val selectedSizes = linkedSetOf<String>()
    private val selectedCrusts = linkedSetOf<String>()
    private val toppingOptions = mutableListOf<String>()
    private val selectedToppings = linkedSetOf<String>()
    private var isUploadingImage = false

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
        selectedCategory = AdminMenuCategory.SIGNATURE
        selectedSizes.clear()
        selectedSizes.addAll(ProductOptionDefaults.sizeOptions)
        selectedCrusts.clear()
        selectedCrusts.addAll(ProductOptionDefaults.crustOptions)
        toppingOptions.clear()
        toppingOptions.addAll(ProductOptionDefaults.toppingOptions)
        selectedToppings.clear()
        selectedToppings.addAll(toppingOptions)
        bindProductHeader(isEditMode = false)
        bindAvailability()
        renderSizeOptions()
        renderCrustOptions()
        renderToppings()
    }

    private fun loadProduct(productId: String) {
        AdminMenuFirestoreRepository.loadProduct(productId) { result ->
            if (_binding == null) return@loadProduct
            result
                .onSuccess { product ->
                    bindExistingProduct(product)
                }
                .onFailure {
                    showToast(R.string.manage_menu_edit_product_failed)
                    parentFragmentManager.popBackStack()
                }
        }
    }

    private fun bindExistingProduct(product: AdminMenuUiModel) = with(binding) {
        etPizzaName.setText(product.name)
        etDescription.setText(product.description)
        etBasePrice.setText(formatPriceValue(product.basePrice))
        etImageUrl.setText(product.imageUrl)
        selectedCategory = product.category
        imageUrl = product.imageUrl
        isAvailable = product.isAvailable

        selectedSizes.clear()
        selectedSizes.addAll(ProductOptionDefaults.sizesOrDefault(product.sizeOptions))
        selectedCrusts.clear()
        selectedCrusts.addAll(ProductOptionDefaults.crustsOrDefault(product.crustOptions))
        toppingOptions.clear()
        toppingOptions.addAll(product.toppingOptions)
        selectedToppings.clear()
        selectedToppings.addAll(product.toppingOptions)

        bindProductHeader(isEditMode = true)
        bindAvailability()
        renderSizeOptions()
        renderCrustOptions()
        renderToppings()
    }

    private fun bindProductHeader(isEditMode: Boolean) = with(binding) {
        tvScreenTitle.text = getString(
            if (isEditMode) R.string.add_edit_product_edit_title else R.string.add_edit_product_add_title,
        )
        btnDiscardChanges.text = getString(
            if (isEditMode) R.string.manage_menu_delete_product else R.string.add_edit_product_discard_changes,
        )
        tvCategoryValue.text = selectedCategory.name
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

    private fun renderSizeOptions() {
        val sizeViews = listOf(
            binding.tvSizeSmall,
            binding.tvSizeMedium,
            binding.tvSizeLarge,
            binding.tvSizeFamily,
        )
        val options = ProductOptionDefaults.sizeOptions.take(sizeViews.size)

        sizeViews.forEachIndexed { index, textView ->
            val option = options[index]
            textView.isVisible = true
            textView.text = option
            bindChipState(textView, selectedSizes.contains(option))
            textView.setOnClickListener {
                toggleSelection(selectedSizes, option)
                renderSizeOptions()
            }
        }
    }

    private fun renderCrustOptions() {
        val crustViews = listOf(
            CrustOptionViews(binding.tvCrustSourdoughCheck, binding.tvCrustSourdough),
            CrustOptionViews(binding.tvCrustGlutenFreeCheck, binding.tvCrustGlutenFree),
            CrustOptionViews(binding.tvCrustCharredCheck, binding.tvCrustCharred),
        )
        val options = ProductOptionDefaults.crustOptions.take(crustViews.size)

        crustViews.forEachIndexed { index, views ->
            val option = options[index]
            views.checkView.isVisible = true
            views.labelView.isVisible = true
            views.labelView.text = option
            bindCheckboxState(views.checkView, selectedCrusts.contains(option))
            val clickListener = View.OnClickListener {
                toggleSelection(selectedCrusts, option)
                renderCrustOptions()
            }
            views.checkView.setOnClickListener(clickListener)
            views.labelView.setOnClickListener(clickListener)
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
                parentFragmentManager.popBackStack()
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
            showToast(R.string.manage_menu_cloudinary_not_configured)
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
                    showToast(R.string.manage_menu_upload_image_saved)
                }
                .onFailure {
                    showToast(R.string.manage_menu_upload_image_failed)
                }
        }
    }

    private fun setImageUploading(uploading: Boolean) {
        isUploadingImage = uploading
        val currentBinding = _binding ?: return

        currentBinding.heroUploadCard.isEnabled = !uploading
        currentBinding.heroUploadCard.alpha = if (uploading) 0.6f else 1f
        currentBinding.btnSaveProduct.isEnabled = !uploading
    }

    private fun showCategoryDialog() {
        val categories = AdminMenuCategory.entries.toTypedArray()
        val labels = categories.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_edit_product_category)
            .setItems(labels) { _, which ->
                selectedCategory = categories[which]
                binding.tvCategoryValue.text = selectedCategory.name
            }
            .show()
    }

    private fun showAddToppingDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.add_edit_product_topping_name_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_edit_product_add_new)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_edit_product_add_new) { _, _ ->
                addTopping(input.text.toString())
            }
            .show()
    }

    private fun addTopping(rawName: String) {
        val topping = rawName.trim()
        if (topping.isBlank()) {
            showToast(R.string.add_edit_product_topping_required)
            return
        }
        val duplicate = toppingOptions.any { it.equals(topping, ignoreCase = true) }
        if (duplicate) {
            showToast(R.string.add_edit_product_topping_duplicate)
            return
        }
        toppingOptions.add(topping)
        selectedToppings.add(topping)
        renderToppings()
    }

    private fun saveProduct() = with(binding) {
        if (isUploadingImage) {
            showToast(R.string.manage_menu_uploading_image)
            return
        }

        val name = etPizzaName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val basePrice = etBasePrice.text.toString().trim().toDoubleOrNull()
        imageUrl = etImageUrl.text.toString().trim()
        val sizes = getSelectedSizes()
        val crusts = getSelectedCrusts()
        val toppings = getSelectedToppings()

        when {
            name.isBlank() -> {
                showToast(R.string.manage_menu_edit_name_required)
                return
            }

            basePrice == null || basePrice <= 0.0 -> {
                showToast(R.string.manage_menu_edit_price_required)
                return
            }

            sizes.isEmpty() -> {
                showToast(R.string.add_edit_product_size_required)
                return
            }

            crusts.isEmpty() -> {
                showToast(R.string.add_edit_product_crust_required)
                return
            }
        }

        val existingProductId = productId
        if (existingProductId == null) {
            createProduct(
                productId = normalizeProductId(name),
                name = name,
                description = description,
                basePrice = basePrice,
                sizes = sizes,
                crusts = crusts,
                toppings = toppings,
            )
        } else {
            updateProduct(
                productId = existingProductId,
                name = name,
                description = description,
                basePrice = basePrice,
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
            showToast(R.string.manage_menu_create_id_required)
            return
        }

        AdminMenuFirestoreRepository.createProduct(
            productId = productId,
            name = name,
            description = description,
            basePrice = basePrice,
            categoryId = selectedCategory.name,
            imageUrl = imageUrl,
            available = isAvailable,
            sizeOptions = sizes,
            crustOptions = crusts,
            toppingOptions = toppings,
        ) { result ->
            if (_binding == null) return@createProduct
            result
                .onSuccess {
                    showToast(R.string.manage_menu_create_product_saved)
                    parentFragmentManager.popBackStack()
                }
                .onFailure {
                    showToast(R.string.manage_menu_create_product_failed)
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
            categoryId = selectedCategory.name,
            imageUrl = imageUrl,
            available = isAvailable,
            sizeOptions = sizes,
            crustOptions = crusts,
            toppingOptions = toppings,
        ) { result ->
            if (_binding == null) return@updateProduct
            result
                .onSuccess {
                    showToast(R.string.manage_menu_edit_product_saved)
                    parentFragmentManager.popBackStack()
                }
                .onFailure {
                    showToast(R.string.manage_menu_edit_product_failed)
                }
        }
    }

    private fun showDeleteProductConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.manage_menu_delete_product_title)
            .setMessage(R.string.manage_menu_delete_product_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.manage_menu_confirm_delete_product) { _, _ ->
                deleteProduct()
            }
            .show()
    }

    private fun deleteProduct() {
        val id = productId ?: return
        AdminMenuFirestoreRepository.deleteProduct(id) { result ->
            if (_binding == null) return@deleteProduct
            result
                .onSuccess {
                    showToast(R.string.manage_menu_delete_product_saved)
                    parentFragmentManager.popBackStack()
                }
                .onFailure {
                    showToast(R.string.manage_menu_delete_product_failed)
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

    private fun getSelectedSizes(): List<String> = selectedSizes.toList()

    private fun getSelectedCrusts(): List<String> = selectedCrusts.toList()

    private fun getSelectedToppings(): List<String> = selectedToppings.toList()

    private fun normalizeProductId(rawName: String): String {
        return rawName.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun formatPriceValue(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        isUploadingImage = false
        super.onDestroyView()
        _binding = null
    }

    private data class CrustOptionViews(
        val checkView: TextView,
        val labelView: TextView,
    )

    companion object {
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
