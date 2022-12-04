package xyz.aprildown.timer.presentation.stream

import android.text.SpannableString
import android.text.SpannableStringBuilder
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.HalfAction
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.VoiceAction
import xyz.aprildown.timer.domain.entities.toHalfAction
import xyz.aprildown.timer.domain.utils.AppTracker

/**
 * The whole work is doubled because we need to check loop index.
 * @return null if there is no more step
 */
internal fun getNextIndexWithStep(
    steps: List<StepEntity>,
    totalLoop: Int,
    currentIndex: TimerIndex,
    defaultLast: TimerIndex = TimerIndex.End
): Pair<TimerIndex, StepEntity.Step?> {
    val totalSteps = steps.size
    require(totalSteps > 0)
    return when (currentIndex) {
        is TimerIndex.Start -> {
            // From the Start to the first step
            when (val first = steps[0]) {
                is StepEntity.Step -> TimerIndex.Step(loopIndex = 0, stepIndex = 0) to first
                is StepEntity.Group -> TimerIndex.Group(
                    loopIndex = 0,
                    stepIndex = 0,
                    groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                ) to first.steps[0] as StepEntity.Step
            }
        }
        is TimerIndex.Step -> {
            val (currentLoopIndex, currentStepIndex) = currentIndex
            val nextStepIndex = currentStepIndex + 1
            if (nextStepIndex < totalSteps) {
                // From this step to the next step
                when (val nextStep: StepEntity = steps[nextStepIndex]) {
                    is StepEntity.Step ->
                        TimerIndex.Step(
                            loopIndex = currentLoopIndex,
                            stepIndex = nextStepIndex
                        ) to nextStep
                    is StepEntity.Group ->
                        TimerIndex.Group(
                            loopIndex = currentLoopIndex,
                            stepIndex = nextStepIndex,
                            groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                        ) to nextStep.steps[0] as StepEntity.Step
                }
            } else {
                // No more steps, to the next loop or End
                val nextLoopIndex = currentLoopIndex + 1
                if (nextLoopIndex < totalLoop) {
                    // To the next loop
                    when (val firstStep: StepEntity = steps[0]) {
                        is StepEntity.Step ->
                            TimerIndex.Step(
                                loopIndex = nextLoopIndex,
                                stepIndex = 0
                            ) to firstStep
                        is StepEntity.Group ->
                            TimerIndex.Group(
                                loopIndex = nextLoopIndex,
                                stepIndex = 0,
                                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                            ) to firstStep.steps[0] as StepEntity.Step
                    }
                } else {
                    // to End
                    defaultLast to null
                }
            }
        }
        is TimerIndex.Group -> {
            val (currentLoopIndex, currentStepIndex, currentGroupStepIndex) = currentIndex
            val (currentGroupStepLoopIndex, currentGroupStepIndexStepIndex) = currentGroupStepIndex

            val currentGroup = steps[currentStepIndex] as StepEntity.Group
            val currentGroupSteps = currentGroup.steps

            val nextGroupStepIndex = currentGroupStepIndexStepIndex + 1
            if (nextGroupStepIndex < currentGroupSteps.size) {
                // From a group step to the next group step
                currentIndex.copy(
                    groupStepIndex = TimerIndex.Step(
                        loopIndex = currentGroupStepLoopIndex,
                        stepIndex = nextGroupStepIndex
                    )
                ) to currentGroupSteps[nextGroupStepIndex] as StepEntity.Step
            } else {
                // Change group loop
                val nextGroupLoopIndex = currentGroupStepLoopIndex + 1
                if (nextGroupLoopIndex < currentGroup.loop) {
                    // From a group step to the next group loop
                    currentIndex.copy(
                        groupStepIndex = TimerIndex.Step(
                            loopIndex = nextGroupLoopIndex,
                            stepIndex = 0
                        )
                    ) to currentGroupSteps[0] as StepEntity.Step
                } else {
                    // To the next timer step or group
                    val nextStepIndex = currentStepIndex + 1
                    if (nextStepIndex < totalSteps) {
                        // From a group step the next timer step or group
                        when (val nextStep = steps[nextStepIndex]) {
                            is StepEntity.Step -> TimerIndex.Step(
                                loopIndex = currentLoopIndex,
                                stepIndex = nextStepIndex
                            ) to nextStep
                            is StepEntity.Group -> TimerIndex.Group(
                                loopIndex = currentLoopIndex,
                                stepIndex = nextStepIndex,
                                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                            ) to nextStep.steps[0] as StepEntity.Step
                        }
                    } else {
                        // To the next loop
                        val nextLoopIndex = currentLoopIndex + 1
                        if (nextLoopIndex < totalLoop) {
                            when (val firstStep = steps[0]) {
                                is StepEntity.Step ->
                                    TimerIndex.Step(
                                        loopIndex = nextLoopIndex,
                                        stepIndex = 0
                                    ) to firstStep
                                is StepEntity.Group ->
                                    TimerIndex.Group(
                                        loopIndex = nextLoopIndex,
                                        stepIndex = 0,
                                        groupStepIndex = TimerIndex.Step(
                                            loopIndex = 0,
                                            stepIndex = 0
                                        )
                                    ) to firstStep.steps[0] as StepEntity.Step
                            }
                        } else {
                            defaultLast to null
                        }
                    }
                }
            }
        }
        is TimerIndex.End -> defaultLast to null
    }
}

