package xyz.aprildown.timer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import xyz.aprildown.timer.data.datas.FolderData
import xyz.aprildown.timer.data.datas.SchedulerData
import xyz.aprildown.timer.data.datas.TimerData
import xyz.aprildown.timer.data.datas.TimerInfoData
import xyz.aprildown.timer.data.datas.TimerStampData

@Dao
internal interface TimerDao {
    @Query("SELECT * FROM TimerItem ORDER BY id")
    suspend fun getTimers(): List<TimerData>

    @Query("SELECT * FROM TimerItem WHERE id = :id")
    suspend fun getTimer(id: Int): TimerData?

    @Query("SELECT id, name, folderId FROM TimerItem WHERE id = :timerId")
    suspend fun findTimerInfo(timerId: Int): TimerInfoData?

    @Query("SELECT id, name, folderId FROM TimerItem WHERE folderId = :folderId")
    suspend fun getTimerInfo(folderId: Long): List<TimerInfoData>

    @Query("SELECT id, name, folderId FROM TimerItem WHERE folderId = :folderId")
    fun getTimerInfoFlow(folderId: Long): Flow<List<TimerInfoData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTimer(timerItem: TimerData): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTimer(timerItem: TimerData): Int

    @Query("UPDATE TimerItem SET folderId = :folderId WHERE id = :timerId")
    suspend fun changeTimerFolder(timerId: Int, folderId: Long)

    @Transaction
    suspend fun moveFolderTimersToAnother(originalFolderId: Long, targetFolderId: Long) {
        getTimerInfo(originalFolderId).forEach { timerInfo ->
            changeTimerFolder(timerId = timerInfo.id, folderId = targetFolderId)
        }
    }

    @Query("DELETE FROM TimerItem WHERE id = :id")
    suspend fun deleteTimer(id: Int): Int
}

@Dao
internal interface FolderDao {
    @Query("SELECT * FROM Folder")
    suspend fun getFolders(): List<FolderData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFolder(item: FolderData): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateFolder(item: FolderData): Int

    @Query("DELETE FROM Folder WHERE id = :id")
    suspend fun deleteFolder(id: Long): Int
}

@Dao
internal interface SchedulerDao {
    @Query("SELECT * FROM TimerScheduler ORDER BY timerId")
    suspend fun getSchedulers(): List<SchedulerData>

    @Query("SELECT * FROM TimerScheduler WHERE id = :id")
    suspend fun getScheduler(id: Int): SchedulerData?

    @Query("SELECT * FROM TimerScheduler WHERE timerId = :id")
    suspend fun getSchedulersByTimerId(id: Int): List<SchedulerData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addScheduler(timerScheduler: SchedulerData): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateScheduler(timerScheduler: SchedulerData): Int

    @Query("UPDATE TimerScheduler SET enable = :enable WHERE id = :id")
    suspend fun setSchedulerEnable(id: Int, enable: Int): Int

    @Query("DELETE FROM TimerScheduler WHERE id = :id")
    suspend fun deleteScheduler(id: Int): Int
}

@Dao
internal interface TimerStampDao {

    @Query("SELECT * FROM TimerStamp ORDER BY date")
    suspend fun getTimerStamps(): List<TimerStampData>

    @Query("SELECT * FROM TimerStamp WHERE timerId = :timerId AND date >= :start AND date <= :end ORDER BY date")
    suspend fun getWithTimerIdAndSpan(timerId: Int, start: Long, end: Long): List<TimerStampData>

    @Query("SELECT * FROM TimerStamp WHERE timerId = :timerId ORDER BY date DESC LIMIT 1")
    suspend fun getRecentOne(timerId: Int): TimerStampData?

    @Query("SELECT * FROM TimerStamp ORDER BY date LIMIT 1")
    suspend fun getEarliestOne(): TimerStampData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(stamp: TimerStampData): Long

    @Query("DELETE FROM TimerStamp WHERE timerId = :timerId")
    suspend fun deleteWithTimerId(timerId: Int): Int

    @Query("DELETE FROM TimerStamp WHERE id = :id")
    suspend fun deleteTimerStamp(id: Int): Int
}
