package xyz.aprildown.timer.app.base.utils

import android.widget.TextView
import com.github.deweyreed.tools.helper.setTextIfChanged

fun TextView.setTime(value: Long?) {
    setTextIfChanged((value ?: 0).produceTime())
}
