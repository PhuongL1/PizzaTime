package com.devpro.pizzatime.feature.customer.cart

import android.content.Context
import android.util.Log
import com.devpro.pizzatime.R
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

object CartStore {

    private val cartItems = mutableListOf<CartItemUiModel>()
    private var ownerUserId: String = GUEST_OWNER_ID
    private var appContext: Context? = null
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
        ownerUserId = currentOwnerId()
        restoreFromDisk(ownerUserId)
    }

    fun onUserChanged(uid: String) {
        val newOwnerId = uid.trim()
        if (newOwnerId.isBlank()) {
            clearForLogout()
            return
        }

        if (ownerUserId == newOwnerId) {
            return
        }

        val guestItems = if (ownerUserId == GUEST_OWNER_ID) cartItems.toList() else emptyList()
        ownerUserId = newOwnerId
        restoreFromDisk(newOwnerId)

        if (guestItems.isNotEmpty()) {
            mergeItems(guestItems)
            selectedPromoCode = ""
            promoDiscountAmount = 0.0
            saveToDisk()
            Log.d(TAG, "Guest cart migrated after login count=${cartItems.sumOf { it.quantity }}")
        }
    }

    fun clearForLogout() {
        ownerUserId = GUEST_OWNER_ID
        restoreFromDisk(GUEST_OWNER_ID)
        Log.d(TAG, "Guest cart restored count=${cartItems.sumOf { it.quantity }}")
    }

    fun onGuestSessionStarted() {
        ownerUserId = GUEST_OWNER_ID
        restoreFromDisk(GUEST_OWNER_ID)
        Log.d(TAG, "Guest session entered cartCount=${cartItems.sumOf { it.quantity }}")
    }

    fun addItem(item: CartItemUiModel) {
        syncOwnerWithCurrentUser()
        mergeItems(listOf(item))
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
        val activeOwnerId = currentOwnerId()
        if (ownerUserId == activeOwnerId) {
            return
        }

        if (activeOwnerId == GUEST_OWNER_ID) {
            clearForLogout()
        } else {
            onUserChanged(activeOwnerId)
        }
    }

    private fun restoreFromDisk(ownerId: String) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawCart = prefs.getString(cartKey(ownerId), "").orEmpty()
            .ifBlank { legacyCartPayloadFor(ownerId, prefs) }
            .orEmpty()
        cartItems.clear()
        selectedPromoCode = ""
        promoDiscountAmount = 0.0
        if (rawCart.isBlank()) {
            return
        }

        runCatching {
            val payload = JSONObject(rawCart)
            val itemsJson = payload.optJSONArray("items") ?: JSONArray()
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
    }

    private fun saveToDisk() {
        val context = appContext ?: return
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
            .putString(cartKey(ownerUserId), payload.toString())
            .apply()
    }

    private fun mergeItems(items: List<CartItemUiModel>) {
        items.forEach { item ->
            val index = cartItems.indexOfFirst { it.cartKey == item.cartKey }
            if (index >= 0) {
                val currentItem = cartItems[index]
                cartItems[index] = currentItem.copy(
                    quantity = currentItem.quantity + item.quantity,
                )
            } else {
                cartItems.add(item)
            }
        }
    }

    private fun currentOwnerId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?.trim()
            ?.takeIf { uid -> uid.isNotBlank() }
            ?: GUEST_OWNER_ID
    }

    private fun cartKey(ownerId: String): String {
        return if (ownerId == GUEST_OWNER_ID) {
            KEY_GUEST_CART_JSON
        } else {
            "$KEY_USER_CART_PREFIX$ownerId"
        }
    }

    private fun legacyCartPayloadFor(
        ownerId: String,
        prefs: android.content.SharedPreferences,
    ): String {
        val legacyOwnerId = prefs.getString(KEY_OWNER_ID, "").orEmpty()
        val legacyPayload = prefs.getString(KEY_CART_JSON, "").orEmpty()
        if (legacyPayload.isBlank()) return ""
        return when {
            ownerId == GUEST_OWNER_ID && legacyOwnerId.isBlank() -> legacyPayload
            ownerId != GUEST_OWNER_ID && legacyOwnerId == ownerId -> legacyPayload
            else -> ""
        }
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
            imageRes = optInt("imageRes", R.drawable.img_welcome_hero)
                .takeIf { imageRes -> imageRes != 0 }
                ?: R.drawable.img_welcome_hero,
            selectedSize = optString("selectedSize"),
            selectedCrust = optString("selectedCrust"),
            selectedToppings = toppings,
            imageUrl = optString("imageUrl"),
        )
    }

    private const val PREFS_NAME = "pizza_time_cart"
    private const val GUEST_OWNER_ID = "guest"
    private const val KEY_GUEST_CART_JSON = "cart_guest"
    private const val KEY_USER_CART_PREFIX = "cart_user_"
    private const val KEY_OWNER_ID = "ownerUserId"
    private const val KEY_CART_JSON = "cartJson"
    private const val TAG = "CartStore"
}
