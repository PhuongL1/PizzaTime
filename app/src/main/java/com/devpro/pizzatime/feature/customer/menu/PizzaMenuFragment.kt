package com.devpro.pizzatime.feature.customer.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentPizzaMenuBinding
import com.devpro.pizzatime.databinding.ItemPizzaMenuBinding
import com.devpro.pizzatime.feature.customer.cart.CartItemUiModel
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory
import com.devpro.pizzatime.feature.staff.navigation.openCustomerPromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
import com.devpro.pizzatime.feature.staff.navigation.openPizzaDetailScreen
import com.devpro.pizzatime.core.session.FakeSessionStore

class PizzaMenuFragment : Fragment() {

    private var _binding: FragmentPizzaMenuBinding? = null
    private val binding get() = _binding!!

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

            itemBinding.imgPizza.setImageResource(item.imageRes)
            itemBinding.imgPizza.contentDescription = item.name
            itemBinding.tvPizzaName.text = item.name
            itemBinding.tvPizzaDescription.text = item.description
            itemBinding.tvPizzaPrice.text = item.price
            itemBinding.tvPizzaRating.text = getString(R.string.home_rating_49).replace("4.9", item.rating)

            itemBinding.root.setOnClickListener {
                openPizzaDetailScreen(
                    productId = item.id,
                    productName = item.name,
                    productDescription = item.description,
                    productPrice = item.price,
                    productRating = item.rating,
                )
            }

            itemBinding.btnAddToCart.setOnClickListener {
                CartStore.addItem(
                    CartItemUiModel(
                        id = item.id,
                        name = item.name,
                        price = parsePrice(item.price),
                        quantity = 1,
                        imageRes = item.imageRes,
                    )
                )

                Toast.makeText(
                    requireContext(),
                    "Added ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun parsePrice(price: String): Double {
        return price
            .replace("$", "")
            .trim()
            .toDoubleOrNull() ?: 0.0
    }

    private fun ProductUiModel.toPizzaMenuUiModel() = PizzaMenuUiModel(
        id = id,
        name = name,
        description = description,
        price = "$${String.format("%.2f", basePrice)}",
        rating = String.format("%.1f", rating),
        imageRes = R.drawable.img_welcome_hero,
    )
}