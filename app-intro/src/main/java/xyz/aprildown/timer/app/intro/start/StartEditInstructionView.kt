package xyz.aprildown.timer.app.intro.start

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ImageViewCompat
import com.google.android.material.card.MaterialCardView
import xyz.aprildown.timer.app.base.data.PreferenceData.getTypeColor
import xyz.aprildown.timer.app.base.utils.setTime
import xyz.aprildown.timer.app.intro.Instruction
import xyz.aprildown.timer.app.intro.InstructionView
import xyz.aprildown.timer.app.intro.R
import xyz.aprildown.timer.app.intro.clearInteractionIndicator
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartEditBinding
import xyz.aprildown.timer.app.intro.showTooltip
import xyz.aprildown.timer.component.key.RoundTextView
import xyz.aprildown.timer.component.key.behaviour.EditableBehaviourLayout
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.tools.anko.longToast
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.show
import xyz.aprildown.tools.helper.toColorStateList

internal class StartEditInstructionView : InstructionView<LayoutIntroStartEditBinding> {
    override val layoutRes: Int = R.layout.layout_intro_start_edit
    override lateinit var binding: LayoutIntroStartEditBinding

    override fun initBinding(context: Context, parent: ViewGroup) {
        binding = LayoutIntroStartEditBinding.inflate(LayoutInflater.from(context), parent, false)
    }

    override fun reset() {
        val context = binding.root.context
        val timer = Instruction.getInitialSampleTimer(context)
        binding.run {
            binding.viewIntroStartEditSaveIndicator.clearInteractionIndicator()
            toolbar.menu.findItem(R.id.action_save_timer)?.setOnMenuItemClickListener {
                true
            }

            viewEditNameLoop.nameView.run {
                isEnabled = false
                setText(timer.name)
            }
            viewEditNameLoop.loopView.run {
                clearInteractionIndicator()
                isEnabled = false
                setText(timer.loop.toString())
                (getTag(R.id.tag_loop_change_listener) as? TextWatcher)?.let {
                    removeTextChangedListener(it)
                }
            }

            val steps = timer.steps.map { it as StepEntity.Step }

            stepIntroStartEdit1.show()
            stepIntroStartEdit1.withStepEntity(steps[0])
            stepIntroStartEdit1.run {
                cardView.clearInteractionIndicator()
                lengthTextView.clearInteractionIndicator()
                lengthTextView.setOnClickListener(null)
            }

            stepIntroStartEdit2.gone()
            stepIntroStartEdit2.withStepEntity(steps[1])
            stepIntroStartEdit2.behaviourLayout.run {
                addButton.clearInteractionIndicator()
                setBehaviourAddedOrRemovedCallback(null)
                setListener(
                    object : EditableBehaviourLayout.Listener {
                        override fun showBehaviourSettingsView(
                            view: View,
                            layout: EditableBehaviourLayout,
                            current: BehaviourEntity
                        ) {
                            showTooltip(context.getText(R.string.intro_start_edit_behaviour))
                        }
                    }
                )
            }

            stepIntroStartEdit3.gone()
            stepIntroStartEdit3.withStepEntity(steps[2])
            stepIntroStartEdit3.run {
                lengthTextView.clearInteractionIndicator()
                lengthTextView.setOnClickListener(null)
                behaviourLayout.setListener(
                    object : EditableBehaviourLayout.Listener {
                        override fun showBehaviourSettingsView(
                            view: View,
                            layout: EditableBehaviourLayout,
                            current: BehaviourEntity
                        ) {
                            showTooltip(context.getText(R.string.intro_start_edit_behaviour))
                        }
                    }
                )
            }

            stepIntroStartEdit4.gone()
            stepIntroStartEdit4.withStepEntity(steps[3])

            root.findViewById<View>(R.id.btnAddStep).run {
                clearInteractionIndicator()
                setOnClickListener(null)
            }
            root.findViewById<View>(R.id.btnAddNotifier).run {
                clearInteractionIndicator()
                setOnClickListener(null)
            }
            root.findViewById<View>(R.id.btnAddGroup).setOnClickListener {
                context.longToast(
                    buildString {
                        append(context.getString(R.string.edit_add_group_desp))
                        append("\n")
                        append(context.getString(R.string.intro_save_for_later))
                    }
                )
            }
            root.findViewById<View>(R.id.btnAddStart).setOnClickListener {
                context.longToast(
                    buildString {
                        append(context.getString(R.string.edit_add_start_desp))
                        append("\n")
                        append(context.getString(R.string.intro_save_for_later))
                    }
                )
            }
            root.findViewById<View>(R.id.btnAddEnd).setOnClickListener {
                context.longToast(
                    buildString {
                        append(context.getString(R.string.edit_add_end_desp))
                        append("\n")
                        append(context.getString(R.string.intro_save_for_later))
                    }
                )
            }
        }
    }
}

internal class IntroEditableStep(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val cardView: MaterialCardView
    private val stepColorView: ImageView
    private val nameEditText: EditText
    val lengthTextView: RoundTextView
    val behaviourLayout: EditableBehaviourLayout
    private val addStepButton: ImageView

    init {
        View.inflate(context, R.layout.item_edit_step, this)

        findViewById<View>(R.id.viewStepGroupIndicatorStart).gone()
        findViewById<View>(R.id.viewStepGroupIndicatorEnd).gone()

        cardView = findViewById(R.id.cardEditStep)
        stepColorView = findViewById(R.id.colorStep)
        nameEditText = findViewById(R.id.editStepName)
        lengthTextView = findViewById(R.id.textStepLength)
        behaviourLayout = findViewById(R.id.layoutBehaviour)
        addStepButton = findViewById(R.id.btnStepAdd)

        nameEditText.run {
            isEnabled = false
            setTextColor(textColors.defaultColor)
        }
    }

    fun withStepEntity(entity: StepEntity.Step) {
        val color = entity.type.getTypeColor(context)

        ImageViewCompat.setImageTintList(stepColorView, color.toColorStateList())
        nameEditText.setText(entity.label)
        lengthTextView.setBgColor(color)
        lengthTextView.setTime(entity.length)
        ImageViewCompat.setImageTintList(addStepButton, color.toColorStateList())
        behaviourLayout.run {
            setEnabledColor(color)
            setBehaviours(entity.behaviour)
        }
    }
}
