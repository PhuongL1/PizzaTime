package com.devpro.pizzatime.feature.admin.menu

object CloudinaryConfig {
    const val CLOUD_NAME = "ASK_ME_TO_FILL"
    const val UPLOAD_PRESET = "ASK_ME_TO_FILL"
    const val FOLDER = "pizzatime/products"

    val isConfigured: Boolean
        get() = CLOUD_NAME.isNotBlank() &&
            UPLOAD_PRESET.isNotBlank() &&
            CLOUD_NAME != "ASK_ME_TO_FILL" &&
            UPLOAD_PRESET != "ASK_ME_TO_FILL"
}