internal fun getPrevIndexWithStep(
    steps: List<StepEntity>,
    totalLoop: Int,
    currentIndex: TimerIndex,
    defaultFirst: TimerIndex = TimerIndex.Start
): Pair<TimerIndex, StepEntity.Step?> {
    val totalSteps = steps.size
    return when (currentIndex) {
        is TimerIndex.Start -> defaultFirst to null
        is TimerIndex.Step -> {
            val (currentLoopIndex, currentStepIndex) = currentIndex
            val prevStepIndex = currentStepIndex - 1
            if (prevStepIndex >= 0) {
                // From this step to the previous step
                when (val lastStep = steps[prevStepIndex]) {
                    is StepEntity.Step -> TimerIndex.Step(
                        loopIndex = currentLoopIndex,
                        stepIndex = prevStepIndex
                    ) to lastStep
                    is StepEntity.Group -> {
                        val groupSize = lastStep.steps.size
                        TimerIndex.Group(
                            loopIndex = currentLoopIndex,
                            stepIndex = prevStepIndex,
                            groupStepIndex = TimerIndex.Step(
                                loopIndex = lastStep.loop - 1,
                                stepIndex = groupSize - 1
                            )
                        ) to lastStep.steps[groupSize - 1] as StepEntity.Step
                    }
                }
            } else {
                // To the previous loop
                val prevLoopIndex = currentLoopIndex - 1
                if (prevLoopIndex >= 0) {
                    when (val lastStep = steps[totalSteps - 1]) {
                        is StepEntity.Step -> TimerIndex.Step(
                            loopIndex = prevLoopIndex,
                            stepIndex = totalSteps - 1
                        ) to lastStep
                        is StepEntity.Group -> {
                            val groupSize = lastStep.steps.size
                            TimerIndex.Group(
                                loopIndex = prevLoopIndex,
                                stepIndex = totalSteps - 1,
                                groupStepIndex = TimerIndex.Step(
                                    loopIndex = lastStep.loop - 1,
                                    stepIndex = groupSize - 1
                                )
                            ) to lastStep.steps[groupSize - 1] as StepEntity.Step
                        }
                    }
                } else {
                    defaultFirst to null
                }
            }
        }
        is TimerIndex.Group -> {
            val (currentLoopIndex, currentStepIndex, currentGroupStepIndex) = currentIndex
            val (currentGroupStepLoopIndex, currentGroupStepStepIndex) = currentGroupStepIndex

            val currentGroup = steps[currentStepIndex] as StepEntity.Group
            val currentGroupSteps = currentGroup.steps

            val prevGroupStepStepIndex = currentGroupStepStepIndex - 1
            if (prevGroupStepStepIndex >= 0) {
                // From a group step to the previous group step
                currentIndex.copy(
                    groupStepIndex = TimerIndex.Step(
                        loopIndex = currentGroupStepLoopIndex,
                        stepIndex = prevGroupStepStepIndex
                    )
                ) to currentGroupSteps[prevGroupStepStepIndex] as StepEntity.Step
            } else {
                // To the previous group loop
                val prevGroupStepLoopIndex = currentGroupStepLoopIndex - 1
                if (prevGroupStepLoopIndex >= 0) {
                    val lastGroupStepStepIndex = currentGroupSteps.size - 1
                    currentIndex.copy(
                        groupStepIndex = TimerIndex.Step(
                            loopIndex = prevGroupStepLoopIndex,
                            stepIndex = lastGroupStepStepIndex
                        )
                    ) to currentGroupSteps[lastGroupStepStepIndex] as StepEntity.Step
                } else {
                    // To the previous step or group
                    val prevStepIndex = currentStepIndex - 1
                    if (prevStepIndex >= 0) {
                        when (val prevStep = steps[prevStepIndex]) {
                            is StepEntity.Step -> TimerIndex.Step(
                                loopIndex = currentLoopIndex,
                                stepIndex = prevStepIndex
                            ) to prevStep
                            is StepEntity.Group -> {
                                val lastGroupSize = prevStep.steps.size
                                TimerIndex.Group(
                                    loopIndex = currentLoopIndex,
                                    stepIndex = prevStepIndex,
                                    groupStepIndex = TimerIndex.Step(
                                        loopIndex = prevStep.loop - 1,
                                        stepIndex = lastGroupSize - 1
                                    )
                                ) to prevStep.steps[lastGroupSize - 1] as StepEntity.Step
                            }
                        }
                    } else {
                        // To the previous loop
                        val prevLoopIndex = currentLoopIndex - 1
                        if (prevLoopIndex >= 0) {
                            when (val lastStep = steps[steps.size - 1]) {
                                is StepEntity.Step ->
                                    TimerIndex.Step(
                                        loopIndex = prevLoopIndex,
                                        stepIndex = steps.size - 1
                                    ) to lastStep
                                is StepEntity.Group -> {
                                    val lastGroupSize = lastStep.steps.size
                                    TimerIndex.Group(
                                        loopIndex = prevLoopIndex,
                                        stepIndex = steps.size - 1,
                                        groupStepIndex = TimerIndex.Step(
                                            loopIndex = lastStep.loop - 1,
                                            stepIndex = lastGroupSize - 1
                                        )
                                    ) to lastStep.steps[lastGroupSize - 1] as StepEntity.Step
                                }
                            }
                        } else {
                            defaultFirst to null
                        }
                    }
                }
            }
        }
        is TimerIndex.End -> {
            // From End to the last step
            when (val lastStep = steps[totalSteps - 1]) {
                is StepEntity.Step -> TimerIndex.Step(
                    loopIndex = totalLoop - 1,
                    stepIndex = totalSteps - 1
                ) to lastStep
                is StepEntity.Group -> {
                    val groupSize = lastStep.steps.size
                    TimerIndex.Group(
                        loopIndex = totalLoop - 1,
                        stepIndex = totalSteps - 1,
                        groupStepIndex = TimerIndex.Step(
                            loopIndex = lastStep.loop - 1,
                            stepIndex = groupSize - 1
                        )
                    ) to lastStep.steps[groupSize - 1] as StepEntity.Step
                }
            }
        }
    }
}

