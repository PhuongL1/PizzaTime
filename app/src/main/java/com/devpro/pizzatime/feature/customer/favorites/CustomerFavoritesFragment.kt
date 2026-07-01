package com.devpro.pizzatime.feature.customer.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerFavoritesBinding
import com.devpro.pizzatime.databinding.ItemCustomerFavoriteCompactBinding
import com.devpro.pizzatime.databinding.ItemCustomerFavoriteFeaturedBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.bottomnav.setupCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.topbar.setupCustomerTopBar
import java.util.Locale
import kotlin.math.roundToInt

class CustomerFavoritesFragment : Fragment() {

    private var _binding: FragmentCustomerFavoritesBinding? = null
    private val binding: FragmentCustomerFavoritesBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerFavoritesBinding is only valid between onCreateView and onDestroyView."
        }

    private val favoritesData: CustomerFavoritesUiModel = FakeCustomerFavoritesData.getFavorites()

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
        renderFavorites()
        bindPairing()
        setupTopBar()
        setupBottomNav()
    }

    private fun bindHeader() = with(binding) {
        tvTitle.text = favoritesData.title
        tvSubtitle.text = favoritesData.subtitle
    }

    private fun renderFavorites() = with(binding.favoritesContainer) {
        removeAllViews()

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
            imageRes = item.imageRes,
            setImage = itemBinding.ivFavoriteImage::setImageResource,
            showPlaceholder = { itemBinding.tvImagePlaceholder.isVisible = it },
        )

        itemBinding.btnAddToCart.setOnClickListener {
            showToast(getString(R.string.customer_favorites_add_to_cart_toast, item.name))
        }

        itemBinding.btnHeart.setOnClickListener {
            showToast(getString(R.string.customer_favorites_saved_toast, item.name))
        }

        return itemBinding.root
    }

    private fun createCompactCard(item: CustomerFavoriteItemUiModel): View {
        val itemBinding = ItemCustomerFavoriteCompactBinding.inflate(layoutInflater)

        itemBinding.tvFavoriteName.text = item.name
        itemBinding.tvFavoritePrice.text = formatPrice(item.price)

        bindImage(
            imageRes = item.imageRes,
            setImage = itemBinding.ivFavoriteImage::setImageResource,
            showPlaceholder = { itemBinding.tvImagePlaceholder.isVisible = it },
        )

        itemBinding.btnAdd.setOnClickListener {
            showToast(getString(R.string.customer_favorites_add_to_cart_toast, item.name))
        }

        itemBinding.btnHeart.setOnClickListener {
            showToast(getString(R.string.customer_favorites_saved_toast, item.name))
        }

        return itemBinding.root
    }

    private fun bindImage(
        imageRes: Int?,
        setImage: (Int) -> Unit,
        showPlaceholder: (Boolean) -> Unit,
    ) {
        if (imageRes != null) {
            setImage(imageRes)
            showPlaceholder(false)
        } else {
            showPlaceholder(true)
        }
    }

    private fun bindPairing() = with(binding) {
        tvPairingTitle.text = favoritesData.pairing.title
        tvPairingSubtitle.text = favoritesData.pairing.subtitle
    }

    private fun setupTopBar() = with(binding) {
        setupCustomerTopBar(
            topBar = customerTopBar,
            cartItemCount = 2,
            onCartClick = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.customer_promo_cart_toast),
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }
    private fun setupBottomNav() = with(binding) {
        setupCustomerBottomNav(
            bottomNav = customerBottomNav,
            selectedTab = CustomerBottomNavTab.PROFILE,
            onCustomerMenuClick = {
                // TODO: open pizza menu / customer home
            },
            onCustomerOrdersClick = {
                // TODO: open customer order history
            },
            onCustomerLoyaltyClick = {
                // TODO: open promo codes
            },
        )
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}