package xyz.aprildown.timer.app.base.widgets

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TimePicker
import androidx.core.content.withStyledAttributes
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.app.base.databinding.DialogTimePickerDialogFixBinding
import java.util.Locale

/**
 * [TimePicker] crashes on Pre-N Sam-sung devices.
 */
class TimePickerFix(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private interface Delegate {
        fun init(context: Context, parent: ViewGroup)
        var hours: Int
        var minutes: Int
    }

    private lateinit var delegate: Delegate

    var hours: Int
        get() = delegate.hours
        set(value) {
            delegate.hours = value
        }

    var minutes: Int
        get() = delegate.minutes
        set(value) {
            delegate.minutes = value
        }

    val timePicker: TimePicker? get() = (delegate as? TimePickerDelegate)?.timePicker

    init {
        // https://github.com/material-components/material-components-android/blob/master/lib/java/com/google/android/material/internal/ManufacturerUtils.java
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
            Build.MANUFACTURER.lowercase(Locale.ENGLISH) == "samsung"
        ) {
            delegate = EditTextDelegate()
        } else {
            context.withStyledAttributes(attrs, R.styleable.TimePickerFix) {
                delegate = TimePickerDelegate(
                    timePickerMode = getInt(R.styleable.TimePickerFix_android_timePickerMode, 2)
                )
            }
        }

        delegate.init(context, this)
    }

    private class TimePickerDelegate(private val timePickerMode: Int) : Delegate {

        lateinit var timePicker: TimePicker

        override fun init(context: Context, parent: ViewGroup) {
            View.inflate(
                context,
                when (timePickerMode) {
                    1 -> R.layout.view_time_picker_fix_normal_spinner
                    2 -> R.layout.view_time_picker_fix_normal_clock
                    else -> throw IllegalArgumentException("Wrong time picker mode $timePickerMode")
                },
                parent
            )
            timePicker = parent.findViewById(R.id.timePickerTimePickerFix)
            timePicker.setIs24HourView(DateFormat.is24HourFormat(context))
        }

        override var hours: Int
            get() = timePicker.hour
            set(value) {
                timePicker.hour = value
            }

        override var minutes: Int
            get() = timePicker.minute
            set(value) {
                timePicker.minute = value
            }
    }

    private class EditTextDelegate : Delegate {

        private lateinit var hourEdit: EditText
        private lateinit var minuteEdit: EditText

        override fun init(context: Context, parent: ViewGroup) {
            View.inflate(context, R.layout.view_time_picker_fix_edit, parent)

            fun EditText.limitTime(range: ClosedRange<Int>) {
                doOnTextChanged { text, _, _, _ ->
                    val time = text?.toString()?.toIntOrNull() ?: 0
                    if (time !in range) {
                        setText(time.coerceIn(range).toString())
                        val length = length()
                        if (length > 0) {
                            setSelection(length)
                        }
                    }
                }
            }

            hourEdit = parent.findViewById(R.id.editTimePickerFixHour)
            hourEdit.limitTime(0..23)

            minuteEdit = parent.findViewById(R.id.editTimePickerFixMinute)
            minuteEdit.limitTime(0..59)
        }

        override var hours: Int
            get() = hourEdit.text?.toString()?.toIntOrNull() ?: 0
            set(value) {
                hourEdit.setText(value.toString())
            }
        override var minutes: Int
            get() = minuteEdit.text?.toString()?.toIntOrNull() ?: 0
            set(value) {
                minuteEdit.setText(value.toString())
            }
    }

    companion object {
        fun showDialog(
            context: Context,
            initialHours: Int = 0,
            initialMinutes: Int = 0,
            onPick: (hourOfDay: Int, minute: Int) -> Unit
        ) {
            val binding = DialogTimePickerDialogFixBinding.inflate(LayoutInflater.from(context))
            val view = binding.root
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show()

            view.hours = initialHours
            view.minutes = initialMinutes
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                onPick.invoke(view.hours, view.minutes)
                dialog.dismiss()
            }
        }
    }
}
