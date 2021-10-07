package xyz.aprildown.timer.app.base.utils

import android.widget.TextView
import xyz.aprildown.tools.helper.setTextIfChanged

fun TextView.setTime(value: Long?) {
    setTextIfChanged((value ?: 0).produceTime())
}
