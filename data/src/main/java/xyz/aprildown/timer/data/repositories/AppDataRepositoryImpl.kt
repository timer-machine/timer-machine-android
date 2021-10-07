package xyz.aprildown.timer.data.repositories

import com.squareup.moshi.Moshi
import dagger.Reusable
import xyz.aprildown.timer.data.datas.AppDataData
import xyz.aprildown.timer.data.db.StepConverters
import xyz.aprildown.timer.data.json.BehaviourDataJsonAdapter
import xyz.aprildown.timer.data.json.TimerMoreDataJsonAdapter
import xyz.aprildown.timer.data.mappers.AppDataMapper
import xyz.aprildown.timer.domain.entities.AppDataEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import java.util.Optional
import javax.inject.Inject

@Reusable
internal class AppDataRepositoryImpl @Inject constructor(
    @AppDataRepository.BackupRepositoryQualifier
    private val backupRepository: Optional<AppDataRepository.BackupRepository>,
    private val mapper: AppDataMapper
) : AppDataRepository {

    private val jsonAdapter = Moshi.Builder()
        .add(BehaviourDataJsonAdapter())
        .add(StepConverters.getStepDataJsonAdapter())
        .add(TimerMoreDataJsonAdapter())
        .build()
        .adapter(AppDataData::class.java)

    override suspend fun collectData(appDataEntity: AppDataEntity): String {
        return jsonAdapter.toJson(mapper.mapTo(appDataEntity))
    }

    override suspend fun unParcelData(data: String): AppDataEntity? {
        return fromJson(data).let {
            if (it == null) null else mapper.mapFrom(it)
        }
    }

    private fun fromJson(data: String): AppDataData? = jsonAdapter.fromJson(data)

    override suspend fun notifyDataChanged() {
        backupRepository.orElse(null)?.onAppDataChanged()
    }
}
