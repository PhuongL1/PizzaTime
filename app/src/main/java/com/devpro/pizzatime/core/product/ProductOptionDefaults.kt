package com.devpro.pizzatime.core.product

object ProductOptionDefaults {
    val sizeOptions = listOf("Small", "Medium", "Large")
    val crustOptions = listOf("Classic", "Thin", "Cheese Burst")
    val toppingOptions = listOf("Extra Cheese", "Mushroom", "Olives", "Pepperoni")

    fun sizesOrDefault(options: List<String>): List<String> {
        return options.ifEmpty { sizeOptions }
    }

    fun crustsOrDefault(options: List<String>): List<String> {
        return options.ifEmpty { crustOptions }
    }
}
