package androidx.appcompat.widget

import android.annotation.SuppressLint
import android.os.Build
import android.view.View

object TooltipCompatFix {
    /**
     * Tooltip' platform implementation on Android O(API 26) is broken and crashes.
     * So I use the compat version on all pre-Q devices.
     *
     * https://issuetracker.google.com/issues/64461213
     * [TooltipCompat.setTooltipText]
     */
    @SuppressLint("RestrictedApi")
    fun setTooltipText(view: View, tooltipText: CharSequence?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.tooltipText = tooltipText
        } else {
            TooltipCompatHandler.setTooltipText(view, tooltipText)
        }
    }
}
