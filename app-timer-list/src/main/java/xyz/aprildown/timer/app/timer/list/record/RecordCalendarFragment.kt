package xyz.aprildown.timer.app.timer.list.record

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.next
import com.kizitonwose.calendarview.utils.previous
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import xyz.aprildown.timer.app.base.data.PreferenceData.startDayOfWeek
import xyz.aprildown.timer.app.base.data.PreferenceData.startWeekOn
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.timer.app.base.utils.WeekdaysFormatter
import xyz.aprildown.timer.app.timer.list.R
import xyz.aprildown.timer.app.timer.list.databinding.FragmentRecordCalendarBinding
import xyz.aprildown.timer.app.timer.list.databinding.ListItemCalendarDayEventBinding
import xyz.aprildown.timer.app.timer.list.databinding.ListItemRecordCalendarBinding
import xyz.aprildown.timer.app.timer.list.databinding.ListItemRecordCalendarLegendBinding
import xyz.aprildown.timer.domain.TimeUtils
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.usecases.record.GetRecords
import xyz.aprildown.timer.presentation.timer.RecordViewModel
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.setTextIfChanged
import xyz.aprildown.tools.helper.show
import xyz.aprildown.tools.helper.themeColor
import xyz.aprildown.tools.helper.toColorStateList
import java.text.DateFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import xyz.aprildown.timer.app.base.R as RBase

internal class RecordCalendarFragment : Fragment(R.layout.fragment_record_calendar) {

    private val viewModel: RecordViewModel? get() = (parentFragment as? RecordFragment)?.viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val binding = FragmentRecordCalendarBinding.bind(view)

        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        var minMonth = YearMonth.now()

        var hasInit = false
        fun ensureCalendarInitialization() {
            if (hasInit) return

            val viewModel = viewModel ?: return
            val minDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(
                    viewModel.minDateMilli.value ?: TimerStampEntity.getMinDateMilli()
                ),
                ZoneId.systemDefault()
            )
            minMonth = YearMonth.of(minDateTime.year, minDateTime.month)
            binding.calendarRecord.setup(
                startMonth = minMonth,
                endMonth = currentMonth,
                firstDayOfWeek = context.startDayOfWeek
            )
            binding.calendarRecord.scrollToMonth(currentMonth)

