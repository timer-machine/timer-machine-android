package xyz.aprildown.timer.app.timer.list.record

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.customview.view.AbsSavedState
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.show
import xyz.aprildown.timer.app.timer.list.R
import xyz.aprildown.timer.app.timer.list.databinding.ViewCalendarEventBinding
import xyz.aprildown.timer.app.base.R as RBase

internal class CalendarEventView(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewCalendarEventBinding
    private var currentMode = MODE_EVENT_NONE

    init {
        orientation = HORIZONTAL

        View.inflate(context, R.layout.view_calendar_event, this)

        binding = ViewCalendarEventBinding.bind(this)

        update()
    }

    fun setEvents(count: Int) {
        setMode(
            when (count) {
                0 -> MODE_EVENT_NONE
                1 -> MODE_EVENT_1
                2 -> MODE_EVENT_2
                3 -> MODE_EVENT_3
                else -> MODE_EVENT_MORE
            }
        )
    }

    private fun setMode(newMode: Int) {
        if (newMode != currentMode) {
            currentMode = newMode
            update()
        }
    }

    private fun update() {
        when (currentMode) {
            MODE_EVENT_NONE -> {
                children.forEach { it.gone() }
            }
            MODE_EVENT_1 -> {
                binding.imageCalendarEvent1.gone()
                binding.imageCalendarEvent2.show()
                binding.imageCalendarEvent3.gone()
            }
            MODE_EVENT_2 -> {
                binding.imageCalendarEvent1.show()
                binding.imageCalendarEvent2.show()
                binding.imageCalendarEvent3.gone()
            }
            MODE_EVENT_3 -> {
                children.forEach { it.show() }
                binding.imageCalendarEvent3.setImageResource(
                    R.drawable.background_record_calendar_circle
                )
            }
            MODE_EVENT_MORE -> {
                children.forEach { it.show() }
                binding.imageCalendarEvent3.setImageResource(
                    RBase.drawable.ic_calendar_more_events
                )
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = if (superState == null) SavedState() else SavedState(superState)
        ss.mode = currentMode
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        setMode(ss.mode)
    }

    private class SavedState : AbsSavedState {

        var mode: Int = MODE_EVENT_NONE

        constructor() : super(Parcel.obtain())
        constructor(superState: Parcelable) : super(superState)

        constructor(source: Parcel) : this(source, null)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            mode = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(mode)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        private const val MODE_EVENT_NONE = 0
        private const val MODE_EVENT_1 = 1
        private const val MODE_EVENT_2 = 2
        private const val MODE_EVENT_3 = 3
        private const val MODE_EVENT_MORE = 4
    }
}
