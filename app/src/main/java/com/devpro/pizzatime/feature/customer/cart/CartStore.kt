package com.devpro.pizzatime.feature.customer.cart

import com.google.firebase.auth.FirebaseAuth

object CartStore {

    private val cartItems = mutableListOf<CartItemUiModel>()
    private var ownerUserId: String? = null

    val items: List<CartItemUiModel>
        get() {
            syncOwnerWithCurrentUser()
            return cartItems.toList()
        }

    fun onUserChanged(uid: String) {
        val newOwnerId = uid.trim()
        if (newOwnerId.isBlank()) {
            clearForLogout()
            return
        }

        val previousOwnerId = ownerUserId
        if (previousOwnerId != null && previousOwnerId != newOwnerId) {
            cartItems.clear()
        }

        ownerUserId = newOwnerId
    }

    fun clearForLogout() {
        ownerUserId = null
        cartItems.clear()
    }

    fun addItem(item: CartItemUiModel) {
        syncOwnerWithCurrentUser()
        val index = cartItems.indexOfFirst { it.cartKey == item.cartKey }

        if (index >= 0) {
            val currentItem = cartItems[index]
            cartItems[index] = currentItem.copy(
                quantity = currentItem.quantity + item.quantity
            )
        } else {
            cartItems.add(item)
        }
    }

    fun increaseQuantity(cartKey: String) {
        syncOwnerWithCurrentUser()
        val index = cartItems.indexOfFirst { it.cartKey == cartKey }
        if (index < 0) return

        val item = cartItems[index]
        cartItems[index] = item.copy(quantity = item.quantity + 1)
    }

    fun decreaseQuantity(cartKey: String) {
        syncOwnerWithCurrentUser()
        val index = cartItems.indexOfFirst { it.cartKey == cartKey }
        if (index < 0) return

        val item = cartItems[index]

        if (item.quantity > 1) {
            cartItems[index] = item.copy(quantity = item.quantity - 1)
        } else {
            cartItems.removeAt(index)
        }
    }

    fun removeItem(cartKey: String) {
        syncOwnerWithCurrentUser()
        cartItems.removeAll { it.cartKey == cartKey }
    }

    fun replaceItems(items: List<CartItemUiModel>) {
        syncOwnerWithCurrentUser()
        cartItems.clear()
        cartItems.addAll(items)
    }

    fun clear() {
        cartItems.clear()
    }

    private fun syncOwnerWithCurrentUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            onUserChanged(currentUid)
        }
    }
}