            hasInit = true
        }

        binding.calendarRecord.let { calendar ->
            fun selectDate(newDate: LocalDate) {
                val oldTime = viewModel?.calendarSelectedDate?.value ?: return
                val newTime = newDate.toEpochMilli()
                if (newTime != oldTime) {
                    calendar.notifyDateChanged(oldTime.toLocalDate())
                    calendar.notifyDateChanged(newDate)
                    viewModel?.calendarSelectedDate?.value = newTime
                }
            }

            val dynamicTheme = newDynamicTheme
            val colorPrimary = dynamicTheme.colorPrimary.toColorStateList()
            val colorOnPrimary = dynamicTheme.colorOnPrimary.toColorStateList()
            val enabledTextColor = context.themeColor(android.R.attr.textColorPrimary)

            calendar.dayBinder = object : DayBinder<DayViewContainer> {
                override fun create(view: View): DayViewContainer = DayViewContainer(view) {
                    if (it.isBefore(today) || it == today) {
                        selectDate(it)
                    }
                }

                override fun bind(container: DayViewContainer, day: CalendarDay) {
                    container.day = day
                    container.binding.run {
                        if (day.owner == DayOwner.THIS_MONTH) {
                            textRecordCalendarItem.setTextIfChanged(
                                day.date.dayOfMonth.toString()
                            )
                            val currentDate = day.date

                            fun updateEvents() {
                                viewModel?.calendarEvents?.value?.let {
                                    it as? GetRecords.Signal.Result
                                }?.let {
                                    viewRecordCalendarItemEvents.setEvents(
                                        it.result.getOrDefault(
                                            currentDate.toEpochMilli(),
                                            0
                                        )
                                    )
                                }
                            }

                            when {
                                currentDate.toEpochMilli() == viewModel?.calendarSelectedDate?.value -> {
                                    if (imageRecordCalendarItemSelection.drawable == null) {
                                        imageRecordCalendarItemSelection.setImageResource(
                                            R.drawable.background_record_calendar_circle
                                        )
                                    }
                                    textRecordCalendarItem.setTextColor(colorOnPrimary)
                                    viewRecordCalendarItemEvents.show()
                                    updateEvents()
                                }
                                currentDate == today -> {
                                    run {
                                        imageRecordCalendarItemSelection.setImageResource(0)
                                        textRecordCalendarItem.setTextColor(colorPrimary)
                                        viewRecordCalendarItemEvents.show()
                                        updateEvents()
                                    }
                                }
                                currentDate.isBefore(today) -> {
                                    imageRecordCalendarItemSelection.setImageResource(0)
                                    textRecordCalendarItem.setTextColor(enabledTextColor)
                                    viewRecordCalendarItemEvents.show()
                                    updateEvents()
                                }
                                else -> {
                                    imageRecordCalendarItemSelection.setImageResource(0)
                                    textRecordCalendarItem.text = null
                                    viewRecordCalendarItemEvents.gone()
                                }
                            }
                        } else {
                            imageRecordCalendarItemSelection.setImageResource(0)
                            textRecordCalendarItem.text = null
                            viewRecordCalendarItemEvents.gone()
                        }
                    }
                }
            }
            calendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View): MonthViewContainer = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                    container.binding.root.let { root ->
                        if (root.tag == null) {
                            root.tag = month

                            val weekdaysStrings = DateFormatSymbols().shortWeekdays
                            val weekDayOrder =
                                WeekdaysFormatter.WeekDayOrder.fromStartDay(context.startWeekOn)
                            (root as ViewGroup).children.forEachIndexed { index, view ->
                                val textView = view as TextView
                                val calendarDay = weekDayOrder.days[index]
                                val dayString = weekdaysStrings[calendarDay]
                                textView.text = dayString
                            }
                        }
                    }
                }
            }

            calendar.monthScrollListener = { calendarMonth ->
                val selectYearMonth = calendarMonth.yearMonth
                val targetTime = selectYearMonth.atDay(1).toEpochMilli()
                binding.textRecordCalendarYearMonth.text =
                    DateUtils.formatDateTime(
                        context,
                        targetTime,
                        DateUtils.FORMAT_SHOW_YEAR or
                            DateUtils.FORMAT_SHOW_DATE or
                            DateUtils.FORMAT_NO_MONTH_DAY
                    )
                binding.btnRecordCalendarPreviousMonth.isInvisible =
                    !selectYearMonth.isAfter(minMonth)
                binding.btnRecordCalendarNextMonth.isInvisible =
                    !selectYearMonth.isBefore(currentMonth)
                viewModel?.calendarTimeSpan?.value =
                    TimeUtils.getMonthStart(targetTime) to TimeUtils.getMonthEnd(targetTime)
            }
        }

        binding.btnRecordCalendarPreviousMonth.setOnClickListener {
            binding.calendarRecord.findFirstVisibleMonth()?.let {
                binding.calendarRecord.smoothScrollToMonth(it.yearMonth.previous)
            }
        }
        binding.btnRecordCalendarNextMonth.setOnClickListener {
            binding.calendarRecord.findFirstVisibleMonth()?.let {
                binding.calendarRecord.smoothScrollToMonth(it.yearMonth.next)
            }
        }

        viewModel?.calendarEvents?.observe(viewLifecycleOwner) { signal ->
            if (signal is GetRecords.Signal.Result) {
                ensureCalendarInitialization()
                binding.calendarRecord.notifyCalendarChanged()
            }
        }

        viewModel?.minDateMilli?.observe(viewLifecycleOwner) {
            ensureCalendarInitialization()
        }

        binding.listRecordCalendarEvents.run {
            val itemAdapter = ItemAdapter<CalendarDayEvent>()
            val fastAdapter = FastAdapter.with(itemAdapter)
            adapter = fastAdapter

            viewModel?.calendarSelectedDateEvents?.observe(viewLifecycleOwner) { signal ->
                if (signal is GetRecords.Signal.Result) {
                    itemAdapter.set(
                        signal.result.map {
                            CalendarDayEvent(
                                name = viewModel?.queryTimerName(it.timerId)
                                    ?: getString(RBase.string.record_deleted_timer),
                                stamp = it
                            )
                        }
                    )
                }
            }
        }
    }

    class DayViewContainer(
        view: View,
        private val onSelected: (newDate: LocalDate) -> Unit
    ) : ViewContainer(view) {
        lateinit var day: CalendarDay
        val binding: ListItemRecordCalendarBinding = ListItemRecordCalendarBinding.bind(view)

        init {
            view.setOnClickListener {
                if (day.owner == DayOwner.THIS_MONTH) {
                    onSelected.invoke(day.date)
                }
            }
        }
    }

    class MonthViewContainer(view: View) : ViewContainer(view) {
        val binding: ListItemRecordCalendarLegendBinding =
            ListItemRecordCalendarLegendBinding.bind(view)
    }

    private class CalendarDayEvent(
        private val name: String,
        private val stamp: TimerStampEntity
    ) : AbstractBindingItem<ListItemCalendarDayEventBinding>() {
        override val type: Int = R.layout.list_item_calendar_day_event
        override fun createBinding(
            inflater: LayoutInflater,
            parent: ViewGroup?
        ): ListItemCalendarDayEventBinding {
            return ListItemCalendarDayEventBinding.inflate(inflater, parent, false)
        }

        override fun bindView(binding: ListItemCalendarDayEventBinding, payloads: List<Any>) {
            super.bindView(binding, payloads)
            binding.run {
                val context = root.context
                val startTime = stamp.start
                val endTime = stamp.end
                textCalendarDayEvent.text = if (startTime > 0 && startTime != endTime) {
                    "%s - %s   %s".format(
                        context.formatToTime(startTime),
                        context.formatToTime(endTime),
                        name
                    )
                } else {
                    "%s   %s".format(context.formatToTime(endTime), name)
                }
            }
        }
    }
}

private fun LocalDate.toEpochMilli(): Long = atStartOfDay()
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun Context.formatToTime(time: Long): String {
    return DateFormat.getTimeFormat(this).format(Date(time))
}
