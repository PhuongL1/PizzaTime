package com.devpro.pizzatime.core.product

import java.util.Locale

object ProductOptionDefaults {
    const val CATEGORY_ID_PIZZA = "pizza"
    const val CATEGORY_ID_DRINK = "drink"
    const val CATEGORY_ID_COMBO = "combo"
    const val CATEGORY_ID_DESSERT = "dessert"

    val sizeOptions = listOf("Small", "Medium", "Large")
    val crustOptions = listOf("Classic", "Thin", "Cheese Burst")
    val toppingOptions = listOf("Extra Cheese", "Mushroom", "Olives", "Pepperoni")

    enum class ProductCategory {
        PIZZA,
        DRINK,
        COMBO,
        DESSERT,
        UNKNOWN,
    }

    fun resolveProductCategory(
        categoryId: String,
        categoryName: String = "",
    ): ProductCategory {
        val normalized = listOf(categoryId, categoryName)
            .joinToString(separator = " ")
            .trim()
            .lowercase(Locale.US)
        return when {
            normalized.contains("drink") || normalized.contains("beverage") -> ProductCategory.DRINK
            normalized.contains("combo") -> ProductCategory.COMBO
            normalized.contains("dessert") -> ProductCategory.DESSERT
            normalized.contains("pizza") ||
                normalized.contains("signature") ||
                normalized.contains("classic") ||
                normalized.contains("veggie") -> ProductCategory.PIZZA
            else -> ProductCategory.UNKNOWN
        }
    }

    fun canonicalCategoryId(
        categoryId: String,
        categoryName: String = "",
    ): String {
        return when (resolveProductCategory(categoryId, categoryName)) {
            ProductCategory.PIZZA -> CATEGORY_ID_PIZZA
            ProductCategory.DRINK -> CATEGORY_ID_DRINK
            ProductCategory.COMBO -> CATEGORY_ID_COMBO
            ProductCategory.DESSERT -> CATEGORY_ID_DESSERT
            ProductCategory.UNKNOWN -> categoryId.trim().ifBlank { categoryName.trim() }
        }
    }

    fun supportsSizeOptions(category: ProductCategory): Boolean {
        return category == ProductCategory.PIZZA || category == ProductCategory.DRINK
    }

    fun supportsCrustOptions(category: ProductCategory): Boolean {
        return category == ProductCategory.PIZZA
    }

    fun supportsToppingOptions(category: ProductCategory): Boolean {
        return category == ProductCategory.PIZZA
    }

    fun sizeOptionsFor(category: ProductCategory): List<String> {
        return if (supportsSizeOptions(category)) sizeOptions else emptyList()
    }

    fun crustOptionsFor(category: ProductCategory): List<String> {
        return if (supportsCrustOptions(category)) crustOptions else emptyList()
    }

    fun toppingOptionsFor(category: ProductCategory): List<String> {
        return if (supportsToppingOptions(category)) toppingOptions else emptyList()
    }

    fun sanitizeSizeOptions(
        options: List<String>,
        category: ProductCategory,
    ): List<String> {
        if (!supportsSizeOptions(category)) return emptyList()
        val normalizedSelections = options
            .mapNotNull(::normalizeSizeOption)
            .toSet()
        return sizeOptions.filter(normalizedSelections::contains)
    }

    fun sizesOrDefault(
        options: List<String>,
        category: ProductCategory = ProductCategory.PIZZA,
    ): List<String> {
        val normalized = sanitizeSizeOptions(options, category)
        return if (normalized.isEmpty()) sizeOptionsFor(category) else normalized
    }

    fun sanitizeCrustOptions(
        options: List<String>,
        category: ProductCategory,
    ): List<String> {
        if (!supportsCrustOptions(category)) return emptyList()
        val normalizedSelections = options
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
            .toSet()
        return crustOptions.filter(normalizedSelections::contains)
    }

    fun crustsOrDefault(
        options: List<String>,
        category: ProductCategory = ProductCategory.PIZZA,
    ): List<String> {
        val normalized = sanitizeCrustOptions(options, category)
        return if (normalized.isEmpty()) crustOptionsFor(category) else normalized
    }

    fun sanitizeToppingOptions(
        options: List<String>,
        category: ProductCategory,
    ): List<String> {
        if (!supportsToppingOptions(category)) return emptyList()
        return options
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
    }

    private fun normalizeSizeOption(option: String): String? {
        val normalized = option.trim().lowercase(Locale.US)
        return when {
            normalized.contains("small") -> "Small"
            normalized.contains("medium") -> "Medium"
            normalized.contains("large") || normalized.contains("family") -> "Large"
            else -> null
        }
    }
}
