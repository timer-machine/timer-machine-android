package xyz.aprildown.timer.presentation.timer

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.usecases.folder.AddFolder
import xyz.aprildown.timer.domain.usecases.folder.DeleteFolder
import xyz.aprildown.timer.domain.usecases.folder.FolderSortByRule
import xyz.aprildown.timer.domain.usecases.folder.GetFolders
import xyz.aprildown.timer.domain.usecases.folder.RecentFolder
import xyz.aprildown.timer.domain.usecases.folder.UpdateFolder
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.timer.AddTimer
import xyz.aprildown.timer.domain.usecases.timer.ChangeTimerFolder
import xyz.aprildown.timer.domain.usecases.timer.DeleteTimer
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.usecases.timer.GetTimerInfoFlow
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.testCoroutineDispatcher
import xyz.aprildown.tools.arch.Event

class TimerViewModelTest {

    @JvmField
    @Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val getTimerInfoFlow: GetTimerInfoFlow = mock()
    private val addTimer: AddTimer = mock()
    private val getTimer: GetTimer = mock()
    private val changeTimerFolder: ChangeTimerFolder = mock()
    private val deleteTimer: DeleteTimer = mock()

    private val getFolders: GetFolders = mock()
    private val addFolder: AddFolder = mock()
    private val updateFolder: UpdateFolder = mock()
    private val deleteFolder: DeleteFolder = mock()

    private val folderSortByRule: FolderSortByRule = mock()
    private val recentFolder: RecentFolder = mock()

    private val intentProvider: StreamMachineIntentProvider = mock()

    private val editObserver: Observer<Event<Int>> = mock()
    private val intentObserver: Observer<Event<Intent>> = mock()

    private val allFoldersObserver: Observer<List<FolderEntity>> = mock()
    private val currentFolderIdObserver: Observer<Long> = mock()

    private lateinit var viewModel: TimerViewModel

    private val folders = listOf(
        TestData.defaultFolder,
        TestData.trashFolder,
        TestData.fakeFolder
    ).shuffled()