// region list getters

fun List<StepEntity>.getStep(index: TimerIndex.Step): StepEntity.Step? {
    val i = index.stepIndex
    return if (i in indices) this[i] as? StepEntity.Step else null
}

fun List<StepEntity>.getGroupStep(index: TimerIndex.Group): StepEntity.Step? {
    val (_, stepIndex, groupStepIndex) = index
    val group = if (stepIndex in indices) get(stepIndex) as? StepEntity.Group else null
    val steps = group?.steps ?: return null
    val i = groupStepIndex.stepIndex
    return if (i in steps.indices) steps[i] as? StepEntity.Step? else null
}

fun TimerEntity.getStep(index: TimerIndex): StepEntity.Step? = when (index) {
    is TimerIndex.Start -> startStep
    is TimerIndex.Step -> steps.getStep(index)
    is TimerIndex.Group -> steps.getGroupStep(index)
    is TimerIndex.End -> endStep
}

fun TimerEntity.getGroup(index: TimerIndex): StepEntity.Group? {
    if (index !is TimerIndex.Group) return null
    return steps.getOrNull(index.stepIndex) as? StepEntity.Group
}

fun TimerEntity.getFirstIndex(): TimerIndex {
    if (startStep != null) return TimerIndex.Start
    return when (steps[0]) {
        is StepEntity.Step -> TimerIndex.Step(loopIndex = 0, stepIndex = 0)
        is StepEntity.Group -> TimerIndex.Group(
            loopIndex = 0,
            stepIndex = 0,
            groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
        )
    }
}

