package xyz.aprildown.timer.app.intro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Region
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.TooltipCompatFix
import androidx.core.graphics.withSave
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.postDelayed
import com.github.deweyreed.tools.anko.dp
import com.github.deweyreed.tools.helper.color
import com.github.deweyreed.tools.helper.themeColor
import com.github.deweyreed.tools.utils.ThemeColorUtils
import com.google.android.material.R as RMaterial
import com.mikepenz.materialize.R as RMaterialize

private fun View.findInstructionIndicatorLayout(): InstructionIndicatorLayout? {
    var currentView: View = this
    do {
        currentView = currentView.parent as? View ?: return null
    } while (currentView.id != R.id.motionIntro)
    return currentView.findViewById(R.id.viewIndicator)
}

internal fun View.showInteractionIndicator(
    @ColorInt tint: Int = context.color(RMaterialize.color.md_red_500)
) {
    val overlay = overlay
    val drawable = GradientDrawable().apply {
        setStroke(context.dp(4).toInt(), tint)
    }
    val runnable = object : Runnable {
        override fun run() {
            drawable.setBounds(0, 0, width, height)

            overlay.clear()
            overlay.add(drawable)

            postDelayed(ANIMATION_DURATION) {
                overlay.clear()
            }

            postDelayed(this, ANIMATION_DURATION * 2)
        }
    }
    setTag(R.id.tag_instruction_indicator_runnable, runnable)
    postDelayed(runnable, ANIMATION_DURATION)

    findInstructionIndicatorLayout()?.withView(this)
}

private const val ANIMATION_DURATION = 1000L

internal fun View.clearInteractionIndicator() {
    overlay.clear()
    (getTag(R.id.tag_instruction_indicator_runnable) as? Runnable)?.let {
        removeCallbacks(it)
    }

    findInstructionIndicatorLayout()?.clear()
}

internal class InstructionIndicatorLayout(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val indicatorPath = Path()
    private val insets = context.dp(8)

    private val color = context.themeColor(RMaterial.attr.colorOnSurface)

    private var targetView: View? = null

    private val updateRunnable = object : Runnable {
        private val rootViewBounds = IntArray(2)
        private val childViewBounds = IntArray(2)

        override fun run() {
            val targetView = targetView ?: return
            this@InstructionIndicatorLayout.getLocationOnScreen(rootViewBounds)
            targetView.getLocationOnScreen(childViewBounds)

            val childX = childViewBounds[0] - rootViewBounds[0]
            val childY = childViewBounds[1] - rootViewBounds[1]

            withBounds(childX, childY, childX + targetView.width, childY + targetView.height)

            this@InstructionIndicatorLayout.animate()
                .alpha(1f)
                .setDuration(100L)
                .start()

            postDelayed(ANIMATION_DURATION) {
                this@InstructionIndicatorLayout.animate()
                    .alpha(0f)
                    .setDuration(100L)
                    .start()
            }

            postDelayed(this, ANIMATION_DURATION * 2)
        }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        alpha = 0f
    }

    fun withView(view: View) {
        targetView = view
        view.doOnAttach {
            postDelayed(updateRunnable, ANIMATION_DURATION)
            view.doOnDetach {
                clear()
            }
        }
    }

    private fun withBounds(left: Int, top: Int, right: Int, bottom: Int) {
        indicatorPath.reset()
        indicatorPath.addRoundRect(
            left - insets,
            top - insets,
            right + insets,
            bottom + insets,
            insets,
            insets,
            Path.Direction.CCW
        )
        invalidate()
    }

    fun clear() {
        targetView = null
        removeCallbacks(updateRunnable)

        indicatorPath.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return
        if (indicatorPath.isEmpty) return

        canvas.withSave {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(indicatorPath)
            } else {
                @Suppress("DEPRECATION")
                canvas.clipPath(indicatorPath, Region.Op.DIFFERENCE)
            }
            canvas.drawColor(ThemeColorUtils.adjustAlpha(color, 0.3f))
        }
    }
}

internal fun View.showTooltip(content: CharSequence) {
    TooltipCompatFix.setTooltipText(this, content)
    performLongClick()
}
