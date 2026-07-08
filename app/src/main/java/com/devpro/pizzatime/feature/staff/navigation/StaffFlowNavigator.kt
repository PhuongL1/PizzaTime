package com.devpro.pizzatime.feature.staff.navigation

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.feature.admin.dashboard.AdminDashboardFragment
import com.devpro.pizzatime.feature.admin.menu.ManageMenuFragment
import com.devpro.pizzatime.feature.admin.orders.ManageOrdersFragment
import com.devpro.pizzatime.feature.admin.product.AddEditProductFragment
import com.devpro.pizzatime.feature.admin.promo.ManagePromoCodesFragment
import com.devpro.pizzatime.feature.admin.reports.ReportsFragment
import com.devpro.pizzatime.feature.admin.staff.ManageStaffFragment
import com.devpro.pizzatime.feature.admin.store.StoreSettingsFragment
import com.devpro.pizzatime.feature.auth.LoginFragment
import com.devpro.pizzatime.feature.auth.LoginRequiredFragment
import com.devpro.pizzatime.feature.auth.forgot.ForgotPasswordFragment
import com.devpro.pizzatime.feature.customer.account.CustomerAccountFragment
import com.devpro.pizzatime.feature.customer.cart.CartFragment
import com.devpro.pizzatime.feature.customer.customize.BuildYourPizzaFragment
import com.devpro.pizzatime.feature.customer.home.CustomerHomeFragment
import com.devpro.pizzatime.feature.customer.memberqr.CustomerMemberQrFragment
import com.devpro.pizzatime.feature.customer.notifications.CustomerNotificationsFragment
import com.devpro.pizzatime.feature.customer.order.OrderTypeFragment
import com.devpro.pizzatime.feature.customer.orderdetail.CustomerOrderDetailFragment
import com.devpro.pizzatime.feature.customer.orderhistory.CustomerOrderHistoryFragment
import com.devpro.pizzatime.feature.customer.ordersuccess.OrderSuccessFragment
import com.devpro.pizzatime.feature.customer.promos.CustomerPromoCodesFragment
import com.devpro.pizzatime.feature.customer.support.SupportFaqFragment
import com.devpro.pizzatime.feature.customer.tracking.OrderTrackingFragment
import com.devpro.pizzatime.feature.customer.detail.PizzaDetailFragment
import com.devpro.pizzatime.feature.customer.favorites.CustomerFavoritesFragment
import com.devpro.pizzatime.feature.customer.menu.PizzaMenuFragment
import com.devpro.pizzatime.feature.kitchen.board.KitchenBoardFragment
import com.devpro.pizzatime.feature.kitchen.detail.KitchenOrderDetailFragment
import com.devpro.pizzatime.feature.shipper.dashboard.ShipperDeliveryDashboardFragment
import com.devpro.pizzatime.feature.shipper.detail.ShipperDeliveryDetailFragment
import com.devpro.pizzatime.feature.staff.dashboard.StaffDashboardFragment
import com.devpro.pizzatime.feature.staff.detail.StaffOrderDetailFragment
import com.devpro.pizzatime.shared.drawer.StaffDrawerItem
import com.devpro.pizzatime.shared.drawer.StaffNavigationDrawerDialogFragment
import com.google.firebase.auth.FirebaseAuth

fun Fragment.openStaffDashboard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = StaffDashboardFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openLoginRequiredScreen(addToBackStack: Boolean = true) {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
        )
        .replace(
            R.id.fragmentContainer,
            LoginRequiredFragment(),
        )
        .apply {
            if (addToBackStack) {
                addToBackStack(null)
            }
        }
        .commit()
}
fun Fragment.openLoginScreen(addToBackStack: Boolean = true) {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
        )
        .replace(
            R.id.fragmentContainer,
            LoginFragment(),
        )
        .apply {
            if (addToBackStack) {
                addToBackStack(null)
            }
        }
        .commit()
}

fun Fragment.clearAppBackStack() {
    parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
}

fun Fragment.signOutAndOpenLogin() {
    FirebaseAuth.getInstance().signOut()
    FakeSessionStore.logout()
    com.devpro.pizzatime.feature.customer.cart.CartStore.clearForLogout()
    clearAppBackStack()
    openLoginScreen(addToBackStack = false)
}

fun Fragment.openCustomerHomeScreen() {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
        )
        .replace(
            R.id.fragmentContainer,
            CustomerHomeFragment(),
        )
        .commit()
}
fun Fragment.openKitchenBoard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = KitchenBoardFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openKitchenOrderDetail(orderId: String) {
    replaceStaffFlowFragment(
        fragment = KitchenOrderDetailFragment.newInstance(orderId),
        addToBackStack = true,
    )
}

