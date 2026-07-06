package com.devpro.pizzatime.feature.customer.menu

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.databinding.FragmentPizzaMenuBinding
import com.devpro.pizzatime.databinding.ItemPizzaMenuBinding
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import com.devpro.pizzatime.feature.staff.navigation.openCustomerAccount
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerOrderHistory
import com.devpro.pizzatime.feature.staff.navigation.openCustomerPromoCodes
import com.devpro.pizzatime.feature.staff.navigation.openLoginRequiredScreen
import com.devpro.pizzatime.feature.staff.navigation.openPizzaDetailScreen
import java.util.Locale

class PizzaMenuFragment : Fragment() {

    private var _binding: FragmentPizzaMenuBinding? = null
    private val binding: FragmentPizzaMenuBinding
        get() = checkNotNull(_binding) {
            "FragmentPizzaMenuBinding is only valid between onCreateView and onDestroyView."
        }

    private var allProducts: List<PizzaMenuUiModel> = emptyList()
    private var categoryItems: List<MenuCategoryItem> = emptyList()
    private var selectedCategoryId: String = MenuCategoryItem.ALL_ID
    private var searchQuery: String = ""

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
        setupSearch()
        setupActions()
        loadAndRenderProducts()
    }

    private fun loadAndRenderProducts() {
        FirebaseProductRepository.loadProducts { products ->
            if (_binding == null) return@loadProducts
            allProducts = products.map { it.toPizzaMenuUiModel() }
            rebuildCategories()
            renderMenuItems()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.navMenu.text = getString(R.string.menu_nav_menu)
        binding.bottomNav.navLoyalty.text = getString(R.string.menu_nav_loyalty)
    }

    private fun setupSearch() = with(binding) {
        tvSearchHint.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString().orEmpty()
                renderMenuItems()
            }
        })

        searchBar.setOnClickListener {
            tvSearchHint.requestFocus()
        }

        tvSearchHint.setOnClickListener {
            tvSearchHint.requestFocus()
        }
    }

    private fun setupActions() = with(binding) {
        btnOpenDrawer.setOnClickListener {
            Toast.makeText(requireContext(), "Drawer coming soon", Toast.LENGTH_SHORT).show()
        }

        btnOpenCart.setOnClickListener {
            openCartScreen()
        }

        btnFilter.setOnClickListener {
            categoryScroll.isVisible = !categoryScroll.isVisible
            Toast.makeText(
                requireContext(),
                R.string.menu_filters_updated,
                Toast.LENGTH_SHORT,
            ).show()
        }

        bottomNav.navMenu.setOnClickListener {
            openCustomerHome()
        }

        bottomNav.navOrders.setOnClickListener {
            if (FakeSessionStore.isLoggedIn) openCustomerOrderHistory()
            else openLoginRequiredScreen()
        }

        bottomNav.navLoyalty.setOnClickListener {
            openCustomerPromoCodes()
        }

        bottomNav.navProfile.setOnClickListener {
            if (FakeSessionStore.isLoggedIn) {
                openCustomerAccount()
            } else {
                openLoginRequiredScreen()
            }
        }
    }

    private fun rebuildCategories() {
        val categories = allProducts
            .mapNotNull { item -> item.toMenuCategoryItem() }
            .distinctBy { it.id }
            .sortedBy { it.label.lowercase(Locale.US) }

        categoryItems = buildList {
            add(
                MenuCategoryItem(
                    id = MenuCategoryItem.ALL_ID,
                    label = getString(R.string.menu_category_all),
                ),
            )
            addAll(categories)
        }

        if (selectedCategoryId !in categoryItems.map { it.id }) {
            selectedCategoryId = MenuCategoryItem.ALL_ID
        }

        renderCategoryChips()
    }

    private fun renderCategoryChips() = with(binding.categoryContainer) {
        removeAllViews()
        categoryItems.forEachIndexed { index, category ->
            addView(
                createCategoryChip(category),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    if (index > 0) {
                        marginStart = 12.dp()
                    }
                },
            )
        }
        updateCategoryChipState()
    }

    private fun createCategoryChip(category: MenuCategoryItem): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                38.dp(),
            )
            minWidth = 102.dp()
            setPadding(18.dp(), 0, 18.dp(), 0)
            gravity = android.view.Gravity.CENTER
            text = category.label
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_chip_unselected_dark)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.pt_text_primary))
            setOnClickListener {
                selectedCategoryId = category.id
                renderMenuItems()
            }
        }
    }

    private fun renderMenuItems() {
        val normalizedQuery = searchQuery.trim().lowercase(Locale.US)
        val filteredItems = allProducts.filter { item ->
            (selectedCategoryId == MenuCategoryItem.ALL_ID || item.categoryId == selectedCategoryId) &&
                (normalizedQuery.isBlank() ||
                    item.name.lowercase(Locale.US).contains(normalizedQuery) ||
                    item.description.lowercase(Locale.US).contains(normalizedQuery))
        }

        renderPizzaList(filteredItems)
        updateCategoryChipState()
    }

    private fun renderPizzaList(items: List<PizzaMenuUiModel>) {
        binding.pizzaMenuContainer.removeAllViews()

        if (items.isEmpty()) {
            binding.pizzaMenuContainer.addView(createEmptyStateView())
            return
        }

        items.forEachIndexed { index, item ->
            val itemBinding = ItemPizzaMenuBinding.inflate(
                layoutInflater,
                binding.pizzaMenuContainer,
                false,
            )

            itemBinding.imgPizza.loadProductImage(item.imageUrl, item.imageRes)
            itemBinding.imgPizza.contentDescription = item.name
            itemBinding.tvPizzaName.text = item.name
            itemBinding.tvPizzaDescription.text = item.description
            itemBinding.tvPizzaPrice.text = item.price
            itemBinding.tvPizzaRating.text = getString(R.string.home_rating_49).replace("4.9", item.rating)

            itemBinding.root.setOnClickListener { openPizzaDetail(item) }
            itemBinding.btnAddToCart.setOnClickListener { openPizzaDetail(item) }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                340.dp(),
            ).apply {
                if (index > 0) {
                    topMargin = 18.dp()
                }
            }

            binding.pizzaMenuContainer.addView(itemBinding.root)
        }
    }

    private fun createEmptyStateView(): View {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 18.dp()
            }
            gravity = android.view.Gravity.CENTER
            text = getString(R.string.menu_no_results)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.pt_text_secondary))
            textSize = 15f
        }
    }

    private fun updateCategoryChipState() = with(binding.categoryContainer) {
        for (index in 0 until childCount) {
            val chip = getChildAt(index) as? TextView ?: continue
            val category = categoryItems.getOrNull(index) ?: continue
            val selected = category.id == selectedCategoryId
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_chip_selected_gold else R.drawable.bg_chip_unselected_dark,
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.pt_text_dark else R.color.pt_text_primary,
                ),
            )
        }
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
        price = "$${String.format(Locale.US, "%.2f", basePrice)}",
        rating = String.format(Locale.US, "%.1f", rating),
        imageRes = R.drawable.img_welcome_hero,
        imageUrl = imageUrl,
        categoryId = categoryId,
        categoryName = categoryName,
        sizeOptions = sizeOptions,
        crustOptions = crustOptions,
        toppingOptions = toppingOptions,
    )

    private fun PizzaMenuUiModel.toMenuCategoryItem(): MenuCategoryItem? {
        val id = categoryId.trim()
        if (id.isBlank()) return null
        val label = categoryName.trim().ifBlank { id.toDisplayCategoryLabel() }
        return MenuCategoryItem(id = id, label = label)
    }

    private fun String.toDisplayCategoryLabel(): String {
        return when (uppercase(Locale.US)) {
            "SIGNATURE" -> getString(R.string.manage_menu_category_signature)
            "CLASSIC" -> getString(R.string.manage_menu_category_classic)
            "VEGGIE" -> getString(R.string.manage_menu_category_veggie)
            else -> replace('_', ' ').lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private data class MenuCategoryItem(
        val id: String,
        val label: String,
    ) {
        companion object {
            const val ALL_ID = "__all__"
        }
    }
}
