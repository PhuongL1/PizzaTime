package com.devpro.pizzatime.feature.customer.cart

object CartStore {

    private val cartItems = mutableListOf<CartItemUiModel>()

    val items: List<CartItemUiModel>
        get() = cartItems.toList()

    fun addItem(item: CartItemUiModel) {
        val index = cartItems.indexOfFirst { it.id == item.id }

        if (index >= 0) {
            val currentItem = cartItems[index]
            cartItems[index] = currentItem.copy(
                quantity = currentItem.quantity + item.quantity
            )
        } else {
            cartItems.add(item)
        }
    }

    fun increaseQuantity(id: String) {
        val index = cartItems.indexOfFirst { it.id == id }
        if (index < 0) return

        val item = cartItems[index]
        cartItems[index] = item.copy(quantity = item.quantity + 1)
    }

    fun decreaseQuantity(id: String) {
        val index = cartItems.indexOfFirst { it.id == id }
        if (index < 0) return

        val item = cartItems[index]

        if (item.quantity > 1) {
            cartItems[index] = item.copy(quantity = item.quantity - 1)
        } else {
            cartItems.removeAt(index)
        }
    }

    fun removeItem(id: String) {
        cartItems.removeAll { it.id == id }
    }

    fun clear() {
        cartItems.clear()
    }
}