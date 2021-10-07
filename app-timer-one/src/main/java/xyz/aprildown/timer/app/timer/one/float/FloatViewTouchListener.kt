package xyz.aprildown.timer.app.timer.one.float

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import androidx.core.math.MathUtils
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import kotlin.math.abs

internal class FloatViewTouchListener(
    private val fm: Floater,
    var screenWidth: Int,
    var screenHeight: Int
) : View.OnTouchListener {

    private val context = fm.context
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private var velocityTracker: VelocityTracker? = null

    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.rawX
                startX = x
                lastX = x
                val y = event.rawY
                startY = y
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.rawX
                val y = event.rawY
                fm.updatePos(
                    x = MathUtils.clamp(
                        (fm.currentX + (x - lastX)).toInt(),
                        0,
                        screenWidth - (v?.width ?: 0)
                    ),
                    y = MathUtils.clamp(
                        (fm.currentY + (y - lastY)).toInt(),
                        0,
                        (screenHeight - (v?.height ?: 0))
                    )
                )
                lastX = x
                lastY = y
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000)

                val x = event.rawX
                val y = event.rawY
                if (abs(x - startX) <= slop && abs(y - startY) <= slop) {
                    // Some move is a click
                    v?.performClick()
                } else {
                    velocityTracker?.let { vt ->
                        FlingAnimation(FloatValueHolder()).apply {
                            friction = FRICTION
                            setStartVelocity(vt.xVelocity)
                            setStartValue(fm.currentX.toFloat())
                            setMinValue(0f)
                            setMaxValue((screenWidth - (v?.width ?: 0)).toFloat())
                            addUpdateListener { _, value, _ ->
                                fm.updatePos(x = value.toInt())
                            }
                        }.start()
                        FlingAnimation(FloatValueHolder()).apply {
                            friction = FRICTION
                            setStartVelocity(vt.yVelocity)
                            setStartValue(fm.currentY.toFloat())
                            setMinValue(0f)
                            setMaxValue((screenHeight - (v?.height ?: 0)).toFloat())
                            addUpdateListener { _, value, _ ->
                                fm.updatePos(y = value.toInt())
                            }
                        }.start()
                    }
                }

                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return true
    }
}

private const val FRICTION = 4f
