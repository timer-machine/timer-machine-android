package xyz.aprildown.timer.presentation.stream.task

import xyz.aprildown.timer.presentation.stream.StreamState

internal interface TaskMaster {
    fun onTick(task: Task, time: Long)
    fun onTaskDone(task: Task)
}

internal abstract class TaskManager : TaskMaster {

    var currentTask: Task? = null
        private set
    val currentTaskState: StreamState
        get() = currentTask?.taskState ?: StreamState.RESET

    var beginTime: Long = 0
        private set

    abstract fun provideFirstTask(): Task?
    abstract fun provideNextTask(): Task?

    open fun onManagerBegin() {}
    open fun onManagerStart(newTask: Task) {}
    open fun onManagerPaused(pausedTask: Task) {}
    open fun onManagerTick(task: Task, time: Long) {}
    open fun onManagerDone(oldTask: Task) {}
    open fun onManagerNoMore() {}
    open fun onManagerStopped() {}

    fun start() {
        val current = currentTask
        if (beginTime == 0L) {
            beginTime = System.currentTimeMillis()
            onManagerBegin()
        }
        if (current != null) {
            if (!current.taskState.isRunning) {
                current.start()
                onManagerStart(current)
            }
        } else {
            val first = provideFirstTask()
            if (first != null) {
                currentTask = first
                first.start()
                onManagerStart(first)
            } else {
                onManagerNoMore()
            }
        }
    }

    fun pause() {
        currentTask?.let {
            it.pause()
            onManagerPaused(it)
        }
    }

    fun stop() {
        currentTask?.run {
            forceStop()
            currentTask = null
        }
        onManagerStopped()
    }

    fun interfere(newTask: Task) {
        val current = currentTask

        val isCurrentRunning = current?.taskState == StreamState.RUNNING
        current?.forceStop()

        currentTask = newTask

        if (isCurrentRunning) {
            newTask.start()
            onManagerStart(newTask)
        }
    }

    override fun onTick(task: Task, time: Long) {
        onManagerTick(task, time)
    }

    override fun onTaskDone(task: Task) {
        currentTask?.let {
            onManagerDone(it)
        }

        val next = provideNextTask()
        if (next != null) {
            currentTask = next
            next.start()
            onManagerStart(next)
        } else {
            onManagerNoMore()
        }
    }
}
