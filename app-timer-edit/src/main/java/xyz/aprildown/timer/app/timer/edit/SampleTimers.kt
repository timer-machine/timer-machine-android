package xyz.aprildown.timer.app.timer.edit

import android.content.Context
import androidx.annotation.StringRes
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.app.base.R as RBase

internal fun Context.getOneStageTimer(): TimerEntity = TimerEntity(
    id = TimerEntity.NEW_ID,
    name = getString(RBase.string.sample_timer_template_1_stage_name),
    loop = 3,
    steps = listOf(
        StepEntity.Step(
            label = getString(RBase.string.sample_timer_template_1_stage_work),
            length = 90_000L
        ),
        getTemplateNotifierStep()
    ),
    startStep = getTemplateStartStep(),
    endStep = getTemplateEndStep()
)

internal fun Context.getTwoStagesTimer(): TimerEntity = TimerEntity(
    id = TimerEntity.NEW_ID,
    name = getString(RBase.string.sample_timer_template_2_stages_name),
    loop = 3,
    steps = listOf(
        StepEntity.Step(
            label = getString(RBase.string.sample_timer_template_2_stages_high),
            length = 90_000L
        ),
        getTemplateNotifierStep(),
        StepEntity.Step(
            label = getString(RBase.string.sample_timer_template_2_stages_low),
            length = 30_000L
        ),
        getTemplateNotifierStep()
    ),
    startStep = getTemplateStartStep(RBase.string.sample_timer_template_2_stages_start),
    endStep = getTemplateEndStep(RBase.string.sample_timer_template_2_stages_end)
)

internal fun Context.getThreeStagesTimer(): TimerEntity = TimerEntity(
    id = TimerEntity.NEW_ID,
    name = getString(RBase.string.sample_timer_template_3_stages_name),
    loop = 3,
    steps = listOf(
        StepEntity.Step(
            label = getString(RBase.string.sample_timer_template_3_stages_1),
            length = 60_000L
        ),
        getTemplateNotifierStep(),
        StepEntity.Step(
            label = getString(RBase.string.sample_timer_template_3_stages_2),
            length = 60_000L
        ),
        getTemplateNotifierStep(),
        StepEntity.Step(
            label = getString(RBase.string.sample_timer_template_3_stages_3),
            length = 60_000L
        ),
        getTemplateNotifierStep()
    ),
    startStep = getTemplateStartStep(),
    endStep = getTemplateEndStep()
)

private fun Context.getTemplateNotifierStep(): StepEntity.Step = StepEntity.Step(
    label = getString(RBase.string.sample_timer_template_done),
    length = 5_000L,
    behaviour = listOf(
        BehaviourEntity(BehaviourType.MUSIC),
        BehaviourEntity(BehaviourType.SCREEN)
    ),
    type = StepType.NOTIFIER
)

private fun Context.getTemplateStartStep(
    @StringRes labelRes: Int = RBase.string.sample_timer_template_prepare
): StepEntity.Step = StepEntity.Step(
    label = getString(labelRes),
    length = 10_000L,
    behaviour = listOf(BehaviourEntity(BehaviourType.BEEP)),
    type = StepType.START
)

private fun Context.getTemplateEndStep(
    @StringRes labelRes: Int = RBase.string.sample_timer_template_finish
): StepEntity.Step = StepEntity.Step(
    label = getString(labelRes),
    length = 10_000L,
    behaviour = listOf(BehaviourEntity(BehaviourType.VOICE)),
    type = StepType.END
)
