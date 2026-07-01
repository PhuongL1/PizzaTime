package com.devpro.pizzatime.shared.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.DialogAssignShipperBinding
import com.devpro.pizzatime.databinding.ItemAssignShipperBinding

class AssignShipperDialogFragment : DialogFragment() {

    private var _binding: DialogAssignShipperBinding? = null
    private val binding: DialogAssignShipperBinding
        get() = checkNotNull(_binding) {
            "DialogAssignShipperBinding is only valid between onCreateView and onDestroyView."
        }

    private val orderId: String
        get() = requireArguments().getString(ARG_ORDER_ID).orEmpty()

    private val address: String
        get() = requireArguments().getString(ARG_ADDRESS).orEmpty()

    private val shippers: List<AssignableShipperUiModel> = FakeAssignableShipperData.getShippers()

    private var selectedShipperId: String? = null

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
        _binding = DialogAssignShipperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                dimAmount = DIALOG_DIM_AMOUNT
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindOrderInfo()
        bindInitialSelection()
        bindShippers()
        setupActions()
    }

    private fun bindOrderInfo(): Unit = with(binding) {
        tvOrderIdValue.text = orderId
        tvAddressValue.text = address
    }

    private fun bindInitialSelection() {
        selectedShipperId = shippers
            .filter { shipper -> shipper.isAvailable }
            .minWithOrNull(
                compareBy<AssignableShipperUiModel> { shipper -> shipper.activeDeliveryCount }
                    .thenBy { shipper -> shipper.etaMinutes },
            )
            ?.id
    }

    private fun bindShippers() {
        binding.shippersContainer.removeAllViews()

        shippers.forEach { shipper ->
            val itemBinding = ItemAssignShipperBinding.inflate(
                layoutInflater,
                binding.shippersContainer,
                false,
            )

            bindShipperItem(
                itemBinding = itemBinding,
                shipper = shipper,
            )

            binding.shippersContainer.addView(itemBinding.root)
        }

        updateAssignButtonState()
    }

    private fun bindShipperItem(
        itemBinding: ItemAssignShipperBinding,
        shipper: AssignableShipperUiModel,
    ) {
        val isSelected = shipper.id == selectedShipperId

        itemBinding.root.isEnabled = shipper.isAvailable
        itemBinding.root.alpha = if (shipper.isAvailable) ENABLED_ALPHA else DISABLED_ALPHA
        itemBinding.root.setBackgroundResource(
            when {
                isSelected -> R.drawable.bg_assign_shipper_row_selected
                shipper.isAvailable -> R.drawable.bg_assign_shipper_row
                else -> R.drawable.bg_assign_shipper_row_disabled
            },
        )

        itemBinding.ivAvatar.setImageResource(shipper.avatarRes)
        itemBinding.tvShipperName.text = shipper.name
        itemBinding.tvDeliveryMeta.text = getString(
            R.string.assign_shipper_delivery_meta,
            shipper.activeDeliveryCount,
            shipper.etaMinutes,
        )

        itemBinding.tvAvailability.text = if (shipper.isAvailable) {
            getString(R.string.assign_shipper_available)
        } else {
            getString(R.string.assign_shipper_busy)
        }

        itemBinding.tvAvailability.setBackgroundResource(
            if (shipper.isAvailable) {
                R.drawable.bg_assign_shipper_available_badge
            } else {
                R.drawable.bg_assign_shipper_busy_badge
            },
        )

        itemBinding.ivSelectedCheck.isVisible = isSelected

        itemBinding.root.setOnClickListener {
            if (!shipper.isAvailable) return@setOnClickListener

            selectedShipperId = shipper.id
            bindShippers()
        }
    }

    private fun setupActions(): Unit = with(binding) {
        btnCancelAssign.setOnClickListener {
            dismiss()
        }

        btnAssignShipper.setOnClickListener {
            assignSelectedShipper()
        }
    }

    private fun assignSelectedShipper() {
        val selectedShipper = shippers.firstOrNull { shipper ->
            shipper.id == selectedShipperId
        }

        if (selectedShipper == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.assign_shipper_no_shipper_selected),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val result = Bundle().apply {
            putString(KEY_ORDER_ID, orderId)
            putString(KEY_SHIPPER_ID, selectedShipper.id)
            putString(KEY_SHIPPER_NAME, selectedShipper.name)
        }

        parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
        dismiss()
    }

    private fun updateAssignButtonState() {
        val hasSelectedShipper = selectedShipperId != null

        binding.btnAssignShipper.isEnabled = hasSelectedShipper
        binding.btnAssignShipper.alpha = if (hasSelectedShipper) {
            ENABLED_ALPHA
        } else {
            DISABLED_ALPHA
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "assign_shipper_request"
        const val KEY_ORDER_ID = "key_order_id"
        const val KEY_SHIPPER_ID = "key_shipper_id"
        const val KEY_SHIPPER_NAME = "key_shipper_name"

        private const val ARG_ORDER_ID = "arg_order_id"
        private const val ARG_ADDRESS = "arg_address"
        private const val DIALOG_DIM_AMOUNT = 0.72f
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.45f

        fun newInstance(
            orderId: String,
            address: String,
        ): AssignShipperDialogFragment {
            return AssignShipperDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_ID, orderId)
                    putString(ARG_ADDRESS, address)
                }
            }
        }
    }
}