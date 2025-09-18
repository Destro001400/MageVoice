package com.game.voicespells.presentation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onMoveListener: ((Float, Float) -> Unit)? = null

    private val outerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF") // Semi-transparent white
        style = Paint.Style.FILL
    }

    private val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0FFFFFF") // More opaque white
        style = Paint.Style.FILL
    }

    private var outerCircleRadius = 0f
    private var innerCircleRadius = 0f
    private var outerCircleCenterX = 0f
    private var outerCircleCenterY = 0f
    private var innerCircleCenterX = 0f
    private var innerCircleCenterY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val diameter = min(w, h)
        outerCircleRadius = diameter / 2f
        innerCircleRadius = outerCircleRadius / 2.5f
        outerCircleCenterX = w / 2f
        outerCircleCenterY = h / 2f
        resetInnerCirclePosition()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(outerCircleCenterX, outerCircleCenterY, outerCircleRadius, outerCirclePaint)
        canvas.drawCircle(innerCircleCenterX, innerCircleCenterY, innerCircleRadius, innerCirclePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - outerCircleCenterX
                val dy = event.y - outerCircleCenterY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance < outerCircleRadius) {
                    innerCircleCenterX = event.x
                    innerCircleCenterY = event.y
                } else {
                    innerCircleCenterX = outerCircleCenterX + dx / distance * outerCircleRadius
                    innerCircleCenterY = outerCircleCenterY + dy / distance * outerCircleRadius
                }

                val angle = atan2(dy, dx)
                val strength = min(1f, distance / outerCircleRadius)
                onMoveListener?.invoke(angle, strength)
            }
            MotionEvent.ACTION_UP -> {
                resetInnerCirclePosition()
                onMoveListener?.invoke(0f, 0f) // Reset movement
            }
        }
        invalidate()
        return true
    }

    private fun resetInnerCirclePosition() {
        innerCircleCenterX = outerCircleCenterX
        innerCircleCenterY = outerCircleCenterY
    }

    fun setOnMoveListener(listener: (angle: Float, strength: Float) -> Unit) {
        onMoveListener = listener
    }
}
