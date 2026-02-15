package com.example.fitnesscoachai.ui.workout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintPoint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var points: List<PosePoint>? = null
    private var segments: List<Segment> = emptyList()

    var mirrorX: Boolean = true

    fun updatePose(points: List<PosePoint>?, segments: List<Segment>) {
        this.points = points
        this.segments = segments
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pts = points ?: return
        if (pts.size < 18) return

        val w = width.toFloat()
        val h = height.toFloat()

        fun mapX(x: Float): Float {
            val px = x.coerceIn(0f, 1f) * w
            return if (mirrorX) (w - px) else px
        }

        fun mapY(y: Float): Float = y.coerceIn(0f, 1f) * h

        // линии
        for (s in segments) {
            if (s.a !in pts.indices || s.b !in pts.indices) continue
            paintLine.color = runCatching { Color.parseColor(s.color) }.getOrElse { Color.GREEN }
            val a = pts[s.a]
            val b = pts[s.b]
            canvas.drawLine(mapX(a.x), mapY(a.y), mapX(b.x), mapY(b.y), paintLine)
        }

        // точки (по желанию)
        val r = max(6f, w * 0.008f)
        for (p in pts) {
            canvas.drawCircle(mapX(p.x), mapY(p.y), r, paintPoint)
        }
    }
}
