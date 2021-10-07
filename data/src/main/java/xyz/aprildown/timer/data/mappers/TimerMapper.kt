package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.TimerData
import xyz.aprildown.timer.data.datas.TimerInfoData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import javax.inject.Inject

@Reusable
internal class TimerMapper @Inject constructor(
    val stepMapper: StepMapper,
    private val moreMapper: TimerMoreMapper
) : Mapper<TimerData, TimerEntity>() {

    private val stepOnlyMapper = stepMapper.stepOnlyMapper

    override fun mapFrom(from: TimerData): TimerEntity {
        return TimerEntity(
            id = from.id,
            name = from.name,
            loop = from.loop,
            steps = stepMapper.mapFrom(from.steps),
            startStep = from.startStep.let {
                return@let if (it == null) null else stepOnlyMapper.mapFrom(it)
            },
            endStep = from.endStep.let {
                return@let if (it == null) null else stepOnlyMapper.mapFrom(it)
            },
            more = moreMapper.mapFrom(from.more),
            folderId = from.folderId
        )
    }

    override fun mapTo(from: TimerEntity): TimerData {
        return TimerData(
            id = from.id,
            name = from.name,
            loop = from.loop,
            steps = stepMapper.mapTo(from.steps),
            startStep = from.startStep.let {
                return@let if (it == null) null else stepOnlyMapper.mapTo(it)
            },
            endStep = from.endStep.let {
                return@let if (it == null) null else stepOnlyMapper.mapTo(it)
            },
            more = moreMapper.mapTo(from.more),
            folderId = from.folderId
        )
    }
}

@Reusable
internal class TimerInfoMapper @Inject constructor() : Mapper<TimerInfoData, TimerInfo>() {
    override fun mapFrom(from: TimerInfoData): TimerInfo {
        return TimerInfo(
            id = from.id,
            name = from.name,
            folderId = from.folderId
        )
    }
}
