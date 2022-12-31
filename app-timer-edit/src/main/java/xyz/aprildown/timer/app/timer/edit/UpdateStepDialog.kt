package xyz.aprildown.timer.app.timer.edit

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.DialogFragment
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.toColorStateList
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.data.PreferenceData.getTypeColor
import xyz.aprildown.timer.app.base.ui.StepUpdater
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.component.key.DurationPicker
import xyz.aprildown.timer.component.key.RoundTextView
import xyz.aprildown.timer.component.key.behaviour.EditableBehaviourLayout
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.toBeepAction
import xyz.aprildown.timer.domain.entities.toCountAction
import xyz.aprildown.timer.domain.entities.toHalfAction
import xyz.aprildown.timer.domain.entities.toMusicAction
import xyz.aprildown.timer.domain.entities.toNotificationAction
import xyz.aprildown.timer.domain.entities.toScreenAction
import xyz.aprildown.timer.domain.entities.toVibrationAction
import xyz.aprildown.timer.domain.entities.toVoiceAction
import xyz.aprildown.ultimateringtonepicker.RingtonePickerDialog
import xyz.aprildown.ultimateringtonepicker.UltimateRingtonePicker
import xyz.aprildown.timer.app.base.R as RBase

class UpdateStepDialog :
    DialogFragment(),
    EditableBehaviourLayout.Listener {

    private lateinit var editName: EditText
    private lateinit var behaviourLayout: EditableBehaviourLayout

    private var length = 0L

    var step: StepEntity.Step? = null
    var onUpdate: ((StepEntity.Step) -> Unit)? = null

    init {
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = View.inflate(context, R.layout.dialog_update_step, null)
        val step = step
        val onUpdate = onUpdate
        if (step == null || onUpdate == null) {
            dismiss()
            return super.onCreateDialog(savedInstanceState)
        }
        view.setUpUpdateStepView(step)
        return MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setTitle(RBase.string.edit_step)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onUpdate.invoke(
                    StepEntity.Step(
                        editName.text.toString(),
                        length,
                        behaviourLayout.getBehaviours(),
                        step.type
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun View.setUpUpdateStepView(step: StepEntity.Step) {
        val context = context

        findViewById<CardView>(R.id.cardEditStep).cardElevation = 0f

        val color = step.type.getTypeColor(context)
        ImageViewCompat.setImageTintList(findViewById(R.id.colorStep), color.toColorStateList())
        findViewById<View>(R.id.viewStepGroupIndicatorStart).gone()
        findViewById<View>(R.id.viewStepGroupIndicatorEnd).gone()

        editName = findViewById(R.id.editStepName)
        editName.setText(step.label)

        val lengthLabel = findViewById<RoundTextView>(R.id.textStepLength)
        lengthLabel.setBgColor(color)
        fun updateLength(length: Long) {
            lengthLabel.text = length.produceTime()
            this@UpdateStepDialog.length = length
        }
        updateLength(step.length)
        lengthLabel.setOnClickListener {
            DurationPicker(context) { hours, minutes, seconds ->
                updateLength((hours * 3600L + minutes * 60L + seconds) * 1000L)
            }.show()
        }

        findViewById<View>(R.id.btnStepAdd).gone()

        behaviourLayout = findViewById(R.id.layoutBehaviour)
        behaviourLayout.setEnabledColor(color)
        behaviourLayout.setBehaviours(step.behaviour)
        behaviourLayout.setListener(this@UpdateStepDialog)
    }

    override fun showBehaviourSettingsView(
        view: View,
        layout: EditableBehaviourLayout,
        current: BehaviourEntity
    ) {
        val context = view.context
        val type = current.type
        popupMenu {
            when (type) {
                BehaviourType.MUSIC -> {
                    val action = current.toMusicAction()
                    addMusicItems(
                        context = context,
                        action = action,
                        onPickMusicClick = {
                            RingtonePickerDialog.createEphemeralInstance(
                                settings = context.generateRingtonePickerSettings(
                                    select = action.uri.toUri().takeIf { it != Uri.EMPTY }
                                ),
                                dialogTitle = getString(RBase.string.music_pick_ringtone),
                                listener = object : UltimateRingtonePicker.RingtonePickerListener {
                                    override fun onRingtonePicked(
                                        ringtones: List<UltimateRingtonePicker.RingtoneEntry>
                                    ) {
                                        val ringtone =
                                            ringtones.firstOrNull() ?: return
                                        changeBehaviour(BehaviourType.MUSIC) {
                                            it.toMusicAction()
                                                .copy(
                                                    title = ringtone.name,
                                                    uri = ringtone.uri.toString()
                                                )
                                                .toBehaviourEntity()
                                        }
                                    }
                                }
                            ).show(childFragmentManager, null)
                        },
                        onLoopChanged = { isChecked ->
                            changeBehaviour(BehaviourType.MUSIC) {
                                it.toMusicAction().copy(loop = isChecked).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.VIBRATION -> {
                    addVibrationItems(
                        context = context,
                        action = current.toVibrationAction(),
                        onNewCount = { newCount ->
                            changeBehaviour(BehaviourType.VIBRATION) {
                                it.toVibrationAction().copy(count = newCount)
                                    .toBehaviourEntity()
                            }
                        },
                        onNewPattern = { newPattern ->
                            changeBehaviour(BehaviourType.VIBRATION) {
                                it.toVibrationAction().copy(vibrationPattern = newPattern)
                                    .toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.SCREEN -> {
                    addScreenItems(
                        context = context,
                        action = current.toScreenAction(),
                        onFullscreenChanged = { isChecked ->
                            changeBehaviour(BehaviourType.SCREEN) {
                                it.toScreenAction().copy(fullScreen = isChecked).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.VOICE -> {
                    addVoiceItems(
                        context = context,
                        action = current.toVoiceAction(),
                        onVoiceContent = { newContent ->
                            changeBehaviour(BehaviourType.VOICE) {
                                it.toVoiceAction().copy(content = newContent).toBehaviourEntity()
                            }
                        },
                        onVoice2Content = { newContent ->
                            changeBehaviour(BehaviourType.VOICE) {
                                it.toVoiceAction().copy(content2 = newContent).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.BEEP -> {
                    addBeepItems(
                        context = context,
                        action = current.toBeepAction(),
                        onBeepCount = { newCount ->
                            changeBehaviour(BehaviourType.BEEP) {
                                it.toBeepAction().copy(count = newCount).toBehaviourEntity()
                            }
                        },
                        onBeepSound = { toneIndex ->
                            changeBehaviour(BehaviourType.BEEP) {
                                it.toBeepAction().copy(soundIndex = toneIndex)
                                    .toBehaviourEntity()
                            }
                        },
                        onRespect = { newPause ->
                            changeBehaviour(BehaviourType.BEEP) {
                                it.toBeepAction().copy(respectOtherSound = newPause)
                                    .toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.HALF -> {
                    addHalfItems(
                        context = context,
                        action = current.toHalfAction(),
                        onHalfOption = { newOption ->
                            changeBehaviour(BehaviourType.HALF) {
                                it.toHalfAction().copy(option = newOption).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.COUNT -> {
                    addCountItems(
                        context = context,
                        action = current.toCountAction(),
                        onCountTimes = { newTimes ->
                            changeBehaviour(BehaviourType.COUNT) {
                                it.toCountAction().copy(newTimes).toBehaviourEntity()
                            }
                        }
                    )
                }
                BehaviourType.NOTIFICATION -> {
                    addNotificationItems(
                        context = context,
                        action = current.toNotificationAction(),
                        onNotificationDuring = { newDuration ->
                            changeBehaviour(BehaviourType.NOTIFICATION) {
                                it.toNotificationAction().copy(duration = newDuration)
                                    .toBehaviourEntity()
                            }
                        }
                    )
                }
                else -> {
                }
            }
            section {
                item {
                    label = getString(RBase.string.delete)
                    icon = RBase.drawable.ic_delete
                    callback = { layout.removeBehaviour(type) }
                }
            }
        }.show(context, view)
    }

    private fun changeBehaviour(
        type: BehaviourType,
        transform: (BehaviourEntity) -> BehaviourEntity
    ) {
        behaviourLayout.setBehaviours(
            behaviourLayout.getBehaviours().map {
                if (it.type == type) transform.invoke(it) else it
            }
        )
    }

    companion object : StepUpdater {
        override fun updateStep(
            step: StepEntity.Step,
            onUpdate: (StepEntity.Step) -> Unit
        ): DialogFragment = UpdateStepDialog().apply {
            this.step = step
            this.onUpdate = onUpdate
        }
    }
}
