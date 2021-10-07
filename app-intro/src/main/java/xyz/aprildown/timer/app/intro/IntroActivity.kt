package xyz.aprildown.timer.app.intro

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.text.buildSpannedString
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.intro.databinding.ActivityIntroBinding
import xyz.aprildown.timer.app.intro.start.AddNotifier
import xyz.aprildown.timer.app.intro.start.AddReminder
import xyz.aprildown.timer.app.intro.start.AddWalkStep
import xyz.aprildown.timer.app.intro.start.CreateTimer
import xyz.aprildown.timer.app.intro.start.EnterLoop
import xyz.aprildown.timer.app.intro.start.Finish
import xyz.aprildown.timer.app.intro.start.Notifier
import xyz.aprildown.timer.app.intro.start.OurPlan
import xyz.aprildown.timer.app.intro.start.ReminderUsage
import xyz.aprildown.timer.app.intro.start.RunTimer
import xyz.aprildown.timer.app.intro.start.StepCard
import xyz.aprildown.timer.app.intro.start.StepTime
import xyz.aprildown.timer.app.intro.start.TimerButtons
import xyz.aprildown.timer.app.intro.start.TimerDone
import xyz.aprildown.timer.app.intro.start.TimerSteps
import xyz.aprildown.timer.app.intro.start.TimerTime
import xyz.aprildown.timer.app.intro.start.WalkNotifier
import xyz.aprildown.timer.app.intro.start.WalkStepTime
import xyz.aprildown.timer.app.intro.start.Welcome
import xyz.aprildown.timer.component.key.behaviour.EditableBehaviourLayout
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.presentation.intro.IntroViewModel
import xyz.aprildown.tools.helper.color
import javax.inject.Inject

@AndroidEntryPoint
class IntroActivity : BaseActivity() {

    private val viewModel: IntroViewModel by viewModels()

    @Inject
    lateinit var appTracker: AppTracker

    private lateinit var binding: ActivityIntroBinding

    private val instructionManager = InstructionManager()
    private var instructionView: InstructionView<ViewBinding>? = null

    private val isOnBoarding: Boolean by lazy {
        intent?.getBooleanExtra(EXTRA_IS_ON_BOARDING, false) == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpInstructions(savedInstanceState)
        setUpViews()
        setUpNavigation()

        if (isOnBoarding && savedInstanceState == null) {
            appTracker.trackEvent(event = "OnBoardingShow")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_INSTRUCTION_INDEX, instructionManager.currentIndex)
    }

    private fun setUpInstructions(savedInstanceState: Bundle?) {
        instructionManager.callback = object : InstructionManager.Callback {
            override fun onNewInstruction(instruction: Instruction<out ViewBinding>) {
                if (instructionView == null || instruction.layoutRes != instructionView?.layoutRes) {
                    TransitionManager.beginDelayedTransition(
                        binding.root.findViewById(R.id.container),
                        MaterialSharedAxis(MaterialSharedAxis.Y, true)
                    )

                    val createdView =
                        instruction.createInstructionView(this@IntroActivity, binding.root)
                    instructionView = createdView
                    binding.container.removeAllViews()
                    binding.container.addView(createdView.binding.root)
                } else {
                    TransitionManager.beginDelayedTransition(
                        binding.root.findViewById(R.id.container),
                        AutoTransition().apply {
                            // AutoTransition makes EditableBehaviourLayout blink.
                            excludeChildren(EditableBehaviourLayout::class.java, true)
                        }
                    )
                }

                val currentInstructionView = instructionView
                requireNotNull(currentInstructionView)
                currentInstructionView.reset()

                // We create the binding from instruction if types don't match.
                @Suppress("UNCHECKED_CAST")
                (instruction as? Instruction<ViewBinding>)?.setUpViews(currentInstructionView.binding)

                binding.viewIntroPanel.withInstruction(
                    instruction = instruction,
                    progressText = instructionManager.currentProgressText
                )
                binding.viewIntroPanel.withPreviousOrExit(
                    hasPrevious = !instructionManager.isTheFirst,
                    hasNext = !instructionManager.isTheLast
                )
            }

            override fun onFinalInstruction() {
                if (isOnBoarding) {
                    viewModel.addSampleTimer(Instruction.getInitialSampleTimer(this@IntroActivity))
                }

                binding.confetti.build()
                    .addColors(
                        color(R.color.md_red_500),
                        color(R.color.md_amber_700),
                        color(R.color.md_light_green_700),
                        color(R.color.md_blue_500),
                        color(R.color.md_green_500),
                        color(R.color.md_purple_500),
                    )
                    .setDirection(0.0, 180.0)
                    .setSpeed(5f, 10f)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(1000L)
                    .addShapes(Shape.Square, Shape.Circle)
                    .addSizes(Size(12, 5f), Size(16, 6f))
                    .setPosition(-50f, binding.confetti.width + 50f, -50f, 50f)
                    .setDelay(500)
                    .streamFor(300, 1000L)
            }

            override fun onNoMoreInstruction() {
                if (instructionManager.isTheLast) {
                    finish()
                } else {
                    confirmToExit()
                }
            }
        }
        instructionManager.withInstructions(
            listOf(
                Welcome(),
                OurPlan(),
                CreateTimer(),

                EnterLoop(),
                StepCard(),
                StepTime(),
                Notifier(),
                AddNotifier(),
                AddReminder(),
                ReminderUsage(),
                AddWalkStep(),
                WalkStepTime(),
                WalkNotifier(),
                TimerDone(),

                RunTimer(),

                TimerTime(),
                TimerSteps(),
                TimerButtons(),
                Finish(),
            )
        )

        val toIndex = savedInstanceState?.getInt(EXTRA_INSTRUCTION_INDEX, 0) ?: 0
        if (toIndex != instructionManager.currentIndex) {
            instructionManager.to(toIndex)
            if (binding.motionIntro.currentState == binding.motionIntro.startState) {
                binding.motionIntro.transitionToEnd()
            }
        }
    }

