package com.devpro.pizzatime.feature.customer.promos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentCustomerPromoCodesBinding
import com.devpro.pizzatime.databinding.ItemCustomerPromoCardBinding
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerTopBar
import kotlin.math.roundToInt

class CustomerPromoCodesFragment : Fragment() {

    private var _binding: FragmentCustomerPromoCodesBinding? = null
    private val binding: FragmentCustomerPromoCodesBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerPromoCodesBinding is only valid between onCreateView and onDestroyView."
        }

    private var promoData: CustomerPromoCodesUiModel = FakeCustomerPromoCodesData.getPromoCodes()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCustomerPromoCodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindHeader()
        setupTopBar()
        setupBottomNav()
        setupTabs()
        renderActivePromos()
        renderPastPromos()
        loadFirestorePromos()
    }

    private fun loadFirestorePromos() {
        CustomerPromoFirestoreRepository.loadActivePromos { result ->
            if (_binding == null) return@loadActivePromos
            result.onSuccess { promos ->
                promoData = promoData.copy(activePromos = promos, pastPromos = emptyList())
                renderActivePromos()
                renderPastPromos()
            }
        }
    }

    private fun bindHeader() = with(binding) {
        tvTitle.text = promoData.title
        tvSubtitle.text = promoData.subtitle
    }

    private fun setupTopBar() = with(binding) {
        bindCustomerTopBar(
            root = customerTopBar.root,
            cartItemCount = 2,
        )
    }
    private fun setupBottomNav() = with(binding) {
        bindCustomerBottomNav(
            root = customerBottomNav.root,
            selectedTab = CustomerBottomNavTab.LOYALTY,
        )
    }
    private fun setupTabs() = with(binding) {
        chipActive.setOnClickListener {
            showComingSoon(getString(R.string.customer_promo_tab_active))
        }

        chipPastRewards.setOnClickListener {
            showComingSoon(getString(R.string.customer_promo_tab_past_rewards))
        }

        chipPointsHistory.setOnClickListener {
            showComingSoon(getString(R.string.customer_promo_tab_points_history))
        }
    }

    private fun renderActivePromos() = with(binding.activePromosContainer) {
        removeAllViews()

        promoData.activePromos.forEach { promo ->
            addView(createPromoCard(promo), createCardLayoutParams())
        }
    }

    private fun renderPastPromos() = with(binding.pastPromosContainer) {
        removeAllViews()

        promoData.pastPromos.forEach { promo ->
            addView(createPromoCard(promo), createCardLayoutParams())
        }
    }

    private fun createPromoCard(promo: CustomerPromoUiModel): View {
        val itemBinding = ItemCustomerPromoCardBinding.inflate(layoutInflater)

        itemBinding.tvCategory.text = promo.category
        itemBinding.tvCode.text = promo.code
        itemBinding.tvDescription.text = promo.description
        itemBinding.tvMetaLabel.text = promo.metaLabel
        itemBinding.tvMetaValue.text = promo.metaValue
        itemBinding.tvStatus.text = promo.statusLabel

        itemBinding.ivPromoImage.isVisible = promo.imageRes != null
        promo.imageRes?.let(itemBinding.ivPromoImage::setImageResource)

        itemBinding.btnApply.isVisible = promo.actionLabel != null
        itemBinding.btnApply.text = promo.actionLabel.orEmpty()
        itemBinding.btnApply.setOnClickListener {
            showComingSoon(promo.code)
        }

        renderPromoState(itemBinding, promo.state)
        return itemBinding.root
    }

    private fun renderPromoState(
        itemBinding: ItemCustomerPromoCardBinding,
        state: CustomerPromoState,
    ) = with(itemBinding) {
        when (state) {
            CustomerPromoState.ACTIVE -> {
                tvStatus.setBackgroundResource(R.drawable.bg_customer_promo_status_active)
                tvStatus.setTextColor(requireContext().getColor(R.color.pt_text_primary_dark_bg))
                btnApply.setBackgroundResource(R.drawable.bg_customer_promo_apply_button)
            }

            CustomerPromoState.USED -> {
                tvStatus.setBackgroundResource(R.drawable.bg_customer_promo_status_muted)
                tvStatus.setTextColor(requireContext().getColor(R.color.pt_text_secondary_dark_bg))
                btnApply.isVisible = false
            }

            CustomerPromoState.EXPIRED -> {
                tvStatus.setBackgroundResource(R.drawable.bg_customer_promo_status_muted)
                tvStatus.setTextColor(requireContext().getColor(R.color.pt_copper))
                btnApply.isVisible = false
            }
        }
    }

    private fun createCardLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = 24.dp()
        }
    }

    private fun showComingSoon(label: String) {
        Toast.makeText(
            requireContext(),
            getString(R.string.customer_promo_coming_soon, label),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}
