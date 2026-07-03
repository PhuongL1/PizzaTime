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
import com.devpro.pizzatime.feature.customer.favorites.CustomerFavoritesFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.google.firebase.auth.FirebaseAuth

class PizzaDetailFragment : Fragment() {

    private var _binding: FragmentPizzaDetailBinding? = null
    private val binding: FragmentPizzaDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentPizzaDetailBinding is only valid between onCreateView and onDestroyView."
        }

    private var quantity = 1
    private var isFavorite = false

    private lateinit var pizzaDetail: PizzaDetailUiModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPizzaDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pizzaDetail = buildPizzaDetail()
        bindPizzaDetail(pizzaDetail)
        renderToppings(pizzaDetail.toppings)
        setupActions()
        loadFavoriteState()
    }

    private fun buildPizzaDetail(): PizzaDetailUiModel {
        val args = arguments ?: return FakePizzaDetailData.truffleNoir
        val name = args.getString(ARG_NAME).orEmpty()
        if (name.isBlank()) return FakePizzaDetailData.truffleNoir
        return PizzaDetailUiModel(
            id = args.getString(ARG_PRODUCT_ID, ""),
            name = name,
            description = args.getString(ARG_DESCRIPTION, ""),
            price = args.getString(ARG_PRICE, ""),
            rating = args.getString(ARG_RATING, "0.0"),
            time = "25-30 MIN",
            kcal = "840 KCAL",
            imageRes = R.drawable.img_welcome_hero,
            toppings = FakePizzaDetailData.truffleNoir.toppings,
        )
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
            toggleFavorite()
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

    private fun loadFavoriteState() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val productId = pizzaDetail.id
        if (productId.isBlank()) return

        CustomerFavoritesFirestoreRepository.isFavorite(uid, productId) { result ->
            if (_binding == null) return@isFavorite
            result.onSuccess { favorite ->
                isFavorite = favorite
                bindFavoriteIcon()
            }
        }
    }

    private fun toggleFavorite() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val productId = pizzaDetail.id
        if (uid == null) {
            Toast.makeText(requireContext(), R.string.customer_favorites_login_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (productId.isBlank()) {
            Toast.makeText(requireContext(), R.string.customer_favorites_update_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val nextFavorite = !isFavorite
        val updateCallback: (Result<Unit>) -> Unit = callback@{ result ->
            if (_binding == null) return@callback
            result
                .onSuccess {
                    isFavorite = nextFavorite
                    bindFavoriteIcon()
                    Toast.makeText(
                        requireContext(),
                        if (isFavorite) {
                            getString(R.string.customer_favorites_saved_toast, pizzaDetail.name)
                        } else {
                            getString(R.string.customer_favorites_removed_toast, pizzaDetail.name)
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure {
                    Toast.makeText(requireContext(), R.string.customer_favorites_update_failed, Toast.LENGTH_SHORT).show()
                }
        }

        if (nextFavorite) {
            CustomerFavoritesFirestoreRepository.addFavorite(uid, productId, updateCallback)
        } else {
            CustomerFavoritesFirestoreRepository.removeFavorite(uid, productId, updateCallback)
        }
    }

    private fun bindFavoriteIcon() {
        binding.imgFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_heart else R.drawable.ic_empty_heart,
        )
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"
        private const val ARG_NAME = "name"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_PRICE = "price"
        private const val ARG_RATING = "rating"

        fun newInstance(
            productId: String,
            name: String,
            description: String,
            price: String,
            rating: String,
        ) = PizzaDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PRODUCT_ID, productId)
                putString(ARG_NAME, name)
                putString(ARG_DESCRIPTION, description)
                putString(ARG_PRICE, price)
                putString(ARG_RATING, rating)
            }
        }
    }
}
