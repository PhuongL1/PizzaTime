package com.devpro.pizzatime.core.ui.message

import androidx.annotation.StringRes

sealed interface UiText {

    data class Resource(
        @param:StringRes @get:StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    @JvmInline
    value class Dynamic private constructor(
        val value: String,
    ) : UiText {

        companion object {
            fun from(value: String): Dynamic? {
                return value.trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let(::Dynamic)
            }
        }
    }
}

data class UiMessage(
    val text: UiText,
    val type: UiMessageType = UiMessageType.INFO,
    val duration: UiMessageDuration = UiMessageDuration.SHORT,
)
