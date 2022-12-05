package xyz.aprildown.timer.app.timer.one.float

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import xyz.aprildown.timer.domain.utils.AppTracker

internal class Floater(
    val context: Context,
    val view: View,
    initialWidth: Int = WindowManager.LayoutParams.WRAP_CONTENT,
    initialHeight: Int = WindowManager.LayoutParams.WRAP_CONTENT,
    gravity: Int = Gravity.TOP or Gravity.START,
    private val appTracker: AppTracker,
) {

    private val wm: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val lp = WindowManager.LayoutParams().apply {
        format = PixelFormat.RGBA_8888
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = initialWidth
        height = initialHeight
        this.gravity = gravity
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    var currentX: Int = 0
        private set
        get() = lp.x

    var currentY: Int = 0
        private set
        get() = lp.y

    private var isViewAdded = false
    private var floatViewTouchListener: FloatViewTouchListener? = null

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    init {
        updateScreenSize()
    }

    private var currentOrientation = context.resources.configuration.orientation
    private val orientationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent?.action != Intent.ACTION_CONFIGURATION_CHANGED) return

            val newOrientation = context.resources.configuration.orientation
            if (currentOrientation != newOrientation) {
                currentOrientation = newOrientation
                val oldXP = currentX.toFloat() / screenWidth.toFloat()
                val oldYP = currentY.toFloat() / screenHeight.toFloat()
                updateScreenSize()
                floatViewTouchListener?.let {
                    it.screenWidth = screenWidth
                    it.screenHeight = screenHeight
                }
                updatePos(
                    x = (screenWidth * oldXP).toInt().coerceIn(0, screenWidth - view.width),
                    y = (screenHeight * oldYP).toInt().coerceIn(0, screenHeight - view.height)
                )
            }
        }
    }

    fun updateScreenSize() {
        val displayMetrics = Resources.getSystem().displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    fun show() {
        floatViewTouchListener = FloatViewTouchListener(this, screenWidth, screenHeight)
        view.setOnTouchListener(floatViewTouchListener)
        wm.addView(view, lp)
        isViewAdded = true

        ContextCompat.registerReceiver(
            context,
            orientationReceiver,
            IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun dismiss() {
        floatViewTouchListener = null
        isViewAdded = false
        wm.removeView(view)

        try {
            context.unregisterReceiver(orientationReceiver)
        } catch (e: IllegalArgumentException) {
            appTracker.trackError(e)
        }
    }

    fun updatePos(x: Int = currentX, y: Int = currentY) {
        lp.x = x
        currentX = x
        lp.y = y
        currentY = y
        update()
    }

    private fun update() {
        if (!isViewAdded) return
        wm.updateViewLayout(view, lp)
    }
}
