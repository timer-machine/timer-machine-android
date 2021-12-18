package xyz.aprildown.timer.app.scheduler

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.app.base.ui.SpecialItemTouchHelperCallback
import xyz.aprildown.timer.app.base.utils.NavigationUtils.subLevelNavigate
import xyz.aprildown.timer.app.scheduler.databinding.FragmentSchedulerBinding
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.usecases.scheduler.SetSchedulerEnable
import xyz.aprildown.timer.domain.utils.AppConfig
import xyz.aprildown.timer.presentation.scheduler.SchedulerViewModel
import xyz.aprildown.tools.anko.longSnackbar
import xyz.aprildown.tools.arch.observeEvent
import xyz.aprildown.tools.helper.SoftDeleteHelper
import xyz.aprildown.tools.helper.color
import xyz.aprildown.tools.helper.drawable
import xyz.aprildown.tools.helper.getNumberFormattedQuantityString
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.tools.helper.tinted
import xyz.aprildown.tools.utils.withEmptyView
import com.mikepenz.materialize.R as RMaterialize
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class SchedulerFragment : Fragment(R.layout.fragment_scheduler), MainCallback.FragmentCallback {

    private lateinit var mainCallback: MainCallback.ActivityCallback

    private val viewModel: SchedulerViewModel by viewModels()

    private var deletedAny = false
    private val softDeleteHelper by lazy { SoftDeleteHelper() }
    private var snackbar: Snackbar? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainCallback = context as MainCallback.ActivityCallback
        if (AppConfig.showFirstTimeInfo) {
            val sp = context.safeSharedPreference
            if (sp.getBoolean(SP_SHOW_SCHEDULER_DIALOG, true)) {
                sp.edit {
                    putBoolean(SP_SHOW_SCHEDULER_DIALOG, false)
                }
                showSchedulerAlertDialog(context)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSchedulerBinding.bind(view)
        setUpMainActions()
        setUpRecyclerView(binding)
        setUpObservers()
        viewModel.load()
    }

    override fun onStop() {
        super.onStop()
        snackbar?.dismiss()
        if (deletedAny) {
            softDeleteHelper.execute()
            deletedAny = false
        }
    }

    override fun onFabClick(view: View) {
        editScheduler()
    }

    private fun setUpMainActions() {
        mainCallback.actionFab.contentDescription = getString(RBase.string.scheduler_title)
    }

    private fun setUpRecyclerView(binding: FragmentSchedulerBinding) {
        val context = binding.root.context

        val schedulerAdapter = SchedulerAdapter(
            onClickScheduler = {
                editScheduler(it)
            },
            schedulerCallback = object : VisibleScheduler.Callback {
                override fun onSchedulerStateChange(id: Int, enable: Boolean) {
                    viewModel.toggleSchedulerState(id, enable)
                }
            }
        )

        binding.list.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = schedulerAdapter
            adapter?.withEmptyView(binding.viewEmpty)
        }
        ItemTouchHelper(
            SpecialItemTouchHelperCallback(
                context,
                SpecialItemTouchHelperCallback.Config.getEditDeleteConfig(context),
                startSwipeCallback = object : SpecialItemTouchHelperCallback.SwipeCallback {
                    override fun onSwipe(viewHolder: RecyclerView.ViewHolder) {
                        val pos = viewHolder.bindingAdapterPosition
                        if (pos in 0 until schedulerAdapter.itemCount) {
                            softDelete(schedulerAdapter, pos)
                        }
                    }
                },
                endSwipeCallback = object : SpecialItemTouchHelperCallback.SwipeCallback {
                    override fun onSwipe(viewHolder: RecyclerView.ViewHolder) {
                        val pos = viewHolder.bindingAdapterPosition
                        if (pos in 0 until schedulerAdapter.itemCount) {
                            editScheduler(schedulerAdapter.get(pos).scheduler)
                            schedulerAdapter.notifyItemChanged(pos)
                        }
                    }
                }
            )
        ).attachToRecyclerView(binding.list)

        viewModel.schedulerWithTimerInfo.observe(viewLifecycleOwner) { items ->
            schedulerAdapter.set(
                items.map { (scheduler, timerInfo) ->
                    VisibleScheduler.fromSchedulerEntity(
                        scheduler,
                        context,
                        timerInfo?.name ?: getString(RBase.string.unknown),
                    )
                }
            )
        }
    }

    private fun editScheduler(scheduler: SchedulerEntity? = null) {
        NavHostFragment.findNavController(this).subLevelNavigate(
            RBase.id.dest_edit_scheduler,
            EditSchedulerFragment.getBundle(scheduler?.id ?: SchedulerEntity.NEW_ID)
        )
    }

    private fun softDelete(adapter: SchedulerAdapter, pos: Int) {
        if (pos !in 0 until adapter.itemCount) return

        deletedAny = true
        val scheduler = adapter.get(pos)
        softDeleteHelper.schedule {
            snackbar = null
            viewModel.delete(scheduler.scheduler.id)
        }

        val actionSize = softDeleteHelper.actionSize
        snackbar = mainCallback.snackbarView.longSnackbar(
            getString(
                RBase.string.delete_done_template,
                if (actionSize == 1) {
                    scheduler.scheduler.label
                } else {
                    resources.getNumberFormattedQuantityString(RBase.plurals.schedulers, actionSize)
                }
            ),
            getString(RBase.string.undo)
        ) {
            softDeleteHelper.undo()
            viewModel.load()
        }
        adapter.remove(pos)
    }

    private fun showSchedulerAlertDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setIcon(
                context.drawable(RBase.drawable.ic_warning)
                    .tinted(context.color(RMaterialize.color.md_red_500))
            )
            .setTitle(RBase.string.scheduler_alert_title)
            .setMessage(RBase.string.scheduler_alert_content)
            .setPositiveButton(RBase.string.understand, null)
            .show()
    }

    private fun setUpObservers() {
        viewModel.scheduleEvent.observeEvent(viewLifecycleOwner) {
            mainCallback.snackbarView.longSnackbar(
                when (it) {
                    is SetSchedulerEnable.Result.Scheduled -> {
                        it.time.formatElapsedTimeUntilScheduler(resources)
                    }
                    is SetSchedulerEnable.Result.Canceled -> {
                        getString(
                            RBase.string.scheduler_canceled_template,
                            resources.getNumberFormattedQuantityString(
                                RBase.plurals.schedulers,
                                it.count
                            )
                        )
                    }
                    is SetSchedulerEnable.Result.Failed -> {
                        getString(RBase.string.scheduler_schedule_failed)
                    }
                }
            )
        }
    }
}

