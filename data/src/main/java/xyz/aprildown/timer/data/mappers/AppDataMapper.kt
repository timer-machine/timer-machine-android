package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.AppDataData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.AppDataEntity
import javax.inject.Inject

@Reusable
internal class AppDataMapper @Inject constructor(
    private val folderMapper: FolderMapper,
    private val timerMapper: TimerMapper,
    private val timerStampMapper: TimerStampMapper,
    private val schedulerMapper: SchedulerMapper
) : Mapper<AppDataData, AppDataEntity>() {

    private val stepOnlyMapper = timerMapper.stepMapper.stepOnlyMapper

    override fun mapFrom(from: AppDataData): AppDataEntity {
        return AppDataEntity(
            folders = from.folders.fromWithMapper(folderMapper),
            timers = timerMapper.mapFrom(from.timers),
            notifier = from.notifier.let {
                if (it == null) null else stepOnlyMapper.mapFrom(it)
            },
            timerStamps = timerStampMapper.mapFrom(from.timerStamps),
            schedulers = schedulerMapper.mapFrom(from.schedulers),
            prefs = from.prefs
        )
    }

    override fun mapTo(from: AppDataEntity): AppDataData {
        return AppDataData(
            folders = from.folders.toWithMapper(folderMapper),
            timers = timerMapper.mapTo(from.timers),
            notifier = from.notifier.let {
                if (it == null) null else stepOnlyMapper.mapTo(it)
            },
            timerStamps = timerStampMapper.mapTo(from.timerStamps),
            schedulers = schedulerMapper.mapTo(from.schedulers),
            prefs = from.prefs
        )
    }
}
