package com.devpro.pizzatime.feature.customer.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentOrderTypeBinding
import com.devpro.pizzatime.feature.customer.menu.PizzaMenuFragment
import com.devpro.pizzatime.feature.staff.navigation.replaceForward

class OrderTypeFragment : Fragment() {

    private var _binding: FragmentOrderTypeBinding? = null
    private val binding: FragmentOrderTypeBinding
        get() = checkNotNull(_binding) {
            "FragmentOrderTypeBinding is only valid between onCreateView and onDestroyView."
        }

    private var selectedType: OrderType = OrderType.DELIVERY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrderTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupListeners()
        renderSelectedType()
    }

    private fun setupListeners() = with(binding) {
        optionDelivery.setOnClickListener {
            selectOrderType(OrderType.DELIVERY)
        }

        optionSelfCollect.setOnClickListener {
            selectOrderType(OrderType.SELF_COLLECT)
        }

        optionDineIn.setOnClickListener {
            selectOrderType(OrderType.DINE_IN)
        }

        btnContinue.setOnClickListener {
            continueWithSelectedType()
        }
    }

    private fun selectOrderType(type: OrderType) {
        selectedType = type
        renderSelectedType()
    }

    private fun renderSelectedType() = with(binding) {
        optionDelivery.setBackgroundResource(
            if (selectedType == OrderType.DELIVERY) {
                R.drawable.bg_order_type_option_selected
            } else {
                R.drawable.bg_order_type_option
            },
        )

        optionSelfCollect.setBackgroundResource(
            if (selectedType == OrderType.SELF_COLLECT) {
                R.drawable.bg_order_type_option_selected
            } else {
                R.drawable.bg_order_type_option
            },
        )

        optionDineIn.setBackgroundResource(
            if (selectedType == OrderType.DINE_IN) {
                R.drawable.bg_order_type_option_selected
            } else {
                R.drawable.bg_order_type_option
            },
        )
    }

    private fun continueWithSelectedType() {
        parentFragmentManager.replaceForward(
            containerId = R.id.fragmentContainer,
            fragment = PizzaMenuFragment(),
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private enum class OrderType(
        @StringRes val titleRes: Int,
    ) {
        DELIVERY(R.string.order_type_delivery_title),
        SELF_COLLECT(R.string.order_type_self_collect_title),
        DINE_IN(R.string.order_type_dine_in_title),
    }

    companion object {
        fun newInstance(): OrderTypeFragment = OrderTypeFragment()
    }
}
