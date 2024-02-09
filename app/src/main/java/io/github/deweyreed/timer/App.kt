package io.github.deweyreed.timer

import android.Manifest
import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.preference.PreferenceManager
import androidx.work.DelegatingWorkerFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.github.deweyreed.tools.helper.hasPermissions
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import io.github.deweyreed.timer.utils.DynamicThemeDelegate
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.aprildown.theme.Theme
import xyz.aprildown.timer.app.base.data.DarkTheme
import xyz.aprildown.timer.app.base.data.PreferenceData.appTheme
import xyz.aprildown.timer.app.base.data.PreferenceData.disablePhoneCallBehavior
import xyz.aprildown.timer.app.base.data.PreferenceData.shouldPausePhoneCall
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
import androidx.work.Configuration as WorkManagerConfiguration
import xyz.aprildown.timer.app.base.R as RBase

@HiltAndroidApp
class App : Application(), WorkManagerConfiguration.Provider, ImageLoaderFactory {

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppThemeUtils.syncDynamicTheme(this)
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
            themeRes = RBase.style.AppTheme,
            appIconRes = RBase.drawable.ic_launcher_round
        ) {
            colorPrimaryRes = RBase.color.colorPrimary
            colorPrimaryVariantRes = RBase.color.colorPrimaryVariant
            colorOnPrimaryRes = RBase.color.colorOnPrimary
            colorSecondaryRes = RBase.color.colorSecondary
            colorSecondaryVariantRes = RBase.color.colorSecondaryVariant
            colorOnSecondaryRes = RBase.color.colorOnSecondary
            colorStatusBarRes = RBase.color.colorStatusBar
            lightStatusByPrimary = true
        }
        Theme.installDelegates(DynamicThemeDelegate())
        // Theme.get().enabled = false

        // Apply again because dynamic colors may change
        AppThemeUtils.configAppTheme(this, appTheme)
    }

    private fun setUpForFirstStart() {
        val themeMigrationKey = "new_theme"
        val iapMigrationKey = "new_iap"
        val phoneCallMigrationKey = "phone_call"
        if (sharedPreferences.getBoolean(PREF_FIRST_START, true)) {
            sharedPreferences.edit {
                putBoolean(PREF_FIRST_START, false)
                putBoolean(themeMigrationKey, false)
                putBoolean(iapMigrationKey, false)
                putBoolean(phoneCallMigrationKey, false)
                putBoolean(PREF_SP_MIGRATED, true)
            }
            sharedPreferences.useVoiceContent2 = true
            if (BuildConfig.APPLICATION_ID != packageName) {
                appTracker.trackError(
                    IllegalStateException("Tampered ${BuildConfig.APPLICATION_ID} to $packageName")
                )
            }
        } else {
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

            // Imported since 7.0.0 and shouldn't be removed.
            if (sharedPreferences.getBoolean(phoneCallMigrationKey, true)) {
                sharedPreferences.edit {
                    putBoolean(phoneCallMigrationKey, false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        shouldPausePhoneCall &&
                        !hasPermissions(Manifest.permission.READ_PHONE_STATE)
                    ) {
                        sharedPreferences.disablePhoneCallBehavior()
                    }
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

    override val workManagerConfiguration: WorkManagerConfiguration
        get() {
            return WorkManagerConfiguration.Builder()
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // https://coil-kt.github.io/coil/gifs/
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
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
