package com.devpro.pizzatime.feature.customer.promos

import android.util.Log
import android.os.Bundle
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
import com.devpro.pizzatime.databinding.FragmentCustomerPromoCodesBinding
import com.devpro.pizzatime.databinding.ItemCustomerPromoCardBinding
import com.devpro.pizzatime.feature.customer.cart.CartStore
import com.devpro.pizzatime.feature.customer.checkout.CheckoutConsistencyRepository
import com.devpro.pizzatime.feature.customer.common.bottomnav.CustomerBottomNavTab
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerBottomNav
import com.devpro.pizzatime.feature.customer.common.navigation.bindCustomerTopBar
import com.devpro.pizzatime.feature.staff.navigation.openCartScreen
import java.util.Locale
import kotlin.math.roundToInt

class CustomerPromoCodesFragment : Fragment() {

    private var _binding: FragmentCustomerPromoCodesBinding? = null
    private val binding: FragmentCustomerPromoCodesBinding
        get() = checkNotNull(_binding) {
            "FragmentCustomerPromoCodesBinding is only valid between onCreateView and onDestroyView."
        }

    private var activePromos: List<CustomerPromoUiModel> = emptyList()
    private var pastPromos: List<CustomerPromoUiModel> = emptyList()
    private var selectedTab = PromoTab.ACTIVE
    private var promoLoadFailed = false

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
        loadPromos()
    }

    private fun bindHeader() = with(binding) {
        tvTitle.text = getString(R.string.customer_promo_title)
        tvSubtitle.text = getString(R.string.customer_promo_subtitle)
    }

    private fun loadPromos() {
        CustomerPromoFirestoreRepository.loadPromos { result ->
            if (_binding == null) return@loadPromos
            result
                .onSuccess { promos ->
                    promoLoadFailed = false
                    activePromos = promos.filter { it.state == CustomerPromoState.ACTIVE }
                    pastPromos = promos.filter { it.state != CustomerPromoState.ACTIVE }
                    renderCurrentTab()
                }
                .onFailure { error ->
                    Log.e(TAG, "Could not load customer promo codes", error)
                    promoLoadFailed = true
                    activePromos = emptyList()
                    pastPromos = emptyList()
                    renderCurrentTab()
                    showToast(R.string.promo_load_failed)
                }
        }
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
            selectedTab = CustomerBottomNavTab.LOYALTY,
        )
    }

    private fun setupTabs() = with(binding) {
        chipActive.setOnClickListener {
            selectedTab = PromoTab.ACTIVE
            renderCurrentTab()
        }

        chipPastRewards.setOnClickListener {
            selectedTab = PromoTab.PAST
            renderCurrentTab()
        }

        chipPointsHistory.setOnClickListener {
            selectedTab = PromoTab.POINTS
            renderCurrentTab()
        }
    }

    private fun renderCurrentTab() = with(binding) {
        tvUsedExpiredTitle.isVisible = selectedTab != PromoTab.ACTIVE
        tvUsedExpiredTitle.text = when (selectedTab) {
            PromoTab.ACTIVE -> ""
            PromoTab.PAST -> getString(R.string.customer_promo_tab_past_rewards)
            PromoTab.POINTS -> getString(R.string.customer_promo_tab_points_history)
        }

        activePromosContainer.isVisible = selectedTab != PromoTab.PAST
        pastPromosContainer.isVisible = selectedTab == PromoTab.PAST

        activePromosContainer.removeAllViews()
        pastPromosContainer.removeAllViews()

        when (selectedTab) {
            PromoTab.ACTIVE -> renderPromoList(
                container = activePromosContainer,
                promos = activePromos,
                emptyText = if (promoLoadFailed) R.string.promo_load_failed else R.string.no_active_promos,
            )

            PromoTab.PAST -> renderPromoList(
                container = pastPromosContainer,
                promos = pastPromos,
                emptyText = if (promoLoadFailed) R.string.promo_load_failed else R.string.no_past_rewards,
            )

            PromoTab.POINTS -> {
                activePromosContainer.isVisible = true
                activePromosContainer.addView(
                    createEmptyStateView(R.string.no_points_history),
                    createCardLayoutParams(),
                )
            }
        }

        renderChipState()
    }

    private fun renderPromoList(
        container: LinearLayout,
        promos: List<CustomerPromoUiModel>,
        emptyText: Int,
    ) {
        if (promos.isEmpty()) {
            container.addView(createEmptyStateView(emptyText), createCardLayoutParams())
            return
        }

        promos.forEach { promo ->
            container.addView(createPromoCard(promo), createCardLayoutParams())
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

        itemBinding.btnApply.isVisible = promo.actionLabel != null && promo.state == CustomerPromoState.ACTIVE
        itemBinding.btnApply.text = promo.actionLabel.orEmpty()
        itemBinding.btnApply.setOnClickListener {
            applyPromo(promo)
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

            CustomerPromoState.EXPIRED,
            CustomerPromoState.UNAVAILABLE -> {
                tvStatus.setBackgroundResource(R.drawable.bg_customer_promo_status_muted)
                tvStatus.setTextColor(requireContext().getColor(R.color.pt_copper))
                btnApply.isVisible = false
            }
        }
    }

    private fun renderChipState() = with(binding) {
        chipActive.bindChip(selectedTab == PromoTab.ACTIVE)
        chipPastRewards.bindChip(selectedTab == PromoTab.PAST)
        chipPointsHistory.bindChip(selectedTab == PromoTab.POINTS)
    }

    private fun TextView.bindChip(isSelected: Boolean) {
        setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_customer_promo_chip_selected
            } else {
                R.drawable.bg_customer_promo_chip_outline
            },
        )

        setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) {
                    R.color.pt_text_dark
                } else {
                    R.color.pt_text_secondary_dark_bg
                },
            ),
        )
    }

    private fun applyPromo(promo: CustomerPromoUiModel) {
        if (CartStore.items.isEmpty()) {
            showToast(R.string.promo_add_items_first)
            return
        }

        val subtotal = CartStore.items.sumOf { item -> item.price * item.quantity }
        CheckoutConsistencyRepository.validatePromoCode(
            promoCode = promo.code,
            subtotal = subtotal,
        ) { result ->
            if (_binding == null) return@validatePromoCode
            result
                .onSuccess { validation ->
                    when (validation) {
                        is CheckoutConsistencyRepository.PromoValidationResult.Valid -> {
                            CartStore.setPromo(validation.promoCode, validation.discount)
                            showToast(R.string.promo_applied)
                            openCartScreen()
                        }

                        is CheckoutConsistencyRepository.PromoValidationResult.Invalid -> {
                            showPromoValidationFailure(validation.reason)
                        }
                    }
                }
                .onFailure {
                    showToast(R.string.promo_unavailable)
                }
        }
    }

    private fun showPromoValidationFailure(
        reason: CheckoutConsistencyRepository.PromoValidationFailureReason,
    ) {
        when (reason) {
            CheckoutConsistencyRepository.PromoValidationFailureReason.NOT_ELIGIBLE -> {
                showToast(R.string.promo_not_eligible_for_this_cart)
            }

            CheckoutConsistencyRepository.PromoValidationFailureReason.UNAVAILABLE -> {
                showToast(R.string.promo_unavailable)
            }
        }
    }

    private fun createEmptyStateView(messageRes: Int): TextView {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            gravity = android.view.Gravity.CENTER
            text = getString(messageRes)
            textSize = 15f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.pt_text_secondary_dark_bg))
            setPadding(0, 18.dp(), 0, 18.dp())
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

    private fun showToast(messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private enum class PromoTab {
        ACTIVE,
        PAST,
        POINTS,
    }

    companion object {
        private const val TAG = "CustomerPromoCodes"
    }
}
