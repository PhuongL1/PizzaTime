package com.devpro.pizzatime.shared.drawer

import androidx.fragment.app.Fragment

fun Fragment.showStaffNavigationDrawer(
    selectedItem: StaffDrawerItem = StaffDrawerItem.STAFF_SCHEDULE,
) {
    val existingDrawer = parentFragmentManager.findFragmentByTag(
        StaffNavigationDrawerDialogFragment.TAG,
    )

    if (existingDrawer != null) {
        return
    }

    StaffNavigationDrawerDialogFragment
        .newInstance(selectedItem)
        .show(
            parentFragmentManager,
            StaffNavigationDrawerDialogFragment.TAG,
        )
}

fun Fragment.setStaffNavigationDrawerResultListener(
    onItemSelected: (StaffDrawerItem) -> Unit,
) {
    parentFragmentManager.setFragmentResultListener(
        StaffNavigationDrawerDialogFragment.REQUEST_KEY,
        viewLifecycleOwner,
    ) { _, bundle ->
        val item = StaffDrawerItem.fromAction(
            bundle.getString(StaffNavigationDrawerDialogFragment.KEY_ACTION),
        ) ?: return@setFragmentResultListener

        onItemSelected(item)
    }
}