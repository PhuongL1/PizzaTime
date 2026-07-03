package com.devpro.pizzatime.core.notification

import com.google.firebase.messaging.FirebaseMessagingService

@Suppress("DEPRECATION")
class PizzaTimeFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenRegistrar.saveTokenForCurrentUser(token)
    }
}
