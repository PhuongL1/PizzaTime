package com.devpro.pizzatime.feature.customer.ordersuccess

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.image.loadProductImage
import com.devpro.pizzatime.databinding.FragmentOrderSuccessBinding
import com.devpro.pizzatime.feature.order.OrderCodeGenerator
import com.devpro.pizzatime.feature.staff.navigation.openCustomerHome
import com.devpro.pizzatime.feature.staff.navigation.openOrderTracking

class OrderSuccessFragment : Fragment(R.layout.fragment_order_success) {

    private var _binding: FragmentOrderSuccessBinding? = null
    private val binding: FragmentOrderSuccessBinding
        get() = checkNotNull(_binding) {
            "FragmentOrderSuccessBinding is only valid between onViewCreated and onDestroyView."
        }

    private var orderSuccess: OrderSuccessUiModel? = null
    private val requestedOrderId: String
        get() = arguments?.getString(ARG_ORDER_ID).orEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderSuccessBinding.bind(view)

        bindOrderSuccess(createPlaceholderOrderSuccess())
        loadOrderSuccess()
        setupActions()
    }

    private fun loadOrderSuccess() {
        OrderSuccessFirestoreRepository.loadOrderSuccess(requestedOrderId) { result ->
            if (_binding == null || !isAdded) return@loadOrderSuccess
            result
                .onSuccess { uiModel ->
                    orderSuccess = uiModel
                    bindOrderSuccess(uiModel)
                }
                .onFailure { error ->
                    Log.w(TAG, "Could not load order success for orderId=$requestedOrderId", error)
                }
        }
    }

    private fun bindOrderSuccess(model: OrderSuccessUiModel) = with(binding) {
        ivHeroPizza.loadProductImage(model.heroImageUrl, model.heroImageRes)
        tvOrderId.text = getString(
            R.string.order_success_order_id_format,
            model.displayOrderCode.removePrefix("#"),
        )
        tvTitle.text = model.title
        tvMessage.text = model.message
        tvEstimatedArrival.text = model.estimatedArrival
        tvStatusValue.text = model.statusLabel
    }

    private fun setupActions() = with(binding) {
        btnTrackPizza.setOnClickListener {
            val orderId = orderSuccess?.orderId ?: requestedOrderId
            openOrderTracking(orderId = orderId)
        }

        btnReturnHome.setOnClickListener {
            openCustomerHome(addToBackStack = false)
        }
    }

    private fun createPlaceholderOrderSuccess(): OrderSuccessUiModel {
        val orderId = requestedOrderId
        return OrderSuccessUiModel(
            orderId = orderId,
            displayOrderCode = OrderCodeGenerator.displayOrderCode(orderId),
            title = getString(R.string.order_success_title),
            message = getString(R.string.order_success_message),
            estimatedArrival = getString(R.string.order_success_estimated_time_default),
            statusLabel = getString(R.string.order_success_status_preparing),
            heroImageRes = R.drawable.img_pizza_time,
        )
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "OrderSuccessFragment"
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
