package xyz.aprildown.timer.component.settings

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.data.DarkTheme
import xyz.aprildown.timer.app.base.widgets.TimePickerFix
import xyz.aprildown.timer.component.settings.databinding.DialogDarkThemeBinding
import xyz.aprildown.timer.domain.TimeUtils
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.show
import xyz.aprildown.timer.app.base.R as RBase

class DarkThemeDialog(private val context: Context) {
    fun showSettingsDialog(onDone: (() -> Unit)? = null) {
        val binding = DialogDarkThemeBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.dark_theme_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        val darkTheme = DarkTheme(context)
        var currentScheduleEnable = darkTheme.scheduleEnabled
        var currentScheduleRange = darkTheme.scheduleRange

        val radioManual = binding.listItemDarkThemeManual.getLayoutView<CompoundButton>()

        val listItemScheduled = binding.listItemDarkThemeScheduled
        val switchScheduled = listItemScheduled.getLayoutView<CompoundButton>()

        val listItemScheduledFrom = binding.listItemDarkThemeScheduledFrom
        val textFrom = listItemScheduledFrom.getLayoutView<TextView>()
        val listItemScheduledTo = binding.listItemDarkThemeScheduledTo
        val textTo = listItemScheduledTo.getLayoutView<TextView>()

        val listItemDefault = binding.listItemDarkThemeDefault
        val radioDefault = listItemDefault.getLayoutView<CompoundButton>()

        val listItemSaver = binding.listItemDarkThemeSaver
        val radioSaver = listItemSaver.getLayoutView<CompoundButton>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listItemSaver.gone()
        } else {
            listItemDefault.gone()
        }

        val allRadios = listOf(radioManual, radioDefault, radioSaver)

        when (darkTheme.darkThemeValue) {
            DarkTheme.DARK_THEME_MANUAL -> {
                radioManual.isChecked = true
                if (!darkTheme.scheduleEnabled) {
                    listItemScheduledFrom.gone()
                    listItemScheduledTo.gone()
                }
            }
            DarkTheme.DARK_THEME_SYSTEM_DEFAULT -> {
                radioDefault.isChecked = true
                listItemScheduled.gone()
                listItemScheduledFrom.gone()
                listItemScheduledTo.gone()
            }
            DarkTheme.DARK_THEME_BATTERY_SAVER -> {
                radioSaver.isChecked = true
                listItemScheduled.gone()
                listItemScheduledFrom.gone()
                listItemScheduledTo.gone()
            }
        }
        switchScheduled.isChecked = currentScheduleEnable
        textFrom.text = formatTime(currentScheduleRange.fromHour, currentScheduleRange.fromMinute)
        textTo.text = formatTime(currentScheduleRange.toHour, currentScheduleRange.toMinute)

        allRadios.forEach { setUpRadio ->
            setUpRadio.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    setUpRadio.turnOffOtherRadios(allRadios)
                }
            }
        }
        // This overrides the code above.
        radioManual.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                radioManual.turnOffOtherRadios(allRadios)
                listItemScheduled.show()
                if (currentScheduleEnable) {
                    listItemScheduledFrom.show()
                    listItemScheduledTo.show()
                }
            } else {
                listItemScheduled.gone()
                listItemScheduledFrom.gone()
                listItemScheduledTo.gone()
            }
        }
        switchScheduled.setOnCheckedChangeListener { _, isChecked ->
            currentScheduleEnable = isChecked
            if (isChecked) {
                listItemScheduledFrom.show()
                listItemScheduledTo.show()
            } else {
                listItemScheduledFrom.gone()
                listItemScheduledTo.gone()
            }
        }
        listItemScheduledFrom.setOnClickListener {
            TimePickerFix.showDialog(
                context = context,
                initialHours = currentScheduleRange.fromHour,
                initialMinutes = currentScheduleRange.fromMinute
            ) { hourOfDay, minute ->
                textFrom.text = formatTime(hourOfDay, minute)
                currentScheduleRange = currentScheduleRange.copy(
                    fromHour = hourOfDay,
                    fromMinute = minute
                )
            }
        }
        listItemScheduledTo.setOnClickListener {
            TimePickerFix.showDialog(
                context = context,
                initialHours = currentScheduleRange.toHour,
                initialMinutes = currentScheduleRange.toMinute
            ) { hourOfDay, minute ->
                textTo.text = formatTime(hourOfDay, minute)
                currentScheduleRange = currentScheduleRange.copy(
                    toHour = hourOfDay,
                    toMinute = minute
                )
            }
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            darkTheme.darkThemeValue = when {
                radioManual.isChecked -> DarkTheme.DARK_THEME_MANUAL
                radioDefault.isChecked -> DarkTheme.DARK_THEME_SYSTEM_DEFAULT
                radioSaver.isChecked -> DarkTheme.DARK_THEME_BATTERY_SAVER
                else -> error("No dark theme value selected")
            }
            if (radioManual.isChecked) {
                darkTheme.scheduleEnabled = currentScheduleEnable
                if (currentScheduleEnable) {
                    darkTheme.scheduleRange = currentScheduleRange
                }
            } else {
                darkTheme.scheduleEnabled = false
            }
            dialog.dismiss()
            onDone?.invoke()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return TimeUtils.formattedTodayTime(
            context = context,
            hour = hour,
            minute = minute
        )
    }

    private fun CompoundButton.turnOffOtherRadios(allCompoundButtons: List<CompoundButton>) {
        allCompoundButtons.forEach {
            if (it != this) {
                it.isChecked = false
            }
        }
    }
}
