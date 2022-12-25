package xyz.aprildown.timer.app.timer.list.record

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.aprildown.timer.app.base.data.PreferenceData.startWeekOn
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.timer.list.R
import xyz.aprildown.timer.app.timer.list.databinding.FragmentRecordBinding
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.presentation.timer.RecordViewModel
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
                PopupMenu(context, view, Gravity.TOP or Gravity.END).apply {
                    inflate(R.menu.record_predefined_time)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.record_predefined_today -> {
                                viewModel.updateStartTime(RecordViewModel.START_SINCE_NOW)
                            }
                            R.id.record_predefined_week -> {
                                viewModel.updateStartTime(RecordViewModel.START_SINCE_LAST_WEEK)
                            }
                            R.id.record_predefined_month -> {
                                viewModel.updateStartTime(RecordViewModel.START_SINCE_LAST_MONTH)
                            }
                            R.id.record_predefined_year -> {
                                viewModel.updateStartTime(RecordViewModel.START_SINCE_LAST_YEAR)
                            }
                            R.id.record_predefined_total -> {
                                viewModel.updateStartTime(RecordViewModel.START_SINCE_THE_START)
                            }
                        }
                        true
                    }
                }.show()
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

        binding.tabLayoutRecord.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab == null) return
                TransitionManager.beginDelayedTransition(
                    binding.root as ViewGroup,
                    AutoTransition().apply {
                        ordering = TransitionSet.ORDERING_TOGETHER
                        excludeTarget(RecyclerView::class.java, true)
                        excludeChildren(RecyclerView::class.java, true)
                    }
                )
                binding.groupTime.isVisible = tab.position != 2

                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.preferencesRepository.setInt(PREF_LAST_TAB, tab.position)
                }
            }
        })
        binding.groupTime.isVisible =
            savedInstanceState?.getInt(EXTRA_VIEW_PAGER_POSITION, -1) != 2

        if (savedInstanceState == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                binding.viewPagerRecord.setCurrentItem(
                    viewModel.preferencesRepository.getInt(PREF_LAST_TAB, 0),
                    false
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(
            EXTRA_VIEW_PAGER_POSITION,
            view?.findViewById<ViewPager2>(R.id.viewPagerRecord)?.currentItem ?: -1
        )
    }
}

private const val EXTRA_VIEW_PAGER_POSITION = "position"
private const val PREF_LAST_TAB = "pref_records_last_tab"
