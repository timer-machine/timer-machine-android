package io.github.deweyreed.timer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import io.github.deweyreed.timer.utils.DynamicThemeDelegate
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.aprildown.theme.Theme
import xyz.aprildown.timer.app.base.data.DarkTheme
import xyz.aprildown.timer.app.base.data.PreferenceData.appTheme
import xyz.aprildown.timer.app.base.data.PreferenceData.useVoiceContent2
import xyz.aprildown.timer.app.base.utils.AppThemeUtils
import xyz.aprildown.timer.app.base.utils.LogToFileTree
import xyz.aprildown.timer.data.job.initJob
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import xyz.aprildown.timer.domain.usecases.home.TipManager
import xyz.aprildown.timer.domain.utils.AppConfig
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.domain.utils.Constants
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var prefRepo: PreferencesRepository

    @Inject
    lateinit var workerFactory: Lazy<HiltWorkerFactory>

    @Inject
    lateinit var appTracker: AppTracker

    @Inject
    lateinit var tipManager: TipManager

    override fun onCreate() {
        super.onCreate()

        migrateSharedPreferences()

        setUpAnalytics()
        setUpLogger()
        setUpAndroidJob()
        setUpTheme()
        setUpForFirstStart()
        setUpMissedTimerTip()
    }

    private fun setUpAnalytics() {
        appTracker.init(this)
    }

    private fun setUpLogger() {
        if (AppConfig.openDebug) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(LogToFileTree(this))
    }

    private fun setUpAndroidJob() {
        initJob()
    }

    private fun setUpTheme() {
        DarkTheme(this).applyAppCompatDelegate()
        Theme.init(
            context = this,
            themeRes = R.style.AppTheme,
            appIconRes = R.drawable.ic_launcher_round
        ) {
            colorPrimaryRes = R.color.colorPrimary
            colorPrimaryVariantRes = R.color.colorPrimaryVariant
            colorOnPrimaryRes = R.color.colorOnPrimary
            colorSecondaryRes = R.color.colorSecondary
            colorSecondaryVariantRes = R.color.colorSecondaryVariant
            colorOnSecondaryRes = R.color.colorOnSecondary
            colorStatusBarRes = R.color.colorStatusBar
            lightStatusByPrimary = true
        }
        Theme.installDelegates(DynamicThemeDelegate())
        // Theme.get().enabled = false
    }

    private fun setUpForFirstStart() {
        val themeMigrationKey = "new_theme"
        val iapMigrationKey = "new_iap"
        if (sharedPreferences.getBoolean(PREF_FIRST_START, true)) {
            sharedPreferences.edit {
                putBoolean(PREF_FIRST_START, false)
                putBoolean(themeMigrationKey, false)
                putBoolean(iapMigrationKey, false)
                putBoolean(PREF_SP_MIGRATED, true)
            }
            sharedPreferences.useVoiceContent2 = true
            if (BuildConfig.APPLICATION_ID != packageName) {
                appTracker.trackError(
                    IllegalStateException("Tampered ${BuildConfig.APPLICATION_ID} to $packageName")
                )
            }
        } else {
            // Imported since 4.2.0 and shouldn't be removed.
            if (sharedPreferences.getBoolean(themeMigrationKey, true)) {
                sharedPreferences.edit { putBoolean(themeMigrationKey, false) }
                AppThemeUtils.configAppTheme(this, appTheme)
            }
            // Imported since 5.7.0 and shouldn't be removed.
            if (sharedPreferences.getBoolean(iapMigrationKey, true)) {
                sharedPreferences.edit {
                    putBoolean(iapMigrationKey, false)
                    val hasOldSub =
                        sharedPreferences.getBoolean(Constants.PREF_HAS_ANY_PURCHASE, false)
                    putBoolean(Constants.PREF_HAS_PRO, hasOldSub)
                    putBoolean(Constants.PREF_HAS_BACKUP_SUB, hasOldSub)
                }
            }
        }
    }

    private fun migrateSharedPreferences() {
        if (sharedPreferences.getBoolean(PREF_SP_MIGRATED, false)) return

        val deviceStorageContext =
            ContextCompat.createDeviceProtectedStorageContext(this) ?: return
        val deviceStorageSp = PreferenceManager.getDefaultSharedPreferences(deviceStorageContext)
        if (!deviceStorageSp.contains(PREF_FIRST_START)) {
            // New user
            sharedPreferences.edit { putBoolean(PREF_SP_MIGRATED, true) }
            return
        }
        // Old user
        sharedPreferences.edit {
            copySharedPreferences(from = deviceStorageSp, editor = this)
            copySharedPreferences(
                from = deviceStorageContext.getSharedPreferences(
                    "app_reminder_pref_file",
                    Context.MODE_PRIVATE
                ),
                editor = this
            )
            putBoolean(PREF_SP_MIGRATED, true)
        }
    }

    private fun setUpMissedTimerTip() {
        runBlocking {
            if (prefRepo.getBoolean(Constants.PREF_HAS_RUNNING_TIMERS, false)) {
                prefRepo.setBoolean(Constants.PREF_HAS_RUNNING_TIMERS, false)
                tipManager.setTip(TipManager.TIP_MISSED_TIMER)
            }
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(
                DelegatingWorkerFactory().also { delegate ->
                    val factory = workerFactory.get()
                    if (factory != null) {
                        delegate.addFactory(factory)
                    }
                }
            )
            .run {
                if (AppConfig.openDebug) {
                    setMinimumLoggingLevel(Log.VERBOSE)
                } else {
                    this
                }
            }
            .build()
    }
}

private const val PREF_FIRST_START = "pref_first_start_app"
private const val PREF_SP_MIGRATED = "pref_sp_migrated"

private fun copySharedPreferences(from: SharedPreferences, editor: SharedPreferences.Editor) {
    from.all.forEach { (key, value) ->
        when (value) {
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is String -> editor.putString(key, value)
            is Set<*> -> editor.putStringSet(key, value.map { it.toString() }.toSet())
        }
    }
}
