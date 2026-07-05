package com.devpro.pizzatime.feature.order

import kotlin.random.Random

object OrderCodeGenerator {

    private const val LETTERS = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGIT_BOUND = 10_000
    private const val FALLBACK_ID_LENGTH = 7

    fun generateOrderCodeKey(): String {
        val prefix = buildString {
            repeat(2) {
                append(LETTERS[Random.nextInt(LETTERS.length)])
            }
        }
        val digits = Random.nextInt(DIGIT_BOUND).toString().padStart(4, '0')
        return "$prefix-$digits"
    }

    fun displayOrderCode(key: String): String {
        val normalizedKey = key.trim().removePrefix("#")
        return if (normalizedKey.isBlank()) {
            fallbackFromOrderId(key)
        } else {
            "#$normalizedKey"
        }
    }

    fun displayOrderCode(orderCode: String?, orderId: String): String {
        return orderCode?.takeIf { it.isNotBlank() }?.let { displayOrderCode(it) }
            ?: fallbackFromOrderId(orderId)
    }

    fun fallbackFromOrderId(orderId: String): String {
        val fallback = orderId.trim().removePrefix("#").take(FALLBACK_ID_LENGTH)
        return if (fallback.isBlank()) "#unknown" else "#$fallback"
    }
}
