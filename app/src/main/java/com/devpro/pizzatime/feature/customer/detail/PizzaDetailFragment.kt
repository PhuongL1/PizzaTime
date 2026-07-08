package com.devpro.pizzatime.feature.customer.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.product.ProductOptionDefaults
import com.devpro.pizzatime.databinding.FragmentPizzaDetailBinding
import com.devpro.pizzatime.databinding.ItemExtraToppingBinding
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.common.navigation.bindPizzaFlowTopBar
import com.devpro.pizzatime.feature.customer.common.navigation.updatePizzaFlowCartBadge
import com.devpro.pizzatime.feature.customer.favorites.CustomerFavoritesFirestoreRepository
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class PizzaDetailFragment : Fragment() {

    private var _binding: FragmentPizzaDetailBinding? = null
    private val binding: FragmentPizzaDetailBinding
        get() = checkNotNull(_binding) {
            "FragmentPizzaDetailBinding is only valid between onCreateView and onDestroyView."
        }

    private var quantity = 1
    private var isFavorite = false
    private var selectedSize = ""
    private var selectedCrust = ""
    private var basePrice = 0.0
    private val selectedToppings = linkedSetOf<String>()
    private var productCategory = ProductCategory.UNKNOWN

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
        productCategory = resolveProductCategory(
            categoryId = pizzaDetail.categoryId,
            categoryName = pizzaDetail.categoryName,
        )
        setupTopBar()
        bindPizzaDetail(pizzaDetail)
        renderProductOptions()
        setupActions()
        loadFavoriteState()
        updateCartBadge()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupTopBar()
            updateCartBadge()
            updateAddToCartLabel()
        }
    }

    private fun setupTopBar() {
        bindPizzaFlowTopBar(
            root = binding.pizzaTopBar.root,
            cartItemCount = CartStore.items.sumOf { it.quantity },
            onBackClick = { parentFragmentManager.popBackStack() },
            onCartClick = { openCartScreen() },
        )
    }

    private fun buildPizzaDetail(): PizzaDetailUiModel {
        val args = arguments ?: return FakePizzaDetailData.truffleNoir
        val name = args.getString(ARG_NAME).orEmpty()
        if (name.isBlank()) return FakePizzaDetailData.truffleNoir
        basePrice = parsePrice(args.getString(ARG_PRICE, ""))
        return PizzaDetailUiModel(
            id = args.getString(ARG_PRODUCT_ID, ""),
            name = name,
            description = args.getString(ARG_DESCRIPTION, ""),
            price = args.getString(ARG_PRICE, ""),
            rating = args.getString(ARG_RATING, getString(R.string.no_ratings)),
            categoryId = args.getString(ARG_CATEGORY_ID, ""),
            categoryName = args.getString(ARG_CATEGORY_NAME, ""),
            time = "25-30 MIN",
            kcal = "840 KCAL",
            imageRes = R.drawable.img_welcome_hero,
            imageUrl = args.getString(ARG_IMAGE_URL, ""),
            toppings = args.getStringArrayList(ARG_TOPPING_OPTIONS)
                .orEmpty()
                .map { topping ->
                    ExtraToppingUiModel(
                        id = topping.lowercase(Locale.US).replace(Regex("\\s+"), "_"),
                        name = topping,
                        price = "",
                    )
                },
            sizeOptions = args.getStringArrayList(ARG_SIZE_OPTIONS).orEmpty(),
            crustOptions = args.getStringArrayList(ARG_CRUST_OPTIONS).orEmpty(),
        )
    }

    private fun renderProductOptions() {
        when (productCategory) {
            ProductCategory.PIZZA -> {
                binding.sizeSection.isVisible = true
                binding.crustSection.isVisible = true
                binding.toppingsSection.isVisible = true
                renderSizeOptions(pizzaDetail.sizeOptions)
                renderCrustOptions(pizzaDetail.crustOptions)
                renderToppings(pizzaDetail.toppings)
            }

            ProductCategory.DRINK -> {
                clearCrustAndToppingsSelection()
                binding.sizeSection.isVisible = true
                binding.crustSection.isVisible = false
                binding.toppingsSection.isVisible = false
                renderSizeOptions(pizzaDetail.sizeOptions)
                updateAddToCartLabel()
            }

            ProductCategory.COMBO,
            ProductCategory.DESSERT,
            ProductCategory.UNKNOWN,
            -> {
                clearAllCustomizations()
                binding.sizeSection.isVisible = false
                binding.crustSection.isVisible = false
                binding.toppingsSection.isVisible = false
                updateAddToCartLabel()
            }
        }
    }

    private fun bindPizzaDetail(item: PizzaDetailUiModel) {
        binding.imgPizzaHero.loadProductImage(item.imageUrl, item.imageRes)
        binding.imgPizzaHero.contentDescription = item.name
        binding.tvPizzaName.text = item.name
        binding.tvPizzaDescription.text = item.description
        binding.tvRating.text = formatRatingText(item.rating)
        binding.tvTime.text = item.time
        binding.tvKcal.text = item.kcal
        binding.tvQuantity.text = quantity.toString()
        updateAddToCartLabel()
    }

    private fun renderSizeOptions(options: List<String>) {
        val sizes = ProductOptionDefaults.sizesOrDefault(options)
            .filter { option ->
                option.equals("Small", ignoreCase = true) ||
                    option.equals("Medium", ignoreCase = true) ||
                    option.equals("Large", ignoreCase = true)
            }
            .ifEmpty { ProductOptionDefaults.sizeOptions }
        val sizeViews = listOf(
            binding.btnSizeSmall,
            binding.btnSizeMedium,
            binding.btnSizeLarge,
        )
        if (selectedSize !in sizes) {
            selectedSize = sizes.firstOrNull().orEmpty()
        }

        sizeViews.forEachIndexed { index, view ->
            val size = sizes.getOrNull(index)
            view.isVisible = size != null
            if (size != null) {
                view.text = formatSizeLabel(size)
                bindSizeState(view, selectedSize == size)
                view.setOnClickListener {
                    selectedSize = size
                    renderSizeOptions(sizes)
                    updateAddToCartLabel()
                }
            }
        }
        updateAddToCartLabel()
    }

    private fun renderCrustOptions(options: List<String>) {
        val crusts = ProductOptionDefaults.crustsOrDefault(options)
        val crustViews = listOf(
            binding.btnCrustClassic,
            binding.btnCrustThin,
            binding.btnCrustThick,
        )
        if (selectedCrust !in crusts) {
            selectedCrust = crusts.firstOrNull().orEmpty()
        }

        crustViews.forEachIndexed { index, view ->
            val crust = crusts.getOrNull(index)
            view.isVisible = crust != null
            if (crust != null) {
                view.text = crust
                bindCrustState(view, selectedCrust == crust)
                view.setOnClickListener {
                    selectedCrust = crust
                    renderCrustOptions(crusts)
                }
            }
        }
    }

    private fun renderToppings(items: List<ExtraToppingUiModel>) {
        binding.toppingContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemExtraToppingBinding.inflate(
                layoutInflater,
                binding.toppingContainer,
                false,
            )

            itemBinding.tvToppingName.text = item.name
            itemBinding.tvToppingPrice.text = item.price

            var isSelected = selectedToppings.contains(item.name) || item.isSelected
            updateToppingState(itemBinding, isSelected)

            itemBinding.root.setOnClickListener {
                isSelected = !isSelected
                if (isSelected) {
                    selectedToppings.add(item.name)
                } else {
                    selectedToppings.remove(item.name)
                }
                updateToppingState(itemBinding, isSelected)
                updateAddToCartLabel()

                Toast.makeText(
                    requireContext(),
                    if (isSelected) "Selected ${item.name}" else "Removed ${item.name}",
                    Toast.LENGTH_SHORT,
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
        updateAddToCartLabel()
    }

    private fun bindSizeState(view: TextView, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) R.drawable.bg_size_option_selected else R.drawable.bg_size_option_unselected,
        )
        view.setTextColor(
            requireContext().getColor(
                if (selected) R.color.pt_text_dark else R.color.pt_text_primary,
            ),
        )
    }

    private fun bindCrustState(view: TextView, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) R.drawable.bg_crust_selected else R.drawable.bg_crust_unselected,
        )
        view.setTextColor(
            requireContext().getColor(
                if (selected) R.color.pt_text_dark else R.color.pt_text_primary,
            ),
        )
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
            },
        )
    }

    private fun setupActions() {
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        binding.btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
                updateAddToCartLabel()
            }
        }

        binding.btnPlus.setOnClickListener {
            quantity++
            binding.tvQuantity.text = quantity.toString()
            updateAddToCartLabel()
        }

        binding.btnAddToCart.setOnClickListener {
            val unitPrice = computeCurrentUnitPrice()
            CartStore.addItem(
                CartItemUiModel(
                    id = pizzaDetail.id,
                    name = pizzaDetail.name,
                    description = pizzaDetail.description,
                    price = unitPrice,
                    quantity = quantity,
                    imageRes = pizzaDetail.imageRes,
                    selectedSize = selectedCartSize(),
                    selectedCrust = selectedCartCrust(),
                    selectedToppings = selectedCartToppings(),
                    imageUrl = pizzaDetail.imageUrl,
                ),
            )
            updateCartBadge()
            Toast.makeText(
                requireContext(),
                "Added $quantity ${pizzaDetail.name}",
                Toast.LENGTH_SHORT,
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

    private fun parsePrice(price: String): Double {
        return price
            .replace("$", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun updateAddToCartLabel() {
        val totalPrice = computeCurrentUnitPrice() * quantity
        binding.btnAddToCart.text = getString(
            R.string.detail_add_to_cart_format,
            formatPrice(totalPrice),
        )
    }

    private fun computeCurrentUnitPrice(): Double {
        return when (productCategory) {
            ProductCategory.PIZZA -> {
                val toppingPrice = selectedToppings.size * TOPPING_PRICE
                basePrice * sizeMultiplier(selectedSize) + toppingPrice
            }

            ProductCategory.DRINK -> basePrice * sizeMultiplier(selectedSize)
            ProductCategory.COMBO,
            ProductCategory.DESSERT,
            ProductCategory.UNKNOWN,
            -> basePrice
        }
    }

    private fun updateCartBadge() = with(binding) {
        val count = CartStore.items.sumOf { it.quantity }
        updatePizzaFlowCartBadge(
            root = pizzaTopBar.root,
            cartItemCount = count,
        )
    }

    private fun sizeMultiplier(size: String): Double {
        return when (size.trim().lowercase(Locale.US)) {
            "medium" -> 1.12
            "large" -> 1.20
            else -> 1.0
        }
    }

    private fun formatSizeLabel(size: String): String {
        return when (size.trim().lowercase(Locale.US)) {
            "small" -> getString(
                R.string.detail_size_small_display,
                getString(R.string.small),
                sizeSubtitleFor("small"),
            )

            "medium" -> getString(
                R.string.detail_size_medium_display,
                getString(R.string.medium),
                sizeSubtitleFor("medium"),
            )

            "large" -> getString(
                R.string.detail_size_large_display,
                getString(R.string.large),
                sizeSubtitleFor("large"),
            )

            else -> size
        }
    }

    private fun sizeSubtitleFor(size: String): String {
        return when (productCategory) {
            ProductCategory.DRINK -> when (size.lowercase(Locale.US)) {
                "small" -> getString(R.string.detail_size_small_ml)
                "medium" -> getString(R.string.detail_size_medium_ml)
                "large" -> getString(R.string.detail_size_large_ml)
                else -> ""
            }

            else -> when (size.lowercase(Locale.US)) {
                "small" -> getString(R.string.ten_inches)
                "medium" -> getString(R.string.twelve_inches)
                "large" -> getString(R.string.fourteen_inches)
                else -> ""
            }
        }
    }

    private fun selectedCartSize(): String {
        return if (productCategory == ProductCategory.PIZZA || productCategory == ProductCategory.DRINK) {
            selectedSize
        } else {
            ""
        }
    }

    private fun selectedCartCrust(): String {
        return if (productCategory == ProductCategory.PIZZA) selectedCrust else ""
    }

    private fun selectedCartToppings(): List<String> {
        return if (productCategory == ProductCategory.PIZZA) {
            selectedToppings.toList()
        } else {
            emptyList()
        }
    }

    private fun clearCrustAndToppingsSelection() {
        selectedCrust = ""
        selectedToppings.clear()
    }

    private fun clearAllCustomizations() {
        selectedSize = ""
        clearCrustAndToppingsSelection()
    }

    private fun resolveProductCategory(
        categoryId: String,
        categoryName: String,
    ): ProductCategory {
        val normalized = listOf(categoryId, categoryName)
            .joinToString(separator = " ")
            .trim()
            .lowercase(Locale.US)
        return when {
            normalized.contains("pizza") -> ProductCategory.PIZZA
            normalized.contains("drink") || normalized.contains("beverage") -> ProductCategory.DRINK
            normalized.contains("combo") -> ProductCategory.COMBO
            normalized.contains("dessert") -> ProductCategory.DESSERT
            else -> ProductCategory.UNKNOWN
        }
    }

    private fun formatRatingText(rating: String): String {
        val value = rating.trim()
        if (value.isBlank() || value == "0.0" || value == "0" || value.equals(getString(R.string.no_ratings), true)) {
            return getString(R.string.no_ratings)
        }
        return getString(R.string.detail_rating_format, value)
    }

    private fun formatPrice(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

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
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_CATEGORY_ID = "category_id"
        private const val ARG_CATEGORY_NAME = "category_name"
        private const val ARG_SIZE_OPTIONS = "size_options"
        private const val ARG_CRUST_OPTIONS = "crust_options"
        private const val ARG_TOPPING_OPTIONS = "topping_options"
        private const val TOPPING_PRICE = 0.49

        fun newInstance(
            productId: String,
            name: String,
            description: String,
            price: String,
            rating: String,
            imageUrl: String = "",
            categoryId: String = "",
            categoryName: String = "",
            sizeOptions: List<String> = emptyList(),
            crustOptions: List<String> = emptyList(),
            toppingOptions: List<String> = emptyList(),
        ) = PizzaDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PRODUCT_ID, productId)
                putString(ARG_NAME, name)
                putString(ARG_DESCRIPTION, description)
                putString(ARG_PRICE, price)
                putString(ARG_RATING, rating)
                putString(ARG_IMAGE_URL, imageUrl)
                putString(ARG_CATEGORY_ID, categoryId)
                putString(ARG_CATEGORY_NAME, categoryName)
                putStringArrayList(ARG_SIZE_OPTIONS, ArrayList(sizeOptions))
                putStringArrayList(ARG_CRUST_OPTIONS, ArrayList(crustOptions))
                putStringArrayList(ARG_TOPPING_OPTIONS, ArrayList(toppingOptions))
            }
        }
    }

    private enum class ProductCategory {
        PIZZA,
        DRINK,
        COMBO,
        DESSERT,
        UNKNOWN,
    }
}
