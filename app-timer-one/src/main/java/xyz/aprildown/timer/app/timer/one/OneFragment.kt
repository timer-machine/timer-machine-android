package xyz.aprildown.timer.app.timer.one

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.MenuItemCompat
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import com.github.deweyreed.tools.anko.dp
import com.github.deweyreed.tools.anko.snackbar
import com.github.deweyreed.tools.anko.toast
import com.github.deweyreed.tools.arch.observeEvent
import com.github.deweyreed.tools.arch.observeNonNull
import com.github.deweyreed.tools.helper.hasPermissions
import com.github.deweyreed.tools.helper.setTextIfChanged
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneFourActions
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneTimeSize
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneUsingStep
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneUsingTimingBar
import xyz.aprildown.timer.app.base.data.PreferenceData.timePanels
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.one.databinding.FragmentOneBinding
import xyz.aprildown.timer.app.timer.one.layout.TweakTimeLayout
import xyz.aprildown.timer.app.timer.one.step.StepListView
import xyz.aprildown.timer.component.key.TimePanelLayout
import xyz.aprildown.timer.component.key.switchItem
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getNiceLoopString
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class OneFragment :
    BaseOneFragment<FragmentOneBinding>(R.layout.fragment_one),
    FiveActionsView.Listener {

    private var postNotificationsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val binding = FragmentOneBinding.bind(view)
        setUpViews(binding)
        setUpMenu(context)
        applySettings(binding)
        setUpObservers(binding)
    }

    private fun setUpViews(binding: FragmentOneBinding) {
        val context = binding.root.context
        binding.fiveActionsOne.run {
            setActionClickListener(this@OneFragment)
            setMainFabClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !context.hasPermissions(Manifest.permission.POST_NOTIFICATIONS) &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    actionStartPause()
                }
            }
        }

        binding.layoutOneTweakTime.setCallback(
            requireActivity(),
            object : TweakTimeLayout.Callback {
                override fun onTweakTime(amount: Long) {
                    actionTweakTime(amount)
                }
            }
        )

        viewModel.messageEvent.observeEvent(viewLifecycleOwner) {
            when (it) {
                RBase.string.one_ui_locked -> {
                    binding.layoutOneRoot.snackbar(it, RBase.string.one_ui_unlock) {
                        actionLockUi(false)
                    }
                }
                else -> {
                    binding.layoutOneRoot.snackbar(it)
                }
            }
        }
        binding.textOneLoop.setOnClickListener {
            showPickLoopDialog(viewModel.timer.value?.loop ?: 1) {
                val currentIndex = viewModel.timerCurrentIndex.value ?: return@showPickLoopDialog
                actionJump(
                    when (currentIndex) {
                        is TimerIndex.Start, is TimerIndex.End -> {
                            val firstStep = viewModel.timer.value?.steps?.get(0)
                                ?: return@showPickLoopDialog
                            if (firstStep is StepEntity.Step) {
                                TimerIndex.Step(loopIndex = it, stepIndex = 0)
                            } else {
                                TimerIndex.Group(
                                    loopIndex = it,
                                    stepIndex = 0,
                                    groupStepIndex = TimerIndex.Step(0, 0)
                                )
                            }
                        }
                        is TimerIndex.Step -> currentIndex.copy(loopIndex = it)
                        is TimerIndex.Group -> currentIndex.copy(loopIndex = it)
                    }
                )
            }
        }
        binding.listOneSteps.run {
            setHasFixedSize(true)
            listener = object : StepListView.StepLongClickListener {
                override fun onJumpToStep(index: TimerIndex) {
                    actionJump(index)
                }

                override fun onGroupTitleClicked(defaultIndex: TimerIndex.Group) {
                    val step = viewModel.timer.value?.steps?.get(defaultIndex.stepIndex)
                    val maxLoop = (step as? StepEntity.Group)?.loop ?: 1
                    showPickLoopDialog(maxLoop) {
                        actionJump(
                            defaultIndex.copy(
                                groupStepIndex = defaultIndex.groupStepIndex.copy(loopIndex = it)
                            )
                        )
                    }
                }

                override fun onEditStep(index: TimerIndex) {
                    actionUpdateStep(index)
                }

                override fun onEditStepTime(index: TimerIndex) {
                    actionUpdateStepTime(index)
                }
            }
            imageCheckListener = {
                startActivity(appNavigator.getImagePreviewIntent(it.data))
            }
        }

        viewModel.timer.observe(viewLifecycleOwner) {
            setToolbarTitle(it.name)
        }
        viewModel.editTimerEvent.observeEvent(viewLifecycleOwner) {
            startActivity(appNavigator.getEditIntent(timerId = it))
        }
        viewModel.intentEvent.observeEvent(viewLifecycleOwner) {
            context.startService(it)
        }
    }

    override fun onActionClick(index: Int, view: View) {
        when (view.tag) {
            PreferenceData.ONE_LAYOUT_ONE_ACTION_STOP -> actionStop()
            PreferenceData.ONE_LAYOUT_ONE_ACTION_PREV -> actionPrevStep()
            PreferenceData.ONE_LAYOUT_ONE_ACTION_NEXT -> actionNextStep()
            PreferenceData.ONE_LAYOUT_ONE_ACTION_MORE -> showActionsMenu(view.context, view)
            PreferenceData.ONE_LAYOUT_ONE_ACTION_LOCK -> actionLockUi(
                !(viewModel.uiLocked.value ?: false)
            )
            PreferenceData.ONE_LAYOUT_ONE_ACTION_EDIT -> actionEditTimer()
        }
    }

    private fun showActionsMenu(context: Context, view: View) {
        popupMenu {
            section {
                switchItem {
                    label = context.getString(RBase.string.one_action_lock_ui)
                    onBind = {
                        it.isChecked = viewModel.uiLocked.value ?: false
                    }
                    onCheckedChange = { _, isChecked ->
                        actionLockUi(isChecked)
                    }
                }
                item {
                    label = context.getString(RBase.string.one_action_show_float)
                    callback = { actionFloating() }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                ) {
                    item {
                        label = context.getString(RBase.string.one_action_pip)
                        icon = RBase.drawable.settings_pip
                        callback = { actionEnterPipMode() }
                    }
                }
            }
            section {
                item {
                    label = context.getString(RBase.string.one_action_edit_layout)
                    icon = RBase.drawable.settings_customize
                    callback = { actionToOneLayoutSetting() }
                }
                item {
                    label = context.getString(RBase.string.one_action_add_shortcut)
                    icon = RBase.drawable.ic_watch
                    callback = { actionCreateShortcut() }
                }
                item {
                    label = context.getString(RBase.string.one_action_edit_timer)
                    icon = RBase.drawable.ic_edit
                    callback = { actionEditTimer() }
                }
            }
        }.show(context, view)
    }

    private fun setUpMenu(context: Context) {
        val sharedPreferences = context.safeSharedPreference
        activity?.addMenuProvider(
            object : MenuProvider {
                private val id = View.generateViewId()
                private var toast: Toast? = null

                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    val menuItem =
                        menu.add(0, id, 0, RBase.string.pref_screen_title)
                    MenuItemCompat.setContentDescription(
                        menuItem,
                        context.getString(RBase.string.pref_screen_title)
                    )
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    menuItem.updateState()
                }

                private fun MenuItem.updateState() {
                    when (
                        sharedPreferences.getString(
                            PreferenceData.KEY_SCREEN,
                            context.getString(RBase.string.pref_screen_value_default)
                        )
                    ) {
                        context.getString(RBase.string.pref_screen_value_keep) -> {
                            setIcon(RBase.drawable.settings_brightness_keep)
                            setTitle(RBase.string.pref_screen_title_on)
                        }
                        context.getString(RBase.string.pref_screen_value_dim) -> {
                            setIcon(RBase.drawable.settings_brightness_dim)
                            setTitle(RBase.string.pref_screen_title_dim)
                        }
                        else -> {
                            setIcon(RBase.drawable.settings_brightness)
                            setTitle(RBase.string.pref_screen_title_default)
                        }
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId != id) return false
                    toast?.cancel()
                    when (
                        context.safeSharedPreference.getString(
                            PreferenceData.KEY_SCREEN,
                            context.getString(RBase.string.pref_screen_value_default)
                        )
                    ) {
                        context.getString(RBase.string.pref_screen_value_keep) -> {
                            sharedPreferences.edit {
                                putString(
                                    PreferenceData.KEY_SCREEN,
                                    context.getString(RBase.string.pref_screen_value_dim)
                                )
                            }
                            toast = context.toast(RBase.string.pref_screen_title_dim)
                        }
                        context.getString(RBase.string.pref_screen_value_dim) -> {
                            sharedPreferences.edit {
                                putString(
                                    PreferenceData.KEY_SCREEN,
                                    context.getString(RBase.string.pref_screen_value_default)
                                )
                            }
                            toast = context.toast(RBase.string.pref_screen_title_default)
                        }
                        else -> {
                            sharedPreferences.edit {
                                putString(
                                    PreferenceData.KEY_SCREEN,
                                    context.getString(RBase.string.pref_screen_value_keep)
                                )
                            }
                            toast = context.toast(RBase.string.pref_screen_title_on)
                        }
                    }
                    menuItem.updateState()
                    return true
                }
            },
            viewLifecycleOwner
        )
    }

    private fun applySettings(binding: FragmentOneBinding) {
        val context = binding.root.context

        binding.layoutOneStepName.root.isVisible = context.oneOneUsingStep
        binding.layoutOneStepName.text.textSize = context.dp(context.oneOneTimeSize)

        if (context.oneOneUsingTimingBar) {
            val mpb = binding.stubTimingBar.inflate() as LinearProgressIndicator
            viewModel.timerCurrentTime.observe(viewLifecycleOwner) {
                val stepTime = viewModel.timerStepTime
                if (stepTime > 0) {
                    mpb.progress = (it.toDouble() / stepTime.toDouble() * 100).toInt()
                }
            }
        }

        binding.textOneTime.textSize = context.dp(context.oneOneTimeSize)

        context.timePanels.let {
            if (it.isNotEmpty()) {
                val timePanelLayout = binding.stubTimePanel.inflate() as TimePanelLayout
                timePanelLayout.setPanels(it)
                setUpTimePanels(timePanelLayout, it)
            }
        }

        val actions = context.oneOneFourActions
        binding.fiveActionsOne.withActions(getFourActionsFromKeys(actions))
        if (actions.contains(PreferenceData.ONE_LAYOUT_ONE_ACTION_LOCK)) {
            viewModel.uiLocked.observe(viewLifecycleOwner) {
                if (it == true) {
                    binding.fiveActionsOne.changeAction(
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_LOCK,
                        RBase.string.one_action_unlock_ui,
                        RBase.drawable.ic_locked
                    )
                } else {
                    binding.fiveActionsOne.changeAction(
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_LOCK,
                        RBase.string.one_action_lock_ui,
                        RBase.drawable.ic_unlocked
                    )
                }
            }
        }
    }

    private fun setUpTimePanels(
        timePanelLayout: TimePanelLayout,
        timePanels: List<PreferenceData.TimePanel>
    ) {
        viewModel.timerElapsedTime.observeNonNull(viewLifecycleOwner) { elapsedTime: Long ->
            val timerTotalTime = viewModel.timerTotalTime.toDouble()
            timePanels.forEach { timePanel ->
                timePanelLayout.updateText(
                    timePanel,
                    when (timePanel) {
                        PreferenceData.TimePanel.ELAPSED_TIME -> elapsedTime.toDouble()
                        PreferenceData.TimePanel.ELAPSED_PERCENT ->
                            elapsedTime / timerTotalTime * 100
                        PreferenceData.TimePanel.REMAINING_TIME ->
                            timerTotalTime - elapsedTime
                        PreferenceData.TimePanel.REMAINING_PERCENT ->
                            (timerTotalTime - elapsedTime) / timerTotalTime * 100
                        PreferenceData.TimePanel.STEP_END_TIME ->
                            System.currentTimeMillis() +
                                (viewModel.timerCurrentTime.value ?: 0L).toDouble()
                        PreferenceData.TimePanel.TIMER_END_TIME ->
                            System.currentTimeMillis() + timerTotalTime - elapsedTime
                    }
                )
            }
        }
    }

    private fun setUpObservers(binding: FragmentOneBinding) {
        viewModel.timerCurrentTime.observe(viewLifecycleOwner) { time ->
            binding.textOneTime.text = (time ?: 0L).produceTime()
        }
        viewModel.timerCurrentIndex.observe(viewLifecycleOwner) { index ->
            if (index == null) return@observe
            val timer = viewModel.timer.value ?: return@observe
            binding.layoutOneStepName.text.setTextIfChanged(timer.getStep(index)?.label)
            binding.textOneLoop.setTextIfChanged(index.getNiceLoopString(timer.loop))
            binding.listOneSteps.toIndex(index)
        }
        viewModel.timer.observe(viewLifecycleOwner) { timer ->
            if (timer == null) return@observe
            val index = viewModel.timerCurrentIndex.value
            if (index != null) {
                binding.layoutOneStepName.text.setTextIfChanged(timer.getStep(index)?.label)
            }
            binding.listOneSteps.setTimer(timer)
        }
        viewModel.timerCurrentState.observe(viewLifecycleOwner) { state ->
            binding.fiveActionsOne.changeState(
                if (state?.isRunning == true) {
                    FiveActionsView.STATE_PAUSE
                } else {
                    FiveActionsView.STATE_PLAY
                }
            )
        }
    }

    companion object {
        fun getFourActionsFromKeys(keys: List<String>): List<FiveActionsView.Action> =
            keys.map {
                when (it) {
                    PreferenceData.ONE_LAYOUT_ONE_ACTION_STOP -> {
                        FiveActionsView.Action(
                            tag = it,
                            nameRes = RBase.string.one_action_stop,
                            defaultDrawableRes = RBase.drawable.ic_stop
                        )
                    }
                    PreferenceData.ONE_LAYOUT_ONE_ACTION_PREV -> {
                        FiveActionsView.Action(
                            tag = it,
                            nameRes = RBase.string.one_action_prev,
                            defaultDrawableRes = RBase.drawable.ic_arrow_up
                        )
                    }
                    PreferenceData.ONE_LAYOUT_ONE_ACTION_NEXT -> {
                        FiveActionsView.Action(
                            tag = it,
                            nameRes = RBase.string.one_action_next,
                            defaultDrawableRes = RBase.drawable.ic_arrow_down
                        )
                    }
                    PreferenceData.ONE_LAYOUT_ONE_ACTION_MORE -> {
                        FiveActionsView.Action(
                            tag = it,
                            nameRes = RBase.string.one_action_more,
                            defaultDrawableRes = RBase.drawable.ic_overflow
                        )
                    }
                    PreferenceData.ONE_LAYOUT_ONE_ACTION_LOCK -> {
                        FiveActionsView.Action(
                            tag = it,
                            nameRes = RBase.string.one_action_lock_ui,
                            defaultDrawableRes = RBase.drawable.ic_unlocked
                        )
                    }
                    PreferenceData.ONE_LAYOUT_ONE_ACTION_EDIT -> {
                        FiveActionsView.Action(
                            tag = it,
                            nameRes = RBase.string.one_action_edit_timer,
                            defaultDrawableRes = RBase.drawable.ic_edit
                        )
                    }
                    else -> error("Unknown action $it")
                }
            }
    }
}
