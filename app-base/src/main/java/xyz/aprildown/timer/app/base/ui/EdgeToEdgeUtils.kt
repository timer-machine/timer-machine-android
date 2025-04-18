package xyz.aprildown.timer.app.base.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.github.deweyreed.tools.anko.dp
import com.github.deweyreed.tools.utils.ThemeColorUtils

class StatusBarView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isVisible = false
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val top = systemBarInsets.top
            if (top > 0) {
                isVisible = true
                updateLayoutParams { height = top }
            } else {
                isVisible = false
            }
            insets
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isInEditMode) {
            super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(context.dp(24).toInt(), MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}

class NavigationBarView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val controller = context.getActivity()?.run {
        WindowCompat.getInsetsController(window, window.decorView)
    }
    private val initialLightNavBar = controller?.isAppearanceLightNavigationBars

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isVisible = false
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottom = systemBarInsets.bottom
            if (bottom > 0) {
                isVisible = true
                updateLayoutParams { height = bottom }
                val backgroundColor = (background as? ColorDrawable)?.color
                if (backgroundColor != null) {
                    controller?.isAppearanceLightNavigationBars =
                        ThemeColorUtils.isLightColor(backgroundColor)
                }
            } else {
                isVisible = false
            }
            insets
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (initialLightNavBar != null) {
            controller?.isAppearanceLightNavigationBars = initialLightNavBar
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isInEditMode) {
            super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(context.dp(24).toInt(), MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}

private fun Context.getActivity(): Activity? {
    if (this is Activity) return this
    if (this is ContextWrapper) return baseContext.getActivity()
    return null
}
