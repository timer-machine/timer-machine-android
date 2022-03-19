package xyz.aprildown.timer.app.timer.edit

import android.app.Activity
import android.content.ClipboardManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.view.MenuCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.CustomEventHook
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.showTimerTotalTime
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.base.utils.ShortcutHelper
import xyz.aprildown.timer.app.timer.edit.databinding.ActivityEditTimerBinding
import xyz.aprildown.timer.app.timer.edit.utils.SaveInstanceHelper
import xyz.aprildown.timer.component.key.DurationPicker
import xyz.aprildown.timer.component.key.behaviour.EditableBehaviourLayout
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.toBeepAction
import xyz.aprildown.timer.domain.entities.toCountAction
import xyz.aprildown.timer.domain.entities.toHalfAction
import xyz.aprildown.timer.domain.entities.toMusicAction
import xyz.aprildown.timer.domain.entities.toNotificationAction
import xyz.aprildown.timer.domain.entities.toScreenAction
import xyz.aprildown.timer.domain.entities.toVibrationAction
import xyz.aprildown.timer.domain.entities.toVoiceAction
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.presentation.edit.EditViewModel
import xyz.aprildown.timer.presentation.stream.accumulateTime
import xyz.aprildown.tools.anko.dip
import xyz.aprildown.tools.anko.snackbar
import xyz.aprildown.tools.arch.observeEvent
import xyz.aprildown.tools.helper.IntentHelper
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.show
import xyz.aprildown.tools.helper.startActivityOrNothing
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class EditActivity :
    BaseActivity(),
    EditableStep.Handler,
    EditableGroup.Handler,
    EditableGroupEnd.Handler {

    private lateinit var binding: ActivityEditTimerBinding

    private val viewModel: EditViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    private lateinit var startAdapter: ItemAdapter<EditableStep>
    private lateinit var stepAdapter: ItemAdapter<IItem<*>>
    private lateinit var endAdapter: ItemAdapter<EditableStep>
    private lateinit var fastAdapter: FastAdapter<IItem<*>>

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        getRingtonePickerResultCallback()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.init(
            timerId = intent?.getIntExtra(Constants.EXTRA_TIMER_ID, TimerEntity.NEW_ID)
                ?: TimerEntity.NEW_ID,
            folderId = intent?.getLongExtra(EXTRA_FOLDER_ID, FolderEntity.FOLDER_DEFAULT)
                ?: FolderEntity.FOLDER_DEFAULT
        )

        setUpToolbar()
        setUpNameLoopView()
        setUpRecyclerView()
        setUpOtherViews()
        setUpSnackbar()
        subscribeToChanges()

        if (savedInstanceState == null) {
            loadData()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        SaveInstanceHelper.withHelper(this) {
            save(EXTRA_DATA_STEPS, getStepEntityFromFastAdapter())
            save(EXTRA_DATA_START, startAdapter.getSingleStepFromAdapter())
            save(EXTRA_DATA_END, endAdapter.getSingleStepFromAdapter())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        SaveInstanceHelper.withHelper(this) {
            getAndRemove<List<StepEntity>>(EXTRA_DATA_STEPS)?.moveIntoStepsAdapter()
            getAndRemove<StepEntity.Step>(EXTRA_DATA_START)?.moveIntoSingleAdapter(startAdapter)
            getAndRemove<StepEntity.Step>(EXTRA_DATA_END)?.moveIntoSingleAdapter(endAdapter)
        }
    }

    // region Toolbar menu actions

    override fun onBackPressed() {
        userLeave { finishEditTimerActivity() }
    }

    // endregion Toolbar menu actions

    // region SetUps and main actions

    private fun setUpToolbar() {
        binding.toolbar.run {
            inflateMenu(R.menu.edit)
            MenuCompat.setGroupDividerEnabled(menu, true)
            if (viewModel.isNewTimer) {
                menu?.let { menu ->
                    menu.findItem(R.id.action_delete_timer)?.isVisible = false
                }
            }
            setNavigationIcon(RBase.drawable.ic_back)
            setNavigationContentDescription(RBase.string.nav_up)
            setNavigationOnClickListener { onBackPressed() }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save_timer -> {
                        saveTimer()
                    }
                    R.id.action_delete_timer -> {
                        MaterialAlertDialogBuilder(this@EditActivity)
                            .setMessage(
                                buildSpannedString {
                                    append(
                                        getString(RBase.string.delete_confirmation_template)
                                            .format(viewModel.name.value)
                                    )
                                    append("\n\n")
                                    append(
                                        getString(RBase.string.folder_delete_timer_explanation),
                                        StyleSpan(Typeface.BOLD),
                                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                                    )
                                }
                            )
                            .setPositiveButton(RBase.string.delete) { _, _ ->
                                ShortcutHelper.disableTimerShortcut(this@EditActivity, viewModel.id)
                                viewModel.deleteTimer()
                            }
                            .setNegativeButton(RBase.string.cancel, null)
                            .show()
                    }
                    R.id.action_timer_template -> MaterialAlertDialogBuilder(this@EditActivity)
                        .setTitle(RBase.string.sample_timer_template_title)
                        .setItems(RBase.array.sample_timers) { _, which ->
                            userLeave {
                                when (which) {
                                    0 -> viewModel.loadSampleTimer(getOneStageTimer())
                                    1 -> viewModel.loadSampleTimer(getTwoStagesTimer())
                                    2 -> viewModel.loadSampleTimer(getThreeStagesTimer())
                                }
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                    R.id.action_timer_copy_to_clipboard -> {
                        viewModel.generateTimerString(
                            newSteps = getStepEntityFromFastAdapter(),
                            start = startAdapter.getSingleStepFromAdapter(),
                            end = endAdapter.getSingleStepFromAdapter()
                        )
                    }
                    R.id.action_timer_create_from_clipboard -> {
                        userLeave {
                            val content = getSystemService<ClipboardManager>()
                                ?.primaryClip
                                ?.getItemAt(0)
                                ?.coerceToText(this@EditActivity)
                                ?.toString() ?: return@userLeave
                            viewModel.populateWithString(content)
                        }
                    }
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            setTitle(
                if (viewModel.isNewTimer) RBase.string.edit_create_timer
                else RBase.string.edit_edit_timer
            )
        }
    }

    private fun setUpNameLoopView() {
        viewModel.name.observe(this) {
            binding.viewEditNameLoop.setName(it)
        }
        binding.viewEditNameLoop.nameView.doAfterTextChanged {
            viewModel.name.value = binding.viewEditNameLoop.getName()
        }
        viewModel.loop.observe(this) {
            binding.viewEditNameLoop.setLoop(it)
        }
        binding.viewEditNameLoop.loopView.doAfterTextChanged {
            viewModel.loop.value = binding.viewEditNameLoop.getLoop()
        }
    }

    private fun setUpRecyclerView() {
        startAdapter = ItemAdapter()
        stepAdapter = ItemAdapter()
        endAdapter = ItemAdapter()
        val footerStepAdapter: ItemAdapter<EditableFooter> = ItemAdapter()
        fastAdapter = FastAdapter.with(
            listOf(startAdapter, stepAdapter, endAdapter, footerStepAdapter)
        )

        binding.listEditSteps.run {
            layoutManager = LinearLayoutManager(this@EditActivity)
            adapter = fastAdapter
        }
        val stepTouchHelper = StepTouchHelper(
            onItemMoved = { from, to ->
                stepAdapter.move(from, to)
            },
            onItemDropped = { _, to ->
                if (shouldUpdateTotalTime) {
                    binding.listEditSteps.updatePadding(top = dip(64))
                }
                var isInGroup = false
                for (index in to downTo 0) {
                    val item = fastAdapter.getItem(index)
                    if (item is EditableGroup) {
                        isInGroup = true
                        break
                    } else if (item is EditableGroupEnd) {
                        isInGroup = false
                        break
                    }
                }
                val targetItem = fastAdapter.getItem(to)
                if (targetItem != null && targetItem is EditableStep) {
                    targetItem.isInAGroup = isInGroup
                    fastAdapter.notifyItemChanged(to, EditableStep.Event.InOutGroup)
                    postUpdateTotalTime()
                }
            },
            onItemSwiped = { position ->
                val item = fastAdapter.getItem(position) as? EditableStep
                when (item?.stepType) {
                    StepType.START -> startAdapter.clear()
                    StepType.END -> endAdapter.clear()
                    else -> stepAdapter.remove(position)
                }
                postUpdateTotalTime()
            }
        )

        val itemTouchHelper = ItemTouchHelper(stepTouchHelper)
        fastAdapter.addEventHook(
            object : CustomEventHook<GenericItem>() {
                override fun onBind(viewHolder: RecyclerView.ViewHolder): View {
                    return viewHolder.itemView
                }

                override fun attachEvent(view: View, viewHolder: RecyclerView.ViewHolder) {
                    view.setOnLongClickListener {
                        val item = FastAdapter.getHolderAdapterItem<GenericItem>(viewHolder)
                        if (item != null && StepTouchHelper.canItemBeMoved(item)) {
                            if (shouldUpdateTotalTime) {
                                // Top padding affects item dragging.
                                binding.listEditSteps.updatePadding(top = 0)
                            }
                            itemTouchHelper.startDrag(viewHolder)
                            true
                        } else {
                            val msgRes = when {
                                item is EditableStep && item.stepType == StepType.START -> {
                                    RBase.string.edit_cant_move_start
                                }
                                item is EditableStep && item.stepType == StepType.END -> {
                                    RBase.string.edit_cant_move_end
                                }
                                item is EditableGroup || item is EditableGroupEnd -> {
                                    RBase.string.edit_cant_move_group
                                }
                                item is EditableFooter -> {
                                    RBase.string.edit_cant_move_footer
                                }
                                else -> throw IllegalStateException("What are you moving? $item")
                            }
                            binding.root.snackbar(msgRes)
                            false
                        }
                    }
                }
            }
        )

        itemTouchHelper.attachToRecyclerView(binding.listEditSteps)
        footerStepAdapter.add(EditableFooter(buildFooterClickListeners()))
    }

    override fun onStepNameChange(position: Int, newName: String) {
        if (getStepFromFastAdapter(position).stepType == StepType.NOTIFIER) {
            viewModel.notifier = viewModel.notifier.copy(label = newName)
        }
    }

    override fun onLengthClick(view: View, position: Int) {
        DurationPicker(this) { hours, minutes, seconds ->
            val time = (hours * 3600L + minutes * 60L + seconds) * 1000L
            val item = getStepFromFastAdapter(position)
            if (item.stepType == StepType.NOTIFIER) {
                viewModel.notifier = viewModel.notifier.copy(length = time)
            }
            item.length = time
            fastAdapter.notifyAdapterItemChanged(position, EditableStep.Event.Length)
            postUpdateTotalTime()
        }.show()
    }

    override fun onAddBtnClick(view: View, position: Int) {
        val editableStep = fastAdapter.getItem(position) as EditableStep
        val isInAGroup = editableStep.isInAGroup
        popupMenu {
            dropdownGravity = Gravity.START or Gravity.TOP
            section {
                item {
                    label = getString(RBase.string.edit_add_normal)
                    callback = {
                        if (isInAGroup) addNormalStepToGroup(position + 1)
                        else addNormalStep(position + 1)
                    }
                }
                item {
                    label = getString(RBase.string.edit_add_notifier)
                    callback = {
                        if (isInAGroup) addNotifierStepToGroup(position + 1)
                        else addNotifierStep(position + 1)
                    }
                }
                if (!isInAGroup) {
                    item {
                        label = getString(RBase.string.edit_add_group)
                        callback = { addGroup(position + 1) }
                    }
                }
            }
            section {
                item {
                    label = getString(RBase.string.duplicate)
                    callback = {
                        val currentStep = editableStep.toStep()
                        addStep(
                            position + 1,
                            EditableStep(
                                label = currentStep.label,
                                length = currentStep.length,
                                behaviour = currentStep.behaviour,
                                stepType = currentStep.type,
                                handler = this@EditActivity,
                                isInAGroup = editableStep.isInAGroup
                            )
                        )
                    }
                }
            }
        }.show(view.context, view)
    }

    override fun onGroupDeleteButtonClick(view: View, position: Int) {
        var removeCount = 1
        for (bias in 1 until stepAdapter.adapterItemCount) {
            val iItem = fastAdapter.getItem(position + bias)
            ++removeCount
            if (iItem is EditableGroupEnd) {
                break
            }
        }
        val validRange = 0 until fastAdapter.itemCount
        if (position in validRange && (position + removeCount) in validRange) {
            stepAdapter.removeRange(position, removeCount)
            postUpdateTotalTime()
        }
    }

    override fun onGroupLoopChanged() {
        postUpdateTotalTime()
    }

    override fun onGroupEndClick(view: View, position: Int) {
        popupMenu {
            dropdownGravity = Gravity.START or Gravity.TOP
            section {
                item {
                    label = getString(RBase.string.edit_add_normal)
                    callback = {
                        addNormalStep(position + 1)
                    }
                }
                item {
                    label = getString(RBase.string.edit_add_notifier)
                    callback = {
                        addNotifierStep(position + 1)
                    }
                }
                item {
                    label = getString(RBase.string.edit_add_group)
                    callback = { addGroup(position + 1) }
                }
            }
            section {
                item {
                    label = getString(RBase.string.duplicate)
                    callback = {
                        val adapter = fastAdapter
                        var groupItem: EditableGroup? = null
                        val groupSteps = mutableListOf<EditableStep>()
                        for (index in position downTo 0) {
                            when (val item = adapter.getItem(index)) {
                                is EditableGroup -> {
                                    groupItem = item
                                    break
                                }
                                is EditableStep -> {
                                    groupSteps.add(0, item)
                                }
                            }
                        }
                        if (groupItem != null) {
                            val groupItems = mutableListOf<IItem<*>>().apply {
                                add(
                                    EditableGroup(
                                        name = groupItem.name,
                                        loop = groupItem.loop,
                                        totalTime = groupItem.totalTime,
                                        handler = this@EditActivity,
                                        showTotalTime = shouldUpdateTotalTime
                                    )
                                )
                                addAll(
                                    groupSteps.map { editableStep ->
                                        val currentStep = editableStep.toStep()
                                        EditableStep(
                                            label = currentStep.label,
                                            length = currentStep.length,
                                            behaviour = currentStep.behaviour,
                                            stepType = currentStep.type,
                                            handler = this@EditActivity,
                                            isInAGroup = editableStep.isInAGroup
                                        )
                                    }
                                )
                                add(EditableGroupEnd(this@EditActivity))
                            }
                            stepAdapter.add(position + 1, groupItems)
                            postListScrollAction(position + 1)
                            postUpdateTotalTime()
                        }
                    }
                }
            }
        }.show(view.context, view)
    }

    private fun setUpOtherViews() {
        binding.btnEditMore.setOnClickListener {
            showBottomMoreDialog(viewModel, appNavigator)
        }
        if (showTimerTotalTime) {
            binding.viewEditStepInfo.show()
            binding.viewEditNameLoop.loopView.doAfterTextChanged {
                postUpdateTotalTime()
            }
        } else {
            binding.viewEditStepInfo.gone()
            binding.listEditSteps.updatePadding(top = 0)
        }
    }

    private fun setUpSnackbar() {
        viewModel.message.observeEvent(this) {
            binding.layoutEditRoot.snackbar(it)
        }
    }

    private fun subscribeToChanges() {
        viewModel.updatedEvent.observeEvent(this) {
            setResult(RESULT_OK)
            finishEditTimerActivity()
        }
        viewModel.stepsEvent.observeEvent(this) {
            it.moveIntoStepsAdapter()
            binding.listEditSteps.post {
                binding.listEditSteps.scrollToPosition(0)
            }
        }
        viewModel.startEndEvent.observeEvent(this) {
            it.first?.moveIntoSingleAdapter(startAdapter)
            it.second?.moveIntoSingleAdapter(endAdapter)
        }

        viewModel.shareStringEvent.observeEvent(this) { fruit ->
            when (fruit) {
                is Fruit.Ripe -> {
                    startActivityOrNothing(IntentHelper.share(this, fruit.data))
                }
                is Fruit.Rotten -> {
                    binding.root.snackbar(fruit.exception.message.toString())
                }
            }
        }
    }

    private fun loadData() {
        viewModel.loadTimerData()
        viewModel.loadStoredNotifierStep()
        if (viewModel.isNewTimer) {
            binding.listEditSteps.post { addNormalStep() }
        }
    }

    override fun showBehaviourSettingsView(
        view: View,
        layout: EditableBehaviourLayout,
        current: BehaviourEntity,
        position: Int
    ) {
        val type = current.type
        popupMenu {
            when (current.type) {
                BehaviourType.MUSIC -> {
                    val action = current.toMusicAction()
                    addMusicItems(
                        this@EditActivity, action,
                        onPickMusicClick = {
                            ringtonePickerLauncher.launch(
                                RingtonePickerActivity.getIntent(
                                    context = this@EditActivity,
                                    settings = generateRingtonePickerSettings(
                                        select = action.uri.toUri().takeIf { it != Uri.EMPTY }
                                    ),
                                    windowTitle = getString(RBase.string.music_pick_ringtone),
                                    reference = position
                                )
                            )
                        },
                        onLoopChanged = { isChecked ->
                            changeBehaviour(BehaviourType.MUSIC, position) {
                                it.toMusicAction().copy(loop = isChecked).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.VIBRATION -> {
                    addVibrationItems(
                        this@EditActivity, current.toVibrationAction(),
                        onNewCount = { newCount ->
                            changeBehaviour(BehaviourType.VIBRATION, position) {
                                it.toVibrationAction().copy(count = newCount)
                                    .toBehaviourEntity()
                            }
                        },
                        onNewPattern = { newPattern ->
                            changeBehaviour(BehaviourType.VIBRATION, position) {
                                it.toVibrationAction().copy(vibrationPattern = newPattern)
                                    .toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.SCREEN -> {
                    addScreenItems(
                        this@EditActivity, current.toScreenAction(),
                        onFullscreenChanged = { isChecked ->
                            changeBehaviour(BehaviourType.SCREEN, position) {
                                it.toScreenAction().copy(fullScreen = isChecked).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.VOICE -> {
                    addVoiceItems(
                        this@EditActivity, current.toVoiceAction(),
                        onVoiceContent = { newContent ->
                            changeBehaviour(BehaviourType.VOICE, position) {
                                it.toVoiceAction().copy(content = newContent).toBehaviourEntity()
                            }
                        },
                        onVoice2Content = { newContent ->
                            changeBehaviour(BehaviourType.VOICE, position) {
                                it.toVoiceAction().copy(content2 = newContent).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.BEEP -> {
                    addBeepItems(
                        this@EditActivity, current.toBeepAction(),
                        onBeepCount = { newCount ->
                            changeBehaviour(BehaviourType.BEEP, position) {
                                it.toBeepAction().copy(count = newCount).toBehaviourEntity()
                            }
                        },
                        onBeepSound = { toneIndex ->
                            changeBehaviour(BehaviourType.BEEP, position) {
                                it.toBeepAction().copy(soundIndex = toneIndex)
                                    .toBehaviourEntity()
                            }
                        },
                        onRespect = { isChecked ->
                            changeBehaviour(BehaviourType.BEEP, position) {
                                it.toBeepAction().copy(respectOtherSound = isChecked)
                                    .toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.HALF -> {
                    addHalfItems(
                        this@EditActivity, current.toHalfAction(),
                        onHalfOption = { newOption ->
                            changeBehaviour(BehaviourType.HALF, position) {
                                it.toHalfAction().copy(option = newOption).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.COUNT -> {
                    addCountItems(
                        this@EditActivity, current.toCountAction(),
                        onCountTimes = { newTimes ->
                            changeBehaviour(BehaviourType.COUNT, position) {
                                it.toCountAction().copy(times = newTimes).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.NOTIFICATION -> {
                    addNotificationItems(
                        this@EditActivity, current.toNotificationAction(),
                        onNotificationDuring = { newDuration ->
                            changeBehaviour(BehaviourType.NOTIFICATION, position) {
                                it.toNotificationAction().copy(duration = newDuration)
                                    .toBehaviourEntity()
                            }
                        }
                    )
                }
                else -> Unit
            }
            section {
                item {
                    label = getString(RBase.string.delete)
                    icon = RBase.drawable.ic_delete
                    callback = { layout.removeBehaviour(type) }
                }
            }
        }.show(this, view)
    }

    private fun getRingtonePickerResultCallback(): ActivityResultCallback<ActivityResult> {
        return ActivityResultCallback { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_CANCELED || data == null) {
                return@ActivityResultCallback
            }
            val position = RingtonePickerActivity.getPickerReference(data)
            val entry = RingtonePickerActivity.getPickerResult(data).firstOrNull()
            val uri = entry?.uri
            val title = entry?.name
            // requestCode is also the position
            if (uri != null &&
                uri != Uri.EMPTY &&
                title != null &&
                position in 0 until (fastAdapter.itemCount - 1)
            ) {
                changeBehaviour(BehaviourType.MUSIC, position) {
                    it.toMusicAction().copy(title = title, uri = uri.toString())
                        .toBehaviourEntity()
                }
            }
        }
    }

    private fun changeBehaviour(
        type: BehaviourType,
        position: Int,
        transform: (BehaviourEntity) -> BehaviourEntity
    ) {
        val step = getStepFromFastAdapter(position)
        val newBehaviours = mutableListOf<BehaviourEntity>()
        step.behaviour.forEach {
            newBehaviours.add(if (it.type == type) transform(it) else it)
        }
        // The code after this causes setBehaviors which doesn't call onBehaviourAddedOrRemoved
        // so we check here.
        if (step.stepType == StepType.NOTIFIER) {
            viewModel.notifier = viewModel.notifier.copy(behaviour = newBehaviours)
        }
        step.behaviour = newBehaviours
        fastAdapter.notifyAdapterItemChanged(position, EditableStep.Event.Behaviour)
    }

    override fun onBehaviourAddedOrRemoved(position: Int, newBehaviours: List<BehaviourEntity>) {
        if (getStepFromFastAdapter(position).stepType == StepType.NOTIFIER) {
            viewModel.notifier = viewModel.notifier.copy(behaviour = newBehaviours)
        }
    }

    private fun postUpdateTotalTime() {
        if (shouldUpdateTotalTime) {
            binding.viewEditStepInfo.run {
                removeCallbacks(updateTotalTime)
                post(updateTotalTime)
            }
        }
    }

    private val updateTotalTime by lazy {
        Runnable {
            val internalSteps = getStepEntityFromFastAdapter(
                doOnGroup = { group, steps ->
                    group.totalTime = steps.accumulateTime() * group.loop
                    fastAdapter.notifyItemChanged(
                        fastAdapter.getPosition(group),
                        EditableGroup.TotalTimeChanged
                    )
                }
            )
            val startStep = startAdapter.getSingleStepFromAdapter()
            val endStep = endAdapter.getSingleStepFromAdapter()
            binding.viewEditStepInfo.setDuration(
                (startStep?.length ?: 0) +
                    internalSteps.accumulateTime() * (viewModel.loop.value ?: 1) +
                    (endStep?.length ?: 0)
            )
        }
    }

    private val shouldUpdateTotalTime: Boolean
        get() = binding.viewEditStepInfo.isVisible

    // endregion SetUps and main actions

    // region Exit entries

    private fun finishEditTimerActivity() {
        viewModel.saveNotifierStep()
        finish()
    }

    // endregion Exit entries

    // region Footer steps actions

    private fun buildFooterClickListeners(): List<View.OnClickListener> = listOf(
        View.OnClickListener { addNormalStep() },
        View.OnClickListener { addNotifierStep() },
        View.OnClickListener { addStartStep() },
        View.OnClickListener { addEndStep() },
        View.OnClickListener { addGroup() }
    )

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addNormalStep(position: Int = -1) {
        addStep(position, getDefaultNaiveStep(isInAGroup = false))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addNotifierStep(position: Int = -1) {
        addStep(position, getNotifierCopy(false))
    }

    private fun addStep(position: Int = -1, step: EditableStep) {
        if (position == -1) {
            stepAdapter.add(step)
            postListScrollAction(fastAdapter.itemCount - 2)
        } else {
            stepAdapter.add(position, step)
            postListScrollAction(position)
        }
        postUpdateTotalTime()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addGroup(position: Int = -1) {
        val groupItems = mutableListOf<IItem<*>>().apply {
            add(
                EditableGroup(
                    name = getString(RBase.string.edit_group),
                    loop = 1,
                    handler = this@EditActivity,
                    showTotalTime = shouldUpdateTotalTime
                )
            )
            add(getDefaultNaiveStep(isInAGroup = true))
            add(EditableGroupEnd(this@EditActivity))
        }
        if (position == -1) {
            stepAdapter.add(groupItems)
            postListScrollAction(fastAdapter.itemCount - 2)
        } else {
            stepAdapter.add(position, groupItems)
            postListScrollAction(position)
        }
        postUpdateTotalTime()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addNormalStepToGroup(position: Int = -1) {
        addStep(position, getDefaultNaiveStep(isInAGroup = true))
        postUpdateTotalTime()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addNotifierStepToGroup(position: Int = -1) {
        addStep(position, getNotifierCopy(true))
        postUpdateTotalTime()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addStartStep() {
        if (startAdapter.adapterItemCount == 0) {
            startAdapter.add(
                EditableStep(
                    getString(RBase.string.edit_start_step), 10_000, listOf(), StepType.START, this
                )
            )
        } else {
            binding.root.snackbar(RBase.string.edit_add_start_too_many)
        }
        postListScrollAction(0)
        postUpdateTotalTime()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun addEndStep() {
        if (endAdapter.adapterItemCount == 0) {
            endAdapter.add(
                EditableStep(
                    getString(RBase.string.edit_end_step), 10_000, listOf(), StepType.END, this
                )
            )
        } else {
            binding.root.snackbar(RBase.string.edit_add_end_too_many)
        }
        postListScrollAction(fastAdapter.itemCount - 2)
        postUpdateTotalTime()
    }

    private fun getDefaultNaiveStep(isInAGroup: Boolean): EditableStep {
        return EditableStep(
            getString(RBase.string.edit_default_step_name), 60000, mutableListOf(), StepType.NORMAL,
            this, isInAGroup = isInAGroup
        )
    }

    private fun postListScrollAction(position: Int) {
        binding.listEditSteps.post {
            binding.listEditSteps.smoothScrollToPosition(position)
        }
    }

    // endregion Footer steps actions

    // region adapter data

    private fun getStepFromFastAdapter(position: Int): EditableStep {
        return fastAdapter.getItem(position) as EditableStep
    }

    private fun StepEntity.Step.toEditable(isInAGroup: Boolean = false) = EditableStep(
        label, length, behaviour, type, this@EditActivity, isInAGroup
    )

    private fun List<StepEntity>.moveIntoStepsAdapter() {
        stepAdapter.clear()
        this.forEach {
            when (it) {
                is StepEntity.Step -> {
                    stepAdapter.add(it.toEditable())
                }
                is StepEntity.Group -> {
                    val items = mutableListOf<IItem<*>>()
                    items.add(
                        EditableGroup(
                            name = it.name,
                            loop = it.loop,
                            handler = this@EditActivity,
                            showTotalTime = shouldUpdateTotalTime
                        )
                    )
                    it.steps.forEach { subItem ->
                        items.add((subItem as StepEntity.Step).toEditable(isInAGroup = true))
                    }
                    items.add(EditableGroupEnd(this@EditActivity))

                    stepAdapter.add(items)
                }
            }
        }
        postUpdateTotalTime()
    }

    private fun StepEntity.Step.moveIntoSingleAdapter(itemAdapter: ItemAdapter<EditableStep>) {
        itemAdapter.clear()
        itemAdapter.add(this.toEditable())
        postUpdateTotalTime()
    }

    private fun EditableStep.toStep(): StepEntity.Step = StepEntity.Step(
        label, length, behaviour, stepType
    )

    private fun getStepEntityFromFastAdapter(
        doOnGroup: ((group: EditableGroup, steps: List<StepEntity.Step>) -> Unit)? = null
    ): List<StepEntity> {
        val result = mutableListOf<StepEntity>()

        var isCollectingGroup = false
        var group: EditableGroup? = null
        val groupSteps = mutableListOf<StepEntity.Step>()

        stepAdapter.adapterItems.forEach { item ->
            when (item) {
                is EditableStep -> {
                    if (isCollectingGroup) {
                        if (item.isInAGroup) {
                            groupSteps.add(item.toStep())
                        } else {
                            throw IllegalStateException(
                                buildString {
                                    append("isCollectingGroup but a step is not in a group: ")
                                    append(
                                        stepAdapter.adapterItems.joinToString {
                                            "$it(${(it as? EditableStep)?.isInAGroup})"
                                        }
                                    )
                                }
                            )
                        }
                    } else {
                        result.add(item.toStep())
                    }
                }
                is EditableGroup -> {
                    isCollectingGroup = true
                    group = item
                }
                is EditableGroupEnd -> {
                    require(isCollectingGroup)
                    val currentGroup = group
                    if (currentGroup != null && groupSteps.isNotEmpty()) {
                        val newGroupSteps = groupSteps.map { it.copy() }
                        result.add(
                            StepEntity.Group(
                                name = currentGroup.name,
                                loop = currentGroup.loop.coerceAtLeast(1),
                                steps = newGroupSteps
                            )
                        )
                        doOnGroup?.invoke(currentGroup, newGroupSteps)
                    }
                    isCollectingGroup = false
                    group = null
                    groupSteps.clear()
                }
            }
        }
        return result
    }

    private fun ItemAdapter<EditableStep>.getSingleStepFromAdapter(): StepEntity.Step? {
        return if (adapterItemCount == 1) getAdapterItem(0).toStep() else null
    }

    // endregion adapter data

    // Every notifier should be independent.
    private fun getNotifierCopy(isInAGroup: Boolean): EditableStep = with(viewModel.notifier) {
        EditableStep(label, length, behaviour, type, this@EditActivity, isInAGroup)
    }

    private fun saveTimer() {
        val internalSteps = getStepEntityFromFastAdapter()
        val startStep = startAdapter.getSingleStepFromAdapter()
        val endStep = endAdapter.getSingleStepFromAdapter()
        viewModel.saveTimer(internalSteps, startStep, endStep)
    }

    private fun userLeave(onLeave: () -> Unit) {
        if (viewModel.isTimerRemainingSame(
                getStepEntityFromFastAdapter(),
                startAdapter.getSingleStepFromAdapter(),
                endAdapter.getSingleStepFromAdapter()
            )
        ) {
            onLeave.invoke()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(RBase.string.save_changes)
                .setPositiveButton(RBase.string.save) { _, _ ->
                    saveTimer()
                }
                .setNegativeButton(RBase.string.discard) { _, _ ->
                    onLeave.invoke()
                }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        }
    }

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id"
    }
}

private const val EXTRA_DATA_STEPS = "data_steps"
private const val EXTRA_DATA_START = "data_start"
private const val EXTRA_DATA_END = "data_end"
