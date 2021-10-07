package xyz.aprildown.timer.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting
import androidx.core.content.contentValuesOf
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import xyz.aprildown.timer.data.R
import xyz.aprildown.timer.data.datas.FolderData
import xyz.aprildown.timer.data.datas.SchedulerData
import xyz.aprildown.timer.data.datas.StepData
import xyz.aprildown.timer.data.datas.TimerData
import xyz.aprildown.timer.data.datas.TimerStampData
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode

@Database(
    entities = [
        TimerData::class,
        FolderData::class,
        SchedulerData::class,
        TimerStampData::class,
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(
    StepConverters::class,
    TimerMoreConverters::class,
    BooleanConverters::class,
    SchedulerRepeatModeConverter::class,
)
abstract class MachineDatabase : RoomDatabase() {

    internal abstract fun timerDao(): TimerDao
    internal abstract fun folderDao(): FolderDao
    internal abstract fun schedulerDao(): SchedulerDao
    internal abstract fun timerStampDao(): TimerStampDao

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DB_NAME = "timer_db"

        private fun Builder<MachineDatabase>.addMyMigrations(context: Context): Builder<MachineDatabase> {
            addMigrations(getMigration1to2())
            addMigrations(getMigration2to3())
            addMigrations(getMigration3to4())
            addMigrations(getMigration4to5())
            addMigrations(getMigration5to6())
            addMigrations(getMigration6to7())
            addMigrations(getMigration7to8(context))
            return this
        }

        private fun Builder<MachineDatabase>.addCallback(context: Context): Builder<MachineDatabase> {
            return addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        addHostFolders(context, db)
                    }
                }
            )
        }

        private fun addHostFolders(context: Context, db: SupportSQLiteDatabase) {
            db.insert(
                "Folder",
                SQLiteDatabase.CONFLICT_IGNORE,
                contentValuesOf(
                    "id" to FolderEntity.FOLDER_DEFAULT,
                    "name" to context.getString(R.string.folder_default),
                )
            )
            db.insert(
                "Folder",
                SQLiteDatabase.CONFLICT_IGNORE,
                contentValuesOf(
                    "id" to FolderEntity.FOLDER_TRASH,
                    "name" to context.getString(R.string.folder_trash),
                )
            )
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun createInMemoryDatabase(context: Context): MachineDatabase {
            return Room.inMemoryDatabaseBuilder(context, MachineDatabase::class.java)
                .allowMainThreadQueries()
                .addMyMigrations(context)
                .addCallback(context)
                .build()
        }

        fun createPersistentDatabase(context: Context): MachineDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MachineDatabase::class.java, DB_NAME
            )
                .addMyMigrations(context)
                .addCallback(context)
                .build()
        }

        /**
         * Add [TimerData.startStep], [TimerData.endStep] and [StepData.Step.type]
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getMigration1to2(): Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE TimerItem ADD COLUMN startStep TEXT DEFAULT ''")
                database.execSQL("ALTER TABLE TimerItem ADD COLUMN endStep TEXT DEFAULT ''")
            }
        }

        /**
         * Add the missing proguard rules so we had to repopulate data.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getMigration2to3(): Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM TimerItem")
                database.execSQL("DELETE FROM TimerScheduler")
            }
        }

        /**
         * Add SchedulerEntity repeat mode field.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getMigration3to4(): Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE TimerScheduler ADD COLUMN repeatMode TEXT DEFAULT 'ONCE'")
                val converter = BooleanConverters()
                val repeatEveryWeekCv = ContentValues().apply {
                    put("repeatMode", SchedulerRepeatMode.EVERY_WEEK.name)
                }
                val repeatOnceCv = ContentValues().apply {
                    put("repeatMode", SchedulerRepeatMode.ONCE.name)
                }
                database.query("SELECT * FROM TimerScheduler").run {
                    if (moveToFirst()) {
                        while (!isAfterLast) {
                            val days =
                                converter.stringToBooleanList(getString(getColumnIndex("days")))
                            database.update(
                                "TimerScheduler",
                                SQLiteDatabase.CONFLICT_REPLACE,
                                if (days.any { it }) repeatEveryWeekCv else repeatOnceCv,
                                "id = ?",
                                arrayOf(getInt(getColumnIndex("id")))
                            )
                            moveToNext()
                        }
                    }
                }
            }
        }

        /**
         * Add the new TimerStamp table
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getMigration4to5(): Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `TimerStamp` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timerId` INTEGER NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`timerId`) REFERENCES `TimerItem`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE NO ACTION )"
                )
            }
        }

        /**
         * Add the foreign key between TimerEntity and TimerStamp
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getMigration5to6(): Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE INDEX `index_TimerStamp_timerId` ON `TimerStamp` (`timerId`)"
                )
            }
        }

        /**
         * Add the start column to TimerStampEntity.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getMigration6to7(): Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE TimerStamp ADD COLUMN start INTEGER DEFAULT 0 NOT NULL"
                )
            }
        }

        /**
         * Add [FolderData] and [TimerData.folderId].
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getMigration7to8(context: Context): Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE TimerItem " +
                        "ADD COLUMN `folderId` INTEGER NOT NULL DEFAULT ${FolderEntity.FOLDER_DEFAULT}"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Folder` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
                )
                addHostFolders(context, database)
            }
        }
    }
}
