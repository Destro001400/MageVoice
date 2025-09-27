package com.game.voicespells.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val outerCirclePaint: Paint
    private val innerCirclePaint: Paint

    private var outerCircleRadius: Float = 0f
    private var innerCircleRadius: Float = 0f
    private var outerCircleCenterX: Float = 0f
    private var outerCircleCenterY: Float = 0f

    // Position of the inner circle (knob)
    private var actualInnerCircleX: Float = 0f
    private var actualInnerCircleY: Float = 0f

    private var joystickListener: JoystickListener? = null
    private var isActive: Boolean = false

    // Normalized joystick output (-1 to 1 for each axis)
    var normalizedX: Float = 0f
        private set
    var normalizedY: Float = 0f
        private set

    init {
        outerCirclePaint = Paint().apply {
            color = Color.parseColor("#80AAAAAA") // Light gray, semi-transparent
            style = Paint.Style.FILL_AND_STROKE
        }
        innerCirclePaint = Paint().apply {
            color = Color.parseColor("#FF666666") // Darker gray
            style = Paint.Style.FILL
        }

        // It's good practice to make the view not clickable by default if it only handles touch events
        // isClickable = false // This might interfere with onTouchEvent, let's test
    }

    interface JoystickListener {
        fun onJoystickMoved(xPercent: Float, yPercent: Float, angle: Double, strength: Float)
        fun onJoystickReleased()
    }

    fun setJoystickListener(listener: JoystickListener) {
        this.joystickListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val diameter = min(w, h)
        outerCircleRadius = diameter / 2.5f // Outer circle is a bit smaller than the view bounds
        innerCircleRadius = outerCircleRadius / 2.5f

        outerCircleCenterX = w / 2f
        outerCircleCenterY = h / 2f

        // Initialize inner circle at the center
        actualInnerCircleX = outerCircleCenterX
        actualInnerCircleY = outerCircleCenterY
        invalidate() // Redraw with new sizes
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw outer circle
        canvas.drawCircle(outerCircleCenterX, outerCircleCenterY, outerCircleRadius, outerCirclePaint)
        // Draw inner circle (knob)
        canvas.drawCircle(actualInnerCircleX, actualInnerCircleY, innerCircleRadius, innerCirclePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touch is within the outer circle (or a larger activation area)
                val distanceToCenter = sqrt(
                    (touchX - outerCircleCenterX).pow(2) + (touchY - outerCircleCenterY).pow(2)
                )
                if (distanceToCenter <= outerCircleRadius * 1.5f) { // Allow slightly outside touch to activate
                    isActive = true
                    updateKnobPosition(touchX, touchY)
                    return true // Consume event
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isActive) {
                    updateKnobPosition(touchX, touchY)
                    return true // Consume event
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isActive) {
                    resetKnobPosition()
                    joystickListener?.onJoystickReleased()
                    isActive = false
                    return true // Consume event
                }
            }
        }
        return super.onTouchEvent(event) // Allow other touch handling if not consumed
    }

    private fun updateKnobPosition(touchX: Float, touchY: Float) {
        val deltaX = touchX - outerCircleCenterX
        val deltaY = touchY - outerCircleCenterY
        val distance = sqrt(deltaX.pow(2) + deltaY.pow(2))

        val angle = atan2(deltaY, deltaX) // Radians

        if (distance > outerCircleRadius - innerCircleRadius) {
            // Keep knob within the bounds of the outer circle
            actualInnerCircleX = outerCircleCenterX + (outerCircleRadius - innerCircleRadius) * cos(angle)
            actualInnerCircleY = outerCircleCenterY + (outerCircleRadius - innerCircleRadius) * sin(angle)
            normalizedX = cos(angle)
            normalizedY = sin(angle)
        } else {
            actualInnerCircleX = touchX
            actualInnerCircleY = touchY
            normalizedX = deltaX / (outerCircleRadius - innerCircleRadius)
            normalizedY = deltaY / (outerCircleRadius - innerCircleRadius)
        }

        val strength = min(1f, distance / (outerCircleRadius - innerCircleRadius))

        // Convert angle to degrees if preferred by listener, or keep as radians
        // val angleDegrees = Math.toDegrees(angle.toDouble())

        joystickListener?.onJoystickMoved(normalizedX, normalizedY, angle.toDouble(), strength)
        invalidate() // Request redraw
    }

    private fun resetKnobPosition() {
        actualInnerCircleX = outerCircleCenterX
        actualInnerCircleY = outerCircleCenterY
        normalizedX = 0f
        normalizedY = 0f
        invalidate() // Request redraw
    }
}
