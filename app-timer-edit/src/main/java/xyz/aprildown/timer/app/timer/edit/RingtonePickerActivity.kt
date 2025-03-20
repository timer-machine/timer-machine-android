package xyz.aprildown.timer.app.timer.edit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.text.buildSpannedString
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.ultimateringtonepicker.RingtonePickerFragment
import xyz.aprildown.ultimateringtonepicker.UltimateRingtonePicker
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class RingtonePickerActivity :
    BaseActivity(),
    UltimateRingtonePicker.RingtonePickerListener {

    private var reference: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringtone_picker)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            title = intent?.getStringExtra(EXTRA_TITLE)
        }

        reference = intent?.getIntExtra(EXTRA_REFERENCE, -1) ?: -1

        if (savedInstanceState == null) {
            val settings = IntentCompat.getParcelableExtra(
                intent,
                EXTRA_SETTINGS,
                UltimateRingtonePicker.Settings::class.java
            )
            if (settings == null) {
                finish()
                return
            }
            val fragment = settings.createFragment()
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.viewRingtonePickerFragmentContainer,
                    fragment,
                    TAG_RINGTONE_PICKER
                )
                .setPrimaryNavigationFragment(fragment)
                .commit()
        }

        findViewById<View>(R.id.btnRingtonePickerSelect).setOnClickListener {
            getRingtonePickerFragment().onSelectClick()
        }
        findViewById<View>(R.id.btnRingtonePickerCancel).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun getRingtonePickerFragment(): RingtonePickerFragment {
        return supportFragmentManager.findFragmentByTag(TAG_RINGTONE_PICKER) as RingtonePickerFragment
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add("")?.run {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            setOnMenuItemClickListener {
                usingSafPick = !usingSafPick
                applyNewSafPickSetting()
                true
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.getItem(0)
        menuItem?.setTitle(
            buildSpannedString {
                append("SAF")
                if (usingSafPick) {
                    setSpan(
                        ForegroundColorSpan(newDynamicTheme.colorSecondary),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        )
        return true
    }

    private fun applyNewSafPickSetting() {
        invalidateOptionsMenu()

        val currentSettings = IntentCompat.getParcelableExtra(
            intent,
            EXTRA_SETTINGS,
            UltimateRingtonePicker.Settings::class.java
        )
        if (currentSettings == null) {
            finish()
            return
        }
        val newSettings = currentSettings.copy(
            systemRingtonePicker = currentSettings.systemRingtonePicker?.copy(
                customSection = currentSettings.systemRingtonePicker?.customSection?.copy(
                    useSafSelect = usingSafPick
                )
            )
        )
        // In case the activity is recreated later
        intent.putExtra(EXTRA_SETTINGS, newSettings)

        val newFragment = newSettings.createFragment()
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.viewRingtonePickerFragmentContainer,
                newFragment,
                TAG_RINGTONE_PICKER
            )
            .setPrimaryNavigationFragment(newFragment)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onRingtonePicked(ringtones: List<UltimateRingtonePicker.RingtoneEntry>) {
        if (ringtones.isNotEmpty()) {
            setResult(
                Activity.RESULT_OK,
                Intent()
                    .putExtra(EXTRA_RESULT, ringtones.toTypedArray())
                    .putExtra(EXTRA_REFERENCE, reference)
            )
        }
        finish()
    }

    companion object {

        private const val TAG_RINGTONE_PICKER = "ringtone_picker"

        private const val EXTRA_SETTINGS = "settings"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_RESULT = "result"
        private const val EXTRA_REFERENCE = "reference"

        fun getIntent(
            context: Context,
            settings: UltimateRingtonePicker.Settings,
            windowTitle: CharSequence,
            reference: Int
        ): Intent = Intent(context, RingtonePickerActivity::class.java).apply {
            putExtra(EXTRA_SETTINGS, settings)
            putExtra(EXTRA_TITLE, windowTitle)
            putExtra(EXTRA_REFERENCE, reference)
        }

        fun getPickerResult(intent: Intent): List<UltimateRingtonePicker.RingtoneEntry> {
            return IntentCompat.getParcelableArrayExtra(
                intent,
                EXTRA_RESULT,
                UltimateRingtonePicker.RingtoneEntry::class.java
            )?.filterIsInstance<UltimateRingtonePicker.RingtoneEntry>() ?: emptyList()
        }

        fun getPickerReference(intent: Intent): Int {
            return intent.getIntExtra(EXTRA_REFERENCE, -1)
        }
    }
}

private const val PREF_USING_SAF_PICK = "pref_ringtone_saf_pick"
private var Context.usingSafPick: Boolean
    get() = safeSharedPreference.getBoolean(PREF_USING_SAF_PICK, false)
    set(value) = safeSharedPreference.edit { putBoolean(PREF_USING_SAF_PICK, value) }

internal fun Context.generateRingtonePickerSettings(select: Uri?): UltimateRingtonePicker.Settings {
    return UltimateRingtonePicker.Settings(
        preSelectUris = mutableListOf<Uri>().apply {
            if (select != null) {
                add(select)
            }
        },
        streamType = storedAudioTypeValue,
        systemRingtonePicker = UltimateRingtonePicker.SystemRingtonePicker(
            customSection = UltimateRingtonePicker.SystemRingtonePicker.CustomSection(
                useSafSelect = usingSafPick,
                launchSafOnPermissionDenied = false,
                launchSafOnPermissionPermanentlyDenied = false
            ),
            defaultSection = UltimateRingtonePicker.SystemRingtonePicker.DefaultSection(
                showSilent = false,
                defaultUri = UltimateRingtonePicker.createRawRingtoneUri(
                    context = this,
                    resourceId = RBase.raw.default_ringtone
                )
            ),
            ringtoneTypes = listOf(
                RingtoneManager.TYPE_NOTIFICATION,
                RingtoneManager.TYPE_ALARM,
                RingtoneManager.TYPE_RINGTONE
            )
        ),
        deviceRingtonePicker = UltimateRingtonePicker.DeviceRingtonePicker(
            deviceRingtoneTypes = listOf(
                UltimateRingtonePicker.RingtoneCategoryType.All,
                UltimateRingtonePicker.RingtoneCategoryType.Artist,
                UltimateRingtonePicker.RingtoneCategoryType.Album,
                UltimateRingtonePicker.RingtoneCategoryType.Folder
            )
        )
    )
}