fun TimerEntity.getLastIndex(): TimerIndex {
    if (endStep != null) return TimerIndex.End
    val lastIndex = steps.size - 1
    return when (val last = steps[lastIndex]) {
        is StepEntity.Step -> TimerIndex.Step(loopIndex = loop - 1, stepIndex = lastIndex)
        is StepEntity.Group -> {
            TimerIndex.Group(
                loopIndex = loop - 1,
                stepIndex = lastIndex,
                groupStepIndex = TimerIndex.Step(
                    loopIndex = last.loop - 1,
                    stepIndex = last.steps.size - 1
                )
            )
        }
    }
}

fun TimerEntity.getTimerLoop(index: TimerIndex): Int = when (index) {
    is TimerIndex.Start -> 0
    is TimerIndex.Step -> index.loopIndex
    is TimerIndex.Group -> index.loopIndex
    is TimerIndex.End -> loop - 1
}

fun TimerEntity.getTotalTime(): Long {
    return steps.accumulateTime() * loop + (startStep?.length ?: 0L) + (endStep?.length ?: 0L)
}

fun TimerEntity.getTimeBeforeIndex(
    index: TimerIndex,
    appTracker: AppTracker? = null
): Long {
    return when (index) {
        TimerIndex.Start -> 0L
        is TimerIndex.Step ->
            (startStep?.length ?: 0L) +
                steps.accumulateTime() * index.loopIndex +
                steps.subList(0, index.stepIndex).accumulateTime()
        is TimerIndex.Group -> {
            when (val step = steps[index.stepIndex]) {
                is StepEntity.Group -> {
                    val groupTimeBeforeIndex =
                        step.steps.accumulateTime() * index.groupStepIndex.loopIndex +
                            step.steps.subList(0, index.groupStepIndex.stepIndex).accumulateTime()
                    (startStep?.length ?: 0L) +
                        steps.accumulateTime() * index.loopIndex +
                        steps.subList(0, index.stepIndex).accumulateTime() +
                        groupTimeBeforeIndex
                }
                is StepEntity.Step -> {
                    appTracker?.trackError(IllegalStateException("getTimeBeforeIndex $index -> $this"))
                    (startStep?.length ?: 0L) +
                        steps.accumulateTime() * index.loopIndex +
                        steps.subList(0, index.stepIndex).accumulateTime()
                }
            }
        }
        TimerIndex.End -> getTotalTime() - (endStep?.length ?: 0L)
    }
}

fun List<StepEntity>.accumulateTime(): Long {
    var total = 0L
    forEach {
        total += when (it) {
            is StepEntity.Step -> it.length
            is StepEntity.Group -> it.steps.accumulateTime() * it.loop
        }
    }
    return total
}

// endregion list getters

fun TimerEntity.isThisIndexValid(index: TimerIndex): Boolean = when (index) {
    is TimerIndex.Start -> startStep != null
    is TimerIndex.Step -> {
        val (loopIndex, stepIndex) = index
        (loopIndex in 0 until loop) &&
            (stepIndex in steps.indices && steps[stepIndex] is StepEntity.Step)
    }
    is TimerIndex.Group -> {
        val (loopIndex, stepIndex, groupStepIndex) = index
        if (loopIndex !in 0 until loop || stepIndex !in steps.indices) false
        else {
            val groupStep = steps[stepIndex]
            if (groupStep !is StepEntity.Group) false
            else {
                val (groupStepLoopIndex, groupStepStepIndex) = groupStepIndex
                groupStepLoopIndex in 0 until groupStep.loop &&
                    groupStepStepIndex in groupStep.steps.indices
            }
        }
    }
    is TimerIndex.End -> endStep != null
}

