package xyz.aprildown.timer.presentation.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.StepEntity

class TimerMachineHelperKtTest {

    @Test
    fun `next start`() {
        getNextIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Start
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 0, stepIndex = 0), index)
            assertEquals(step1, step)
        }

        getNextIndexWithStep(
            listOf(StepEntity.Group("group", 3, listOf(step1, step2)), step3),
            3,
            TimerIndex.Start
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 0,
                    stepIndex = 0,
                    groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                ),
                index
            )
            assertEquals(step1, step)
        }
    }

    @Test
    fun `next step`() {
        // From the first step to the second step
        getNextIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Step(loopIndex = 1, stepIndex = 0)
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 1, stepIndex = 1), index)
            assertEquals(step2, step)
        }
        // From the second step to the third step
        getNextIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Step(loopIndex = 2, stepIndex = 1)
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 2, stepIndex = 2), index)
            assertEquals(step3, step)
        }

        // From the first step to the first step of a group
        getNextIndexWithStep(
            listOf(step1, step1, StepEntity.Group("group", 3, listOf(step2, step3)), step3),
            5,
            TimerIndex.Step(loopIndex = 4, stepIndex = 1)
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 2,
                    groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                ),
                index
            )
            assertEquals(step2, step)
        }

        // From the third step to the first step
        getNextIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Step(loopIndex = 1, stepIndex = 2)
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 2, stepIndex = 0), index)
            assertEquals(step1, step)
        }
        // From the third step to the first step of a group
        getNextIndexWithStep(
            listOf(StepEntity.Group("group", 3, listOf(step2, step3)), step1, step3),
            5,
            TimerIndex.Step(loopIndex = 3, stepIndex = 2)
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 0,
                    groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                ),
                index
            )
            assertEquals(step2, step)
        }

        // From the last step to the End
        getNextIndexWithStep(
            listOf(StepEntity.Group("group", 3, listOf(step2, step3)), step1, step3),
            5,
            TimerIndex.Step(loopIndex = 4, stepIndex = 2)
        ).let { (index, step) ->
            assertEquals(TimerIndex.End, index)
            assertEquals(null, step)
        }
        // From the last group to the End
        getNextIndexWithStep(
            listOf(step1, step3, StepEntity.Group("group", 3, listOf(step2, step3))),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 2,
                groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 1)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.End, index)
            assertEquals(null, step)
        }
    }

    @Test
    fun `next group`() {
        // From a group step to the next group step
        getNextIndexWithStep(
            listOf(step1, StepEntity.Group("group", 3, listOf(step2, step3, step4)), step1),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 1)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 2)
                ),
                index
            )
            assertEquals(step4, step)
        }
        // From the last group step to the first group step
        getNextIndexWithStep(
            listOf(step1, StepEntity.Group("group", 3, listOf(step2, step3, step4)), step1),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 1, stepIndex = 2)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 0)
                ),
                index
            )
            assertEquals(step2, step)
        }
        // From a group step to the next step
        getNextIndexWithStep(
            listOf(step1, StepEntity.Group("group", 3, listOf(step2, step3)), step4),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 2)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 4, stepIndex = 2), index)
            assertEquals(step4, step)
        }
        // From a group step to the next group
        getNextIndexWithStep(
            listOf(
                step1,
                StepEntity.Group("group1", 3, listOf(step2, step2)),
                StepEntity.Group("group2", 7, listOf(step3, step4))
            ),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 1)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 2,
                    groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                ),
                index
            )
            assertEquals(step3, step)
        }
        // From the last group step to the first step
        getNextIndexWithStep(
            listOf(
                step1,
                StepEntity.Group("group2", 7, listOf(step3, step4))
            ),
            5,
            TimerIndex.Group(
                loopIndex = 3,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 6, stepIndex = 1)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 4, stepIndex = 0), index)
            assertEquals(step1, step)
        }
        // From the last group step to the first group
        getNextIndexWithStep(
            listOf(
                StepEntity.Group("group1", 3, listOf(step2, step3)),
                step1,
                StepEntity.Group("group2", 8, listOf(step1, step1))
            ),
            5,
            TimerIndex.Group(
                loopIndex = 3,
                stepIndex = 2,
                groupStepIndex = TimerIndex.Step(loopIndex = 7, stepIndex = 1)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 0,
                    groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
                ),
                index
            )
            assertEquals(step2, step)
        }
        // From the last group to the End
        getNextIndexWithStep(
            listOf(
                step1,
                StepEntity.Group("group2", 7, listOf(step2, step3))
            ),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 6, stepIndex = 1)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.End, index)
            assertEquals(null, step)
        }
    }

    @Test
    fun `next end`() {
        getNextIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.End
        ).let { (index, step) ->
            assertEquals(TimerIndex.End, index)
            assertNull(step)
        }
    }

    @Test
    fun `prev start`() {
        getPrevIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Start
        ).let { (index, step) ->
            assertEquals(TimerIndex.Start, index)
            assertNull(step)
        }
    }

    @Test
    fun `prev step`() {
        // From a step to the prev step
        getPrevIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Step(loopIndex = 2, stepIndex = 1)
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 2, stepIndex = 0), index)
            assertEquals(step1, step)
        }
        // From a step to the prev group
        getPrevIndexWithStep(
            listOf(step1, StepEntity.Group("group1", 3, listOf(step2, step4)), step3),
            5,
            TimerIndex.Step(loopIndex = 3, stepIndex = 2)
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 3, stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 1)
                ),
                index
            )
            assertEquals(step4, step)
        }
        // From the first step to the last step
        getPrevIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Step(loopIndex = 2, stepIndex = 0)
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 1, stepIndex = 2), index)
            assertEquals(step3, step)
        }
        // From the first step to the last group
        getPrevIndexWithStep(
            listOf(step1, step3, StepEntity.Group("group1", 3, listOf(step2, step4))),
            5,
            TimerIndex.Step(loopIndex = 3, stepIndex = 0)
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 2, stepIndex = 2,
                    groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 1)
                ),
                index
            )
            assertEquals(step4, step)
        }
        // From the first step to the Start
        getPrevIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.Step(loopIndex = 0, stepIndex = 0)
        ).let { (index, step) ->
            assertEquals(TimerIndex.Start, index)
            assertEquals(null, step)
        }
        // From the first step to the last group
        getPrevIndexWithStep(
            listOf(StepEntity.Group("group1", 3, listOf(step2, step4)), step1, step3),
            5,
            TimerIndex.Group(
                loopIndex = 0,
                stepIndex = 0,
                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.Start, index)
            assertEquals(null, step)
        }
    }

    @Test
    fun `prev group`() {
        // From a group step to the prev group step
        getPrevIndexWithStep(
            listOf(step1, StepEntity.Group("group", 3, listOf(step2, step3, step4)), step1),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 1)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 0)
                ),
                index
            )
            assertEquals(step2, step)
        }
        // From the first group step to the last group step
        getPrevIndexWithStep(
            listOf(step1, StepEntity.Group("group", 3, listOf(step2, step3, step4)), step1),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 1, stepIndex = 0)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 2)
                ),
                index
            )
            assertEquals(step4, step)
        }
        // From a group step to the prev step
        getPrevIndexWithStep(
            listOf(step1, StepEntity.Group("group", 3, listOf(step2, step3)), step4),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 1,
                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 4, stepIndex = 0), index)
            assertEquals(step1, step)
        }
        // From a group step to the prev group
        getPrevIndexWithStep(
            listOf(
                step1,
                StepEntity.Group("group1", 3, listOf(step3, step4)),
                StepEntity.Group("group2", 7, listOf(step2, step2))
            ),
            5,
            TimerIndex.Group(
                loopIndex = 4,
                stepIndex = 2,
                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 4,
                    stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 2, stepIndex = 1)
                ),
                index
            )
            assertEquals(step4, step)
        }
        // From the first group step to the last step
        getPrevIndexWithStep(
            listOf(
                StepEntity.Group("group2", 7, listOf(step3, step4)),
                step1
            ),
            5,
            TimerIndex.Group(
                loopIndex = 3,
                stepIndex = 0,
                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 2, stepIndex = 1), index)
            assertEquals(step1, step)
        }
        // From the first group step to the last group
        getPrevIndexWithStep(
            listOf(
                StepEntity.Group("group1", 3, listOf(step2, step3)),
                step1,
                StepEntity.Group("group2", 8, listOf(step1, step4))
            ),
            5,
            TimerIndex.Group(
                loopIndex = 3,
                stepIndex = 0,
                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
            )
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 2,
                    stepIndex = 2,
                    groupStepIndex = TimerIndex.Step(loopIndex = 7, stepIndex = 1)
                ),
                index
            )
            assertEquals(step4, step)
        }
        // From the first group to the Start
        getPrevIndexWithStep(
            listOf(
                StepEntity.Group("group2", 7, listOf(step2, step3)),
                step1
            ),
            5,
            TimerIndex.Group(
                loopIndex = 0,
                stepIndex = 0,
                groupStepIndex = TimerIndex.Step(loopIndex = 0, stepIndex = 0)
            )
        ).let { (index, step) ->
            assertEquals(TimerIndex.Start, index)
            assertEquals(null, step)
        }
    }

    @Test
    fun `prev end`() {
        // From the End to the last step
        getPrevIndexWithStep(
            listOf(step1, step2, step3),
            3,
            TimerIndex.End
        ).let { (index, step) ->
            assertEquals(TimerIndex.Step(loopIndex = 2, stepIndex = 2), index)
            assertEquals(step3, step)
        }
        // From the End to the last group
        getPrevIndexWithStep(
            listOf(step1, StepEntity.Group("group1", 5, listOf(step2, step3))),
            3,
            TimerIndex.End
        ).let { (index, step) ->
            assertEquals(
                TimerIndex.Group(
                    loopIndex = 2, stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 4, stepIndex = 1)
                ),
                index
            )
            assertEquals(step3, step)
        }
    }

    @Test
    fun `time before start`() {
        val timer = TestData.fakeTimerAdvanced
        assertEquals(0, timer.getTimeBeforeIndex(TimerIndex.Start))
    }

    @Test
    fun `time before loop 0 step 0`() {
        val timer = TestData.fakeTimerAdvanced
        assertEquals(
            timer.startStep!!.length,
            timer.getTimeBeforeIndex(TimerIndex.Step(loopIndex = 0, stepIndex = 0))
        )
    }

    @Test
    fun `time before loop 0 step 2`() {
        val timer = TestData.fakeTimerAdvanced
        val steps = timer.steps
        assertEquals(
            timer.startStep!!.length +
                (steps[0] as StepEntity.Step).length +
                (steps[1] as StepEntity.Group).let { it.steps.accumulateTime() * it.loop },
            timer.getTimeBeforeIndex(TimerIndex.Step(loopIndex = 0, stepIndex = 2))
        )
    }

    @Test
    fun `time before loop 0 step 4`() {
        val timer = TestData.fakeTimerAdvanced
        val steps = timer.steps
        assertEquals(
            timer.startStep!!.length +
                (steps[0] as StepEntity.Step).length +
                (steps[1] as StepEntity.Group).let { it.steps.accumulateTime() * it.loop } +
                (steps[2] as StepEntity.Step).length +
                (steps[3] as StepEntity.Step).length,
            timer.getTimeBeforeIndex(TimerIndex.Step(loopIndex = 0, stepIndex = 4))
        )
    }

    @Test
    fun `time before loop 1 step 0`() {
        val timer = TestData.fakeTimerAdvanced
        assertEquals(
            timer.startStep!!.length + timer.steps.accumulateTime(),
            timer.getTimeBeforeIndex(TimerIndex.Step(loopIndex = 1, stepIndex = 0))
        )
    }

    @Test
    fun `time before loop final step 0`() {
        val timer = TestData.fakeTimerAdvanced
        assertEquals(
            timer.startStep!!.length +
                timer.steps.accumulateTime() * (timer.loop - 1),
            timer.getTimeBeforeIndex(TimerIndex.Step(loopIndex = timer.loop - 1, stepIndex = 0))
        )
    }

    @Test
    fun `time before loop 2 group 1 (loop 1 step 1)`() {
        val timer = TestData.fakeTimerAdvanced
        val steps = timer.steps
        val groupSteps = TestData.fakeStepD.steps
        assertEquals(
            timer.startStep!!.length +
                steps.accumulateTime() * 2 +
                (steps[0] as StepEntity.Step).length +
                groupSteps.accumulateTime() +
                (groupSteps[0] as StepEntity.Step).length,
            timer.getTimeBeforeIndex(
                TimerIndex.Group(
                    loopIndex = 2, stepIndex = 1,
                    groupStepIndex = TimerIndex.Step(loopIndex = 1, stepIndex = 1)
                )
            )
        )
    }

    @Test
    fun `time before loop 3 group 4 (loop 1 step 2)`() {
        val timer = TestData.fakeTimerAdvanced
        val steps = timer.steps
        val group = TestData.fakeStepD
        val groupSteps = group.steps
        assertEquals(
            timer.startStep!!.length +
                steps.accumulateTime() * 3 +
                (steps[0] as StepEntity.Step).length +
                groupSteps.accumulateTime() * group.loop +
                (steps[2] as StepEntity.Step).length +
                (steps[3] as StepEntity.Step).length +
                groupSteps.accumulateTime() +
                (groupSteps[0] as StepEntity.Step).length +
                (groupSteps[1] as StepEntity.Step).length,
            timer.getTimeBeforeIndex(
                TimerIndex.Group(
                    loopIndex = 3, stepIndex = 4,
                    groupStepIndex = TimerIndex.Step(loopIndex = 1, stepIndex = 2)
                )
            )
        )
    }

    @Test
    fun `time before end`() {
        val timer = TestData.fakeTimerAdvanced
        assertEquals(
            timer.getTotalTime() - timer.endStep!!.length,
            timer.getTimeBeforeIndex(TimerIndex.End)
        )
    }
}

private val step1 = StepEntity.Step("step1", 0L, listOf())
private val step2 = StepEntity.Step("step2", 0L, listOf())
private val step3 = StepEntity.Step("step3", 0L, listOf())
private val step4 = StepEntity.Step("step4", 0L, listOf())
