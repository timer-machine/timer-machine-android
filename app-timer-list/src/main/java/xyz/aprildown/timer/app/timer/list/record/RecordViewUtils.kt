package xyz.aprildown.timer.app.timer.list.record

import android.view.View

internal fun View.animateShowGraphs() {
    animate()
        .withStartAction { alpha = 0f }
        .alpha(1f)
        .start()
}

internal fun View.animateHideGraphs() {
    animate()
        .alpha(0f)
        .start()
}
