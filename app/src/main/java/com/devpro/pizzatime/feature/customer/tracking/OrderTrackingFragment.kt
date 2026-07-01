package com.devpro.pizzatime.feature.customer.tracking

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentOrderTrackingBinding
import com.devpro.pizzatime.databinding.ItemOrderTrackingStepBinding

class OrderTrackingFragment : Fragment() {

    private var _binding: FragmentOrderTrackingBinding? = null
    private val binding get() = _binding!!

    private val steps = FakeTrackingData.steps
    private val product = FakeTrackingData.product

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrderTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        renderTrackingSteps(steps)
        bindProduct(product)
        setupBottomNav()
        setupActions()
    }

    private fun renderTrackingSteps(items: List<TrackingStepUiModel>) {
        binding.statusStepContainer.removeAllViews()

        items.forEachIndexed { index, item ->
            val itemBinding = ItemOrderTrackingStepBinding.inflate(
                layoutInflater,
                binding.statusStepContainer,
                false
            )

            itemBinding.tvStepTitle.text = item.title
            itemBinding.tvStepSubtitle.text = item.subtitle
            itemBinding.tvStepSubtitle.isVisible = item.subtitle.isNotBlank()

            itemBinding.topLine.isVisible = index != 0
            itemBinding.bottomLine.isVisible = index != items.lastIndex

            updateStepState(itemBinding, item.state)

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                92.dp
            )

            binding.statusStepContainer.addView(itemBinding.root)
        }
    }

    private fun updateStepState(
        itemBinding: ItemOrderTrackingStepBinding,
        state: TrackingStepState,
    ) {
        when (state) {
            TrackingStepState.DONE -> {
                itemBinding.statusCircle.setBackgroundResource(R.drawable.bg_timeline_done)
                itemBinding.tvStatusIcon.text = "✓"
                itemBinding.tvStepTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_primary)
                )
                itemBinding.tvStepSubtitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_secondary)
                )
                itemBinding.topLine.setBackgroundColor(Color.parseColor("#D1843D"))
                itemBinding.bottomLine.setBackgroundColor(Color.parseColor("#D1843D"))
            }

            TrackingStepState.CURRENT -> {
                itemBinding.statusCircle.setBackgroundResource(R.drawable.bg_timeline_current)
                itemBinding.tvStatusIcon.text = "🔥"
                itemBinding.tvStepTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_gold)
                )
                itemBinding.tvStepSubtitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_primary)
                )
                itemBinding.topLine.setBackgroundColor(Color.parseColor("#D1843D"))
                itemBinding.bottomLine.setBackgroundColor(Color.parseColor("#5A4332"))
            }

            TrackingStepState.PENDING -> {
                itemBinding.statusCircle.setBackgroundResource(R.drawable.bg_timeline_pending)
                itemBinding.tvStatusIcon.text = ""
                itemBinding.tvStepTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_secondary)
                )
                itemBinding.tvStepSubtitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.pt_text_muted)
                )
                itemBinding.topLine.setBackgroundColor(Color.parseColor("#5A4332"))
                itemBinding.bottomLine.setBackgroundColor(Color.parseColor("#5A4332"))
            }
        }
    }

    private fun bindProduct(item: TrackingProductUiModel) {
        binding.imgProduct.setImageResource(item.imageRes)
        binding.imgProduct.contentDescription = item.name
        binding.tvProductName.text = item.name
        binding.tvProductOption.text = item.optionText
        binding.tvProductPrice.text = item.price
    }

    private fun setupBottomNav() {
        binding.bottomNav.navMenu.text = getString(R.string.tracking_nav_menu)
        binding.bottomNav.navOrders.text = getString(R.string.tracking_nav_orders)
        binding.bottomNav.navLoyalty.text = getString(R.string.tracking_nav_loyalty)
        binding.bottomNav.navProfile.text = getString(R.string.tracking_nav_profile)

        val primaryColor = ContextCompat.getColor(requireContext(), R.color.pt_text_primary)
        val darkColor = ContextCompat.getColor(requireContext(), R.color.pt_text_dark)

        listOf(
            binding.bottomNav.navMenu,
            binding.bottomNav.navOrders,
            binding.bottomNav.navLoyalty,
            binding.bottomNav.navProfile,
        ).forEach { item ->
            item.setBackgroundResource(0)
            item.setTextColor(primaryColor)
        }

        binding.bottomNav.navOrders.setBackgroundResource(R.drawable.bg_bottom_nav_item_selected)
        binding.bottomNav.navOrders.setTextColor(darkColor)
    }

    private fun setupActions() {
        binding.btnMenu.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCart.setOnClickListener {
            Toast.makeText(requireContext(), "Cart coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnProductDetail.setOnClickListener {
            Toast.makeText(requireContext(), "Open order item detail", Toast.LENGTH_SHORT).show()
        }

        binding.btnSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Support coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.navMenu.setOnClickListener {
            Toast.makeText(requireContext(), "Menu coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.navLoyalty.setOnClickListener {
            Toast.makeText(requireContext(), "Loyalty coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.navProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}