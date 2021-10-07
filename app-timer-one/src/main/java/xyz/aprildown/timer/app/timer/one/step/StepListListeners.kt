package xyz.aprildown.timer.app.timer.one.step

import com.mikepenz.fastadapter.IItem

internal interface CurrentPositionCallback {
    val currentPosition: Int
}

internal interface OnStepLongClickListener {
    fun onStepLongClick(item: IItem<*>)
    fun onStepTimeLongClick(item: IItem<*>)
    fun onStepDoubleTap(item: IItem<*>)
}
