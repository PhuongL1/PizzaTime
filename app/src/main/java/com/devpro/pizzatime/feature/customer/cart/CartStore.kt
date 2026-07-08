package com.devpro.pizzatime.feature.customer.cart

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

object CartStore {

    private val cartItems = mutableListOf<CartItemUiModel>()
    private var ownerUserId: String? = null
    private var appContext: Context? = null
    private var restored = false
    var selectedPromoCode: String = ""
        private set
    var promoDiscountAmount: Double = 0.0
        private set

    val items: List<CartItemUiModel>
        get() {
            syncOwnerWithCurrentUser()
            return cartItems.toList()
        }

    fun init(context: Context) {
        appContext = context.applicationContext
        restoreFromDisk()
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
            clearPromo()
        }

        ownerUserId = newOwnerId
        if (!restored) {
            restoreFromDisk()
        }
    }

    fun clearForLogout() {
        ownerUserId = null
        cartItems.clear()
        clearPromo()
        clearPersistedCart()
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
        clearPromo()
        saveToDisk()
    }

    fun increaseQuantity(cartKey: String) {
        syncOwnerWithCurrentUser()
        val index = cartItems.indexOfFirst { it.cartKey == cartKey }
        if (index < 0) return

        val item = cartItems[index]
        cartItems[index] = item.copy(quantity = item.quantity + 1)
        clearPromo()
        saveToDisk()
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
        clearPromo()
        saveToDisk()
    }

    fun removeItem(cartKey: String) {
        syncOwnerWithCurrentUser()
        cartItems.removeAll { it.cartKey == cartKey }
        clearPromo()
        saveToDisk()
    }

    fun replaceItems(items: List<CartItemUiModel>) {
        syncOwnerWithCurrentUser()
        cartItems.clear()
        cartItems.addAll(items)
        clearPromo()
        saveToDisk()
    }

    fun clear() {
        cartItems.clear()
        clearPromo()
        saveToDisk()
    }

    fun setPromo(code: String, discountAmount: Double) {
        selectedPromoCode = code.trim()
        promoDiscountAmount = discountAmount.coerceAtLeast(0.0)
        saveToDisk()
    }

    fun clearPromo() {
        selectedPromoCode = ""
        promoDiscountAmount = 0.0
        saveToDisk()
    }

    private fun syncOwnerWithCurrentUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            onUserChanged(currentUid)
        }
    }

    private fun restoreFromDisk() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val persistedOwnerId = prefs.getString(KEY_OWNER_ID, "").orEmpty()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (currentUid.isNotBlank()) {
            ownerUserId = currentUid
        }
        val effectiveOwnerId = ownerUserId.orEmpty()
        if (persistedOwnerId.isNotBlank() && effectiveOwnerId.isNotBlank() && persistedOwnerId != effectiveOwnerId) {
            restored = true
            cartItems.clear()
            selectedPromoCode = ""
            promoDiscountAmount = 0.0
            return
        }

        val rawCart = prefs.getString(KEY_CART_JSON, "").orEmpty()
        if (rawCart.isBlank()) {
            restored = true
            return
        }

        runCatching {
            val payload = JSONObject(rawCart)
            val itemsJson = payload.optJSONArray("items") ?: JSONArray()
            cartItems.clear()
            for (index in 0 until itemsJson.length()) {
                val itemJson = itemsJson.optJSONObject(index) ?: continue
                cartItems.add(itemJson.toCartItem())
            }
            selectedPromoCode = payload.optString("selectedPromoCode").trim()
            promoDiscountAmount = payload.optDouble("promoDiscountAmount", 0.0).coerceAtLeast(0.0)
        }.onFailure {
            cartItems.clear()
            selectedPromoCode = ""
            promoDiscountAmount = 0.0
        }
        restored = true
    }

    private fun saveToDisk() {
        val context = appContext ?: return
        val ownerId = ownerUserId ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val payload = JSONObject().apply {
            put("selectedPromoCode", selectedPromoCode)
            put("promoDiscountAmount", promoDiscountAmount)
            put(
                "items",
                JSONArray().apply {
                    cartItems.forEach { item -> put(item.toJson()) }
                },
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_OWNER_ID, ownerId)
            .putString(KEY_CART_JSON, payload.toString())
            .apply()
    }

    private fun clearPersistedCart() {
        val context = appContext ?: return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_OWNER_ID)
            .remove(KEY_CART_JSON)
            .apply()
    }

    private fun CartItemUiModel.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("price", price)
            put("quantity", quantity)
            put("imageRes", imageRes)
            put("selectedSize", selectedSize)
            put("selectedCrust", selectedCrust)
            put("imageUrl", imageUrl)
            put(
                "selectedToppings",
                JSONArray().apply {
                    selectedToppings.forEach { topping -> put(topping) }
                },
            )
        }
    }

    private fun JSONObject.toCartItem(): CartItemUiModel {
        val toppingsJson = optJSONArray("selectedToppings") ?: JSONArray()
        val toppings = buildList {
            for (index in 0 until toppingsJson.length()) {
                val topping = toppingsJson.optString(index).trim()
                if (topping.isNotBlank()) add(topping)
            }
        }
        return CartItemUiModel(
            id = optString("id"),
            name = optString("name"),
            description = optString("description"),
            price = optDouble("price", 0.0),
            quantity = optInt("quantity", 1).coerceAtLeast(1),
            imageRes = optInt("imageRes", 0),
            selectedSize = optString("selectedSize"),
            selectedCrust = optString("selectedCrust"),
            selectedToppings = toppings,
            imageUrl = optString("imageUrl"),
        )
    }

    private const val PREFS_NAME = "pizza_time_cart"
    private const val KEY_OWNER_ID = "ownerUserId"
    private const val KEY_CART_JSON = "cartJson"
}
