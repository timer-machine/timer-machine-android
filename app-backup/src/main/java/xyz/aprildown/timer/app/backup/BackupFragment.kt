package xyz.aprildown.timer.app.backup

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.ui.FlavorUiInjector
import xyz.aprildown.timer.app.base.ui.FlavorUiInjectorQualifier
import xyz.aprildown.timer.app.base.utils.NavigationUtils.subLevelNavigate
import java.util.Optional
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class BackupFragment : PreferenceFragmentCompat() {

    @Inject
    @FlavorUiInjectorQualifier
    lateinit var flavorUiInjector: Optional<FlavorUiInjector>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_backup, rootKey)

        flavorUiInjector.run {
            val preference = findPreference<Preference>(getString(RBase.string.backup_key_cloud))
            requireNotNull(preference)
            if (isPresent) {
                preference.setOnPreferenceClickListener {
                    get().toCloudBackupFragment(this@BackupFragment)
                    true
                }
            } else {
                preference.isVisible = false
            }
        }

        findPreference<Preference>(getString(RBase.string.backup_key_export))?.setOnPreferenceClickListener {
            NavHostFragment.findNavController(this)
                .subLevelNavigate(RBase.id.dest_export)
            true
        }
        findPreference<Preference>(getString(RBase.string.backup_key_import))?.setOnPreferenceClickListener {
            NavHostFragment.findNavController(this)
                .subLevelNavigate(RBase.id.dest_import)
            true
        }
    }
}
