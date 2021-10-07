package xyz.aprildown.timer.data.repositories

import android.content.Context
import androidx.annotation.StringRes
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.data.R
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.SampleTimerRepository
import xyz.aprildown.timer.domain.usecases.timer.SampleTimerProvider
import javax.inject.Inject

@Reusable
internal class SampleTimerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SampleTimerRepository {
    override suspend fun getSampleTimer(id: Int): TimerEntity = when (id) {
        SampleTimerProvider.TEMPLATE_ONE_STAGE -> context.getOneStageTimer()
        SampleTimerProvider.TEMPLATE_TWO_STAGES -> context.getTwoStagesTimer()
        SampleTimerProvider.TEMPLATE_THREE_STAGES -> context.getThreeStagesTimer()

        else -> throw IllegalArgumentException("Wrong id $id for a sample timer")
    }
}

// region Sample Timers

private fun Context.getOneStageTimer(): TimerEntity = TimerEntity(
    id = TimerEntity.NEW_ID,
    name = getString(R.string.sample_timer_template_1_stage_name),
    loop = 3,
    steps = listOf(
        StepEntity.Step(
            label = getString(R.string.sample_timer_template_1_stage_work),
            length = 90_000L
        ),
        getTemplateNotifierStep()
    ),
    startStep = getTemplateStartStep(),
    endStep = getTemplateEndStep()
)

private fun Context.getTwoStagesTimer(): TimerEntity = TimerEntity(
    id = TimerEntity.NEW_ID,
    name = getString(R.string.sample_timer_template_2_stages_name),
    loop = 3,
    steps = listOf(
        StepEntity.Step(
            label = getString(R.string.sample_timer_template_2_stages_high),
            length = 90_000L
        ),
        getTemplateNotifierStep(),
        StepEntity.Step(
            label = getString(R.string.sample_timer_template_2_stages_low),
            length = 30_000L
        ),
        getTemplateNotifierStep()
    ),
    startStep = getTemplateStartStep(R.string.sample_timer_template_2_stages_start),
    endStep = getTemplateEndStep(R.string.sample_timer_template_2_stages_end)
)

private fun Context.getThreeStagesTimer(): TimerEntity = TimerEntity(
    id = TimerEntity.NEW_ID,
    name = getString(R.string.sample_timer_template_3_stages_name),
    loop = 3,
    steps = listOf(
        StepEntity.Step(
            label = getString(R.string.sample_timer_template_3_stages_1),
            length = 60_000L
        ),
        getTemplateNotifierStep(),
        StepEntity.Step(
            label = getString(R.string.sample_timer_template_3_stages_2),
            length = 60_000L
        ),
        getTemplateNotifierStep(),
        StepEntity.Step(
            label = getString(R.string.sample_timer_template_3_stages_3),
            length = 60_000L
        ),
        getTemplateNotifierStep()
    ),
    startStep = getTemplateStartStep(),
    endStep = getTemplateEndStep()
)

private fun Context.getTemplateNotifierStep(): StepEntity.Step = StepEntity.Step(
    label = getString(R.string.sample_timer_template_done),
    length = 5_000L,
    behaviour = listOf(
        BehaviourEntity(BehaviourType.MUSIC),
        BehaviourEntity(BehaviourType.SCREEN)
    ),
    type = StepType.NOTIFIER
)

private fun Context.getTemplateStartStep(
    @StringRes labelRes: Int = R.string.sample_timer_template_prepare
): StepEntity.Step = StepEntity.Step(
    label = getString(labelRes),
    length = 10_000L,
    behaviour = listOf(BehaviourEntity(BehaviourType.BEEP)),
    type = StepType.START
)

private fun Context.getTemplateEndStep(
    @StringRes labelRes: Int = R.string.sample_timer_template_finish
): StepEntity.Step = StepEntity.Step(
    label = getString(labelRes),
    length = 10_000L,
    behaviour = listOf(BehaviourEntity(BehaviourType.VOICE)),
    type = StepType.END
)

// endregion Sample Timers
