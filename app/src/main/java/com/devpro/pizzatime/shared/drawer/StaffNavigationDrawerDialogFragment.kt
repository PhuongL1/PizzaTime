package com.devpro.pizzatime.shared.drawer

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import com.devpro.pizzatime.R
import com.devpro.pizzatime.databinding.DialogStaffNavigationDrawerBinding

class StaffNavigationDrawerDialogFragment : DialogFragment(R.layout.dialog_staff_navigation_drawer) {

    private var _binding: DialogStaffNavigationDrawerBinding? = null
    private val binding: DialogStaffNavigationDrawerBinding
        get() = checkNotNull(_binding) {
            "DialogStaffNavigationDrawerBinding is only valid between onViewCreated and onDestroyView."
        }

    private val selectedItem: StaffDrawerItem
        get() {
            val action = arguments?.getString(ARG_SELECTED_ITEM)
            return StaffDrawerItem.fromAction(action) ?: StaffDrawerItem.STAFF_SCHEDULE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.PizzaTimeRightDrawerDialog)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            window.setGravity(Gravity.END)
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = DialogStaffNavigationDrawerBinding.bind(view)

        setupDrawerWidth()
        setupActions()
        bindSelectedItem()
        animateDrawerIn()
    }

    private fun setupDrawerWidth() = with(binding.drawerPanel) {
        post {
            val screenWidth = resources.displayMetrics.widthPixels
            val maxDrawerWidth = dpToPx(MAX_DRAWER_WIDTH_DP)
            val targetWidth = minOf(
                (screenWidth * DRAWER_WIDTH_RATIO).toInt(),
                maxDrawerWidth,
            )

            updateLayoutParams<FrameLayout.LayoutParams> {
                width = targetWidth
            }
        }
    }

    private fun setupActions() = with(binding) {
        drawerRoot.setOnClickListener {
            closeDrawer()
        }

        drawerPanel.setOnClickListener {
            // Consume click, do not close when tapping inside drawer.
        }

        navOrderHistory.setOnClickListener {
            publishAction(StaffDrawerItem.ORDER_HISTORY)
        }

        navInventory.setOnClickListener {
            publishAction(StaffDrawerItem.INVENTORY)
        }

        navStaffSchedule.setOnClickListener {
            publishAction(StaffDrawerItem.STAFF_SCHEDULE)
        }

        navSupport.setOnClickListener {
            publishAction(StaffDrawerItem.SUPPORT)
        }

        navLogout.setOnClickListener {
            publishAction(StaffDrawerItem.LOGOUT)
        }
    }

    private fun bindSelectedItem() = with(binding) {
        bindDrawerItem(
            view = navOrderHistory,
            item = StaffDrawerItem.ORDER_HISTORY,
        )
        bindDrawerItem(
            view = navInventory,
            item = StaffDrawerItem.INVENTORY,
        )
        bindDrawerItem(
            view = navStaffSchedule,
            item = StaffDrawerItem.STAFF_SCHEDULE,
        )
        bindDrawerItem(
            view = navSupport,
            item = StaffDrawerItem.SUPPORT,
        )
        bindDrawerItem(
            view = navLogout,
            item = StaffDrawerItem.LOGOUT,
        )
    }

    private fun bindDrawerItem(
        view: TextView,
        item: StaffDrawerItem,
    ) {
        val isSelected = item == selectedItem

        view.setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_staff_drawer_item_selected
            } else {
                0
            },
        )

        view.setTextColor(
            requireContext().getColor(
                if (isSelected) {
                    R.color.pt_text_secondary_dark_bg
                } else {
                    R.color.pt_border_warm
                },
            ),
        )
    }

    private fun animateDrawerIn() = with(binding.drawerPanel) {
        post {
            translationX = width.toFloat()
            animate()
                .translationX(0f)
                .setDuration(DRAWER_ENTER_DURATION_MS)
                .start()
        }
    }

    private fun publishAction(item: StaffDrawerItem) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(KEY_ACTION, item.name)
            },
        )

        closeDrawer()
    }

    private fun closeDrawer() {
        val drawerPanel = _binding?.drawerPanel

        if (drawerPanel == null) {
            dismissAllowingStateLoss()
            return
        }

        drawerPanel.animate()
            .translationX(drawerPanel.width.toFloat())
            .setDuration(DRAWER_EXIT_DURATION_MS)
            .withEndAction {
                dismissAllowingStateLoss()
            }
            .start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "StaffNavigationDrawerDialogFragment"
        const val REQUEST_KEY = "staff_navigation_drawer_request"
        const val KEY_ACTION = "key_action"

        private const val ARG_SELECTED_ITEM = "arg_selected_item"
        private const val MAX_DRAWER_WIDTH_DP = 360
        private const val DRAWER_WIDTH_RATIO = 0.9f
        private const val DRAWER_ENTER_DURATION_MS = 240L
        private const val DRAWER_EXIT_DURATION_MS = 180L

        fun newInstance(selectedItem: StaffDrawerItem): StaffNavigationDrawerDialogFragment {
            return StaffNavigationDrawerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_ITEM, selectedItem.name)
                }
            }
        }
    }
}