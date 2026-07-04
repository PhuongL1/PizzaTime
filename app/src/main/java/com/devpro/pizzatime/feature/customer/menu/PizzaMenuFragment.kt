package com.devpro.pizzatime.feature.customer.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.databinding.FragmentPizzaMenuBinding
import com.devpro.pizzatime.databinding.ItemPizzaMenuBinding
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory
import com.devpro.pizzatime.feature.staff.navigation.openCustomerPromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
import com.devpro.pizzatime.feature.staff.navigation.openPizzaDetailScreen

class PizzaMenuFragment : Fragment() {

    private var _binding: FragmentPizzaMenuBinding? = null
    private val binding: FragmentPizzaMenuBinding
        get() = checkNotNull(_binding) {
            "FragmentPizzaMenuBinding is only valid between onCreateView and onDestroyView."
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPizzaMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupBottomNav()
        loadAndRenderProducts()
        setupActions()
    }

    private fun loadAndRenderProducts() {
        FirebaseProductRepository.loadProducts { products ->
            if (_binding == null) return@loadProducts
            renderPizzaList(products.map { it.toPizzaMenuUiModel() })
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.navMenu.text = getString(R.string.menu_nav_menu)
        binding.bottomNav.navLoyalty.text = getString(R.string.menu_nav_loyalty)
    }

    private fun renderPizzaList(items: List<PizzaMenuUiModel>) {
        binding.pizzaMenuContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemPizzaMenuBinding.inflate(
                layoutInflater,
                binding.pizzaMenuContainer,
                false
            )

            itemBinding.imgPizza.loadProductImage(item.imageUrl, item.imageRes)
            itemBinding.imgPizza.contentDescription = item.name
            itemBinding.tvPizzaName.text = item.name
            itemBinding.tvPizzaDescription.text = item.description
            itemBinding.tvPizzaPrice.text = item.price
            itemBinding.tvPizzaRating.text = getString(R.string.home_rating_49).replace("4.9", item.rating)

            itemBinding.root.setOnClickListener {
                openPizzaDetail(item)
            }

            itemBinding.btnAddToCart.setOnClickListener {
                openPizzaDetail(item)
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                340.dp
            ).apply {
                if (index > 0) {
                    topMargin = 18.dp
                }
            }

            binding.pizzaMenuContainer.addView(itemBinding.root)
        }
    }

    private fun setupActions() {
        binding.btnOpenDrawer.setOnClickListener {
            Toast.makeText(requireContext(), "Drawer coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnOpenCart.setOnClickListener {
            openCartScreen()
        }

        binding.searchBar.setOnClickListener {
            Toast.makeText(requireContext(), "Search coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Filter coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.navMenu.setOnClickListener {
            openCustomerHome()
        }

        binding.bottomNav.navOrders.setOnClickListener {
            if (FakeSessionStore.isLoggedIn) openCustomerOrderHistory()
            else openLoginRequiredScreen()
        }

        binding.bottomNav.navLoyalty.setOnClickListener {
            openCustomerPromoCodes()
        }

        binding.bottomNav.navProfile.setOnClickListener {
            if (FakeSessionStore.isLoggedIn) {
                Toast.makeText(requireContext(), "Profile coming soon", Toast.LENGTH_SHORT).show()
            } else {
                openLoginRequiredScreen()
            }
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openPizzaDetail(item: PizzaMenuUiModel) {
        openPizzaDetailScreen(
            productId = item.id,
            productName = item.name,
            productDescription = item.description,
            productPrice = item.price,
            productRating = item.rating,
            productImageUrl = item.imageUrl,
            productSizeOptions = item.sizeOptions,
            productCrustOptions = item.crustOptions,
            productToppingOptions = item.toppingOptions,
        )
    }

    private fun ProductUiModel.toPizzaMenuUiModel() = PizzaMenuUiModel(
        id = id,
        name = name,
        description = description,
        price = "$${String.format("%.2f", basePrice)}",
        rating = String.format("%.1f", rating),
        imageRes = R.drawable.img_welcome_hero,
        imageUrl = imageUrl,
        sizeOptions = sizeOptions,
        crustOptions = crustOptions,
        toppingOptions = toppingOptions,
    )
}
