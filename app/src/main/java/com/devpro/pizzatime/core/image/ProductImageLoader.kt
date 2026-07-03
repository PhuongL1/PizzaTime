package com.devpro.pizzatime.core.image

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide

fun ImageView.loadProductImage(
    imageUrl: String,
    @DrawableRes fallbackRes: Int,
) {
    if (imageUrl.isBlank()) {
        setImageResource(fallbackRes)
        return
    }

    Glide.with(this)
        .load(imageUrl)
        .placeholder(fallbackRes)
        .error(fallbackRes)
        .fallback(fallbackRes)
        .into(this)
}