    private fun setUpViews() {
        binding.viewIntroPanel.callback = object : IntroPanelView.Callback {
            override fun onNextInstruction() {
                if (binding.motionIntro.currentState == binding.motionIntro.startState) {
                    binding.motionIntro.transitionToEnd()
                    binding.motionIntro.setTransitionListener(
                        object : MotionLayout.TransitionListener {
                            override fun onTransitionStarted(
                                motionLayout: MotionLayout,
                                startId: Int,
                                endId: Int
                            ) = Unit

                            override fun onTransitionChange(
                                motionLayout: MotionLayout,
                                startId: Int,
                                endId: Int,
                                progress: Float
                            ) = Unit

                            override fun onTransitionTrigger(
                                motionLayout: MotionLayout,
                                triggerId: Int,
                                positive: Boolean,
                                progress: Float
                            ) = Unit

                            override fun onTransitionCompleted(
                                motionLayout: MotionLayout,
                                currentId: Int
                            ) {
                                if (currentId == motionLayout.endState) {
                                    binding.imageIntroAppLogo.setImageDrawable(null)
                                    instructionManager.next()
                                }
                            }
                        }
                    )
                } else {
                    instructionManager.next()
                }
            }

            override fun onPreviousInstruction() {
                instructionManager.previous()
            }
        }
    }

    private fun setUpNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            confirmToExit()
        }
    }

    private fun confirmToExit() {
        MaterialAlertDialogBuilder(this@IntroActivity)
            .setTitle(R.string.intro_exit_confirmation)
            .setMessage(
                buildSpannedString {
                    if (isOnBoarding) {
                        append(
                            getText(R.string.intro_exit_difficult_app_alert),
                            StyleSpan(Typeface.BOLD),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    if (!instructionManager.isTheFirst) {
                        if (isNotBlank()) {
                            append("\n\n")
                        }
                        append(getText(R.string.intro_exit_previous_hint))
                    }

                    if (isNotBlank()) {
                        append("\n\n")
                    }
                    append(getText(R.string.intro_location))
                }.takeIf { it.isNotBlank() }
            )
            .setPositiveButton(R.string.ok) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isOnBoarding && isFinishing) {
            appTracker.trackEvent(
                event = "OnBoardingEnd",
                property = "Step",
                value = (instructionManager.currentIndex + 1).toString()
            )
        }
    }

    companion object {
        private const val EXTRA_IS_ON_BOARDING = "is_on_boarding"
        private const val EXTRA_INSTRUCTION_INDEX = "instruction_index"

        fun getIntent(context: Context, isOnBoarding: Boolean = false): Intent {
            return Intent(context, IntroActivity::class.java)
                .putExtra(EXTRA_IS_ON_BOARDING, isOnBoarding)
        }
    }
}
