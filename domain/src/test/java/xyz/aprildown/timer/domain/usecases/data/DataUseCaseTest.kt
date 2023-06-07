package xyz.aprildown.timer.domain.usecases.data

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.AppDataEntity
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.AppPreferencesProvider
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository

class DataUseCaseTest {

    private val appDataRepository: AppDataRepository = mock()
    private val folderRepository: FolderRepository = mock()
    private val timerRepository: TimerRepository = mock()
    private val notifierRepository: NotifierRepository = mock()
    private val timerStampRepository: TimerStampRepository = mock()
    private val schedulerRepository: SchedulerRepository = mock()
    private val appPreferencesProvider: AppPreferencesProvider = mock()

    private val appData = TestData.fakeAppData
    private val folders = appData.folders
    private val timers = appData.timers
    private val notifier = appData.notifier
    private val timerStamps = appData.timerStamps
    private val schedulers = appData.schedulers
    private val disabledSchedulers = schedulers.map { it.copy(enable = 0) }
    private val prefs = appData.prefs

    private val result = "Successful"

    // region Export

    @Test
    fun `export none`() = runTest {
        val exportAppData = setUpExportExpectation()
        whenever(
            appDataRepository.collectData(
                AppDataEntity(emptyList(), emptyList(), null, emptyList(), emptyList(), emptyMap())
            )
        ).thenReturn(result)
        val r = exportAppData.execute(
            ExportAppData.Params(
                exportTimers = false,
                exportTimerStamps = false,
                exportSchedulers = false,
                exportPreferences = false,
            )
        )
        assertEquals(result, r)

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(notifierRepository)
        verifyNoMoreInteractions(timerStampRepository)
        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `export timers`() = runTest {
        val exportAppData = setUpExportExpectation()
        whenever(
            appDataRepository.collectData(
                AppDataEntity(folders, timers, notifier, emptyList(), emptyList(), prefs)
            )
        ).thenReturn(result)
        val r = exportAppData.execute(
            ExportAppData.Params(
                exportTimers = true,
                exportTimerStamps = false,
                exportSchedulers = false,
                exportPreferences = true,
            )
        )
        assertEquals(result, r)

        verify(folderRepository).getFolders()
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).get()
        verifyNoMoreInteractions(notifierRepository)

        verifyNoMoreInteractions(timerStampRepository)

        verifyNoMoreInteractions(schedulerRepository)

        verify(appPreferencesProvider).getAppPreferences()
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `export timers and timer stamps`() = runTest {
        val exportAppData = setUpExportExpectation()
        whenever(
            appDataRepository.collectData(
                AppDataEntity(folders, timers, notifier, timerStamps, emptyList(), prefs)
            )
        ).thenReturn(result)
        val r = exportAppData.execute(
            ExportAppData.Params(
                exportTimers = true,
                exportTimerStamps = true,
                exportSchedulers = false,
                exportPreferences = true,
            )
        )
        assertEquals(result, r)

        verify(folderRepository).getFolders()
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).get()
        verifyNoMoreInteractions(notifierRepository)

        verify(timerStampRepository).getAll()
        verifyNoMoreInteractions(timerStampRepository)

        verifyNoMoreInteractions(schedulerRepository)

        verify(appPreferencesProvider).getAppPreferences()
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `export timers and schedulers`() = runTest {
        val exportAppData = setUpExportExpectation()
        whenever(
            appDataRepository.collectData(
                AppDataEntity(folders, timers, notifier, emptyList(), disabledSchedulers, prefs)
            )
        ).thenReturn(result)
        val r = exportAppData.execute(
            ExportAppData.Params(
                exportTimers = true,
                exportTimerStamps = false,
                exportSchedulers = true,
                exportPreferences = true,
            )
        )
        assertEquals(result, r)

        verify(folderRepository).getFolders()
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).get()
        verifyNoMoreInteractions(notifierRepository)

        verifyNoMoreInteractions(timerStampRepository)

        verify(schedulerRepository).items()
        verifyNoMoreInteractions(schedulerRepository)

        verify(appPreferencesProvider).getAppPreferences()
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `export all`() = runTest {
        val exportAppData = setUpExportExpectation()
        whenever(
            appDataRepository.collectData(
                AppDataEntity(folders, timers, notifier, timerStamps, disabledSchedulers, prefs)
            )
        ).thenReturn(result)
        val r = exportAppData.execute(
            ExportAppData.Params(
                exportTimers = true,
                exportTimerStamps = true,
                exportSchedulers = true,
                exportPreferences = true,
            )
        )
        assertEquals(result, r)

        verify(folderRepository).getFolders()
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).get()
        verifyNoMoreInteractions(notifierRepository)

        verify(timerStampRepository).getAll()
        verifyNoMoreInteractions(timerStampRepository)

        verify(schedulerRepository).items()
        verifyNoMoreInteractions(schedulerRepository)

        verify(appPreferencesProvider).getAppPreferences()
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `export timer stamps`() {
        ExportAppData.Params(
            exportTimers = false,
            exportTimerStamps = true,
            exportSchedulers = false,
            exportPreferences = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `export schedulers`() {
        ExportAppData.Params(
            exportTimers = false,
            exportTimerStamps = false,
            exportSchedulers = true,
            exportPreferences = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `export timer stamps and schedulers`() {
        ExportAppData.Params(
            exportTimers = false,
            exportTimerStamps = true,
            exportSchedulers = true,
            exportPreferences = false,
        )
    }

    private suspend fun TestScope.setUpExportExpectation(): ExportAppData {
        whenever(folderRepository.getFolders()).thenReturn(folders)
        whenever(timerRepository.items()).thenReturn(timers)
        whenever(timerStampRepository.getAll()).thenReturn(timerStamps)
        whenever(notifierRepository.get()).thenReturn(notifier)
        whenever(schedulerRepository.items()).thenReturn(schedulers)
        whenever(appPreferencesProvider.getAppPreferences()).thenReturn(prefs)
        return ExportAppData(
            StandardTestDispatcher(testScheduler),
            appDataRepository,
            folderRepository,
            timerRepository,
            notifierRepository,
            timerStampRepository,
            schedulerRepository,
            appPreferencesProvider,
        )
    }

    // endregion Export

    // region Import

    @Test
    fun `wipe nothing`() = runTest {
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = false,
                importTimerStamps = false,
                importSchedulers = false,
                importPreferences = false,
            )
        )

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(notifierRepository)
        verifyNoMoreInteractions(timerStampRepository)
        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `wipe first`() = runTest {
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = false,
                importTimerStamps = false,
                importSchedulers = false,
                importPreferences = false,
            )
        )

        verify(folderRepository).getFolders()
        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).deleteFolder(it.id)
            }
        }
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        timers.forEach {
            verify(timerRepository).delete(it.id)
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(null)
        verifyNoMoreInteractions(notifierRepository)

        verify(timerStampRepository).getAll()
        timerStamps.forEach {
            verify(timerStampRepository).delete(it.id)
        }
        verifyNoMoreInteractions(timerStampRepository)

        verify(schedulerRepository).items()
        schedulers.forEach {
            verify(schedulerRepository).delete(it.id)
        }
        verifyNoMoreInteractions(schedulerRepository)

        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import timers and don't wipe`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = false,
                importPreferences = false,
            )
        )

        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        verifyNoMoreInteractions(timerStampRepository)

        verifyNoMoreInteractions(schedulerRepository)

        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import timers and wipe first`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = false,
                importPreferences = false,
            )
        )

        verify(folderRepository).getFolders()
        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).deleteFolder(it.id)
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).delete(it.id)
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(null)
        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        verify(timerStampRepository).getAll()
        timerStamps.forEach {
            verify(timerStampRepository).delete(it.id)
        }
        verifyNoMoreInteractions(timerStampRepository)

        verify(schedulerRepository).items()
        schedulers.forEach {
            verify(schedulerRepository).delete(it.id)
        }
        verifyNoMoreInteractions(schedulerRepository)

        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import timers and timer stamps and don't wipe`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = false,
                importPreferences = false,
            )
        )

        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        timerStamps.forEach {
            verify(timerStampRepository).add(
                it.copy(id = TimerStampEntity.NEW_ID, timerId = newTimerId)
            )
        }
        verifyNoMoreInteractions(timerStampRepository)

        verifyNoMoreInteractions(schedulerRepository)

        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import timers and timer stamps and wipe first`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = false,
                importPreferences = false,
            )
        )

        verify(folderRepository).getFolders()
        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).deleteFolder(it.id)
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).delete(it.id)
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(null)
        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        verify(timerStampRepository).getAll()
        timerStamps.forEach {
            verify(timerStampRepository).delete(it.id)
        }
        timerStamps.forEach {
            verify(timerStampRepository).add(
                it.copy(id = TimerStampEntity.NEW_ID, timerId = newTimerId)
            )
        }
        verifyNoMoreInteractions(timerStampRepository)

        verify(schedulerRepository).items()
        schedulers.forEach {
            verify(schedulerRepository).delete(it.id)
        }
        verifyNoMoreInteractions(schedulerRepository)

        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import timers and schedulers and don't wipe`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = true,
                importPreferences = false,
            )
        )

        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        verifyNoMoreInteractions(timerStampRepository)

        schedulers.forEach {
            verify(schedulerRepository).add(
                it.copy(id = SchedulerEntity.NEW_ID, timerId = newTimerId, enable = 0)
            )
        }
        verifyNoMoreInteractions(schedulerRepository)

        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import timers and schedulers and wipe first`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = true,
                importPreferences = false,
            )
        )

        verify(folderRepository).getFolders()
        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).deleteFolder(it.id)
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).delete(it.id)
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(null)
        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        verify(timerStampRepository).getAll()
        timerStamps.forEach {
            verify(timerStampRepository).delete(it.id)
        }
        verifyNoMoreInteractions(timerStampRepository)

        verify(schedulerRepository).items()
        schedulers.forEach {
            verify(schedulerRepository).delete(it.id)
            verify(schedulerRepository).add(
                it.copy(id = SchedulerEntity.NEW_ID, timerId = newTimerId, enable = 0)
            )
        }
        verifyNoMoreInteractions(schedulerRepository)

        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import all and don't wipe`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = true,
                importPreferences = true,
            )
        )

        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        timerStamps.forEach {
            verify(timerStampRepository).add(
                it.copy(id = TimerStampEntity.NEW_ID, timerId = newTimerId)
            )
        }
        verifyNoMoreInteractions(timerStampRepository)

        schedulers.forEach {
            verify(schedulerRepository).add(
                it.copy(id = SchedulerEntity.NEW_ID, timerId = newTimerId, enable = 0)
            )
        }
        verifyNoMoreInteractions(schedulerRepository)

        verify(appPreferencesProvider).applyAppPreferences(prefs)
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test
    fun `import all and wipe first`() = runTest {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = true,
                importPreferences = true,
            )
        )

        verify(folderRepository).getFolders()
        folders.forEach {
            if (!it.isTrash && !it.isDefault) {
                verify(folderRepository).deleteFolder(it.id)
                verify(folderRepository).addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }
        verifyNoMoreInteractions(folderRepository)

        verify(timerRepository).items()
        timers.forEach {
            val folderId = it.folderId
            verify(timerRepository).delete(it.id)
            verify(timerRepository).add(
                it.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = if (folderId == FolderEntity.FOLDER_DEFAULT ||
                        folderId == FolderEntity.FOLDER_TRASH
                    ) {
                        folderId
                    } else {
                        newFolderId
                    }
                )
            )
        }
        verifyNoMoreInteractions(timerRepository)

        verify(notifierRepository).set(null)
        verify(notifierRepository).set(notifier)
        verifyNoMoreInteractions(notifierRepository)

        verify(timerStampRepository).getAll()
        timerStamps.forEach {
            verify(timerStampRepository).delete(it.id)
        }
        timerStamps.forEach {
            verify(timerStampRepository).add(
                it.copy(id = TimerStampEntity.NEW_ID, timerId = newTimerId)
            )
        }
        verifyNoMoreInteractions(timerStampRepository)

        verify(schedulerRepository).items()
        schedulers.forEach {
            verify(schedulerRepository).delete(it.id)
            verify(schedulerRepository).add(
                it.copy(id = SchedulerEntity.NEW_ID, timerId = newTimerId, enable = 0)
            )
        }
        verifyNoMoreInteractions(schedulerRepository)

        verify(appPreferencesProvider).applyAppPreferences(prefs)
        verifyNoMoreInteractions(appPreferencesProvider)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and wipe first`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = true,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = false,
            importPreferences = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and don't wipe`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = false,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = false,
            importPreferences = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import schedulers and wipe first`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = true,
            importTimers = false,
            importTimerStamps = false,
            importSchedulers = true,
            importPreferences = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import schedulers and don't wipe`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = false,
            importTimers = false,
            importTimerStamps = false,
            importSchedulers = true,
            importPreferences = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and schedulers and wipe first`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = true,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = true,
            importPreferences = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and schedulers and don't wipe`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = false,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = true,
            importPreferences = false,
        )
    }

    private suspend fun TestScope.setUpImportExpectation(): ImportAppData {
        whenever(folderRepository.getFolders()).thenReturn(folders)
        whenever(timerRepository.items()).thenReturn(timers)
        whenever(timerStampRepository.getAll()).thenReturn(timerStamps)
        whenever(schedulerRepository.items()).thenReturn(schedulers)
        whenever(appDataRepository.unParcelData(result)).thenReturn(
            AppDataEntity(folders, timers, notifier, timerStamps, schedulers, prefs)
        )
        return ImportAppData(
            StandardTestDispatcher(testScheduler),
            appDataRepository,
            folderRepository,
            timerRepository,
            notifierRepository,
            timerStampRepository,
            schedulerRepository,
            appPreferencesProvider,
        )
    }

    // endregion Import
}
