package com.devpro.pizzatime.feature.customer.ordersuccess

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.FragmentOrderSuccessBinding
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openOrderTracking

class OrderSuccessFragment : Fragment(R.layout.fragment_order_success) {

    private var _binding: FragmentOrderSuccessBinding? = null
    private val binding: FragmentOrderSuccessBinding
        get() = checkNotNull(_binding) {
            "FragmentOrderSuccessBinding is only valid between onViewCreated and onDestroyView."
        }

    private val orderSuccess: OrderSuccessUiModel by lazy {
        FakeOrderSuccessData.getOrderSuccess(
            orderId = arguments?.getString(ARG_ORDER_ID).orEmpty(),
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderSuccessBinding.bind(view)

        bindOrderSuccess()
        setupActions()
    }

    private fun bindOrderSuccess() = with(binding) {
        ivHeroPizza.setImageResource(orderSuccess.heroImageRes)
        tvOrderId.text = getString(R.string.order_success_order_id_format, orderSuccess.orderId)
        tvTitle.text = orderSuccess.title
        tvMessage.text = orderSuccess.message
        tvEstimatedArrival.text = orderSuccess.estimatedArrival
        tvStatusValue.text = orderSuccess.statusLabel
    }

    private fun setupActions() = with(binding) {
        btnTrackPizza.setOnClickListener {
            openOrderTracking(orderId = orderSuccess.orderId)
        }

        btnReturnHome.setOnClickListener {
            openCustomerHome(addToBackStack = false)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"

        fun newInstance(orderId: String): OrderSuccessFragment {
            return OrderSuccessFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                }
            }
        }
    }
}