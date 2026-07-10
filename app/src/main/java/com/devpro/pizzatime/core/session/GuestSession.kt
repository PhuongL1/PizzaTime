package com.devpro.pizzatime.core.session

import com.google.firebase.auth.FirebaseAuth

object GuestSession {

    fun isGuest(): Boolean = FirebaseAuth.getInstance().currentUser == null

    fun isSignedIn(): Boolean = !isGuest()
}
