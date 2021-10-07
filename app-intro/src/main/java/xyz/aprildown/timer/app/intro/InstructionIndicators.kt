package xyz.aprildown.timer.app.intro

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.TooltipCompatFix
import androidx.core.view.postDelayed
import xyz.aprildown.tools.anko.dip
import xyz.aprildown.tools.helper.color

internal fun View.showInteractionIndicator(@ColorInt tint: Int = context.color(R.color.md_red_500)) {
    val overlay = overlay
    val drawable = GradientDrawable().apply {
        setStroke(context.dip(4), tint)
    }
    val runnable = object : Runnable {
        override fun run() {
            drawable.setBounds(0, 0, width, height)

            overlay.clear()
            overlay.add(drawable)

            postDelayed(750) {
                overlay.clear()
            }

            postDelayed(this, 1500L)
        }
    }
    setTag(R.id.tag_instruction_indicator_runnable, runnable)
    postDelayed(runnable, 750L)
}

internal fun View.clearInteractionIndicator() {
    overlay.clear()
    (getTag(R.id.tag_instruction_indicator_runnable) as? Runnable)?.let {
        removeCallbacks(it)
    }
}

internal fun View.showTooltip(content: CharSequence) {
    TooltipCompatFix.setTooltipText(this, content)
    performLongClick()
}
