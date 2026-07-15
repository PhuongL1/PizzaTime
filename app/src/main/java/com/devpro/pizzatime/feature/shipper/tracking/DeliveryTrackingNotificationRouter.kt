package com.devpro.pizzatime.feature.shipper.tracking

import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.devpro.pizzatime.R
import com.devpro.pizzatime.core.config.AppEdition
import com.devpro.pizzatime.core.config.AppEditionConfig
import com.devpro.pizzatime.core.notification.DeliveryTrackingNotificationContract
import com.devpro.pizzatime.core.session.FakeSessionStore
import com.devpro.pizzatime.core.session.UserRole
import com.devpro.pizzatime.feature.shipper.detail.ShipperDeliveryDetailFragment
import com.google.firebase.auth.FirebaseAuth

/** Routes only the foreground-service notification and never touches the inbox/read pipeline. */
object DeliveryTrackingNotificationRouter {
    private var pendingOrderId: String? = null

    fun captureIntent(intent: Intent?) {
        val rawOrderId = intent?.getStringExtra(DeliveryTrackingNotificationContract.EXTRA_ORDER_ID)
            ?: return
        intent.removeExtra(DeliveryTrackingNotificationContract.EXTRA_ORDER_ID)
        pendingOrderId = DeliveryTrackingOrderIdPolicy.normalize(rawOrderId)
    }

    fun handlePending(fragmentManager: FragmentManager): Boolean {
        val orderId = pendingOrderId ?: return false
        if (AppEditionConfig.current != AppEdition.SHIPPER) {
            pendingOrderId = null
            return false
        }
        if (FirebaseAuth.getInstance().currentUser == null || !FakeSessionStore.isLoggedIn) {
            return false
        }
        if (FakeSessionStore.currentRole != UserRole.SHIPPER) {
            pendingOrderId = null
            return false
        }
        if (fragmentManager.isStateSaved) {
            return false
        }

        val current = fragmentManager.findFragmentById(R.id.fragmentContainer)
        if (
            current is ShipperDeliveryDetailFragment &&
            current.arguments?.getString(ShipperDeliveryDetailFragment.ARG_ORDER_ID) == orderId
        ) {
            pendingOrderId = null
            return true
        }

        pendingOrderId = null
        fragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(
                R.id.fragmentContainer,
                ShipperDeliveryDetailFragment.newInstance(orderId),
            )
            .addToBackStack(null)
            .commit()
        return true
    }

    internal fun clearForTest() {
        pendingOrderId = null
    }
}
