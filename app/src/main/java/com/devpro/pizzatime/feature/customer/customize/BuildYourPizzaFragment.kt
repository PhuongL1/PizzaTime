package com.devpro.pizzatime.feature.customer.customize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentBuildYourPizzaBinding
import com.devpro.pizzatime.databinding.ItemBuildPizzaCheeseBinding
import com.devpro.pizzatime.databinding.ItemBuildPizzaSauceBinding
import com.devpro.pizzatime.databinding.ItemBuildPizzaSizeBinding
import com.devpro.pizzatime.databinding.ItemBuildPizzaToppingGroupBinding
import java.util.Locale
import kotlin.math.roundToInt

class BuildYourPizzaFragment : Fragment() {

    private var _binding: FragmentBuildYourPizzaBinding? = null
    private val binding: FragmentBuildYourPizzaBinding
        get() = checkNotNull(_binding) {
            "FragmentBuildYourPizzaBinding is only valid between onCreateView and onDestroyView."
        }

    private val buildPizzaData: BuildPizzaUiModel = FakeBuildPizzaData.getBuildPizza()

    private var selectedSizeId: String = ""
    private var selectedSauceId: String = ""
    private val selectedCheeseIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBuildYourPizzaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupInitialState()
        bindStaticContent()
        bindSizeOptions()
        bindSauceOptions()
        bindCheeseOptions()
        bindToppingGroups()
        setupActions()
        updateTotal()
    }

    private fun setupInitialState() {
        selectedSizeId = buildPizzaData.sizes.firstOrNull { it.selected }?.id
            ?: buildPizzaData.sizes.first().id

        selectedSauceId = buildPizzaData.sauces.firstOrNull { it.selected }?.id
            ?: buildPizzaData.sauces.first().id

        buildPizzaData.cheeses
            .filter { it.selected || it.included }
            .mapTo(selectedCheeseIds) { it.id }
    }

    private fun bindStaticContent() = with(binding) {
        ivPizzaPreview.setImageResource(buildPizzaData.previewImageRes)
        tvCrustDropdown.text = buildPizzaData.selectedCrust
    }

    private fun bindSizeOptions(): Unit = with(binding.sizeContainer) {
        removeAllViews()

        buildPizzaData.sizes.forEachIndexed { index, option ->
            val itemBinding = ItemBuildPizzaSizeBinding.inflate(layoutInflater, this, false)

            itemBinding.tvSizeLabel.text = option.label
            itemBinding.tvSizePrice.text = formatPrice(option.price)
            itemBinding.root.setBackgroundResource(
                if (option.id == selectedSizeId) {
                    R.drawable.bg_build_pizza_size_selected
                } else {
                    R.drawable.bg_build_pizza_size
                },
            )
            itemBinding.root.setOnClickListener {
                selectedSizeId = option.id
                bindSizeOptions()
                updateTotal()
            }

            val endMargin = if (index == buildPizzaData.sizes.lastIndex) 0 else 12
            addView(
                itemBinding.root,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f,
                ).apply {
                    marginEnd = endMargin.dp()
                },
            )
        }
    }

    private fun bindSauceOptions(): Unit = with(binding.sauceContainer) {
        removeAllViews()

        buildPizzaData.sauces.forEachIndexed { index, option ->
            val itemBinding = ItemBuildPizzaSauceBinding.inflate(layoutInflater, this, false)

            itemBinding.vSaucePreview.setBackgroundResource(option.previewBackgroundRes)
            itemBinding.tvSauceName.text = option.name
            itemBinding.tvSauceSubtitle.text = option.subtitle
            itemBinding.root.setBackgroundResource(
                if (option.id == selectedSauceId) {
                    R.drawable.bg_build_pizza_sauce_card_selected
                } else {
                    R.drawable.bg_build_pizza_sauce_card
                },
            )
            itemBinding.root.setOnClickListener {
                selectedSauceId = option.id
                bindSauceOptions()
            }

            val endMargin = if (index == buildPizzaData.sauces.lastIndex) 0 else 18
            addView(
                itemBinding.root,
                LinearLayout.LayoutParams(
                    176.dp(),
                    190.dp(),
                ).apply {
                    marginEnd = endMargin.dp()
                },
            )
        }
    }

    private fun bindCheeseOptions(): Unit = with(binding.cheeseContainer) {
        removeAllViews()

        buildPizzaData.cheeses.forEachIndexed { index, option ->
            val itemBinding = ItemBuildPizzaCheeseBinding.inflate(layoutInflater, this, false)

            itemBinding.cbCheese.text = option.name
            itemBinding.cbCheese.isChecked = selectedCheeseIds.contains(option.id)
            itemBinding.cbCheese.isEnabled = !option.included
            itemBinding.tvCheesePrice.text = if (option.included) {
                getString(R.string.build_pizza_included)
            } else {
                formatExtraPrice(option.extraPrice)
            }

            itemBinding.cbCheese.setOnCheckedChangeListener { _, checked ->
                updateCheeseSelection(option.id, checked)
            }

            itemBinding.root.setOnClickListener {
                if (!option.included) {
                    itemBinding.cbCheese.isChecked = !itemBinding.cbCheese.isChecked
                }
            }

            addView(
                itemBinding.root,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    72.dp(),
                ).apply {
                    if (index > 0) {
                        topMargin = 10.dp()
                    }
                },
            )
        }
    }

    private fun bindToppingGroups(): Unit = with(binding.toppingGroupContainer) {
        removeAllViews()

        buildPizzaData.toppingGroups.forEachIndexed { index, group ->
            val itemBinding = ItemBuildPizzaToppingGroupBinding.inflate(layoutInflater, this, false)

            itemBinding.tvToppingGroupTitle.text = group.title
            itemBinding.toppingItemsContainer.removeAllViews()

            group.items.forEach { item ->
                itemBinding.toppingItemsContainer.addView(createToppingItemText(item))
            }

            val endMargin = if (index == buildPizzaData.toppingGroups.lastIndex) 0 else 10
            addView(
                itemBinding.root,
                LinearLayout.LayoutParams(
                    0,
                    180.dp(),
                    1f,
                ).apply {
                    marginEnd = endMargin.dp()
                },
            )
        }
    }

    private fun createToppingItemText(text: String): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                34.dp(),
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
            setText(text)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.pt_text_primary_dark_bg))
            textSize = 16f
        }
    }

    private fun setupActions(): Unit = with(binding) {
        btnAddToOrder.setOnClickListener {
        showUiMessage(R.string.build_pizza_added_message, UiMessageType.SUCCESS)

            // TODO: Sau này thay bằng CartRepository.addItem(...)
            // Fake UI hiện chỉ tính giá và show toast.
        }
    }

    private fun updateCheeseSelection(cheeseId: String, checked: Boolean) {
        if (checked) {
            selectedCheeseIds.add(cheeseId)
        } else {
            selectedCheeseIds.remove(cheeseId)
        }

        updateTotal()
    }

    private fun updateTotal() {
        val sizePrice = buildPizzaData.sizes
            .first { it.id == selectedSizeId }
            .price

        val cheesePrice = buildPizzaData.cheeses
            .filter { selectedCheeseIds.contains(it.id) && !it.included }
            .sumOf { it.extraPrice }

        binding.tvEstimatedTotal.text = formatPrice(sizePrice + cheesePrice)
    }

    private fun formatPrice(value: Double): String {
        return if (value % 1.0 == 0.0) {
            "$${value.toInt()}.00"
        } else {
            String.format(Locale.US, "$%.2f", value)
        }
    }

    private fun formatExtraPrice(value: Double): String {
        return "+${formatPrice(value)}"
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