internal fun TimerIndex.isTheLastInTimer(timer: TimerEntity): Boolean {
    when (val index = this) {
        TimerIndex.Start -> {
            if (timer.startStep == null) return false

            return timer.steps.isEmpty() && timer.endStep == null
        }
        is TimerIndex.Step -> {
            if (timer.endStep != null) return false

            if (timer.steps.isEmpty()) return false
            if (timer.steps.last() !is StepEntity.Step) return false

            return index.loopIndex == (timer.loop - 1) &&
                index.stepIndex == (timer.steps.size - 1)
        }
        is TimerIndex.Group -> {
            if (timer.endStep != null) return false

            if (timer.steps.isEmpty()) return false

            if (index.loopIndex != (timer.loop - 1)) return false
            if (index.stepIndex != (timer.steps.size - 1)) return false

            val lastGroup = timer.steps.last()
            if (lastGroup !is StepEntity.Group) return false

            val groupIndex = index.groupStepIndex
            return groupIndex.loopIndex == (lastGroup.loop - 1) &&
                groupIndex.stepIndex == (lastGroup.steps.size - 1)
        }
        TimerIndex.End -> return true
    }
}

internal interface TimeFormatter {
    fun formatDuration(duration: Long): CharSequence
    fun formatTime(time: Long): CharSequence
}

