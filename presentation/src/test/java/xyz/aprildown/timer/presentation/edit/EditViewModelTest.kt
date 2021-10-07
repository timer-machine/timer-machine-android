package xyz.aprildown.timer.presentation.edit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.TimerMoreEntity
import xyz.aprildown.timer.domain.entities.toTimerInfo
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.notifier.GetNotifier
import xyz.aprildown.timer.domain.usecases.notifier.SaveNotifier
import xyz.aprildown.timer.domain.usecases.timer.AddTimer
import xyz.aprildown.timer.domain.usecases.timer.DeleteTimer
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.usecases.timer.SampleTimerProvider
import xyz.aprildown.timer.domain.usecases.timer.SaveTimer
import xyz.aprildown.timer.presentation.R
import xyz.aprildown.timer.presentation.testCoroutineDispatcher
import xyz.aprildown.tools.arch.Event

class EditViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val addTimer: AddTimer = mock()
    private val saveTimer: SaveTimer = mock()
    private val deleteTimer: DeleteTimer = mock()
    private val getTimer: GetTimer = mock()
    private val findTimerInfo: FindTimerInfo = mock()
    private val getNotifier: GetNotifier = mock()
    private val saveNotifier: SaveNotifier = mock()
    private val sampleTimerProvider: SampleTimerProvider = mock()

    private val snackMessageObserver: Observer<Event<Int>> = mock()
    private val timerUpdatedObserver: Observer<Event<Unit>> = mock()
    private val startEndLoaderObserver: Observer<Event<Pair<StepEntity?, StepEntity?>>> = mock()
    private val timerInfoObserver: Observer<Event<TimerInfo?>> = mock()

    private lateinit var viewModel: EditViewModel

    @Before
    fun setUp() {
        viewModel = EditViewModel(
            mainDispatcher = testCoroutineDispatcher,
            addTimer = addTimer,
            saveTimer = saveTimer,
            deleteTimer = { deleteTimer },
            getTimer = getTimer,
            findTimerInfo = findTimerInfo,
            getNotifier = getNotifier,
            saveNotifier = saveNotifier,
            defaultName = "Test Default Name",
            sampleTimerProviderProvider = { sampleTimerProvider },
            shareTimer = mock(),
        )
        viewModel.message.observeForever(snackMessageObserver)
        viewModel.updatedEvent.observeForever(timerUpdatedObserver)
        viewModel.startEndEvent.observeForever(startEndLoaderObserver)
        viewModel.timerInfoEvent.observeForever(timerInfoObserver)
    }

    @Test
    fun notifier() = runBlocking {
        whenever(getNotifier()).thenReturn(TestData.fakeStepC)
        viewModel.loadStoredNotifierStep().join()
        assertEquals(viewModel.notifier, TestData.fakeStepC)
        verify(getNotifier).invoke()
        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun add_wrongCases() = runBlocking {
        viewModel.init(TimerEntity.NEW_ID, FolderEntity.FOLDER_DEFAULT)
        assertTrue(viewModel.isNewTimer)

        val result = viewModel.loadTimerData()
        assertNull(result)

        val new = TestData.fakeTimerAdvanced

        whenever(addTimer(new.copy(id = TimerEntity.NEW_ID))).thenReturn(new.id)
        viewModel.name.value = ""
        viewModel.saveTimer(new.steps)
        argumentCaptor<Event<Int>> {
            verify(snackMessageObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(R.string.edit_wrong_empty_name == firstValue.peekContent())
        }

        // Right name
        viewModel.name.value = new.name

        viewModel.loop.value = -1
        viewModel.saveTimer(new.steps)
        argumentCaptor<Event<Int>> {
            verify(snackMessageObserver, times(2)).onChanged(capture())
            assertEquals(2, allValues.size)
            assertTrue(R.string.edit_wrong_empty_name == firstValue.peekContent())
            assertTrue(R.string.edit_wrong_negative_loop == secondValue.peekContent())
        }

        // Right loop
        viewModel.loop.value = new.loop

        viewModel.saveTimer(listOf())
        argumentCaptor<Event<Int>> {
            verify(snackMessageObserver, times(3)).onChanged(capture())
            assertEquals(3, allValues.size)
            assertTrue(R.string.edit_wrong_empty_name == firstValue.peekContent())
            assertTrue(R.string.edit_wrong_negative_loop == secondValue.peekContent())
            assertTrue(R.string.edit_wrong_empty_steps == thirdValue.peekContent())
        }

        verifyZeroInteractions(timerUpdatedObserver)

        viewModel.more.value = new.more
        viewModel.saveTimer(new.steps, new.startStep, new.endStep)?.join()
        verify(addTimer).invoke(new.copy(id = TimerEntity.NEW_ID))
        argumentCaptor<Event<Unit>> {
            verify(timerUpdatedObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertEquals(Unit, firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun save() = runBlocking {
        val old = TestData.fakeTimerSimpleA
        val new = TestData.fakeTimerAdvanced.copy(id = old.id)

        whenever(getTimer(old.id)).thenReturn(old)
        whenever(saveTimer(new)).thenReturn(true)

        viewModel.init(old.id, old.folderId)
        assertFalse(viewModel.isNewTimer)

        viewModel.loadTimerData()?.join()
        verify(getTimer).invoke(old.id)
        assertEquals(old.name, viewModel.name.value)
        assertEquals(old.loop, viewModel.loop.value?.toInt())
        assertEquals(old.steps, viewModel.stepsEvent.value?.peekContent())
        argumentCaptor<Event<Pair<StepEntity?, StepEntity?>>> {
            verify(startEndLoaderObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(Pair(old.startStep, old.endStep) == firstValue.peekContent())
        }

        viewModel.name.value = new.name
        viewModel.loop.value = new.loop
        viewModel.more.value = new.more
        viewModel.saveTimer(new.steps, new.startStep, new.endStep)?.join()
        verify(saveTimer).invoke(new)
        argumentCaptor<Event<Unit>> {
            verify(timerUpdatedObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertEquals(Unit, firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `load timer info`() = runBlocking {
        val timer = TestData.fakeTimerSimpleA
        val timerId = timer.id
        whenever(findTimerInfo.invoke(timerId)).thenReturn(timer.toTimerInfo())

        viewModel.requestTimerInfoByTimerId(timerId).join()

        verify(findTimerInfo).invoke(timerId)
        argumentCaptor<Event<TimerInfo?>> {
            verify(timerInfoObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertEquals(timer.toTimerInfo(), firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun delete() = runBlocking {
        val timer = TestData.fakeTimerSimpleA

        viewModel.init(timer.id, timer.folderId)
        viewModel.deleteTimer().join()

        verify(deleteTimer).invoke(timer.id)
        argumentCaptor<Event<Unit>> {
            verify(timerUpdatedObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertEquals(Unit, firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same new`() = runBlocking {
        viewModel.init(TimerEntity.NEW_ID, FolderEntity.FOLDER_DEFAULT)
        viewModel.loadTimerData()?.join()
        val newSteps = listOf(StepEntity.Step("", 60_000, listOf()))
        assertTrue(viewModel.isTimerRemainingSame(newSteps))

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same new edit name`() = runBlocking {
        viewModel.init(TimerEntity.NEW_ID, FolderEntity.FOLDER_DEFAULT)
        viewModel.loadTimerData()
        viewModel.name.value = "abc"
        val newSteps = listOf(StepEntity.Step("", 60_000, listOf()))
        assertFalse(viewModel.isTimerRemainingSame(newSteps))

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same test new edit loop`() = runBlocking {
        viewModel.init(TimerEntity.NEW_ID, FolderEntity.FOLDER_DEFAULT)
        viewModel.loadTimerData()
        viewModel.loop.value = 1
        val newSteps = listOf(StepEntity.Step("", 60_000, listOf()))
        assertFalse(viewModel.isTimerRemainingSame(newSteps))

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same new edit more`() = runBlocking {
        viewModel.init(TimerEntity.NEW_ID, FolderEntity.FOLDER_DEFAULT)
        viewModel.loadTimerData()
        viewModel.more.value = TimerMoreEntity(showNotif = false, notifCount = false)
        val newSteps = listOf(StepEntity.Step("", 60_000, listOf()))
        assertFalse(viewModel.isTimerRemainingSame(newSteps))

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same new edit steps`() = runBlocking {
        viewModel.init(TimerEntity.NEW_ID, FolderEntity.FOLDER_DEFAULT)
        viewModel.loadTimerData()
        assertFalse(viewModel.isTimerRemainingSame(listOf(StepEntity.Step("", 60_001, listOf()))))
        assertFalse(
            viewModel.isTimerRemainingSame(
                listOf(StepEntity.Step("a", 60_000, listOf(BehaviourEntity(BehaviourType.MUSIC))))
            )
        )
        assertFalse(
            viewModel.isTimerRemainingSame(
                listOf(StepEntity.Step("", 60_000, listOf())),
                TestData.fakeStepA, TestData.fakeStepB
            )
        )

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same old`() = runBlocking {
        val timer = TestData.fakeTimerAdvanced
        whenever(getTimer(timer.id)).thenReturn(timer)
        viewModel.init(timer.id, timer.folderId)
        viewModel.loadTimerData()?.join()
        assertTrue(viewModel.isTimerRemainingSame(timer.steps, timer.startStep, timer.endStep))

        verify(getTimer).invoke(timer.id)
        argumentCaptor<Event<Pair<StepEntity?, StepEntity?>>> {
            verify(startEndLoaderObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(timer.startStep to timer.endStep == firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same old edit name`() = runBlocking {
        val timer = TestData.fakeTimerAdvanced
        whenever(getTimer(timer.id)).thenReturn(timer)
        viewModel.init(timer.id, timer.folderId)
        viewModel.loadTimerData()?.join()
        viewModel.name.value = "a"
        assertFalse(viewModel.isTimerRemainingSame(timer.steps, timer.startStep, timer.endStep))

        verify(getTimer).invoke(timer.id)
        argumentCaptor<Event<Pair<StepEntity?, StepEntity?>>> {
            verify(startEndLoaderObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(timer.startStep to timer.endStep == firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same old edit loop`() = runBlocking {
        val timer = TestData.fakeTimerAdvanced
        whenever(getTimer(timer.id)).thenReturn(timer)
        viewModel.init(timer.id, timer.folderId)
        viewModel.loadTimerData()?.join()
        viewModel.loop.value = 24
        assertFalse(viewModel.isTimerRemainingSame(timer.steps, timer.startStep, timer.endStep))

        verify(getTimer).invoke(timer.id)
        argumentCaptor<Event<Pair<StepEntity?, StepEntity?>>> {
            verify(startEndLoaderObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(timer.startStep to timer.endStep == firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same old edit more`() = runBlocking {
        val timer = TestData.fakeTimerAdvanced
        whenever(getTimer(timer.id)).thenReturn(timer)
        viewModel.init(timer.id, timer.folderId)
        viewModel.loadTimerData()?.join()
        viewModel.more.value = TestData.fakeTimerMoreA
        assertFalse(viewModel.isTimerRemainingSame(timer.steps, timer.startStep, timer.endStep))

        verify(getTimer).invoke(timer.id)
        argumentCaptor<Event<Pair<StepEntity?, StepEntity?>>> {
            verify(startEndLoaderObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(timer.startStep to timer.endStep == firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `same old edit steps`() = runBlocking {
        val timer = TestData.fakeTimerAdvanced
        whenever(getTimer(timer.id)).thenReturn(timer)
        viewModel.init(timer.id, timer.folderId)
        viewModel.loadTimerData()?.join()
        assertFalse(viewModel.isTimerRemainingSame(listOf()))
        assertFalse(
            viewModel.isTimerRemainingSame(listOf(), TestData.fakeStepA, TestData.fakeStepB)
        )
        assertFalse(
            viewModel.isTimerRemainingSame(listOf(), TestData.fakeStepB, TestData.fakeStepC)
        )
        assertFalse(
            viewModel.isTimerRemainingSame(timer.steps.dropLast(1), timer.startStep, timer.endStep)
        )

        verify(getTimer).invoke(timer.id)
        argumentCaptor<Event<Pair<StepEntity?, StepEntity?>>> {
            verify(startEndLoaderObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(timer.startStep to timer.endStep == firstValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun saveNotifier() = runBlocking {
        val step = TestData.fakeStepA
        whenever(saveNotifier.invoke(step)).thenReturn(true)
        viewModel.notifier = step
        viewModel.saveNotifierStep().join()
        verify(saveNotifier).invoke(step)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `don't save notifier if not changed`() = runBlocking {
        viewModel.saveNotifierStep().join()
        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `populate sample timer`() = runBlocking {
        val id = TestData.fakeTimerId
        val timer = TestData.fakeTimerAdvanced
        whenever(sampleTimerProvider.invoke(id)).thenReturn(timer)
        viewModel.loadSampleTimer(id)?.join()
        assertEquals(timer.name, viewModel.name.value)
        assertEquals(timer.loop, viewModel.loop.value?.toInt())
        assertEquals(timer.steps, viewModel.stepsEvent.value?.peekContent())
        argumentCaptor<Event<Pair<StepEntity?, StepEntity?>>> {
            verify(startEndLoaderObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(timer.startStep to timer.endStep == firstValue.peekContent())
        }
        assertEquals(timer.more, viewModel.more.value)

        verify(sampleTimerProvider).invoke(id)

        verifyNoMoreInteractionsForAll()
    }

    private fun verifyNoMoreInteractionsForAll() {
        verifyNoMoreInteractions(addTimer)
        verifyNoMoreInteractions(saveTimer)
        verifyNoMoreInteractions(deleteTimer)
        verifyNoMoreInteractions(getTimer)
        verifyNoMoreInteractions(findTimerInfo)
        verifyNoMoreInteractions(getNotifier)
        verifyNoMoreInteractions(saveNotifier)
        verifyNoMoreInteractions(sampleTimerProvider)
        verifyNoMoreInteractions(snackMessageObserver)
        verifyNoMoreInteractions(timerUpdatedObserver)
        verifyNoMoreInteractions(startEndLoaderObserver)
        verifyNoMoreInteractions(timerInfoObserver)
    }
}
