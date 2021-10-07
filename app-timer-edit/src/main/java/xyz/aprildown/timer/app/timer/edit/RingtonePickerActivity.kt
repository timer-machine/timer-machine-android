package xyz.aprildown.timer.app.timer.edit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.edit
import androidx.core.view.MenuItemCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.tools.helper.toColorStateList
import xyz.aprildown.ultimateringtonepicker.RingtonePickerFragment
import xyz.aprildown.ultimateringtonepicker.UltimateRingtonePicker
import javax.inject.Inject

@AndroidEntryPoint
class RingtonePickerActivity : BaseActivity(),
    UltimateRingtonePicker.RingtonePickerListener {

    @Inject
    lateinit var appTracker: AppTracker

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
            val settings =
                intent.getParcelableExtra<UltimateRingtonePicker.Settings>(EXTRA_SETTINGS)
            requireNotNull(settings)
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
        menuInflater.inflate(R.menu.ringtone_picker, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_ringtone_picker_saf)?.let {
            MenuItemCompat.setIconTintList(
                it,
                if (usingSafPick) newDynamicTheme.colorSecondary.toColorStateList() else null
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_ringtone_picker_saf -> {
                if (usingSafPick) {
                    usingSafPick = false
                    applyNewSafPickSetting()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.music_saf_pick)
                        .setMessage(R.string.music_saf_pick_desp)
                        .setPositiveButton(R.string.enable) { _, _ ->
                            usingSafPick = true
                            applyNewSafPickSetting()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
            else -> return false
        }
        return true
    }

    private fun applyNewSafPickSetting() {
        invalidateOptionsMenu()

        val currentSettings =
            intent.getParcelableExtra<UltimateRingtonePicker.Settings>(EXTRA_SETTINGS)
        requireNotNull(currentSettings)
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
                    .putParcelableArrayListExtra(EXTRA_RESULT, ArrayList(ringtones))
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
            return intent.getParcelableArrayListExtra(EXTRA_RESULT)!!
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
                    resourceId = R.raw.default_ringtone
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