internal fun VoiceAction.generateVoiceContent(
    timer: TimerEntity,
    currentStep: StepEntity.Step,
    index: TimerIndex,
    timeFormatter: TimeFormatter
): CharSequence {
    if (content2.isNotBlank()) {
        return generateVoiceContent(content2, timer, currentStep, index, timeFormatter)
    }

    var original = content
    if (original.isBlank()) {
        return currentStep.label
    }

    fun replaceVariable(variable: String, getContent: () -> CharSequence) {
        if (variable in original) {
            original = original.replace(variable, getContent.invoke().toString())
        }
    }

    fun replaceVariable(vararg variables: String, getContent: () -> String) {
        variables.forEach { replaceVariable(it, getContent) }
    }

    replaceVariable(
        variable = VoiceAction.REPLACER_LOOP,
        getContent = {
            when (index) {
                is TimerIndex.Start -> 1
                is TimerIndex.Step -> index.loopIndex + 1
                is TimerIndex.Group -> index.groupStepIndex.loopIndex + 1
                is TimerIndex.End -> timer.loop
            }.toString()
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_TOTAL_LOOP,
        getContent = {
            if (index !is TimerIndex.Group) {
                timer.loop
            } else {
                (timer.steps.getOrNull(index.stepIndex) as? StepEntity.Group)?.loop
            }.toString()
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_STEP_NAME,
        getContent = {
            currentStep.label
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_STEP_DURATION,
        getContent = {
            timeFormatter.formatDuration(currentStep.length)
        }
    )

    // Long variable first.

    // region Elapsed

    replaceVariable(
        variable = VoiceAction.REPLACER_ELAPSED_TIME_PERCENT_GROUP,
        getContent = {
            if (index !is TimerIndex.Group) {
                produceElapsedTimePercent(timer, index)
            } else {
                produceElapsedTimePercent(
                    timer = createGroupTimerEntity(timer, index) ?: return@replaceVariable "",
                    index = index.groupStepIndex
                )
            }
        }
    )

    replaceVariable(
        variable = VoiceAction.REPLACER_ELAPSED_TIME_GROUP,
        getContent = {
            timeFormatter.formatDuration(
                if (index !is TimerIndex.Group) {
                    timer.getTimeBeforeIndex(index)
                } else {
                    createGroupTimerEntity(timer, index)
                        ?.getTimeBeforeIndex(index.groupStepIndex) ?: 0
                }
            )
        }
    )

    replaceVariable(
        VoiceAction.REPLACER_ELAPSED_TIME_PERCENT,
        VoiceAction.REPLACER_ELAPSED_TIME_PERCENT_OLD,
        getContent = {
            produceElapsedTimePercent(timer, index)
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_ELAPSED_TIME,
        getContent = {
            timeFormatter.formatDuration(timer.getTimeBeforeIndex(index))
        }
    )

    // endregion Elapsed

    // region Remaining

    replaceVariable(
        variable = VoiceAction.REPLACER_REMAINING_TIME_PERCENT_GROUP,
        getContent = {
            if (index !is TimerIndex.Group) {
                produceRemainingTimePercent(timer, index)
            } else {
                produceRemainingTimePercent(
                    timer = createGroupTimerEntity(timer, index) ?: return@replaceVariable "",
                    index = index.groupStepIndex
                )
            }
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_REMAINING_TIME_GROUP,
        getContent = {
            if (index !is TimerIndex.Group) {
                timeFormatter.formatDuration(timer.getTotalTime() - timer.getTimeBeforeIndex(index))
            } else {
                val groupTimer = createGroupTimerEntity(timer, index) ?: return@replaceVariable ""
                timeFormatter.formatDuration(
                    groupTimer.getTotalTime() - groupTimer.getTimeBeforeIndex(index.groupStepIndex)
                )
            }
        }
    )

    replaceVariable(
        VoiceAction.REPLACER_REMAINING_TIME_PERCENT,
        VoiceAction.REPLACER_REMAINING_TIME_PERCENT_OLD,
        getContent = {
            produceRemainingTimePercent(timer, index)
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_REMAINING_TIME,
        getContent = {
            timeFormatter.formatDuration(timer.getTotalTime() - timer.getTimeBeforeIndex(index))
        }
    )

    // endregion Remaining

    replaceVariable(
        variable = VoiceAction.REPLACER_STEP_END_TIME,
        getContent = {
            timeFormatter.formatTime(System.currentTimeMillis() + currentStep.length)
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_TIMER_END_TIME,
        getContent = {
            timeFormatter.formatTime(
                System.currentTimeMillis() +
                    timer.getTotalTime() -
                    timer.getTimeBeforeIndex(index)
            )
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_GROUP_END_TIME,
        getContent = {
            if (index !is TimerIndex.Group) {
                timeFormatter.formatTime(
                    System.currentTimeMillis() +
                        timer.getTotalTime() -
                        timer.getTimeBeforeIndex(index)
                )
            } else {
                val groupTimer = createGroupTimerEntity(timer, index) ?: return@replaceVariable ""
                timeFormatter.formatTime(
                    System.currentTimeMillis() +
                        groupTimer.getTotalTime() -
                        groupTimer.getTimeBeforeIndex(index.groupStepIndex)
                )
            }
        }
    )
    replaceVariable(
        variable = VoiceAction.REPLACER_CURRENT_TIME,
        getContent = {
            timeFormatter.formatTime(System.currentTimeMillis())
        }
    )

    return original
}

private fun createGroupTimerEntity(timer: TimerEntity, index: TimerIndex.Group): TimerEntity? {
    val groupStep = (timer.steps.getOrNull(index.stepIndex) as? StepEntity.Group)
        ?: return null
    return timer.copy(
        name = groupStep.name,
        loop = groupStep.loop,
        steps = groupStep.steps,
        startStep = null,
        endStep = null
    )
}

private fun produceElapsedTimePercent(timer: TimerEntity, index: TimerIndex): String {
    val timeBeforeIndex = timer.getTimeBeforeIndex(index)
    val totalTime = timer.getTotalTime()
    return ((timeBeforeIndex.toFloat() / totalTime.toFloat()) * 100).toInt().toString() + "%"
}

private fun produceRemainingTimePercent(timer: TimerEntity, index: TimerIndex): String {
    val total = timer.getTotalTime().toFloat()
    val timeBeforeIndex = timer.getTimeBeforeIndex(index)
    return (((total - timeBeforeIndex.toFloat()) / total) * 100).toInt().toString() + "%"
}

private fun generateVoiceContent(
    content: String,
    timer: TimerEntity,
    step: StepEntity.Step,
    index: TimerIndex,
    timeFormatter: TimeFormatter
): CharSequence {
    val builder = SpannableStringBuilder()

    fun variableToValue(variable: String): CharSequence? = when (variable) {
        VoiceAction.VOICE_VARIABLE_STEP_NAME,
        VoiceAction.VARIABLE_STEP_NAME -> step.label
        VoiceAction.VOICE_VARIABLE_STEP_DURATION,
        VoiceAction.VARIABLE_STEP_DURATION -> timeFormatter.formatDuration(step.length)
        VoiceAction.VOICE_VARIABLE_STEP_END_TIME,
        VoiceAction.VARIABLE_STEP_END_TIME ->
            timeFormatter.formatTime(System.currentTimeMillis() + step.length)

        VoiceAction.VOICE_VARIABLE_TIMER_NAME,
        VoiceAction.VARIABLE_TIMER_NAME -> timer.name
        VoiceAction.VOICE_VARIABLE_TIMER_LOOP,
        VoiceAction.VARIABLE_TIMER_LOOP -> when (index) {
            is TimerIndex.Start -> 1
            is TimerIndex.Step -> index.loopIndex + 1
            is TimerIndex.Group -> index.loopIndex + 1
            is TimerIndex.End -> timer.loop
        }.toString()
        VoiceAction.VOICE_VARIABLE_TIMER_TOTAL_LOOP,
        VoiceAction.VARIABLE_TIMER_TOTAL_LOOP -> timer.loop.toString()
        VoiceAction.VOICE_VARIABLE_TIMER_DURATION,
        VoiceAction.VARIABLE_TIMER_DURATION ->
            timeFormatter.formatDuration(timer.getTotalTime())
        VoiceAction.VOICE_VARIABLE_TIMER_ELAPSED,
        VoiceAction.VARIABLE_TIMER_ELAPSED ->
            timeFormatter.formatDuration(timer.getTimeBeforeIndex(index))
        VoiceAction.VOICE_VARIABLE_TIMER_ELAPSED_PERCENT,
        VoiceAction.VARIABLE_TIMER_ELAPSED_PERCENT -> produceElapsedTimePercent(timer, index)
        VoiceAction.VOICE_VARIABLE_TIMER_REMAINING,
        VoiceAction.VARIABLE_TIMER_REMAINING ->
            timeFormatter.formatDuration(timer.getTotalTime() - timer.getTimeBeforeIndex(index))
        VoiceAction.VOICE_VARIABLE_TIMER_REMAINING_PERCENT,
        VoiceAction.VARIABLE_TIMER_REMAINING_PERCENT ->
            produceRemainingTimePercent(timer, index)
        VoiceAction.VOICE_VARIABLE_TIMER_END_TIME,
        VoiceAction.VARIABLE_TIMER_END_TIME -> timeFormatter.formatTime(
            System.currentTimeMillis() +
                timer.getTotalTime() -
                timer.getTimeBeforeIndex(index)
        )

        VoiceAction.VOICE_VARIABLE_GROUP_NAME,
        VoiceAction.VARIABLE_GROUP_NAME -> if (index is TimerIndex.Group) {
            (timer.steps.getOrNull(index.stepIndex) as? StepEntity.Group)?.name.toString()
        } else {
            timer.name
        }
        VoiceAction.VOICE_VARIABLE_GROUP_LOOP,
        VoiceAction.VARIABLE_GROUP_LOOP -> when (index) {
            is TimerIndex.Start -> 1
            is TimerIndex.Step -> index.loopIndex + 1
            is TimerIndex.Group -> index.groupStepIndex.loopIndex + 1
            is TimerIndex.End -> timer.loop
        }.toString()
        VoiceAction.VOICE_VARIABLE_GROUP_TOTAL_LOOP,
        VoiceAction.VARIABLE_GROUP_TOTAL_LOOP -> if (index is TimerIndex.Group) {
            (timer.steps.getOrNull(index.stepIndex) as? StepEntity.Group)?.loop.toString()
        } else {
            timer.loop.toString()
        }
        VoiceAction.VOICE_VARIABLE_GROUP_DURATION,
        VoiceAction.VARIABLE_GROUP_DURATION -> timeFormatter.formatDuration(
            if (index is TimerIndex.Group) {
                createGroupTimerEntity(timer, index) ?: timer
            } else {
                timer
            }.getTotalTime()
        )
        VoiceAction.VOICE_VARIABLE_GROUP_ELAPSED,
        VoiceAction.VARIABLE_GROUP_ELAPSED -> timeFormatter.formatDuration(
            if (index !is TimerIndex.Group) {
                timer.getTimeBeforeIndex(index)
            } else {
                createGroupTimerEntity(timer, index)
                    ?.getTimeBeforeIndex(index.groupStepIndex) ?: 0
            }
        )
        VoiceAction.VOICE_VARIABLE_GROUP_ELAPSED_PERCENT,
        VoiceAction.VARIABLE_GROUP_ELAPSED_PERCENT -> if (index !is TimerIndex.Group) {
            produceElapsedTimePercent(timer, index)
        } else {
            produceElapsedTimePercent(
                timer = createGroupTimerEntity(timer, index) ?: timer,
                index = index.groupStepIndex
            )
        }
        VoiceAction.VOICE_VARIABLE_GROUP_REMAINING,
        VoiceAction.VARIABLE_GROUP_REMAINING -> if (index !is TimerIndex.Group) {
            timeFormatter.formatDuration(timer.getTotalTime() - timer.getTimeBeforeIndex(index))
        } else {
            val groupTimer = createGroupTimerEntity(timer, index) ?: timer
            timeFormatter.formatDuration(
                groupTimer.getTotalTime() - groupTimer.getTimeBeforeIndex(index.groupStepIndex)
            )
        }
        VoiceAction.VOICE_VARIABLE_GROUP_REMAINING_PERCENT,
        VoiceAction.VARIABLE_GROUP_REMAINING_PERCENT -> if (index !is TimerIndex.Group) {
            produceRemainingTimePercent(timer, index)
        } else {
            produceRemainingTimePercent(
                timer = createGroupTimerEntity(timer, index) ?: timer,
                index = index.groupStepIndex
            )
        }
        VoiceAction.VOICE_VARIABLE_GROUP_END_TIME,
        VoiceAction.VARIABLE_GROUP_END_TIME -> if (index !is TimerIndex.Group) {
            timeFormatter.formatTime(
                System.currentTimeMillis() +
                    timer.getTotalTime() -
                    timer.getTimeBeforeIndex(index)
            )
        } else {
            val groupTimer = createGroupTimerEntity(timer, index) ?: timer
            timeFormatter.formatTime(
                System.currentTimeMillis() +
                    groupTimer.getTotalTime() -
                    groupTimer.getTimeBeforeIndex(index.groupStepIndex)
            )
        }

        VoiceAction.VOICE_VARIABLE_OTHER_CLOCK_TIME,
        VoiceAction.VARIABLE_CLOCK_TIME -> timeFormatter.formatTime(System.currentTimeMillis())

        else -> null
    }

    var currentVariable = ""
    var isCollectingVariable = false

    for (char in content) {
        when {
            char == "}".single() && isCollectingVariable -> {
                currentVariable += char
                builder.append(variableToValue(currentVariable) ?: currentVariable)
                isCollectingVariable = false
                currentVariable = ""
            }
            char == "{".single() -> {
                if (currentVariable.isNotBlank()) {
                    builder.append(currentVariable)
                }

                isCollectingVariable = true
                currentVariable = ""
                currentVariable += char
            }
            isCollectingVariable -> {
                currentVariable += char
            }
            else -> {
                builder.append(char)
            }
        }
    }
    if (currentVariable.isNotBlank()) {
        builder.append(currentVariable)
    }

    return SpannableString(builder)
}

fun BehaviourEntity.useTts(): Boolean {
    when (type) {
        BehaviourType.VOICE,
        BehaviourType.COUNT -> return true
        BehaviourType.HALF -> if (toHalfAction().option == HalfAction.OPTION_VOICE) {
            return true
        }
        else -> Unit
    }
    return false
}
