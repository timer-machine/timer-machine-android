package xyz.aprildown.timer.app.scheduler

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import xyz.aprildown.timer.app.base.utils.WeekdaysFormatter
import xyz.aprildown.timer.domain.TimeUtils
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode
import java.util.Calendar
import xyz.aprildown.timer.app.base.R as RBase

internal data class VisibleScheduler(
    val scheduler: SchedulerEntity,
    val time: String,
    val action: String,
    val days: String,
    var isSchedulerEnabled: Boolean = (scheduler.enable == 1)
) {

    interface Callback {
        fun onSchedulerStateChange(id: Int, enable: Boolean)
    }

    fun bind(holder: ViewHolder, callback: Callback) {
        holder.run {
            timeText.text = time
            nameActionText.text = "%s · %s".format(scheduler.label, action)
            daysView.text = days
            enabledSwitch.run {
                setOnCheckedChangeListener(null)
                isChecked = isSchedulerEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != isSchedulerEnabled) {
                        isSchedulerEnabled = isChecked
                        callback.onSchedulerStateChange(scheduler.id, isChecked)
                    }
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val context: Context = view.context
        val timeText: TextView = view.findViewById(R.id.textSchedulerTime)
        val nameActionText: TextView = view.findViewById(R.id.textSchedulerNameAction)
        val daysView: TextView = view.findViewById(R.id.textSchedulerDays)
        val enabledSwitch: CompoundButton = view.findViewById(R.id.switchSchedulerEnabled)
    }

    companion object {
        fun fromSchedulerEntity(
            scheduler: SchedulerEntity,
            context: Context,
            timerName: String
        ): VisibleScheduler = with(scheduler) {
            val actionStr = when (action) {
                SchedulerEntity.ACTION_START -> context.getString(RBase.string.scheduler_starts_action_template)
                SchedulerEntity.ACTION_END -> context.getString(RBase.string.scheduler_stops_action_template)
                else -> context.getString(RBase.string.unknown)
            }.format(timerName)

            fun SchedulerEntity.isTomorrow(): Boolean {
                val totalAlarmMinutes = hour * 60 + minute
                val now = Calendar.getInstance()
                val totalNowMinutes =
                    now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                return totalAlarmMinutes <= totalNowMinutes
            }

            return VisibleScheduler(
                scheduler = this,
                time = TimeUtils.formattedTodayTime(
                    context = context,
                    hour = hour,
                    minute = minute
                ),
                action = actionStr,
                days = when {
                    repeatMode == SchedulerRepeatMode.EVERY_WEEK -> {
                        WeekdaysFormatter.createFromContext(context).produceDataStrings(days)
                    }
                    repeatMode == SchedulerRepeatMode.EVERY_DAYS -> {
                        DateUtils.formatDateTime(
                            context,
                            getNextFireTime().timeInMillis,
                            DateUtils.FORMAT_SHOW_YEAR or
                                DateUtils.FORMAT_SHOW_DATE or
                                DateUtils.FORMAT_SHOW_WEEKDAY or
                                DateUtils.FORMAT_SHOW_TIME
                        )
                    }
                    isTomorrow() -> context.getString(RBase.string.scheduler_repeat_tomorrow)
                    else -> context.getString(RBase.string.scheduler_repeat_today)
                }
            )
        }
    }
}
