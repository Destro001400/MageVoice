package com.game.voicespells.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class JoystickView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply { color = Color.LTGRAY }
    private var centerX = 0f
    private var centerY = 0f
    private var knobX = 0f
    private var knobY = 0f
    private var radius = 120f

    var onMove: ((Float, Float) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = (w / 2).toFloat()
        centerY = (h / 2).toFloat()
        knobX = centerX
        knobY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.DKGRAY
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.color = Color.LTGRAY
        canvas.drawCircle(knobX, knobY, radius / 2, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - centerX)
                val dy = (event.y - centerY)
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > radius) {
                    val ratio = radius / dist
                    knobX = centerX + dx * ratio
                    knobY = centerY + dy * ratio
                } else {
                    knobX = event.x
                    knobY = event.y
                }
                invalidate()
                onMove?.invoke((knobX - centerX) / radius, (knobY - centerY) / radius)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                knobX = centerX
                knobY = centerY
                invalidate()
                onMove?.invoke(0f, 0f)
            }
        }
        return true
    }
}
