package com.devpro.pizzatime.feature.admin.product

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentAddEditProductBinding
import java.util.Locale

class AddEditProductFragment : Fragment(R.layout.fragment_add_edit_product) {

    private var _binding: FragmentAddEditProductBinding? = null
    private val binding: FragmentAddEditProductBinding
        get() = checkNotNull(_binding) {
            "FragmentAddEditProductBinding is only valid between onViewCreated and onDestroyView."
        }

    private lateinit var product: AddEditProductUiModel
    private var isAvailable = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddEditProductBinding.bind(view)

        product = FakeAddEditProductData.getProduct(
            productId = arguments?.getString(ARG_PRODUCT_ID),
        )
        isAvailable = product.isAvailable

        bindProduct()
        setupActions()
    }

    private fun bindProduct() = with(binding) {
        tvScreenTitle.text = if (product.isEditMode) {
            getString(R.string.add_edit_product_edit_title)
        } else {
            getString(R.string.add_edit_product_add_title)
        }

        ivHeroImage.setImageResource(product.heroImageRes)
        etPizzaName.setText(product.name)
        etDescription.setText(product.description)
        tvCategoryValue.text = product.category
        etBasePrice.setText(formatPriceValue(product.basePrice))

        bindAvailability()
        bindSizes()
        bindCrustOptions()
        bindToppings()
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

        val thumbOffsetDp = 28
        val animationDurationMs = 180L
        val targetTranslationX = if (isAvailable) {
            dpToPx(thumbOffsetDp).toFloat()
        } else {
            0f
        }

        availabilityThumb.animate().cancel()

        if (animate) {
            availabilityThumb.animate()
                .translationX(targetTranslationX)
                .setDuration(animationDurationMs)
                .start()
        } else {
            availabilityThumb.translationX = targetTranslationX
        }
    }

    private fun bindSizes() {
        val sizeViews = listOf(
            binding.tvSizeSmall,
            binding.tvSizeMedium,
            binding.tvSizeLarge,
        )

        sizeViews.forEachIndexed { index, textView ->
            val option = product.sizes.getOrNull(index)

            textView.isVisible = option != null

            if (option != null) {
                textView.text = option.label
                bindChipState(
                    textView = textView,
                    selected = option.selected,
                )
            }
        }
    }

    private fun bindCrustOptions() {
        val crustViews = listOf(
            CrustOptionViews(
                checkView = binding.tvCrustSourdoughCheck,
                labelView = binding.tvCrustSourdough,
            ),
            CrustOptionViews(
                checkView = binding.tvCrustGlutenFreeCheck,
                labelView = binding.tvCrustGlutenFree,
            ),
            CrustOptionViews(
                checkView = binding.tvCrustCharredCheck,
                labelView = binding.tvCrustCharred,
            ),
        )

        crustViews.forEachIndexed { index, views ->
            val option = product.crustOptions.getOrNull(index)

            views.checkView.isVisible = option != null
            views.labelView.isVisible = option != null

            if (option != null) {
                views.labelView.text = option.label
                bindCheckboxState(
                    checkView = views.checkView,
                    selected = option.selected,
                )
            }
        }
    }

    private fun bindToppings() = with(binding.toppingsContainer) {
        removeAllViews()

        product.toppings.forEach { topping ->
            addView(createToppingChip(topping))
        }
    }

    private fun createToppingChip(topping: String): TextView {
        return TextView(requireContext()).apply {
            text = getString(R.string.add_edit_product_topping_chip_format, topping)
            setTextColor(requireContext().getColor(R.color.pt_text_secondary_dark_bg))
            textSize = 14f
            setBackgroundResource(R.drawable.bg_add_edit_product_topping_chip)
            setPadding(
                dpToPx(14),
                dpToPx(8),
                dpToPx(14),
                dpToPx(8),
            )
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
            showToast(R.string.add_edit_product_upload_toast)
        }

        availabilityCard.setOnClickListener {
            toggleAvailability()
        }

        availabilitySwitch.setOnClickListener {
            toggleAvailability()
        }

        tvCategoryValue.setOnClickListener {
            showToast(R.string.add_edit_product_category_toast)
        }

        btnAddTopping.setOnClickListener {
            showToast(R.string.add_edit_product_add_topping_toast)
        }

        btnDiscardChanges.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSaveProduct.setOnClickListener {
            showToast(R.string.add_edit_product_save_toast)
            parentFragmentManager.popBackStack()
        }
    }

    private fun toggleAvailability() {
        isAvailable = !isAvailable
        bindAvailability(animate = true)
    }

    private fun showToast(messageRes: Int) {
        Toast.makeText(
            requireContext(),
            getString(messageRes),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun formatPriceValue(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
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