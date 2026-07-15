package com.devpro.pizzatime.feature.customer.favorites

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.product.ProductOptionDefaults
import com.devpro.pizzatime.core.ui.message.UiMessage
import com.devpro.pizzatime.core.ui.message.UiMessageType
import com.devpro.pizzatime.core.ui.message.UiText
import com.devpro.pizzatime.core.ui.message.showUiMessage
import com.devpro.pizzatime.databinding.FragmentCustomerFavoritesBinding
import com.devpro.pizzatime.databinding.ItemCustomerFavoriteCompactBinding
import com.devpro.pizzatime.databinding.ItemCustomerFavoriteFeaturedBinding
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerTopBar
import com.devpro.pizzatime.feature.customer.menu.ProductUiModel
import com.devpro.pizzatime.feature.staff.navigation.openPizzaDetailScreen
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import kotlin.math.roundToInt

class CustomerFavoritesFragment : Fragment() {

    private var _binding: FragmentCustomerFavoritesBinding? = null
    private val binding: FragmentCustomerFavoritesBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerFavoritesBinding is only valid between onCreateView and onDestroyView."
        }

    private var favoritesData: CustomerFavoritesUiModel = CustomerFavoritesUiModel(
        title = "",
        subtitle = "",
        favorites = emptyList(),
        pairing = CustomerFavoritePairingUiModel(
            title = "",
            subtitle = "",
        ),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindHeader()
        bindPairing()
        setupTopBar()
        setupBottomNav()
        loadFirestoreFavorites()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            setupTopBar()
            loadFirestoreFavorites()
        }
    }

    private fun loadFirestoreFavorites() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            favoritesData = favoritesData.copy(favorites = emptyList())
            renderFavorites(getString(R.string.customer_favorites_view_login_required))
            return
        }

        CustomerFavoritesFirestoreRepository.loadFavoriteProducts(uid) { result ->
            if (_binding == null) return@loadFavoriteProducts
            result
                .onSuccess { favorites ->
                    favoritesData = favoritesData.copy(favorites = favorites)
                    renderFavorites(getString(R.string.no_favorites_yet))
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load favorites", error)
                    favoritesData = favoritesData.copy(favorites = emptyList())
                    renderFavorites(getString(R.string.no_favorites_yet))
                    showFavoritesMessage(
                        textRes = R.string.feedback_action_failed,
                        type = UiMessageType.ERROR,
                    )
                }
        }
    }

    private fun bindHeader() = with(binding) {
        tvTitle.text = getString(R.string.customer_menu_title_favorites)
        tvSubtitle.text = getString(R.string.customer_menu_subtitle_favorites)
    }

    private fun renderFavorites(emptyMessage: String) = with(binding.favoritesContainer) {
        removeAllViews()

        if (favoritesData.favorites.isEmpty()) {
            addView(createEmptyStateView(emptyMessage))
            return
        }

        favoritesData.favorites.forEach { item ->
            val itemView = when (item.cardType) {
                CustomerFavoriteCardType.FEATURED -> createFeaturedCard(item)
                CustomerFavoriteCardType.COMPACT -> createCompactCard(item)
            }

            addView(
                itemView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = 16.dp()
                },
            )
        }
    }

    private fun createFeaturedCard(item: CustomerFavoriteItemUiModel): View {
        val itemBinding = ItemCustomerFavoriteFeaturedBinding.inflate(layoutInflater)

        itemBinding.tvFavoriteName.text = item.name
        val descriptionText = item.description.trim()
        itemBinding.tvFavoriteDescription.isVisible = descriptionText.isNotBlank()
        itemBinding.tvFavoriteDescription.text = descriptionText
        itemBinding.tvFavoritePrice.text = formatPrice(item.price)

        itemBinding.tvBadge.isVisible = item.badge != null
        itemBinding.tvBadge.text = item.badge.orEmpty()

        bindImage(
            imageView = itemBinding.ivFavoriteImage,
            imageUrl = item.imageUrl,
            imageRes = item.imageRes,
            showPlaceholder = { itemBinding.tvImagePlaceholder.isVisible = it },
        )

        itemBinding.btnAddToCart.setOnClickListener { buttonView ->
            handleAddToCart(item, buttonView)
        }

        itemBinding.btnHeart.setOnClickListener {
            removeFavorite(item)
        }

        itemBinding.root.setOnClickListener {
            openPizzaDetail(
                productId = item.id,
                name = item.name,
                description = item.description,
                price = formatPrice(item.price),
                rating = getString(R.string.no_ratings),
                imageUrl = item.imageUrl,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                sizeOptions = item.sizeOptions,
                crustOptions = item.crustOptions,
                toppingOptions = item.toppingOptions,
            )
        }

        return itemBinding.root
    }

    private fun createCompactCard(item: CustomerFavoriteItemUiModel): View {
        val itemBinding = ItemCustomerFavoriteCompactBinding.inflate(layoutInflater)

        itemBinding.tvFavoriteName.text = item.name
        itemBinding.tvFavoritePrice.text = formatPrice(item.price)

        bindImage(
            imageView = itemBinding.ivFavoriteImage,
            imageUrl = item.imageUrl,
            imageRes = item.imageRes,
            showPlaceholder = { itemBinding.tvImagePlaceholder.isVisible = it },
        )

        itemBinding.btnAdd.setOnClickListener { buttonView ->
            handleAddToCart(item, buttonView)
        }

        itemBinding.btnHeart.setOnClickListener {
            removeFavorite(item)
        }

        itemBinding.root.setOnClickListener {
            openPizzaDetail(
                productId = item.id,
                name = item.name,
                description = item.description,
                price = formatPrice(item.price),
                rating = getString(R.string.no_ratings),
                imageUrl = item.imageUrl,
                categoryId = item.categoryId,
                categoryName = item.categoryName,
                sizeOptions = item.sizeOptions,
                crustOptions = item.crustOptions,
                toppingOptions = item.toppingOptions,
            )
        }

        return itemBinding.root
    }

    private fun removeFavorite(item: CustomerFavoriteItemUiModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showFavoritesMessage(
                textRes = R.string.customer_favorites_view_login_required,
                type = UiMessageType.INFO,
            )
            return
        }

        CustomerFavoritesFirestoreRepository.removeFavorite(uid, item.id) { result ->
            if (_binding == null) return@removeFavorite
            result
                .onSuccess {
                    showFavoritesMessage(
                        textRes = R.string.customer_favorites_removed_toast,
                        type = UiMessageType.SUCCESS,
                        args = listOf(item.name),
                    )
                    loadFirestoreFavorites()
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to remove favorite productId=${item.id}", error)
                    showFavoritesMessage(
                        textRes = R.string.customer_favorites_update_failed,
                        type = UiMessageType.ERROR,
                    )
                }
        }
    }

    private fun handleAddToCart(
        item: CustomerFavoriteItemUiModel,
        buttonView: View,
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showFavoritesMessage(
                textRes = R.string.customer_favorites_view_login_required,
                type = UiMessageType.INFO,
            )
            return
        }

        buttonView.isEnabled = false
        resolveProductForCart(item) { result ->
            if (_binding == null) return@resolveProductForCart

            buttonView.isEnabled = true
            val resolvedProduct = result.getOrElse { error ->
                Log.e(TAG, "Failed to resolve favorite productId=${item.id} for cart", error)
                showFavoritesMessage(
                    textRes = R.string.feedback_action_failed,
                    type = UiMessageType.ERROR,
                )
                return@resolveProductForCart
            }
            if (resolvedProduct == null) {
                showFavoritesMessage(
                    textRes = R.string.customer_favorites_product_unavailable,
                    type = UiMessageType.ERROR,
                )
                return@resolveProductForCart
            }

            val cartItem = resolvedProduct.toCartItemOrNull()
            if (cartItem == null) {
                openPizzaDetail(resolvedProduct)
                return@resolveProductForCart
            }

            CartStore.addItem(cartItem)
            setupTopBar()
            showFavoritesMessage(
                textRes = R.string.customer_favorites_added_to_cart,
                type = UiMessageType.SUCCESS,
            )
        }
    }

    private fun resolveProductForCart(
        item: CustomerFavoriteItemUiModel,
        onResolved: (Result<ResolvedFavoriteProduct?>) -> Unit,
    ) {
        val localProduct = item.toResolvedProductOrNull()
        if (localProduct != null) {
            onResolved(Result.success(localProduct))
            return
        }

        val productId = item.id.trim()
        if (productId.isBlank()) {
            onResolved(Result.success(null))
            return
        }

        CustomerFavoritesFirestoreRepository.loadProduct(productId) { result ->
            if (_binding == null) return@loadProduct
            onResolved(
                result.map { product ->
                    product
                        ?.takeIf { it.available }
                        ?.toResolvedFavoriteProduct(
                            fallbackImageRes = item.imageRes ?: R.drawable.img_pizza_time,
                        )
                },
            )
        }
    }

    private fun bindImage(
        imageView: ImageView,
        imageUrl: String,
        imageRes: Int?,
        showPlaceholder: (Boolean) -> Unit,
    ) {
        if (imageRes != null) {
            imageView.loadProductImage(imageUrl, imageRes)
            showPlaceholder(false)
        } else {
            showPlaceholder(true)
        }
    }

    private fun bindPairing() = with(binding) {
        tvPairingTitle.text = getString(R.string.customer_menu_title_favorites)
        tvPairingSubtitle.text = getString(R.string.customer_menu_subtitle_favorites)
    }

    private fun setupTopBar() = with(binding) {
        bindCustomerTopBar(
            root = customerTopBar.root,
            cartItemCount = CartStore.items.sumOf { it.quantity },
        )
    }

    private fun setupBottomNav() = with(binding) {
        bindCustomerBottomNav(
            root = customerBottomNav.root,
            selectedTab = CustomerBottomNavTab.PROFILE,
        )
    }

    private fun createEmptyStateView(message: String): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 10.dp()
            }
            gravity = android.view.Gravity.CENTER
            text = message
            textSize = 15f
            setTextColor(requireContext().getColor(R.color.pt_text_secondary_dark_bg))
            setPadding(0, 20.dp(), 0, 20.dp())
        }
    }

    private fun showFavoritesMessage(
        @StringRes textRes: Int,
        type: UiMessageType,
        args: List<Any> = emptyList(),
    ) {
        val currentBinding = _binding ?: return
        showUiMessage(
            message = UiMessage(
                text = UiText.Resource(
                    resId = textRes,
                    args = args,
                ),
                type = type,
            ),
            anchorView = currentBinding.customerBottomNav.root,
        )
    }

    private fun formatPrice(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }

    private fun openPizzaDetail(
        product: ResolvedFavoriteProduct,
    ) {
        openPizzaDetail(
            productId = product.id,
            name = product.name,
            description = product.description,
            price = formatPrice(product.basePrice),
            rating = getString(R.string.no_ratings),
            imageUrl = product.imageUrl,
            categoryId = product.categoryId,
            categoryName = product.categoryName,
            sizeOptions = product.sizeOptions,
            crustOptions = product.crustOptions,
            toppingOptions = product.toppingOptions,
        )
    }

    private fun openPizzaDetail(
        productId: String,
        name: String,
        description: String,
        price: String,
        rating: String,
        imageUrl: String,
        categoryId: String,
        categoryName: String,
        sizeOptions: List<String>,
        crustOptions: List<String>,
        toppingOptions: List<String>,
    ) {
        openPizzaDetailScreen(
            productId = productId,
            productName = name,
            productDescription = description,
            productPrice = price,
            productRating = rating,
            productImageUrl = imageUrl,
            productCategoryId = categoryId,
            productCategoryName = categoryName,
            productSizeOptions = sizeOptions,
            productCrustOptions = crustOptions,
            productToppingOptions = toppingOptions,
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private data class ResolvedFavoriteProduct(
        val id: String,
        val name: String,
        val description: String,
        val basePrice: Double,
        val imageRes: Int,
        val imageUrl: String,
        val categoryId: String,
        val categoryName: String,
        val sizeOptions: List<String>,
        val crustOptions: List<String>,
        val toppingOptions: List<String>,
    ) {
        fun toCartItemOrNull(): CartItemUiModel? {
            val category = ProductOptionDefaults.resolveProductCategory(categoryId, categoryName)
            val selectedSize = when (category) {
                ProductOptionDefaults.ProductCategory.PIZZA,
                ProductOptionDefaults.ProductCategory.DRINK,
                -> {
                    val sizes = ProductOptionDefaults.sizesOrDefault(
                        options = sizeOptions,
                        category = category,
                    )
                    sizes.firstOrNull { size -> size.equals(DEFAULT_MEDIUM_SIZE, ignoreCase = true) }
                        ?: sizes.firstOrNull()
                        ?: return null
                }

                ProductOptionDefaults.ProductCategory.COMBO,
                ProductOptionDefaults.ProductCategory.DESSERT,
                -> ""

                ProductOptionDefaults.ProductCategory.UNKNOWN,
                -> if (sizeOptions.isEmpty() && crustOptions.isEmpty() && toppingOptions.isEmpty()) {
                    ""
                } else {
                    return null
                }
            }

            val selectedCrust = when (category) {
                ProductOptionDefaults.ProductCategory.PIZZA -> {
                    val crusts = ProductOptionDefaults.crustsOrDefault(
                        options = crustOptions,
                        category = category,
                    )
                    crusts.firstOrNull() ?: return null
                }

                else -> ""
            }

            return CartItemUiModel(
                id = id,
                name = name,
                description = description,
                price = defaultUnitPrice(category, selectedSize),
                quantity = 1,
                imageRes = imageRes,
                selectedSize = selectedSize,
                selectedCrust = selectedCrust,
                selectedToppings = emptyList(),
                imageUrl = imageUrl,
            )
        }

        private fun defaultUnitPrice(
            category: ProductOptionDefaults.ProductCategory,
            selectedSize: String,
        ): Double {
            return when (category) {
                ProductOptionDefaults.ProductCategory.PIZZA,
                ProductOptionDefaults.ProductCategory.DRINK,
                -> basePrice * sizeMultiplier(selectedSize)

                ProductOptionDefaults.ProductCategory.COMBO,
                ProductOptionDefaults.ProductCategory.DESSERT,
                ProductOptionDefaults.ProductCategory.UNKNOWN,
                -> basePrice
            }
        }

        private fun sizeMultiplier(size: String): Double {
            return when (size.trim().lowercase(Locale.US)) {
                "medium" -> 1.12
                "large" -> 1.20
                else -> 1.0
            }
        }
    }

    private fun CustomerFavoriteItemUiModel.toResolvedProductOrNull(): ResolvedFavoriteProduct? {
        val productId = id.trim()
        val productName = name.trim()
        if (productId.isBlank() || productName.isBlank()) {
            return null
        }

        val hasCategory = categoryId.isNotBlank() || categoryName.isNotBlank()
        if (!hasCategory) {
            return null
        }

        return ResolvedFavoriteProduct(
            id = productId,
            name = productName,
            description = description,
            basePrice = price,
            imageRes = imageRes ?: R.drawable.img_pizza_time,
            imageUrl = imageUrl,
            categoryId = categoryId,
            categoryName = categoryName,
            sizeOptions = sizeOptions,
            crustOptions = crustOptions,
            toppingOptions = toppingOptions,
        )
    }

    private fun ProductUiModel.toResolvedFavoriteProduct(
        fallbackImageRes: Int,
    ): ResolvedFavoriteProduct {
        return ResolvedFavoriteProduct(
            id = id,
            name = name,
            description = description,
            basePrice = basePrice,
            imageRes = fallbackImageRes,
            imageUrl = imageUrl,
            categoryId = categoryId,
            categoryName = categoryName,
            sizeOptions = sizeOptions,
            crustOptions = crustOptions,
            toppingOptions = toppingOptions,
        )
    }

    private companion object {
        private const val TAG = "CustomerFavorites"
        private const val DEFAULT_MEDIUM_SIZE = "Medium"
    }
}
