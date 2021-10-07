package xyz.aprildown.timer.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.repositories.SampleTimerRepository
import xyz.aprildown.timer.domain.usecases.timer.SampleTimerProvider

class SampleTimerProviderTest {
    private val repo: SampleTimerRepository = mock()

    @Test
    fun `get timer`() = runBlocking {
        val timer = TestData.fakeTimerSimpleA
        whenever(repo.getSampleTimer(any())).thenReturn(timer)
        val result = SampleTimerProvider(testCoroutineDispatcher, repo).execute(1)
        assertTrue(timer === result)
    }
}