private const val SP_SHOW_SCHEDULER_DIALOG = "pref_show_scheduler_dialog2"

private fun Long.formatElapsedTimeUntilScheduler(res: Resources): String {
    var delta = this - System.currentTimeMillis()
    // If the alarm will ring within 60 seconds, just report "less than a minute."
    val formats = res.getStringArray(RBase.array.scheduler_set)
    if (delta < DateUtils.MINUTE_IN_MILLIS) {
        return formats[0]
    }

    // Otherwise, format the remaining time until the alarm rings.

    // Round delta upwards to the nearest whole minute. (e.g. 7m 58s -> 8m)
    val remainder = delta % DateUtils.MINUTE_IN_MILLIS
    delta += if (remainder == 0L) 0 else DateUtils.MINUTE_IN_MILLIS - remainder

    var hours = delta.toInt() / (1000 * 60 * 60)
    val minutes = delta.toInt() / (1000 * 60) % 60
    val days = hours / 24
    hours %= 24

    val daySeq = res.getNumberFormattedQuantityString(RBase.plurals.days, days)
    val minSeq = res.getNumberFormattedQuantityString(RBase.plurals.minutes, minutes)
    val hourSeq = res.getNumberFormattedQuantityString(RBase.plurals.hours, hours)

    val showDays = days > 0
    val showHours = hours > 0
    val showMinutes = minutes > 0

    // Compute the index of the most appropriate time format based on the time delta.
    val index =
        (if (showDays) 1 else 0) or (if (showHours) 2 else 0) or if (showMinutes) 4 else 0

    return String.format(formats[index], daySeq, hourSeq, minSeq)
}
