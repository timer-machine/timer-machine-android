package xyz.aprildown.timer.domain.usecases.folder

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.toTimerInfo
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.TestPreferencesRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.timer.DeleteTimer
import kotlin.random.Random

class FolderUseCaseTest {
    private val folderRepo: FolderRepository = mock()
    private val appDataRepo: AppDataRepository = mock()

    private val folder = TestData.fakeFolder

    @Test
    fun `add host folder`() = runTest {
        val useCase = AddFolder(StandardTestDispatcher(testScheduler), folderRepo, appDataRepo)

        useCase(TestData.defaultFolder)
        useCase(TestData.trashFolder)

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `add empty name folder`() = runTest {
        val useCase = AddFolder(StandardTestDispatcher(testScheduler), folderRepo, appDataRepo)

        useCase(FolderEntity(TestData.fakeFolderId, ""))

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
    }

    @Test
    fun `add normal folder`() = runTest {
        whenever(folderRepo.addFolder(folder)).thenReturn(folder.id)

        val useCase = AddFolder(StandardTestDispatcher(testScheduler), folderRepo, appDataRepo)

        useCase(folder)
        verify(folderRepo).addFolder(folder)
        verify(appDataRepo).notifyDataChanged()

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
    }

    @Test
    fun `delete default folder`() = runTest {
        val timerRepo: TimerRepository = mock()
        val deleteTimer: DeleteTimer = mock()

        val useCase =
            DeleteFolder(
                StandardTestDispatcher(testScheduler),
                folderRepo,
                appDataRepo,
                timerRepo,
                deleteTimer
            )

        useCase(FolderEntity.FOLDER_DEFAULT)

        verify(timerRepo).moveFolderTimersToAnother(
            FolderEntity.FOLDER_DEFAULT,
            FolderEntity.FOLDER_TRASH
        )

        verify(appDataRepo).notifyDataChanged()

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
        verifyNoMoreInteractions(timerRepo)
        verifyNoMoreInteractions(deleteTimer)
    }

    @Test
    fun `delete trash folder`() = runTest {
        val timerRepo: TimerRepository = mock()
        val deleteTimer: DeleteTimer = mock()

        val timers = listOf(
            TestData.fakeTimerSimpleA,
            TestData.fakeTimerSimpleB,
            TestData.fakeTimerAdvanced
        )
        val timerInfo = timers.map { it.toTimerInfo() }
        whenever(timerRepo.getTimerInfo(FolderEntity.FOLDER_TRASH)).thenReturn(timerInfo)

        val useCase =
            DeleteFolder(
                StandardTestDispatcher(testScheduler),
                folderRepo,
                appDataRepo,
                timerRepo,
                deleteTimer
            )

        useCase(FolderEntity.FOLDER_TRASH)

        verify(timerRepo).getTimerInfo(FolderEntity.FOLDER_TRASH)
        timerInfo.forEach {
            verify(deleteTimer).invoke(it.id)
        }

        verify(appDataRepo).notifyDataChanged()

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
        verifyNoMoreInteractions(timerRepo)
        verifyNoMoreInteractions(deleteTimer)
    }

    @Test
    fun `delete normal folder`() = runTest {
        val timerRepo: TimerRepository = mock()
        val deleteTimer: DeleteTimer = mock()

        val useCase =
            DeleteFolder(
                StandardTestDispatcher(testScheduler),
                folderRepo,
                appDataRepo,
                timerRepo,
                deleteTimer
            )

        useCase(folder.id)

        verify(timerRepo).moveFolderTimersToAnother(
            folder.id,
            FolderEntity.FOLDER_TRASH
        )
        verify(folderRepo).deleteFolder(folder.id)

        verify(appDataRepo).notifyDataChanged()

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
        verifyNoMoreInteractions(timerRepo)
        verifyNoMoreInteractions(deleteTimer)
    }

    @Test
    fun `get the default folder sorting rule`() = runTest {
        val useCase = FolderSortByRule(
            dispatcher = StandardTestDispatcher(testScheduler),
            preferencesRepository = TestPreferencesRepository(),
        )
        assertEquals(useCase.get(), FolderSortBy.AddedOldest)
    }

    @Test
    fun `set the folder sorting rule`() = runTest {
        val useCase = FolderSortByRule(
            dispatcher = StandardTestDispatcher(testScheduler),
            preferencesRepository = TestPreferencesRepository(),
        )
        val value = FolderSortBy.entries.random()
        useCase.set(value)
        assertEquals(useCase.get(), value)
    }

    @Test
    fun `get folders`() = runTest {
        val folders = listOf(
            TestData.defaultFolder,
            TestData.trashFolder,
            TestData.fakeFolder,
        ).shuffled()
        whenever(folderRepo.getFolders()).thenReturn(folders)

        val useCase = GetFolders(StandardTestDispatcher(testScheduler), folderRepo)
        val result = useCase.invoke()
        assertEquals(
            listOf(
                TestData.defaultFolder,
                TestData.fakeFolder,
                TestData.trashFolder,
            ),
            result
        )

        verify(folderRepo).getFolders()

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
    }

    @Test
    fun `get the recent folder`() = runTest {
        val useCase = RecentFolder(
            dispatcher = StandardTestDispatcher(testScheduler),
            preferencesRepository = TestPreferencesRepository(),
        )
        assertEquals(useCase.get(), FolderEntity.FOLDER_DEFAULT)
    }

    @Test
    fun `set the recent folder`() = runTest {
        val useCase = RecentFolder(
            dispatcher = StandardTestDispatcher(testScheduler),
            preferencesRepository = TestPreferencesRepository(),
        )
        val value = Random.nextLong()
        useCase.set(value)
        assertEquals(useCase.get(), value)
    }

    @Test
    fun `update host folders`() = runTest {
        val useCase = UpdateFolder(StandardTestDispatcher(testScheduler), folderRepo, appDataRepo)

        useCase(TestData.defaultFolder)
        useCase(TestData.trashFolder)

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
    }

    @Test
    fun `update normal folders`() = runTest {
        whenever(folderRepo.updateFolder(folder)).thenReturn(true)
        val useCase = UpdateFolder(StandardTestDispatcher(testScheduler), folderRepo, appDataRepo)

        useCase(folder)

        verify(folderRepo).updateFolder(folder)
        verify(appDataRepo).notifyDataChanged()

        verifyNoMoreInteractions(folderRepo)
        verifyNoMoreInteractions(appDataRepo)
    }
}
