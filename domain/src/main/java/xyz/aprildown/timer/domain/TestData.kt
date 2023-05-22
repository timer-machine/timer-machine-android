package xyz.aprildown.timer.domain

import androidx.annotation.VisibleForTesting
import xyz.aprildown.timer.domain.entities.AppDataEntity
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerMoreEntity
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import java.util.concurrent.ThreadLocalRandom

/**
 * DO NOT TOUCH THESE FAKE DATA UNLESS YOU WANT TO REWRITE ALL TESTS.
 */
@VisibleForTesting(otherwise = VisibleForTesting.NONE)
@Suppress("MemberVisibilityCanBePrivate")
object TestData {
    const val fakeFolderId = 16L
    val fakeFolder = FolderEntity(fakeFolderId, "MyFolder")
    val defaultFolder = FolderEntity(FolderEntity.FOLDER_DEFAULT, "Default Folder")
    val trashFolder = FolderEntity(FolderEntity.FOLDER_TRASH, "Trash Folder")

    const val fakeTimerId = 434

    val fakeBehaviourA = BehaviourEntity(BehaviourType.MUSIC, "Sun", "uri: sun", false)
    val fakeBehaviourB = BehaviourEntity(BehaviourType.VIBRATION)
    val fakeBehaviourC = BehaviourEntity(BehaviourType.SCREEN)
    val fakeBehaviourD = BehaviourEntity(BehaviourType.VOICE)

    val fakeTimerMoreA = TimerMoreEntity()
    val fakeTimerMoreB = TimerMoreEntity(showNotif = false, notifCount = true)

    val fakeStepA = StepEntity.Step("Step Alpha", 60_000, listOf())
    val fakeStepB = StepEntity.Step(
        label = "Step Bravo",
        length = 5_000,
        behaviour = listOf(fakeBehaviourA, fakeBehaviourB),
        type = StepType.NOTIFIER
    )
    val fakeStepC = StepEntity.Step(
        label = "Step Charles",
        length = 1_000_000,
        behaviour = listOf(fakeBehaviourC, fakeBehaviourD),
        type = StepType.END
    )
    val fakeStepD = StepEntity.Group(
        name = "Step Group",
        loop = 3,
        steps = listOf(fakeStepA, fakeStepB, fakeStepC)
    )

    val fakeTimerSimpleA = TimerEntity(
        id = fakeTimerId,
        name = "Timer Alpha",
        loop = 1,
        steps = listOf(fakeStepA)
    )
    val fakeTimerSimpleB = TimerEntity(
        id = fakeTimerId + 1,
        name = "Timer Bravo",
        loop = 5,
        steps = listOf(fakeStepA, fakeStepB),
        startStep = fakeStepA,
        endStep = fakeStepC,
        more = fakeTimerMoreB
    )
    val fakeTimerAdvanced = TimerEntity(
        id = fakeTimerId + 2,
        name = "Timer Advanced",
        loop = 5,
        steps = listOf(fakeStepA, fakeStepD, fakeStepB, fakeStepC, fakeStepD),
        startStep = fakeStepA,
        endStep = fakeStepC,
        more = fakeTimerMoreB
    )

    const val fakeSchedulerId = 10
    val fakeSchedulerA = SchedulerEntity(
        fakeSchedulerId, fakeTimerId,
        "Scheduler Alpha", SchedulerEntity.ACTION_START, 10, 24,
        SchedulerRepeatMode.EVERY_WEEK,
        listOf(true, false, true, false, true, false, true),
        0
    )
    val fakeSchedulerB = SchedulerEntity(
        fakeSchedulerId + 1, fakeTimerId,
        "Scheduler Bravo", SchedulerEntity.ACTION_END, 23, 30,
        SchedulerRepeatMode.EVERY_DAYS,
        listOf(false, true, false, true, false, true, false),
        1
    )

    const val fakeTimerStampIdA = 1255
    const val fakeTimerStampIdB = 1259
    val fakeTimerStampA = TimerStampEntity(fakeTimerStampIdA, fakeTimerId, 1L, 7355608L)
    val fakeTimerStampB = TimerStampEntity(fakeTimerStampIdB, fakeTimerId, 1L, 1479177000000L)

    val fakeAppData = AppDataEntity(
        listOf(defaultFolder, fakeFolder, trashFolder),
        listOf(fakeTimerSimpleA, fakeTimerSimpleB, fakeTimerAdvanced),
        fakeStepC,
        listOf(
            fakeTimerStampA.copy(timerId = fakeTimerSimpleA.id),
            fakeTimerStampB.copy(timerId = fakeTimerAdvanced.id)
        ),
        listOf(fakeSchedulerA, fakeSchedulerB),
        mapOf("pref1" to "10", "prefs2" to "false", "prefs3" to "abc")
    )

    fun getRandomDaysTimerStamp(
        timerId: Int = fakeTimerId,
        from: Long = System.currentTimeMillis(),
        to: Long = from
    ): TimerStampEntity {
        require(from <= to) { "$from >= $to" }
        val (start, end) = getTimerStampSpan(from, to)
        return TimerStampEntity(
            id = fakeTimerStampIdA,
            timerId = timerId,
            start = start,
            end = start + ThreadLocalRandom.current().nextLong(end - start)
        )
    }

    private fun getTimerStampSpan(from: Long, to: Long): Pair<Long, Long> {
        val start = TimeUtils.getDayStart(from)
        var end = TimeUtils.getDayEnd(to)
        val now = System.currentTimeMillis()
        if (end > now) {
            end = now
        }
        return start to end
    }
}
