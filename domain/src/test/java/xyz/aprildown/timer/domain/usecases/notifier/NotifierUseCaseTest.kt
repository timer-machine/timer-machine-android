package xyz.aprildown.timer.domain.usecases.notifier

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.testCoroutineDispatcher
import xyz.aprildown.timer.domain.usecases.invoke

class NotifierUseCaseTest {
    private val notifierRepository: NotifierRepository = mock()
    private val appDataRepository: AppDataRepository = mock()

    private var step = TestData.fakeStepA

    @Test
    fun get() = runBlocking {
        whenever(notifierRepository.get()).thenReturn(step)
        assertEquals(step, GetNotifier(testCoroutineDispatcher, notifierRepository).invoke())
        verify(notifierRepository).get()

        verifyNoMoreInteractions(notifierRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun save() = runBlocking {
        whenever(notifierRepository.set(step)).thenReturn(true)
        val useCase = SaveNotifier(testCoroutineDispatcher, notifierRepository, appDataRepository)

        assertTrue(useCase.execute(step))
        verify(notifierRepository).set(step)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(notifierRepository)
        verifyNoMoreInteractions(appDataRepository)
    }
}
