package xyz.aprildown.timer.data.repositories

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import xyz.aprildown.timer.data.mappers.AppDataMapper
import xyz.aprildown.timer.data.mappers.BehaviourMapper
import xyz.aprildown.timer.data.mappers.FolderMapper
import xyz.aprildown.timer.data.mappers.SchedulerMapper
import xyz.aprildown.timer.data.mappers.StepMapper
import xyz.aprildown.timer.data.mappers.StepOnlyMapper
import xyz.aprildown.timer.data.mappers.TimerMapper
import xyz.aprildown.timer.data.mappers.TimerMoreMapper
import xyz.aprildown.timer.data.mappers.TimerStampMapper
import xyz.aprildown.timer.domain.TestData
import java.util.Optional

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppDataRepositoryImplTest {

    private val appDataRepository = AppDataRepositoryImpl(
        backupRepository = Optional.empty(),
        mapper = AppDataMapper(
            FolderMapper(),
            TimerMapper(StepMapper(StepOnlyMapper(BehaviourMapper())), TimerMoreMapper()),
            TimerStampMapper(),
            SchedulerMapper()
        )
    )

    @Test
    fun test() = runBlocking {
        val ad = TestData.fakeAppData
        val json = appDataRepository.collectData(ad)
        assertEquals(ad, appDataRepository.unParcelData(json))
    }
}