    @Before
    fun setUp() = runBlocking {
        whenever(getFolders()).thenReturn(folders)
        whenever(recentFolder.get()).thenReturn(FolderEntity.FOLDER_DEFAULT)
        whenever(folderSortByRule.get()).thenReturn(FolderSortBy.values().random())

        viewModel = TimerViewModel(
            mainDispatcher = testCoroutineDispatcher,
            getTimerInfoFlow = getTimerInfoFlow,
            addTimer = addTimer,
            getTimer = getTimer,
            changeTimerFolder = changeTimerFolder,
            deleteTimer = { deleteTimer },
            shareTimer = mock(),
            getFolders = getFolders,
            addFolder = addFolder,
            updateFolder = updateFolder,
            deleteFolder = { deleteFolder },
            folderSortByRule = folderSortByRule,
            recentFolder = recentFolder,
            streamMachineIntentProvider = intentProvider,
            tipManager = mock(),
        )
        viewModel.editEvent.observeForever(editObserver)
        viewModel.intentEvent.observeForever(intentObserver)
        viewModel.allFolders.observeForever(allFoldersObserver)
        viewModel.currentFolderId.observeForever(currentFolderIdObserver)

        verify(getFolders).invoke()
        verify(allFoldersObserver).onChanged(folders)
        verify(recentFolder).get()
        verify(currentFolderIdObserver).onChanged(FolderEntity.FOLDER_DEFAULT)
        verify(folderSortByRule).get()

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun bind() = runBlocking {
        val bindIntent = Intent()
        whenever(intentProvider.bindIntent()).thenReturn(bindIntent)

        assert(bindIntent === viewModel.getBindIntent())

        verify(intentProvider).bindIntent()

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun start_pause() = runBlocking {
        val id = TestData.fakeTimerSimpleB.id
        val startIntent = Intent()
        whenever(intentProvider.startIntent(id)).thenReturn(startIntent)
        val pauseIntent = Intent()
        whenever(intentProvider.pauseIntent(id)).thenReturn(pauseIntent)

        viewModel.startPauseAction(id, StreamState.RESET)
        argumentCaptor<Event<Intent>> {
            verify(intentObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(startIntent === firstValue.peekContent())
        }
        viewModel.startPauseAction(id, StreamState.RUNNING)
        argumentCaptor<Event<Intent>> {
            verify(intentObserver, times(2)).onChanged(capture())
            assertEquals(2, allValues.size)
            assertTrue(startIntent === firstValue.peekContent())
            assertTrue(pauseIntent === secondValue.peekContent())
        }
        viewModel.startPauseAction(id, StreamState.PAUSED)
        argumentCaptor<Event<Intent>> {
            verify(intentObserver, times(3)).onChanged(capture())
            assertEquals(3, allValues.size)
            assertTrue(startIntent === firstValue.peekContent())
            assertTrue(pauseIntent === secondValue.peekContent())
            assertTrue(startIntent === thirdValue.peekContent())
        }

        verify(intentProvider, times(2)).startIntent(id)
        verify(intentProvider).pauseIntent(id)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun stop() = runBlocking {
        val id = TestData.fakeTimerSimpleB.id
        val resetIntent = Intent()
        whenever(intentProvider.resetIntent(id)).thenReturn(resetIntent)

        viewModel.stopAction(id, StreamState.RUNNING)
        argumentCaptor<Event<Intent>> {
            verify(intentObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(resetIntent === firstValue.peekContent())
        }
        viewModel.stopAction(id, StreamState.PAUSED)
        argumentCaptor<Event<Intent>> {
            verify(intentObserver, times(2)).onChanged(capture())
            assertEquals(2, allValues.size)
            assertTrue(resetIntent === firstValue.peekContent())
            assertTrue(resetIntent === secondValue.peekContent())
        }
        viewModel.stopAction(id, StreamState.RESET)
        verifyNoMoreInteractions(intentObserver)

        verify(intentProvider, times(2)).resetIntent(id)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun edit_new_and_old() = runBlocking {
        viewModel.addNewTimer()
        argumentCaptor<Event<Int>> {
            verify(editObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            assertTrue(TimerEntity.NEW_ID == firstValue.peekContent())
        }

        val id = TestData.fakeTimerId
        viewModel.openTimerEditScreen(id)
        argumentCaptor<Event<Int>> {
            verify(editObserver, times(2)).onChanged(capture())
            assertEquals(2, allValues.size)
            assertTrue(TimerEntity.NEW_ID == firstValue.peekContent())
            assertTrue(id == secondValue.peekContent())
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun duplicate() = runBlocking {
        val timer = TestData.fakeTimerAdvanced
        val timerId = timer.id
        whenever(getTimer(timerId)).thenReturn(timer)

        viewModel.duplicate(timerId).join()

        verify(getTimer).invoke(timerId)
        verify(addTimer).invoke(timer.copy(id = TimerEntity.NEW_ID))

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `duplicate none timer`() = runBlocking {
        val timerId = TestData.fakeTimerAdvanced.id
        whenever(getTimer(timerId)).thenReturn(null)

        viewModel.duplicate(timerId).join()

        verify(getTimer).invoke(timerId)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun delete() = runBlocking {
        val timerId = TestData.fakeTimerId

        viewModel.deleteTimer(timerId).join()

        verify(deleteTimer).invoke(timerId)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `change folder`() = runBlocking {
        val folderId = TestData.fakeFolderId

        viewModel.changeFolder(folderId)

        verify(currentFolderIdObserver).onChanged(folderId)
        verify(recentFolder).set(folderId)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `create new folder`() = runBlocking {
        val folder = TestData.fakeFolder
        val name = folder.name

        whenever(addFolder(folder.copy(id = FolderEntity.NEW_ID))).thenReturn(folder.id)
        whenever(getFolders()).thenReturn(folders)

        viewModel.createNewFolder(name)

        verify(addFolder).invoke(folder.copy(id = FolderEntity.NEW_ID))
        verify(getFolders, times(2)).invoke()
        verify(allFoldersObserver, times(2)).onChanged(folders)
        verify(currentFolderIdObserver).onChanged(folder.id)
        verify(recentFolder).set(folder.id)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `change folder name`() = runBlocking {
        val folder = TestData.fakeFolder
        val name = folder.name

        val folderId = folder.id
        viewModel.changeFolder(folderId)
        verify(currentFolderIdObserver, times(2)).onChanged(any())
        verify(recentFolder).set(folderId)

        viewModel.changeCurrentFolderName(name)

        verify(updateFolder).invoke(FolderEntity(folderId, name))
        verify(getFolders, times(2)).invoke()
        verify(allFoldersObserver, times(2)).onChanged(folders)
        verify(currentFolderIdObserver, times(2)).onChanged(folderId)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `change sort by`() = runBlocking {
        val sortBy = FolderSortBy.values().random()

        viewModel.changeSortBy(sortBy)

        verify(folderSortByRule).set(sortBy)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `move timer`() = runBlocking {
        val timerId = TestData.fakeTimerId
        val folderId = folders.random().id

        viewModel.moveTimerToFolder(timerId = timerId, folderId = folderId)

        verify(changeTimerFolder).invoke(
            ChangeTimerFolder.Params(
                timerId = timerId,
                folderId = folderId
            )
        )

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `delete the default folder`() = runBlocking {
        viewModel.deleteCurrentFolder()

        verify(deleteFolder).invoke(FolderEntity.FOLDER_DEFAULT)
        verify(getFolders, times(2)).invoke()
        verify(allFoldersObserver, times(2)).onChanged(folders)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `delete a normal folder`() = runBlocking {
        val folderId = TestData.fakeFolderId

        viewModel.changeFolder(folderId)
        verify(currentFolderIdObserver).onChanged(folderId)
        verify(recentFolder).set(folderId)

        viewModel.deleteCurrentFolder()

        verify(deleteFolder).invoke(folderId)
        verify(getFolders, times(2)).invoke()
        verify(allFoldersObserver, times(2)).onChanged(folders)
        verify(currentFolderIdObserver, times(2)).onChanged(FolderEntity.FOLDER_DEFAULT)
        verify(recentFolder).set(FolderEntity.FOLDER_DEFAULT)

        verifyNoMoreInteractionsForAll()
    }

    private fun verifyNoMoreInteractionsForAll() {
        verifyNoMoreInteractions(getTimerInfoFlow)
        verifyNoMoreInteractions(addTimer)
        verifyNoMoreInteractions(getTimer)
        verifyNoMoreInteractions(changeTimerFolder)
        verifyNoMoreInteractions(deleteTimer)
        verifyNoMoreInteractions(getFolders)
        verifyNoMoreInteractions(addFolder)
        verifyNoMoreInteractions(updateFolder)
        verifyNoMoreInteractions(deleteTimer)
        verifyNoMoreInteractions(folderSortByRule)
        verifyNoMoreInteractions(recentFolder)
        verifyNoMoreInteractions(intentProvider)
        verifyNoMoreInteractions(editObserver)
        verifyNoMoreInteractions(intentObserver)
        verifyNoMoreInteractions(allFoldersObserver)
        verifyNoMoreInteractions(currentFolderIdObserver)
    }
}
