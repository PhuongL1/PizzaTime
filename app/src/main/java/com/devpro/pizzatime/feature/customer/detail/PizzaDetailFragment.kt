package com.devpro.pizzatime.feature.customer.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.Toast
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentPizzaDetailBinding
import com.devpro.pizzatime.databinding.ItemExtraToppingBinding
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen

class PizzaDetailFragment : Fragment() {

    private var _binding: FragmentPizzaDetailBinding? = null
    private val binding get() = _binding!!

    private var quantity = 1
    private var isFavorite = false

    private val pizzaDetail = FakePizzaDetailData.truffleNoir

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPizzaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindPizzaDetail(pizzaDetail)
        renderToppings(pizzaDetail.toppings)
        setupActions()
    }

    private fun bindPizzaDetail(item: PizzaDetailUiModel) {
        binding.imgPizzaHero.setImageResource(item.imageRes)
        binding.imgPizzaHero.contentDescription = item.name
        binding.tvPizzaName.text = item.name
        binding.tvPizzaDescription.text = item.description
        binding.tvRating.text = getString(R.string.detail_rating_format, item.rating)
        binding.tvTime.text = item.time
        binding.tvKcal.text = item.kcal
        binding.btnAddToCart.text = getString(R.string.detail_add_to_cart_format, item.price)
        binding.tvQuantity.text = quantity.toString()
    }

    private fun renderToppings(items: List<ExtraToppingUiModel>) {
        binding.toppingContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemExtraToppingBinding.inflate(
                layoutInflater,
                binding.toppingContainer,
                false
            )

            itemBinding.tvToppingName.text = item.name
            itemBinding.tvToppingPrice.text = item.price

            var isSelected = item.isSelected
            updateToppingState(itemBinding, isSelected)

            itemBinding.root.setOnClickListener {
                isSelected = !isSelected
                updateToppingState(itemBinding, isSelected)

                Toast.makeText(
                    requireContext(),
                    if (isSelected) "Selected ${item.name}" else "Removed ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 92.dp
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(6.dp)

                if (index < 2) {
                    topMargin = 0
                }
            }

            binding.toppingContainer.addView(itemBinding.root, params)
        }
    }

    private fun updateToppingState(
        itemBinding: ItemExtraToppingBinding,
        isSelected: Boolean,
    ) {
        itemBinding.boxToppingCheck.setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_chip_selected_gold
            } else {
                R.drawable.bg_topping_checkbox
            }
        )
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCart.setOnClickListener {
            openCartScreen()
        }

        binding.btnFavorite.setOnClickListener {
            isFavorite = !isFavorite

            binding.imgFavorite.setImageResource(
                if (isFavorite) {
                    R.drawable.ic_heart
                } else {
                    R.drawable.ic_empty_heart
                }
            )
        }

        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
            }
        }

        binding.btnPlus.setOnClickListener {
            quantity++
            binding.tvQuantity.text = quantity.toString()
        }

        binding.btnAddToCart.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Added $quantity ${pizzaDetail.name}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnCustomizeToppings.setOnClickListener {
            Toast.makeText(requireContext(), "Profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}