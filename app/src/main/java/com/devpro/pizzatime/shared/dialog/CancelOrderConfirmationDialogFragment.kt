package com.devpro.pizzatime.shared.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.devpro.pizzatime.databinding.DialogCancelOrderConfirmationBinding

class CancelOrderConfirmationDialogFragment : DialogFragment() {

    private var _binding: DialogCancelOrderConfirmationBinding? = null
    private val binding get() = checkNotNull(_binding) {
        "DialogCancelOrderConfirmationBinding is only valid between onCreateView and onDestroyView."
    }

    private val orderId: String
        get() = requireArguments().getString(ARG_ORDER_ID).orEmpty()

    private val currentStatus: String
        get() = requireArguments().getString(ARG_CURRENT_STATUS).orEmpty()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = DialogCancelOrderConfirmationBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            val dialogWidth = (resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt()
            setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOrderInfo()
        setupActions()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun bindOrderInfo() {
        binding.tvOrderIdValue.text = orderId
        binding.tvStatusValue.text = currentStatus
    }

    private fun setupActions() {
        binding.btnKeepOrder.setOnClickListener {
            dismiss()
        }

        binding.btnCancelOrder.setOnClickListener {
            val result = Bundle().apply {
                putString(KEY_ORDER_ID, orderId)
                putString(KEY_CURRENT_STATUS, currentStatus)
                putString(KEY_CANCEL_REASON, binding.etCancelReason.text.toString().trim())
            }

            parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
        }
    }

    companion object {
        const val REQUEST_KEY = "cancel_order_confirmation_request"
        const val KEY_ORDER_ID = "key_order_id"
        const val KEY_CURRENT_STATUS = "key_current_status"
        const val KEY_CANCEL_REASON = "key_cancel_reason"

        private const val ARG_ORDER_ID = "arg_order_id"
        private const val ARG_CURRENT_STATUS = "arg_current_status"
        private const val DIALOG_WIDTH_RATIO = 0.92f

        fun newInstance(
            orderId: String,
            currentStatus: String,
        ): CancelOrderConfirmationDialogFragment {
            return CancelOrderConfirmationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                    putString(ARG_CURRENT_STATUS, currentStatus)
                }
            }
        }
    }
}