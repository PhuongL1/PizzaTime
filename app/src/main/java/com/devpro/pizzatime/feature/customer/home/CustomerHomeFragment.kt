package com.devpro.pizzatime.feature.customer.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerHomeBinding
import com.devpro.pizzatime.databinding.ItemBestSellerPizzaBinding
import com.devpro.pizzatime.databinding.ItemChefSelectionPizzaBinding
import com.devpro.pizzatime.feature.customer.menu.PizzaMenuFragment
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openCustomerPromoCodes

class CustomerHomeFragment : Fragment() {

    private var _binding: FragmentCustomerHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderHomeData()
        setupActions()
    }

    private fun renderHomeData() {
        renderBestSellers(FakeHomeData.bestSellers)
        renderChefSelections(FakeHomeData.chefSelections)
    }

    private fun renderBestSellers(items: List<BestSellerPizzaUiModel>) {
        binding.bestSellerContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemBestSellerPizzaBinding.inflate(
                layoutInflater,
                binding.bestSellerContainer,
                false
            )

            itemBinding.imgPizza.setImageResource(item.imageRes)
            itemBinding.imgPizza.contentDescription = item.name
            itemBinding.tvPizzaName.text = item.name
            itemBinding.tvPizzaDescription.text = item.description
            itemBinding.tvPizzaPrice.text = item.price

            var isFavorite = item.isFavorite

            itemBinding.imgFavoriteIcon.setImageResource(
                if (isFavorite) {
                    R.drawable.ic_heart
                } else {
                    R.drawable.ic_empty_heart
                }
            )

            itemBinding.root.setOnClickListener {
                Toast.makeText(requireContext(), "Open ${item.name}", Toast.LENGTH_SHORT).show()
            }

            itemBinding.btnFavorite.setOnClickListener {
                isFavorite = !isFavorite

                itemBinding.imgFavoriteIcon.setImageResource(
                    if (isFavorite) {
                        R.drawable.ic_heart
                    } else {
                        R.drawable.ic_empty_heart
                    }
                )

                Toast.makeText(
                    requireContext(),
                    if (isFavorite) "Favorited ${item.name}" else "Removed ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            itemBinding.btnAddToCart.setOnClickListener {
                Toast.makeText(requireContext(), "Added ${item.name}", Toast.LENGTH_SHORT).show()
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                178.dp,
                260.dp
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
                false
            )

            itemBinding.imgChefPizza.setImageResource(item.imageRes)
            itemBinding.imgChefPizza.contentDescription = item.name
            itemBinding.tvChefLabel.text = item.label
            itemBinding.tvChefName.text = item.name
            itemBinding.tvChefDescription.text = item.description
            itemBinding.tvChefPrice.text = item.price
            itemBinding.tvChefRating.text = item.rating

            itemBinding.root.setOnClickListener {
                Toast.makeText(requireContext(), "Open ${item.name}", Toast.LENGTH_SHORT).show()
            }

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                300.dp,
                360.dp
            ).apply {
                if (index > 0) {
                    marginStart = 18.dp
                }
            }

            binding.chefSelectionContainer.addView(itemBinding.root)
        }
    }

    private fun setupActions() {
        binding.btnHomeAvatar.setOnClickListener {
            Toast.makeText(requireContext(), "Search coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.searchBar.setOnClickListener {
            Toast.makeText(requireContext(), "Search coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.promoCard.setOnClickListener {
            Toast.makeText(requireContext(), "Promo detail coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.navOrders.setOnClickListener {
            Toast.makeText(requireContext(), "Orders coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.navLoyalty.setOnClickListener {
            openCustomerPromoCodes()
        }

        binding.bottomNav.navProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.bottomNav.navMenu.setOnClickListener {
            openCustomerHome()
        }
        binding.tvSeeAll.setOnClickListener {
            openPizzaMenuScreen()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openPizzaMenuScreen() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, PizzaMenuFragment())
            .addToBackStack(null)
            .commit()
    }
}