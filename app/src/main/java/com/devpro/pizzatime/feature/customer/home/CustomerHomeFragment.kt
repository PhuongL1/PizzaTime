package com.devpro.pizzatime.feature.customer.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.databinding.FragmentCustomerHomeBinding
import com.devpro.pizzatime.databinding.ItemBestSellerPizzaBinding
import com.devpro.pizzatime.databinding.ItemChefSelectionPizzaBinding
import com.devpro.pizzatime.feature.customer.account.CustomerProfileFirestoreRepository
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.favorites.CustomerFavoritesFirestoreRepository
import com.devpro.pizzatime.feature.customer.menu.FirebaseProductRepository
import com.devpro.pizzatime.feature.customer.menu.ProductUiModel
import com.devpro.pizzatime.feature.staff.navigation.openPizzaDetailScreen
import com.devpro.pizzatime.feature.staff.navigation.openPizzaMenuScreen
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class CustomerHomeFragment : Fragment() {

    private var _binding: FragmentCustomerHomeBinding? = null
    private val binding: FragmentCustomerHomeBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerHomeBinding is only valid between onCreateView and onDestroyView."
        }

    private var allAvailableProducts: List<ProductUiModel> = emptyList()
    private var bestSellingProductIds: List<String> = emptyList()
    private var favoriteProductIds: Set<String> = emptySet()
    private var selectedCategory: HomeCategory = HomeCategory.ALL
    private var searchQuery: String = ""
    private var featuredProduct: ProductUiModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSearch()
        setupCategories()
        setupActions()
        loadHomeProfile()
        loadFavoriteProductIds()
        loadHomeData()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadHomeProfile()
            loadFavoriteProductIds()
        }
    }

    private fun loadHomeData() {
        FirebaseProductRepository.loadProducts { products ->
            if (_binding == null) return@loadProducts
            allAvailableProducts = products
            renderFilteredProducts()
            loadBestSellingProducts()
        }
    }

    private fun setupSearch() = with(binding.tvSearchHint) {
        addTextChangedListener { editable ->
            searchQuery = editable?.toString().orEmpty()
            renderFilteredProducts()
        }
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                clearFocus()
                true
            } else {
                false
            }
        }
    }

    private fun setupCategories() = with(binding) {
        bindCategoryChip(chipAll, HomeCategory.ALL)
        bindCategoryChip(chipPizza, HomeCategory.PIZZA)
        bindCategoryChip(chipCombo, HomeCategory.COMBO)
        bindCategoryChip(chipDrinks, HomeCategory.DRINKS)
        bindCategoryChip(chipDessert, HomeCategory.DESSERT)
        renderCategoryState()
    }

    private fun bindCategoryChip(
        chip: TextView,
        category: HomeCategory,
    ) {
        chip.setOnClickListener {
            selectedCategory = category
            renderCategoryState()
            renderFilteredProducts()
        }
    }

    private fun renderCategoryState() = with(binding) {
        chipAll.renderCategoryChip(HomeCategory.ALL)
        chipPizza.renderCategoryChip(HomeCategory.PIZZA)
        chipCombo.renderCategoryChip(HomeCategory.COMBO)
        chipDrinks.renderCategoryChip(HomeCategory.DRINKS)
        chipDessert.renderCategoryChip(HomeCategory.DESSERT)
    }

    private fun TextView.renderCategoryChip(category: HomeCategory) {
        val selected = selectedCategory == category
        setBackgroundResource(
            if (selected) R.drawable.bg_chip_selected_gold else R.drawable.bg_chip_unselected_dark,
        )
        setTextColor(
            requireContext().getColor(
                if (selected) R.color.pt_text_dark else R.color.pt_text_primary,
            ),
        )
    }

    private fun setupActions() {
        binding.btnHomeAvatar.setOnClickListener {
            binding.tvSearchHint.requestFocus()
            showKeyboard(binding.tvSearchHint)
        }
        binding.searchBar.setOnClickListener {
            binding.tvSearchHint.requestFocus()
            showKeyboard(binding.tvSearchHint)
        }

        binding.promoCard.setOnClickListener {
            openFeaturedProductOrToast()
        }

        binding.btnPromoAction.setOnClickListener {
            openFeaturedProductOrToast()
        }

        bindCustomerBottomNav(
            root = binding.bottomNav.root,
            selectedTab = CustomerBottomNavTab.MENU,
        )

        binding.tvSeeAll.setOnClickListener {
            openPizzaMenuScreen()
        }
    }

    private fun loadHomeProfile() {
        binding.tvHomeLocation.text = formatShortAddress(
            binding.tvHomeLocation.text?.toString().orEmpty(),
        )

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            binding.btnHomeAvatar.setImageResource(R.drawable.bg_avatar)
            return
        }

        CustomerProfileFirestoreRepository.loadProfile(uid) { result ->
            if (_binding == null) return@loadProfile
            result.onSuccess { profile ->
                val address = profile.deliveryAddress.trim()
                if (address.isNotBlank()) {
                    binding.tvHomeLocation.text = formatShortAddress(address)
                }
                binding.btnHomeAvatar.loadProductImage(
                    profile.avatarUrl,
                    R.drawable.bg_avatar,
                )
            }
        }
    }

    private fun loadFavoriteProductIds() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            favoriteProductIds = emptySet()
            renderFilteredProducts()
            return
        }

        CustomerFavoritesFirestoreRepository.loadFavoriteProductIds(uid) { result ->
            if (_binding == null) return@loadFavoriteProductIds
            favoriteProductIds = result.getOrDefault(emptyList()).toSet()
            renderFilteredProducts()
        }
    }

    private fun renderFilteredProducts() {
        featuredProduct = resolveFilteredProducts().firstOrNull() ?: allAvailableProducts.firstOrNull()
        renderFeaturedProduct()
        renderBestSellers(
                resolveBestSellerProducts()
                    .map { it.toBestSellerUiModel() },
            )
        renderChefSelections(resolveBestSellingProducts())
    }

    private fun renderFeaturedProduct() {
        val product = featuredProduct
        if (product == null) {
            binding.imgPromoPizza.setImageResource(R.drawable.img_welcome_hero)
            binding.tvPromoTitle.setText(R.string.home_promo_title)
            return
        }

        binding.imgPromoPizza.loadProductImage(product.imageUrl, R.drawable.img_welcome_hero)
        binding.imgPromoPizza.contentDescription = product.name
        binding.tvPromoTitle.text = product.name
    }

    private fun loadBestSellingProducts() {
        CustomerHomeBestSellerRepository.loadBestSellingProductIds { result ->
            if (_binding == null) return@loadBestSellingProductIds
            result
                .onSuccess { productIds ->
                    bestSellingProductIds = productIds.take(BEST_SELLER_LIMIT)
                    renderFilteredProducts()
                }
                .onFailure {
                    renderFilteredProducts()
                }
        }
    }

    private fun resolveBestSellingProducts(): List<ChefPizzaUiModel> {
        val filteredProducts = resolveFilteredProducts()
        val productsById = filteredProducts.associateBy { it.id }
        val fallbackProducts = filteredProducts.sortedWith(
            compareByDescending<ProductUiModel> { it.rating }
                .thenBy { it.name.lowercase(Locale.US) },
        )
        val rankedProducts = bestSellingProductIds.mapNotNull { productsById[it] }
        return (rankedProducts + fallbackProducts)
            .distinctBy { it.id }
            .take(CHEF_SELECTION_LIMIT)
            .mapIndexed { index, product -> product.toChefPizzaUiModel(index + 1) }
    }

    private fun resolveBestSellerProducts(): List<ProductUiModel> {
        val filteredProducts = resolveFilteredProducts()
        val productsById = filteredProducts.associateBy { it.id }
        val fallbackProducts = filteredProducts.sortedWith(
            compareByDescending<ProductUiModel> { it.rating }
                .thenBy { it.name.lowercase(Locale.US) },
        )
        val rankedProducts = bestSellingProductIds.mapNotNull { productsById[it] }
        return (rankedProducts + fallbackProducts)
            .distinctBy { it.id }
            .take(BEST_SELLER_LIMIT)
    }

    private fun resolveFilteredProducts(): List<ProductUiModel> {
        return allAvailableProducts.filter { product ->
            product.matchesCategory(selectedCategory) && product.matchesSearch(searchQuery)
        }
    }

    private fun openFeaturedProductOrToast() {
        val product = featuredProduct
        if (product == null) {
            Toast.makeText(
                requireContext(),
                R.string.home_no_featured_product,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        openProductDetail(product)
    }

    private fun renderBestSellers(items: List<BestSellerPizzaUiModel>) {
        binding.bestSellerContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemBestSellerPizzaBinding.inflate(
                layoutInflater,
                binding.bestSellerContainer,
                false,
            )

            itemBinding.imgPizza.loadProductImage(item.imageUrl, item.imageRes)
            itemBinding.imgPizza.contentDescription = item.name
            itemBinding.tvPizzaName.text = item.name
            itemBinding.tvPizzaDescription.text = item.description
            itemBinding.tvPizzaPrice.text = item.price
            bindBestSellerFavoriteIcon(itemBinding, item.isFavorite)

            itemBinding.root.setOnClickListener {
                openProductDetail(item)
            }

            itemBinding.btnFavorite.setOnClickListener {
                itemBinding.btnFavorite.isEnabled = false
                toggleBestSellerFavorite(item) { isFavorite ->
                    if (_binding == null) return@toggleBestSellerFavorite
                    bindBestSellerFavoriteIcon(itemBinding, isFavorite)
                    itemBinding.btnFavorite.isEnabled = true
                }
            }

            itemBinding.btnAddToCart.setOnClickListener {
                openProductDetail(item)
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                178.dp,
                260.dp,
            ).apply {
                if (index > 0) {
                    marginStart = 16.dp
                }
            }

            binding.bestSellerContainer.addView(itemBinding.root)
        }
    }

    private fun renderChefSelections(items: List<ChefPizzaUiModel>) {
        binding.chefSelectionContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemChefSelectionPizzaBinding.inflate(
                layoutInflater,
                binding.chefSelectionContainer,
                false,
            )

            itemBinding.imgChefPizza.loadProductImage(item.imageUrl, item.imageRes)
            itemBinding.imgChefPizza.contentDescription = item.name
            itemBinding.tvChefLabel.text = item.label
            itemBinding.tvChefName.text = item.name
            itemBinding.tvChefDescription.text = item.description
            itemBinding.tvChefPrice.text = item.price
            itemBinding.tvChefRating.text = item.rating

            itemBinding.root.setOnClickListener {
                openProductDetail(item)
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                300.dp,
                360.dp,
            ).apply {
                if (index > 0) {
                    marginStart = 18.dp
                }
            }

            binding.chefSelectionContainer.addView(itemBinding.root)
        }
    }

    private fun openProductDetail(product: ProductUiModel) {
        openPizzaDetailScreen(
            productId = product.id,
            productName = product.name,
            productDescription = product.description,
            productPrice = product.priceText,
            productRating = product.ratingText,
            productImageUrl = product.imageUrl,
            productCategoryId = product.categoryId,
            productCategoryName = product.categoryName,
            productSizeOptions = product.sizeOptions,
            productCrustOptions = product.crustOptions,
            productToppingOptions = product.toppingOptions,
        )
    }

    private fun openProductDetail(item: BestSellerPizzaUiModel) {
        openPizzaDetailScreen(
            productId = item.id,
            productName = item.name,
            productDescription = item.description,
            productPrice = item.price,
            productRating = item.rating,
            productImageUrl = item.imageUrl,
            productCategoryId = item.categoryId,
            productCategoryName = item.categoryName,
            productSizeOptions = item.sizeOptions,
            productCrustOptions = item.crustOptions,
            productToppingOptions = item.toppingOptions,
        )
    }

    private fun openProductDetail(item: ChefPizzaUiModel) {
        openPizzaDetailScreen(
            productId = item.id,
            productName = item.name,
            productDescription = item.description,
            productPrice = item.price,
            productRating = item.rawRating,
            productImageUrl = item.imageUrl,
            productCategoryId = item.categoryId,
            productCategoryName = item.categoryName,
            productSizeOptions = item.sizeOptions,
            productCrustOptions = item.crustOptions,
            productToppingOptions = item.toppingOptions,
        )
    }

    private fun ProductUiModel.matchesCategory(category: HomeCategory): Boolean {
        if (category == HomeCategory.ALL) return true
        val categoryText = listOf(categoryId, categoryName)
            .joinToString(separator = " ")
            .lowercase(Locale.US)
        return category.matchTerms.any { term -> categoryText.contains(term) }
    }

    private fun ProductUiModel.matchesSearch(query: String): Boolean {
        val normalizedQuery = query.trim().lowercase(Locale.US)
        if (normalizedQuery.isBlank()) return true
        val searchableText = listOf(name, description, categoryId, categoryName)
            .joinToString(separator = " ")
            .lowercase(Locale.US)
        return searchableText.contains(normalizedQuery)
    }

    private val ProductUiModel.priceText: String
        get() = String.format(Locale.US, "$%.2f", basePrice)

    private val ProductUiModel.ratingText: String
        get() = String.format(Locale.US, "%.1f", rating)

    private fun ProductUiModel.toBestSellerUiModel() = BestSellerPizzaUiModel(
        id = id,
        name = name,
        description = description.shortQuickInfo(),
        price = priceText,
        rating = ratingText,
        imageRes = R.drawable.img_welcome_hero,
        imageUrl = imageUrl,
        isFavorite = id in favoriteProductIds,
        categoryId = categoryId,
        categoryName = categoryName,
        sizeOptions = sizeOptions,
        crustOptions = crustOptions,
        toppingOptions = toppingOptions,
    )

    private fun String.shortQuickInfo(): String {
        val compact = trim().replace(Regex("\\s+"), " ")
        return if (compact.length <= QUICK_INFO_MAX_LENGTH) {
            compact
        } else {
            compact.take(QUICK_INFO_MAX_LENGTH).trimEnd() + "..."
        }
    }

    private fun formatShortAddress(address: String): String {
        val trimmed = address.trim()
        if (trimmed.isBlank()) return getString(R.string.home_location)

        val components = trimmed
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (components.size >= 2) {
            components.takeLast(2).joinToString(", ")
        } else {
            trimmed
        }
    }

    private fun ProductUiModel.toChefPizzaUiModel(rank: Int) = ChefPizzaUiModel(
        id = id,
        name = name,
        description = description,
        price = priceText,
        label = getString(R.string.home_chef_rank_label, rank),
        rating = getString(R.string.home_rating_label, ratingText),
        rawRating = ratingText,
        imageRes = R.drawable.img_welcome_hero,
        imageUrl = imageUrl,
        categoryId = categoryId,
        categoryName = categoryName,
        sizeOptions = sizeOptions,
        crustOptions = crustOptions,
        toppingOptions = toppingOptions,
    )

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(binding.tvSearchHint.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                as? InputMethodManager
        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun bindBestSellerFavoriteIcon(
        itemBinding: ItemBestSellerPizzaBinding,
        isFavorite: Boolean,
    ) {
        itemBinding.imgFavoriteIcon.setImageResource(
            if (isFavorite) R.drawable.ic_heart else R.drawable.ic_empty_heart,
        )
    }

    private fun toggleBestSellerFavorite(
        item: BestSellerPizzaUiModel,
        onComplete: (Boolean) -> Unit,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.customer_favorites_login_required, Toast.LENGTH_SHORT).show()
            onComplete(item.id in favoriteProductIds)
            return
        }
        if (item.id.isBlank()) {
            Toast.makeText(requireContext(), R.string.customer_favorites_update_failed, Toast.LENGTH_SHORT).show()
            onComplete(item.id in favoriteProductIds)
            return
        }

        val isFavorite = item.id in favoriteProductIds
        val callback: (Result<Unit>) -> Unit = callback@{ result ->
            if (_binding == null) return@callback
            result
                .onSuccess {
                    favoriteProductIds = if (isFavorite) {
                        favoriteProductIds - item.id
                    } else {
                        favoriteProductIds + item.id
                    }
                    onComplete(!isFavorite)
                    renderFilteredProducts()
                    Toast.makeText(
                        requireContext(),
                        if (isFavorite) {
                            getString(R.string.customer_favorites_removed_toast, item.name)
                        } else {
                            getString(R.string.customer_favorites_saved_toast, item.name)
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure {
                    Toast.makeText(requireContext(), R.string.customer_favorites_update_failed, Toast.LENGTH_SHORT).show()
                    onComplete(isFavorite)
                }
        }

        if (isFavorite) {
            CustomerFavoritesFirestoreRepository.removeFavorite(uid, item.id, callback)
        } else {
            CustomerFavoritesFirestoreRepository.addFavorite(uid, item.id, callback)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private enum class HomeCategory(
        val matchTerms: Set<String>,
    ) {
        ALL(emptySet()),
        PIZZA(setOf("pizza", "signature", "classic", "veggie")),
        COMBO(setOf("combo")),
        DRINKS(setOf("drink", "drinks", "beverage")),
        DESSERT(setOf("dessert", "desserts")),
    }

    private companion object {
        const val CHEF_SELECTION_LIMIT = 5
        const val BEST_SELLER_LIMIT = 3
        const val QUICK_INFO_MAX_LENGTH = 58
    }
}
