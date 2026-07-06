package com.devpro.pizzatime.feature.customer.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.databinding.FragmentCustomerFavoritesBinding
import com.devpro.pizzatime.databinding.ItemCustomerFavoriteCompactBinding
import com.devpro.pizzatime.databinding.ItemCustomerFavoriteFeaturedBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerTopBar
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
            renderFavorites()
            return
        }

        CustomerFavoritesFirestoreRepository.loadFavoriteProducts(uid) { result ->
            if (_binding == null) return@loadFavoriteProducts
            result
                .onSuccess { favorites ->
                    favoritesData = favoritesData.copy(favorites = favorites)
                    renderFavorites()
                }
                .onFailure {
                    favoritesData = favoritesData.copy(favorites = emptyList())
                    renderFavorites()
                }
        }
    }

    private fun bindHeader() = with(binding) {
        tvTitle.text = getString(R.string.customer_menu_title_favorites)
        tvSubtitle.text = getString(R.string.customer_menu_subtitle_favorites)
    }

    private fun renderFavorites() = with(binding.favoritesContainer) {
        removeAllViews()

        if (favoritesData.favorites.isEmpty()) {
            addView(createEmptyStateView(getString(R.string.no_favorites_yet)))
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
                    bottomMargin = 24.dp()
                },
            )
        }
    }

    private fun createFeaturedCard(item: CustomerFavoriteItemUiModel): View {
        val itemBinding = ItemCustomerFavoriteFeaturedBinding.inflate(layoutInflater)

        itemBinding.tvFavoriteName.text = item.name
        itemBinding.tvFavoriteDescription.text = item.description
        itemBinding.tvFavoritePrice.text = formatPrice(item.price)

        itemBinding.tvBadge.isVisible = item.badge != null
        itemBinding.tvBadge.text = item.badge.orEmpty()

        bindImage(
            imageView = itemBinding.ivFavoriteImage,
            imageUrl = item.imageUrl,
            imageRes = item.imageRes,
            showPlaceholder = { itemBinding.tvImagePlaceholder.isVisible = it },
        )

        itemBinding.btnAddToCart.setOnClickListener {
            showToast(getString(R.string.customer_favorites_add_to_cart_toast, item.name))
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

        itemBinding.btnAdd.setOnClickListener {
            showToast(getString(R.string.customer_favorites_add_to_cart_toast, item.name))
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
            )
        }

        return itemBinding.root
    }

    private fun removeFavorite(item: CustomerFavoriteItemUiModel) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showToast(getString(R.string.customer_favorites_login_required))
            return
        }

        CustomerFavoritesFirestoreRepository.removeFavorite(uid, item.id) { result ->
            if (_binding == null) return@removeFavorite
            result
                .onSuccess {
                    showToast(getString(R.string.customer_favorites_removed_toast, item.name))
                    loadFirestoreFavorites()
                }
                .onFailure {
                    showToast(getString(R.string.customer_favorites_update_failed))
                }
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun formatPrice(value: Double): String {
        return String.format(Locale.US, "$%.2f", value)
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }

    private fun openPizzaDetail(
        productId: String,
        name: String,
        description: String,
        price: String,
        rating: String,
        imageUrl: String,
    ) {
        openPizzaDetailScreen(
            productId = productId,
            productName = name,
            productDescription = description,
            productPrice = price,
            productRating = rating,
            productImageUrl = imageUrl,
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
