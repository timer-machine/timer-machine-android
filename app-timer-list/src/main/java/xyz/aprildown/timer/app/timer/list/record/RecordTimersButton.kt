package xyz.aprildown.timer.app.timer.list.record

import android.content.Context
import android.util.AttributeSet
import com.github.deweyreed.tools.helper.getNumberFormattedQuantityString
import com.google.android.material.button.MaterialButton
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.app.base.R as RBase

class RecordTimersButton(
    context: Context,
    attrs: AttributeSet? = null
) : MaterialButton(context, attrs) {
    private var current: List<TimerInfo> = emptyList()

    var maxCount = 0

    fun withTimerInfo(timerInfo: List<TimerInfo>) {
        current = timerInfo

        text = if (current.size == maxCount &&
            current.none { it.folderId == FolderEntity.FOLDER_TRASH }
        ) {
            context.getString(RBase.string.record_all_timers)
        } else {
            current.joinToString { it.name }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (lineCount > 1) {
            text =
                context.getNumberFormattedQuantityString(RBase.plurals.timers, current.size)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
