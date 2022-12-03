package xyz.aprildown.timer.app.scheduler

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.forEachIndexed
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import com.github.deweyreed.tools.anko.longSnackbar
import com.github.deweyreed.tools.helper.getNumberFormattedQuantityString
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.requireCallback
import com.github.deweyreed.tools.helper.scrollToBottom
import com.github.deweyreed.tools.helper.show
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nex3z.togglebuttongroup.MultiSelectToggleGroup
import com.nex3z.togglebuttongroup.button.CircularToggle
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.startWeekOn
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.app.base.utils.WeekdaysFormatter
import xyz.aprildown.timer.app.base.widgets.TimePickerFix
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.presentation.scheduler.EditSchedulerViewModel
import java.text.DateFormatSymbols
import java.time.LocalDateTime
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class EditSchedulerFragment : Fragment(R.layout.fragment_edit_scheduler), MenuProvider {

    private val viewModel: EditSchedulerViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    private var selectedTimerId: Int = TimerEntity.NULL_ID
    private var selectedAction: Int = SchedulerEntity.ACTION_START
    private var repeatMode = SchedulerRepeatMode.ONCE

    private var layoutScrollRoot: ScrollView? = null
    private var editName: EditText? = null
    private var targetTimerTextView: TextView? = null
    private var radioStart: CompoundButton? = null
    private var radioEnd: CompoundButton? = null
    private var timePicker: TimePickerFix? = null
    private var btnRepeat: Button? = null
    private var layoutRepeatEveryWeek: ViewGroup? = null
    private val dayButtons = MutableList<CircularToggle?>(7) { null }
    private var layoutRepeatEveryDays: ViewGroup? = null
    private var editRepeatEveryDays: EditText? = null
    private var textRepeatEveryDaysLabel: TextView? = null

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private lateinit var mainCallback: MainCallback.ActivityCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainCallback = requireCallback()
        val onBackPressedDispatcher = requireActivity().onBackPressedDispatcher
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isTheSameScheduler(getCurrentScheduler())) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(RBase.string.save_changes)
                        .setPositiveButton(RBase.string.save) { _, _ ->
                            saveScheduler()
                        }
                        .setNegativeButton(RBase.string.discard) { _, _ ->
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().addMenuProvider(this, this, Lifecycle.State.STARTED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        setUpViews(view)
        viewModel.load(arguments?.getInt(ARG_ID) ?: SchedulerEntity.NEW_ID)
        viewModel.schedulerWithTimerInfo.observe(viewLifecycleOwner) {
            bindViewsWithScheduler(context, it.first, it.second)
        }
    }

    private fun setUpViews(view: View) {
        layoutScrollRoot = view.findViewById(R.id.layoutSchedulerEditRoot)
        editName = view.findViewById(R.id.editSchedulerName)
        targetTimerTextView = view.findViewById(R.id.btnSchedulerEditTimers)
        radioStart = view.findViewById(R.id.checkSchedulerEditActionStart)
        radioEnd = view.findViewById(R.id.checkSchedulerEditActionEnd)
        timePicker = view.findViewById<TimePickerFix>(R.id.timePickerSchedulerEdit)?.apply {
            timePicker?.run {
                setIs24HourView(DateFormat.is24HourFormat(requireContext()))
            }
        }
        btnRepeat = view.findViewById(R.id.btnSchedulerEditRepeat)
        layoutRepeatEveryWeek =
            view.findViewById<MultiSelectToggleGroup>(R.id.layoutSchedulerRepeatEveryWeek).apply {
                forEachIndexed { index, view ->
                    dayButtons[index] = view as CircularToggle
                }
            }
        layoutRepeatEveryDays = view.findViewById(R.id.layoutSchedulerEditRepeatEveryDays)
        editRepeatEveryDays = view.findViewById(R.id.editSchedulerEditRepeatEveryDays)
        textRepeatEveryDaysLabel = view.findViewById(R.id.textSchedulerEditRepeatEveryDays)
    }

    private fun bindViewsWithScheduler(
        context: Context,
        scheduler: SchedulerEntity,
        timerInfo: TimerInfo?
    ) {
        editName?.run {
            if (scheduler.isNull) {
                setText(RBase.string.scheduler_default_name)
            } else {
                setText(scheduler.label)
            }
        }

        targetTimerTextView?.run {
            selectedTimerId = scheduler.timerId

            setOnClickListener { _ ->
                appNavigator.pickTimer(
                    fm = childFragmentManager,
                    select = listOf(selectedTimerId)
                ) {
                    val newTimerInfo = it.timerInfo.first()
                    selectedTimerId = newTimerInfo.id
                    text = newTimerInfo.name
                }
            }

            if (timerInfo != null) {
                text = timerInfo.name
            } else {
                setText(RBase.string.timer_pick_required)
            }
        }

        selectedAction = scheduler.action
        radioStart?.run {
            if (scheduler.action == SchedulerEntity.ACTION_START) {
                isChecked = true
            }
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedAction = SchedulerEntity.ACTION_START
            }
        }
        radioEnd?.run {
            if (scheduler.action == SchedulerEntity.ACTION_END) {
                isChecked = true
            }
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedAction = SchedulerEntity.ACTION_END
            }
        }

        timePicker?.run {
            if (scheduler.isNull) {
                val now = LocalDateTime.now()
                hours = now.hour
                minutes = now.minute
            } else {
                hours = scheduler.hour
                minutes = scheduler.minute
            }
        }

        repeatMode = scheduler.repeatMode
        layoutRepeatEveryWeek?.run {
            val setting = context.startWeekOn
            val weekdaysStrings = DateFormatSymbols().shortWeekdays

            val weekDayOrder =
                WeekdaysFormatter.WeekDayOrder.fromStartDay(setting)

            dayButtons.forEachIndexed { index, circularToggle ->
                val calendarDay = weekDayOrder.days[index]
                circularToggle?.run {
                    val dayString = weekdaysStrings[calendarDay]
                    text = dayString
                    val shouldCheck =
                        scheduler.days[WeekdaysFormatter.calendarDayToDayIndex(calendarDay)]
                    if (shouldCheck != isChecked) {
                        isChecked = shouldCheck
                    }
                }
            }
        }

        editRepeatEveryDays?.doAfterTextChanged { s ->
            var days = s.toString().toIntOrNull() ?: 1
            if (days !in 1..127) {
                mainCallback.snackbarView.longSnackbar(RBase.string.scheduler_wrong_every_days)
                days = 1
                editRepeatEveryDays?.setText(days.toString())
            }
            updateRepeatEveryDaysLabel(days)
        }

        btnRepeat?.setOnClickListener {
            popupMenu {
                section {
                    item {
                        label = context.getString(RBase.string.scheduler_repeat_once)
                        callback = { toRepeatOnce() }
                    }
                    item {
                        label = context.getString(RBase.string.scheduler_repeat_every_week)
                        callback = { toRepeatEveryWeek() }
                    }
                    item {
                        label = context.getString(RBase.string.scheduler_repeat_every_days)
                        callback = { toRepeatEveryDays() }
                    }
                }
            }.show(context, it)
        }

        when (scheduler.repeatMode) {
            SchedulerRepeatMode.ONCE -> toRepeatOnce()
            SchedulerRepeatMode.EVERY_WEEK -> toRepeatEveryWeek(false)
            SchedulerRepeatMode.EVERY_DAYS -> toRepeatEveryDays(false)
        }
        var days = SchedulerEntity.daysToEveryDay(scheduler.days)
        if (days == 0) days = 1
        editRepeatEveryDays?.setText(days.toString())
        updateRepeatEveryDaysLabel(days)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        layoutScrollRoot = null
        editName = null
        targetTimerTextView = null
        radioStart = null
        radioEnd = null
        timePicker = null
        btnRepeat = null
        layoutRepeatEveryWeek = null
        for (i in 0 until dayButtons.size) {
            dayButtons[i] = null
        }
        layoutRepeatEveryDays = null
        editRepeatEveryDays = null
        textRepeatEveryDaysLabel = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.edit_scheduler, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save_scheduler -> {
                if (selectedTimerId == TimerEntity.NULL_ID) {
                    mainCallback.snackbarView.longSnackbar(RBase.string.scheduler_wrong_timer_id)
                } else {
                    saveScheduler()
                }
                true
            }
            else -> false
        }
    }

    private fun saveScheduler() {
        viewModel.saveScheduler(getCurrentScheduler()) {
            NavHostFragment.findNavController(this).popBackStack()
        }
    }

    private fun getCurrentScheduler(): SchedulerEntity {
        var mode = repeatMode
        val repeatContent = getRepeatContent()
        if (mode == SchedulerRepeatMode.EVERY_WEEK && repeatContent.all { !it }) {
            mode = SchedulerRepeatMode.ONCE
        }
        return SchedulerEntity(
            viewModel.schedulerWithTimerInfo.value?.first?.id ?: SchedulerEntity.NEW_ID,
            selectedTimerId,
            editName?.text.toString(),
            selectedAction,
            timePicker?.hours ?: 0,
            timePicker?.minutes ?: 0,
            mode,
            repeatContent,
            0
        )
    }

    private fun getRepeatContent(): List<Boolean> = when (repeatMode) {
        SchedulerRepeatMode.ONCE -> List(7) { false }
        SchedulerRepeatMode.EVERY_WEEK -> {
            val weekDayOrder =
                WeekdaysFormatter.WeekDayOrder.fromStartDay(requireContext().startWeekOn)
            val result = MutableList(7) { false }
            dayButtons.forEachIndexed { index, circularToggle ->
                val calendarDay = weekDayOrder.days[index]
                val dayIndex = WeekdaysFormatter.calendarDayToDayIndex(calendarDay)
                result[dayIndex] = circularToggle?.isChecked ?: false
            }
            result
        }
        SchedulerRepeatMode.EVERY_DAYS ->
            SchedulerEntity.everyDayToDays(editRepeatEveryDays?.text.toString().toIntOrNull() ?: 1)
    }

    private fun updateRepeatEveryDaysLabel(newValue: Int) {
        textRepeatEveryDaysLabel?.text = resources.getNumberFormattedQuantityString(
            RBase.plurals.scheduler_repeat_days,
            newValue
        )
    }

    private fun toRepeatOnce() {
        repeatMode = SchedulerRepeatMode.ONCE
        btnRepeat?.setText(RBase.string.scheduler_repeat_once)
        layoutRepeatEveryWeek?.gone()
        layoutRepeatEveryDays?.gone()
    }

    private fun toRepeatEveryWeek(scroll: Boolean = true) {
        repeatMode = SchedulerRepeatMode.EVERY_WEEK
        btnRepeat?.setText(RBase.string.scheduler_repeat_every_week)
        layoutRepeatEveryWeek?.show()
        layoutRepeatEveryDays?.gone()
        if (scroll) layoutScrollRoot?.scrollToBottom()
    }

    private fun toRepeatEveryDays(scroll: Boolean = true) {
        repeatMode = SchedulerRepeatMode.EVERY_DAYS
        btnRepeat?.setText(RBase.string.scheduler_repeat_every_days)
        layoutRepeatEveryWeek?.gone()
        layoutRepeatEveryDays?.show()
        if (scroll) layoutScrollRoot?.scrollToBottom()
    }

    companion object {
        private const val ARG_ID = "id"

        fun getBundle(schedulerId: Int): Bundle = bundleOf(ARG_ID to schedulerId)
    }
}
