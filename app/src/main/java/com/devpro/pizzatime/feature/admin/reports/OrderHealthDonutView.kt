package com.devpro.pizzatime.feature.admin.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.devpro.pizzatime.R

class OrderHealthDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pt_stroke_dark)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeWidth = dp(12f)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pt_copper)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeWidth = dp(12f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pt_text_primary)
        textAlign = Paint.Align.CENTER
        textSize = dp(24f)
        isFakeBoldText = true
    }

    private val bounds = RectF()
    private var progressPercent = 92

    fun setProgressPercent(percent: Int) {
        progressPercent = percent.coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        val padding = dp(14f)
        val left = (width - size) / 2f + padding
        val top = (height - size) / 2f + padding
        val right = left + size - padding * 2f
        val bottom = top + size - padding * 2f

        bounds.set(left, top, right, bottom)

        canvas.drawArc(bounds, -90f, 360f, false, trackPaint)
        canvas.drawArc(bounds, -90f, 360f * progressPercent / 100f, false, progressPaint)

        val centerY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("$progressPercent%", width / 2f, centerY, textPaint)
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}
