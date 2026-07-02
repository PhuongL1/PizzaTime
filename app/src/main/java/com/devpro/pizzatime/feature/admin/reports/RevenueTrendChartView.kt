package com.devpro.pizzatime.feature.admin.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.devpro.pizzatime.R

class RevenueTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pt_stroke_dark)
        strokeWidth = dp(1f)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pt_gold_dark)
        alpha = 90
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pt_copper)
        strokeWidth = dp(3f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pt_copper)
        style = Paint.Style.FILL
    }

    private var values = listOf(0.28f, 0.34f, 0.22f, 0.58f, 0.52f, 0.75f, 0.64f, 0.86f, 0.80f)

    fun setValues(newValues: List<Float>) {
        if (newValues.size < 2) return
        values = newValues.map { it.coerceIn(0f, 1f) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = dp(4f)
        val right = width - dp(4f)
        val top = dp(12f)
        val bottom = height - dp(16f)

        drawGrid(canvas, left, right, top, bottom)
        drawTrend(canvas, left, right, top, bottom)
    }

    private fun drawGrid(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float) {
        val step = (bottom - top) / 3f
        repeat(3) { index ->
            val y = top + step * (index + 1)
            canvas.drawLine(left, y, right, y, gridPaint)
        }
    }

    private fun drawTrend(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float) {
        if (values.size < 2) return

        val points = values.mapIndexed { index, value ->
            val x = left + (right - left) * index / (values.lastIndex)
            val y = bottom - (bottom - top) * value
            x to y
        }

        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, point ->
            if (index == 0) {
                linePath.moveTo(point.first, point.second)
                fillPath.moveTo(point.first, bottom)
                fillPath.lineTo(point.first, point.second)
            } else {
                linePath.lineTo(point.first, point.second)
                fillPath.lineTo(point.first, point.second)
            }
        }

        fillPath.lineTo(points.last().first, bottom)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        points.drop(5).forEach { point ->
            canvas.drawCircle(point.first, point.second, dp(4f), dotPaint)
        }
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}
