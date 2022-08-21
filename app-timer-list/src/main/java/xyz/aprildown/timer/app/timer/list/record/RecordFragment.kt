package xyz.aprildown.timer.app.timer.list.record

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.startWeekOn
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.timer.list.R
import xyz.aprildown.timer.app.timer.list.databinding.FragmentRecordBinding
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.presentation.timer.RecordViewModel
import xyz.aprildown.tools.helper.safeSharedPreference
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class RecordFragment : Fragment(R.layout.fragment_record) {

    internal val viewModel: RecordViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val binding = FragmentRecordBinding.bind(view)

        viewModel.allTimerInfo.observe(viewLifecycleOwner) { allTimerInfo ->
            binding.btnRecordTimers.maxCount = allTimerInfo.size
            binding.btnRecordTimers.setOnClickListener {
                appNavigator.pickTimer(
                    fm = childFragmentManager,
                    multiple = true,
                    select = viewModel.params.value?.timerIds ?: emptyList()
                ) { result ->
                    viewModel.updateParams(timerInfo = result.timerInfo)
                }
            }

            fun pickTime(
                initialTime: Long,
                onPicked: (Long) -> Unit
            ) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = initialTime
                }
                DatePickerDialog(
                    context,
                    { _, year, monthOfYear, dayOfMonth ->
                        val time = Calendar.getInstance()
                            .apply {
                                set(year, monthOfYear, dayOfMonth, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        onPicked.invoke(time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                    .apply {
                        datePicker.run {
                            firstDayOfWeek = context.startWeekOn
                            minDate = viewModel.minDateMilli.value
                                ?: TimerStampEntity.getMinDateMilli()
                            maxDate = System.currentTimeMillis()
                        }
                    }
                    .show()
            }

            binding.btnRecordStartTime.setOnClickListener {
                val currentStartTime = viewModel.startTime.value ?: return@setOnClickListener
                val currentEndTime =
                    viewModel.endTime.value ?: return@setOnClickListener
                pickTime(currentStartTime) {
                    viewModel.updateParams(
                        startTime = it,
                        endTime = currentEndTime.coerceAtLeast(it)
                    )
                }
            }
            binding.btnRecordEndTime.setOnClickListener {
                val currentStartTime = viewModel.startTime.value ?: return@setOnClickListener
                val currentEndTime =
                    viewModel.endTime.value ?: return@setOnClickListener
                pickTime(currentEndTime) {
                    viewModel.updateParams(
                        endTime = it,
                        startTime = currentStartTime.coerceAtMost(it)
                    )
                }
            }

            binding.btnRecordRecent.setOnClickListener { view ->
                PopupMenu(context, view, Gravity.TOP or Gravity.END)
                    .apply {
                        inflate(R.menu.record_predefined_time)
                        setOnMenuItemClickListener { menuItem ->
                            val now = Instant.now()
                            val zone = ZoneId.systemDefault()
                            val dateTime = LocalDateTime.ofInstant(now, zone)
                            when (menuItem.itemId) {
                                R.id.record_predefined_today -> {
                                    dateTime
                                }
                                R.id.record_predefined_week -> {
                                    dateTime.minusWeeks(1)
                                }
                                R.id.record_predefined_month -> {
                                    dateTime.minusMonths(1)
                                }
                                R.id.record_predefined_year -> {
                                    dateTime.minusYears(1)
                                }
                                R.id.record_predefined_total -> {
                                    LocalDateTime.ofInstant(
                                        Instant.ofEpochMilli(
                                            viewModel.minDateMilli.value
                                                ?: TimerStampEntity.getMinDateMilli()
                                        ),
                                        zone
                                    )
                                }
                                else -> null
                            }?.atZone(zone)
                                ?.toInstant()
                                ?.toEpochMilli()
                                ?.let { startTime ->
                                    viewModel.updateParams(
                                        startTime = startTime,
                                        endTime = now.toEpochMilli()
                                    )
                                }
                            true
                        }
                    }
                    .show()
            }
        }

        viewModel.selectedTimerInfo.observe(viewLifecycleOwner) {
            binding.btnRecordTimers.withTimerInfo(it)
        }

        viewModel.startTime.observe(viewLifecycleOwner) {
            binding.btnRecordStartTime.text =
                DateFormat.getDateFormat(context).format(Date(it))
        }

        viewModel.endTime.observe(viewLifecycleOwner) {
            binding.btnRecordEndTime.text =
                DateFormat.getDateFormat(context).format(Date(it))
        }

        binding.viewPagerRecord.isUserInputEnabled = false
        binding.viewPagerRecord.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> RecordOverviewFragment()
                1 -> RecordTimelineFragment()
                2 -> RecordCalendarFragment()
                else -> error("Wrong pager position $position")
            }
        }
        TabLayoutMediator(binding.tabLayoutRecord, binding.viewPagerRecord) { tab, position ->
            tab.setText(
                when (position) {
                    0 -> RBase.string.record_overview
                    1 -> RBase.string.record_timeline
                    2 -> RBase.string.record_calendar
                    else -> error("Wrong tab position $position")
                }
            )
        }.attach()

        fun setTimeSettingsVisibility(gone: Boolean) {
            // A ConstraintLayout Group is broken is 1.1.3
            binding.run {
                btnRecordStartTime.isGone = gone
                textRecordTimeDivider.isGone = gone
                btnRecordEndTime.isGone = gone
                btnRecordRecent.isGone = gone
            }
        }
        binding.tabLayoutRecord.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabSelected(tab: TabLayout.Tab?) {
                TransitionManager.beginDelayedTransition(
                    binding.root as ViewGroup,
                    AutoTransition().apply {
                        ordering = TransitionSet.ORDERING_TOGETHER
                        excludeTarget(RecyclerView::class.java, true)
                        excludeChildren(RecyclerView::class.java, true)
                    }
                )
                setTimeSettingsVisibility(gone = tab?.position == 2)
            }
        })
        setTimeSettingsVisibility(
            gone = savedInstanceState?.getInt(EXTRA_VIEW_PAGER_POSITION, -1) == 2
        )
        binding.viewPagerRecord.setCurrentItem(
            context.safeSharedPreference.getInt(PREF_LAST_TAB, 0), false
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_VIEW_PAGER_POSITION, getViewPagerCurrentItem())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        context?.safeSharedPreference?.edit {
            putInt(PREF_LAST_TAB, getViewPagerCurrentItem())
        }
    }

    private fun getViewPagerCurrentItem(): Int {
        return view?.findViewById<ViewPager2>(R.id.viewPagerRecord)?.currentItem ?: -1
    }
}

private const val EXTRA_VIEW_PAGER_POSITION = "position"
private const val PREF_LAST_TAB = "pref_records_last_tab"
