package xyz.aprildown.timer.domain.entities

/**
 * Only for export and import to whole app's data.
 */

data class AppDataEntity(
    val folders: List<FolderEntity> = emptyList(),
    val timers: List<TimerEntity> = emptyList(),
    val notifier: StepEntity.Step? = null,
    val timerStamps: List<TimerStampEntity> = emptyList(),
    val schedulers: List<SchedulerEntity> = emptyList(),
    val prefs: Map<String, String> = emptyMap()
)
