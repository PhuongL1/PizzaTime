package com.devpro.pizzatime.shared.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.DialogStatusUpdateConfirmationBinding

class StatusUpdateConfirmationDialogFragment : DialogFragment() {

    private var _binding: DialogStatusUpdateConfirmationBinding? = null
    private val binding: DialogStatusUpdateConfirmationBinding
        get() = checkNotNull(_binding) {
            "DialogStatusUpdateConfirmationBinding is only valid between onCreateView and onDestroyView."
        }

    private val orderId: String
        get() = requireArguments().getString(ARG_ORDER_ID).orEmpty()

    private val fromStatus: String
        get() = requireArguments().getString(ARG_FROM_STATUS).orEmpty()

    private val toStatus: String
        get() = requireArguments().getString(ARG_TO_STATUS).orEmpty()

    private val confirmLabel: String
        get() = requireArguments().getString(ARG_CONFIRM_LABEL).orEmpty()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogStatusUpdateConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            val dialogWidth = (resources.displayMetrics.widthPixels * DIALOG_WIDTH_RATIO).toInt()
            setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindStatusInfo()
        setupActions()
    }

    private fun bindStatusInfo(): Unit = with(binding) {
        tvOrderIdValue.text = getString(R.string.status_update_order_id_format, orderId)
        tvFromStatus.text = fromStatus
        tvToStatus.text = toStatus
        btnConfirmUpdate.text = confirmLabel.ifBlank {
            getString(R.string.status_update_confirm_update)
        }
    }

    private fun setupActions(): Unit = with(binding) {
        btnNotNow.setOnClickListener {
            dismiss()
        }

        btnConfirmUpdate.setOnClickListener {
            confirmStatusUpdate()
        }
    }

    private fun confirmStatusUpdate() {
        val result = Bundle().apply {
            putString(KEY_ORDER_ID, orderId)
            putString(KEY_FROM_STATUS, fromStatus)
            putString(KEY_TO_STATUS, toStatus)
            putString(KEY_NOTE, binding.etStatusNote.text.toString().trim())
        }

        parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "status_update_confirmation_request"
        const val KEY_ORDER_ID = "key_order_id"
        const val KEY_FROM_STATUS = "key_from_status"
        const val KEY_TO_STATUS = "key_to_status"
        const val KEY_NOTE = "key_note"

        private const val ARG_ORDER_ID = "arg_order_id"
        private const val ARG_FROM_STATUS = "arg_from_status"
        private const val ARG_TO_STATUS = "arg_to_status"
        private const val ARG_CONFIRM_LABEL = "arg_confirm_label"
        private const val DIALOG_WIDTH_RATIO = 0.92f

        fun newInstance(
            orderId: String,
            fromStatus: String,
            toStatus: String,
            confirmLabel: String,
        ): StatusUpdateConfirmationDialogFragment {
            return StatusUpdateConfirmationDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                    putString(ARG_FROM_STATUS, fromStatus)
                    putString(ARG_TO_STATUS, toStatus)
                    putString(ARG_CONFIRM_LABEL, confirmLabel)
                }
            }
        }
    }
}