fun Fragment.openPizzaMenuScreen(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = PizzaMenuFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openPizzaDetailScreen(
    productId: String = "",
    productName: String = "",
    productDescription: String = "",
    productPrice: String = "",
    productRating: String = "",
    productImageUrl: String = "",
    productCategoryId: String = "",
    productCategoryName: String = "",
    productSizeOptions: List<String> = emptyList(),
    productCrustOptions: List<String> = emptyList(),
    productToppingOptions: List<String> = emptyList(),
    addToBackStack: Boolean = true,
) {
    replaceStaffFlowFragment(
        fragment = PizzaDetailFragment.newInstance(
            productId = productId,
            name = productName,
            description = productDescription,
            price = productPrice,
            rating = productRating,
            imageUrl = productImageUrl,
            categoryId = productCategoryId,
            categoryName = productCategoryName,
            sizeOptions = productSizeOptions,
            crustOptions = productCrustOptions,
            toppingOptions = productToppingOptions,
        ),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openShipperDeliveryDashboard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ShipperDeliveryDashboardFragment(),
        addToBackStack = addToBackStack,
    )
}


fun Fragment.openStaffOrderDetail(orderId: String) {
    replaceStaffFlowFragment(
        fragment = StaffOrderDetailFragment.newInstance(orderId),
        addToBackStack = true,
    )
}

fun Fragment.openOrderSuccess(
    orderId: String,
    addToBackStack: Boolean = true,
) {
    replaceStaffFlowFragment(
        fragment = OrderSuccessFragment.newInstance(orderId),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openCustomerHome(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CustomerHomeFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openOrderTracking(orderId: String = "", addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = OrderTrackingFragment.newInstance(orderId),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openShipperDeliveryDetail(orderId: String) {
    replaceStaffFlowFragment(
        fragment = ShipperDeliveryDetailFragment.newInstance(orderId),
        addToBackStack = true,
    )
}

fun Fragment.openAdminDashboard(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = AdminDashboardFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openManageMenu(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ManageMenuFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openManagePromoCodes(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ManagePromoCodesFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openManageStaff(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ManageStaffFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openReports(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ReportsFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openStoreSettings(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = StoreSettingsFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openOrderType(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = OrderTypeFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openBuildYourPizza(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = BuildYourPizzaFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openCustomerOrderDetail(orderId: String) {
    replaceStaffFlowFragment(
        fragment = CustomerOrderDetailFragment.newInstance(orderId),
        addToBackStack = true,
    )
}
fun Fragment.openCustomerOrderHistory(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CustomerOrderHistoryFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openCartScreen(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CartFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openCustomerPromoCodes(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CustomerPromoCodesFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openCustomerMemberQr(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CustomerMemberQrFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openManageOrders(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ManageOrdersFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openAddEditProduct(
    productId: String? = null,
    addToBackStack: Boolean = true,
) {
    replaceStaffFlowFragment(
        fragment = AddEditProductFragment.newInstance(productId),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openForgotPassword(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = ForgotPasswordFragment(),
        addToBackStack = addToBackStack,
    )
}

fun Fragment.openCustomerNotifications(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CustomerNotificationsFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openSupportFaq(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = SupportFaqFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openCustomerAccount(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CustomerAccountFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.openCustomerFavorites(addToBackStack: Boolean = true) {
    replaceStaffFlowFragment(
        fragment = CustomerFavoritesFragment(),
        addToBackStack = addToBackStack,
    )
}
fun Fragment.setupStaffDrawer(
    avatarView: View,
    selectedItem: StaffDrawerItem = StaffDrawerItem.STAFF_SCHEDULE,
    onItemSelected: ((StaffDrawerItem) -> Unit)? = null,
) {
    parentFragmentManager.setFragmentResultListener(
        StaffNavigationDrawerDialogFragment.REQUEST_KEY,
        viewLifecycleOwner,
    ) { _, bundle ->
        val item = StaffDrawerItem.fromAction(
            bundle.getString(StaffNavigationDrawerDialogFragment.KEY_ACTION),
        ) ?: return@setFragmentResultListener

        if (onItemSelected != null) {
            onItemSelected(item)
        } else {
            handleStaffDrawerItem(item)
        }
    }

    avatarView.setOnClickListener {
        openStaffDrawer(selectedItem)
    }
}

fun Fragment.openStaffDrawer(
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

private fun Fragment.handleStaffDrawerItem(item: StaffDrawerItem) {
    when (item) {
        StaffDrawerItem.ORDER_HISTORY -> {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_drawer_order_history_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        StaffDrawerItem.INVENTORY -> {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_drawer_inventory_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        StaffDrawerItem.STAFF_SCHEDULE -> {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_drawer_schedule_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        StaffDrawerItem.SUPPORT -> {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_drawer_support_toast),
                Toast.LENGTH_SHORT,
            ).show()
        }

        StaffDrawerItem.LOGOUT -> {
            Toast.makeText(
                requireContext(),
                getString(R.string.staff_drawer_logout_toast),
                Toast.LENGTH_SHORT,
            ).show()
            signOutAndOpenLogin()
        }
    }
}
fun Fragment.backToPreviousStaffScreen() {
    parentFragmentManager.popBackStack()
}

private fun Fragment.replaceStaffFlowFragment(
    fragment: Fragment,
    addToBackStack: Boolean,
) {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.fragmentContainer, fragment)
        .applyBackStack(addToBackStack)
        .commit()
}

private fun FragmentTransaction.applyBackStack(
    addToBackStack: Boolean,
): FragmentTransaction {
    return if (addToBackStack) {
        addToBackStack(null)
    } else {
        this
    }
}
