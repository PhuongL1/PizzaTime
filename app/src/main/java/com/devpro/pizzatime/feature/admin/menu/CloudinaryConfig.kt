package com.devpro.pizzatime.feature.admin.menu

object CloudinaryConfig {
    const val CLOUD_NAME = "zdyxyf4h"
    const val UPLOAD_PRESET = "pizzatime_products"
    const val FOLDER = "pizzatime/products"
    const val AVATAR_FOLDER = "pizzatime/avatars"

    val isConfigured: Boolean
        get() = CLOUD_NAME.isNotBlank() &&
            UPLOAD_PRESET.isNotBlank()
}
