package com.devpro.pizzatime.core.ui.message

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.devpro.pizzatime.R
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

fun Fragment.showUiMessage(
    message: UiMessage,
    anchorView: View? = null,
    actionText: UiText? = null,
    onAction: (() -> Unit)? = null,
): Snackbar? {
    val hostView = view?.takeIf { it.isAttachedToWindow } ?: return null
    if (Looper.myLooper() != Looper.getMainLooper()) {
        hostView.post {
            if (hostView.isAttachedToWindow) {
                hostView.renderUiMessage(
                    message = message,
                    anchorView = anchorView,
                    actionText = actionText,
                    onAction = onAction,
                )
            }
        }
        return null
    }

    return hostView.renderUiMessage(
        message = message,
        anchorView = anchorView,
        actionText = actionText,
        onAction = onAction,
    )
}

fun Activity.showUiMessage(message: UiMessage): Snackbar? {
    val hostView = findViewById<View>(android.R.id.content)
        ?.takeIf { it.isAttachedToWindow }
        ?: return null
    if (Looper.myLooper() != Looper.getMainLooper()) {
        hostView.post {
            if (hostView.isAttachedToWindow) {
                hostView.renderUiMessage(
                    message = message,
                    anchorView = null,
                    actionText = null,
                    onAction = null,
                )
            }
        }
        return null
    }
    return hostView.renderUiMessage(
        message = message,
        anchorView = null,
        actionText = null,
        onAction = null,
    )
}

fun Fragment.showUiMessage(
    @StringRes textRes: Int,
    type: UiMessageType = UiMessageType.INFO,
    args: List<Any> = emptyList(),
    anchorView: View? = null,
): Snackbar? {
    return showUiMessage(
        message = UiMessage(
            text = UiText.Resource(
                resId = textRes,
                args = args,
            ),
            type = type,
        ),
        anchorView = anchorView,
    )
}

private fun View.renderUiMessage(
    message: UiMessage,
    anchorView: View?,
    actionText: UiText?,
    onAction: (() -> Unit)?,
): Snackbar? {
    val resolvedText = message.text.resolve(context) ?: return null
    val resolvedActionText = actionText?.resolve(context)?.takeIf { onAction != null }
    val previousState = getTag(R.id.ui_message_snackbar_state) as? SnackbarHostState

    if (
        resolvedActionText == null &&
        previousState?.text == resolvedText &&
        previousState.snackbar.isShownOrQueued
    ) {
        return previousState.snackbar
    }
    previousState?.snackbar?.dismiss()

    val duration = message.duration.toSnackbarDuration(hasAction = resolvedActionText != null)
    val snackbar = Snackbar.make(this, resolvedText, duration)
        .setBackgroundTint(ContextCompat.getColor(context, message.type.backgroundColorRes))
        .setTextColor(ContextCompat.getColor(context, R.color.pt_text_primary_dark_bg))
        .setActionTextColor(ContextCompat.getColor(context, R.color.pt_cream))

    (anchorView ?: findUiMessageAnchor())
        ?.takeIf { it.isAttachedToWindow && it.rootView === rootView }
        ?.let(snackbar::setAnchorView)

    if (resolvedActionText != null && onAction != null) {
        snackbar.setAction(resolvedActionText) { onAction() }
    }

    val state = SnackbarHostState(text = resolvedText, snackbar = snackbar)
    setTag(R.id.ui_message_snackbar_state, state)
    val detachListener = createDetachListener(snackbar, state)
    addOnAttachStateChangeListener(detachListener)
    snackbar.addCallback(
        object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                removeOnAttachStateChangeListener(detachListener)
                clearStateIfCurrent(state)
            }
        },
    )
    snackbar.show()
    return snackbar
}

internal fun View.findUiMessageAnchor(): View? {
    if (isShown && id != View.NO_ID) {
        val resourceName = runCatching { resources.getResourceEntryName(id) }.getOrNull()
        if (resourceName?.endsWith("BottomNav", ignoreCase = true) == true) {
            return this
        }
    }
    if (this !is ViewGroup) return null
    return children.firstNotNullOfOrNull { child -> child.findUiMessageAnchor() }
}

private fun View.createDetachListener(
    snackbar: Snackbar,
    state: SnackbarHostState,
): View.OnAttachStateChangeListener {
    return object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
            snackbar.dismiss()
            removeOnAttachStateChangeListener(this)
            clearStateIfCurrent(state)
        }
    }
}

private fun View.clearStateIfCurrent(state: SnackbarHostState) {
    if (getTag(R.id.ui_message_snackbar_state) === state) {
        setTag(R.id.ui_message_snackbar_state, null)
    }
}

private fun UiText.resolve(context: Context): String? {
    val resolved = when (this) {
        is UiText.Dynamic -> value
        is UiText.Resource -> context.getString(resId, *args.toTypedArray())
    }
    return resolved.trim().takeIf { it.isNotEmpty() }
}

private fun UiMessageDuration.toSnackbarDuration(hasAction: Boolean): Int {
    return when {
        hasAction && this == UiMessageDuration.SHORT -> Snackbar.LENGTH_LONG
        this == UiMessageDuration.SHORT -> Snackbar.LENGTH_SHORT
        this == UiMessageDuration.LONG -> Snackbar.LENGTH_LONG
        else -> Snackbar.LENGTH_INDEFINITE
    }
}

@get:ColorRes
private val UiMessageType.backgroundColorRes: Int
    get() = when (this) {
        UiMessageType.SUCCESS -> R.color.pt_status_delivered
        UiMessageType.INFO -> R.color.pt_surface_soft
        UiMessageType.WARNING -> R.color.pt_gold_dark
        UiMessageType.ERROR -> R.color.pt_primary_tomato
    }

private data class SnackbarHostState(
    val text: String,
    val snackbar: Snackbar,
)
