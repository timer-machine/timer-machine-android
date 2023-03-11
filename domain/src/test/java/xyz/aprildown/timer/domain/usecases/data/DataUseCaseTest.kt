package xyz.aprildown.timer.domain.usecases.data

import kotlinx.coroutines.runBlocking
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
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.testCoroutineDispatcher

class DataUseCaseTest {

    private val appDataRepository: AppDataRepository = mock()
    private val folderRepository: FolderRepository = mock()
    private val timerRepository: TimerRepository = mock()
    private val notifierRepository: NotifierRepository = mock()
    private val timerStampRepository: TimerStampRepository = mock()
    private val schedulerRepository: SchedulerRepository = mock()

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
    fun `export none`() = runBlocking {
        val exportAppData = setUpExportExpectation()
        whenever(
            appDataRepository.collectData(
                AppDataEntity(emptyList(), emptyList(), null, emptyList(), emptyList(), prefs)
            )
        ).thenReturn(result)
        val r = exportAppData.execute(
            ExportAppData.Params(
                exportTimers = false,
                exportTimerStamps = false,
                exportSchedulers = false,
                prefs = prefs
            )
        )
        assertEquals(result, r)

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(notifierRepository)
        verifyNoMoreInteractions(timerStampRepository)
        verifyNoMoreInteractions(schedulerRepository)
    }

    @Test
    fun `export timers`() = runBlocking {
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
                prefs = prefs
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
    }

    @Test
    fun `export timers and timer stamps`() = runBlocking {
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
                prefs = prefs
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
    }

    @Test
    fun `export timers and schedulers`() = runBlocking {
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
                prefs = prefs
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
    }

    @Test
    fun `export all`() = runBlocking {
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
                prefs = prefs
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
    }

    @Test(expected = IllegalArgumentException::class)
    fun `export timer stamps`() {
        ExportAppData.Params(
            exportTimers = false,
            exportTimerStamps = true,
            exportSchedulers = false,
            prefs = prefs
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `export schedulers`() {
        ExportAppData.Params(
            exportTimers = false,
            exportTimerStamps = false,
            exportSchedulers = true,
            prefs = prefs
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `export timer stamps and schedulers`() {
        ExportAppData.Params(
            exportTimers = false,
            exportTimerStamps = true,
            exportSchedulers = true,
            prefs = prefs
        )
    }

    private suspend fun setUpExportExpectation(): ExportAppData {
        whenever(folderRepository.getFolders()).thenReturn(folders)
        whenever(timerRepository.items()).thenReturn(timers)
        whenever(timerStampRepository.getAll()).thenReturn(timerStamps)
        whenever(notifierRepository.get()).thenReturn(notifier)
        whenever(schedulerRepository.items()).thenReturn(schedulers)
        return ExportAppData(
            testCoroutineDispatcher,
            appDataRepository,
            folderRepository,
            timerRepository,
            notifierRepository,
            timerStampRepository,
            schedulerRepository
        )
    }

    // endregion Export

    // region Import

    @Test
    fun `wipe nothing`() = runBlocking {
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = false,
                importTimerStamps = false,
                importSchedulers = false
            )
        )
        assertEquals(prefs, p)

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(notifierRepository)
        verifyNoMoreInteractions(timerStampRepository)
        verifyNoMoreInteractions(schedulerRepository)
    }

    @Test
    fun `wipe first`() = runBlocking {
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = false,
                importTimerStamps = false,
                importSchedulers = false
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import timers and don't wipe`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = false
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import timers and wipe first`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = false
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import timers and timer stamps and don't wipe`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = false
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import timers and timer stamps and wipe first`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = false
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import timers and schedulers and don't wipe`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = true
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import timers and schedulers and wipe first`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = false,
                importSchedulers = true
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import all and don't wipe`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = false,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = true
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test
    fun `import all and wipe first`() = runBlocking {
        val newFolderId = TestData.fakeFolderId
        whenever(folderRepository.addFolder(any())).thenReturn(newFolderId)
        val newTimerId = TestData.fakeTimerId
        whenever(timerRepository.add(any())).thenReturn(newTimerId)

        val importAppData = setUpImportExpectation()
        val p = importAppData.execute(
            ImportAppData.Params(
                data = result,
                wipeFirst = true,
                importTimers = true,
                importTimerStamps = true,
                importSchedulers = true
            )
        )
        assertEquals(prefs, p)

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
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and wipe first`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = true,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = false
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and don't wipe`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = false,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = false
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import schedulers and wipe first`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = true,
            importTimers = false,
            importTimerStamps = false,
            importSchedulers = true
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import schedulers and don't wipe`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = false,
            importTimers = false,
            importTimerStamps = false,
            importSchedulers = true
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and schedulers and wipe first`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = true,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = true
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import timer stamps and schedulers and don't wipe`() {
        ImportAppData.Params(
            data = "",
            wipeFirst = false,
            importTimers = false,
            importTimerStamps = true,
            importSchedulers = true
        )
    }

    private suspend fun setUpImportExpectation(): ImportAppData {
        whenever(folderRepository.getFolders()).thenReturn(folders)
        whenever(timerRepository.items()).thenReturn(timers)
        whenever(timerStampRepository.getAll()).thenReturn(timerStamps)
        whenever(schedulerRepository.items()).thenReturn(schedulers)
        whenever(appDataRepository.unParcelData(result)).thenReturn(
            AppDataEntity(folders, timers, notifier, timerStamps, schedulers, prefs)
        )
        return ImportAppData(
            testCoroutineDispatcher,
            appDataRepository,
            folderRepository,
            timerRepository,
            notifierRepository,
            timerStampRepository,
            schedulerRepository
        )
    }

    // endregion Import

    @Test
    fun `notify data changed`() = runBlocking {
        val useCase = NotifyDataChanged(appDataRepository)
        useCase.invoke()
        verify(appDataRepository).notifyDataChanged()
        verifyNoMoreInteractions(appDataRepository)
    }
}
