package xyz.aprildown.timer.app.timer.one.step

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getTimerLoop

class StepListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs), CurrentPositionCallback, OnStepLongClickListener {

    interface StepLongClickListener {
        fun onGroupTitleClicked(defaultIndex: TimerIndex.Group)
        fun onJumpToStep(index: TimerIndex)
        fun onEditStep(index: TimerIndex)
        fun onEditStepTime(index: TimerIndex)
    }

    private val startAdapter: ItemAdapter<VisibleStep> = ItemAdapter()
    private val stepAdapter: ItemAdapter<IItem<*>> = ItemAdapter()
    private val endAdapter: ItemAdapter<VisibleStep> = ItemAdapter()
    private val fastAdapter: FastAdapter<IItem<*>> =
        FastAdapter.with(listOf(startAdapter, stepAdapter, endAdapter))
    private val stepLayoutManager: LinearLayoutManager = LinearLayoutManager(context)

    private lateinit var timerInstance: TimerEntity

    /**
     * Current selected global position. Must be a [VisibleStep].
     */
    override var currentPosition: Int = NO_POSITION
        private set
    private var currentIndex: TimerIndex = TimerIndex.Start

    var listener: StepLongClickListener? = null

    init {
        layoutManager = stepLayoutManager
        addItemDecoration(OffsetEndsItemDecoration())
        itemAnimator?.run {
            changeDuration = ANIMATION_DURATION
            moveDuration = ANIMATION_DURATION
            addDuration = ANIMATION_DURATION
            removeDuration = ANIMATION_DURATION
        }
        adapter = fastAdapter
    }

    override fun onStepLongClick(item: IItem<*>) {
        if (item is VisibleStep) {
            item.toIndex()?.let {
                listener?.onEditStep(it)
            }
        }
    }

    override fun onStepTimeLongClick(item: IItem<*>) {
        if (item is VisibleStep) {
            item.toIndex()?.let {
                listener?.onEditStepTime(it)
            }
        }
    }

    /**
     * Since we must use long click and double tap, timer jump is more frequent and
     * double tap is faster, I decide to use double tap to implement timer jump.
     */
    override fun onStepDoubleTap(item: IItem<*>) {
        when (item) {
            is VisibleStep -> {
                item.toIndex()?.let {
                    listener?.onJumpToStep(it)
                }
            }
            is VisibleGroup -> listener?.onGroupTitleClicked(item.toIndex())
        }
    }

    fun setTimer(timer: TimerEntity) {
        if (!::timerInstance.isInitialized || timerInstance != timer) {
            loadTimer(timer)
            timerInstance = timer
        }
        toIndex(currentIndex, forceRefreshing = true)
    }

    fun toIndex(newIndex: TimerIndex, forceRefreshing: Boolean = false) {
        currentIndex = newIndex
        if (!::timerInstance.isInitialized) return
        val newPosition: Int = when (newIndex) {
            is TimerIndex.Start -> 0
            is TimerIndex.Step -> {
                fastAdapter.getPosition(indexesToIdentifier(newIndex.stepIndex + 1))
            }
            is TimerIndex.Group -> {
                val (_, stepIndex, groupStep) = newIndex

                val groupPosition = fastAdapter.getPosition(indexesToIdentifier(stepIndex + 1))
                val visibleGroup = fastAdapter.getItem(groupPosition) as? VisibleGroup
                visibleGroup?.loopIndex = groupStep.loopIndex
                fastAdapter.notifyItemChanged(groupPosition)

                fastAdapter.getPosition(indexesToIdentifier(stepIndex + 1, groupStep.stepIndex))
            }
            is TimerIndex.End -> fastAdapter.itemCount - 1
        }
        if (newPosition != currentPosition || forceRefreshing) {
            val oldPosition = currentPosition
            currentPosition = newPosition
            fastAdapter.notifyItemChanged(oldPosition)
            fastAdapter.notifyItemChanged(newPosition)
            stepLayoutManager.scrollToPositionWithOffset(newPosition, height / 3)
        }
    }

    private fun loadTimer(timer: TimerEntity) {
        startAdapter.clear()
        stepAdapter.clear()
        endAdapter.clear()

        timer.startStep?.let {
            startAdapter.add(VisibleStep(it, 0, indexesToIdentifier(0), this, this))
        }

        val stepItems = mutableListOf<IItem<*>>()
        timer.steps.forEachIndexed { i, stepEntity ->
            when (stepEntity) {
                is StepEntity.Step -> {
                    stepItems.add(
                        VisibleStep(
                            stepEntity,
                            i + 1,
                            indexesToIdentifier(i + 1),
                            this, this
                        )
                    )
                }
                is StepEntity.Group -> {
                    stepItems.add(
                        VisibleGroup(
                            stepEntity.name,
                            stepEntity.loop,
                            i + 1,
                            indexesToIdentifier(i + 1),
                            this
                        )
                    )
                    stepItems.addAll(
                        stepEntity.steps.mapIndexed { gi: Int, gse: StepEntity ->
                            val step = gse as StepEntity.Step
                            VisibleStep(
                                step,
                                gi + 1,
                                indexesToIdentifier(i + 1, gi),
                                this, this
                            )
                        }
                    )
                    stepItems.add(VisibleGroupEnd())
                }
            }
        }
        stepAdapter.add(stepItems)

        timer.endStep?.let {
            val stepCount = timer.steps.size
            endAdapter.add(
                VisibleStep(
                    it,
                    stepCount + 1,
                    indexesToIdentifier(stepCount + 1),
                    this, this
                )
            )
        }
    }

    /**
     * Converts a [VisibleStep] to its corresponding TimerIndex.
     * @return null if this is not valid [VisibleStep].
     */
    private fun VisibleStep.toIndex(): TimerIndex? {
        val (stepNumber, groupStepIndex) = identifier.toIndexes()
        fun getCurrentLoop(): Int = timerInstance.getTimerLoop(currentIndex)
        return when (stepNumber) {
            0 -> TimerIndex.Start
            timerInstance.steps.size + 1 -> TimerIndex.End
            else -> {
                when {
                    groupStepIndex == -1 ->
                        // normal step
                        TimerIndex.Step(loopIndex = getCurrentLoop(), stepIndex = stepNumber - 1)
                    groupStepIndex >= 0 ->
                        // group step
                        TimerIndex.Group(
                            loopIndex = getCurrentLoop(),
                            stepIndex = stepNumber - 1,
                            groupStepIndex = TimerIndex.Step(
                                loopIndex =
                                (currentIndex as? TimerIndex.Group)?.groupStepIndex?.loopIndex ?: 0,
                                stepIndex = groupStepIndex
                            )
                        )
                    else -> null
                }
            }
        }
    }

    private fun VisibleGroup.toIndex(): TimerIndex.Group {
        val (stepNumber, _) = identifier.toIndexes()
        val currentIndex = currentIndex
        return TimerIndex.Group(
            loopIndex = timerInstance.getTimerLoop(currentIndex),
            stepIndex = stepNumber - 1,
            groupStepIndex =
            if (currentIndex is TimerIndex.Group) currentIndex.groupStepIndex
            else TimerIndex.Step(0, 0)
        )
    }
}

private const val ANIMATION_DURATION = 75L

/**
 * @param stepNumber unique number for this step.
 */
private fun indexesToIdentifier(stepNumber: Int, groupStepIndex: Int = -1): Long {
    return (stepNumber.toLong() shl 32) or (groupStepIndex.toLong() and 0xFFFF_FFFFL)
}

private fun Long.toIndexes(): Pair<Int, Int> {
    return (this shr 32).toInt() to toInt()
}
