package com.devpro.pizzatime.core.ui.message

import androidx.annotation.StringRes
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppUiMessageBus {

    private val mutableMessages = MutableSharedFlow<UiMessage>(
        extraBufferCapacity = MESSAGE_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val messages = mutableMessages.asSharedFlow()

    fun publish(message: UiMessage) {
        mutableMessages.tryEmit(message)
    }

    fun publish(
        @StringRes textRes: Int,
        type: UiMessageType = UiMessageType.INFO,
        args: List<Any> = emptyList(),
    ) {
        publish(
            UiMessage(
                text = UiText.Resource(
                    resId = textRes,
                    args = args,
                ),
                type = type,
            ),
        )
    }

    private const val MESSAGE_BUFFER_CAPACITY = 8
}
