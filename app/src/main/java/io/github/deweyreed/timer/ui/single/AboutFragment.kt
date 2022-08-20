package io.github.deweyreed.timer.ui.single

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.deweyreed.timer.BuildConfig
import io.github.deweyreed.timer.R
import xyz.aprildown.timer.app.base.utils.openWebsiteWithWarning
import xyz.aprildown.timer.app.settings.LogFragment
import xyz.aprildown.timer.domain.utils.AppConfig
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.workshop.ChangeLogDialog
import xyz.aprildown.timer.workshop.Monika
import xyz.aprildown.tools.anko.newTask
import xyz.aprildown.tools.helper.IntentHelper
import xyz.aprildown.tools.helper.startActivityOrNothing
import javax.inject.Inject

class AboutFragment : Fragment(R.layout.fragment_about) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, AboutPreferenceFragment())
                .commit()
        }
    }
}

@AndroidEntryPoint
class AboutPreferenceFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var appTracker: AppTracker

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()

        setPreferencesFromResource(R.xml.pref_about, rootKey)

        findPreference<Preference>("key_about_version")?.summary =
            "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"

        findPreference<Preference>("key_about_crash")?.run {
            isVisible = AppConfig.openDebug
            setOnPreferenceClickListener {
                throw IllegalStateException("This is test crash!")
            }
        }

        findPreference<Preference>("key_about_changelog")?.setOnPreferenceClickListener {
            ChangeLogDialog(context).show()
            true
        }

        findPreference<Preference>("key_about_log")?.setOnPreferenceClickListener {
            val fm = parentFragment?.childFragmentManager
                ?: return@setOnPreferenceClickListener false

            fm.beginTransaction()
                .replace(R.id.fragmentContainer, LogFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit()
            true
        }

        findPreference<Preference>("key_about_privacy_policy")?.setOnPreferenceClickListener {
            context.openWebsiteWithWarning(Constants.getPrivacyPolicyLink())
            true
        }

        findPreference<Preference>("key_about_terms_of_service")?.setOnPreferenceClickListener {
            context.openWebsiteWithWarning(Constants.getTermsOfServiceLink())
            true
        }

        findPreference<Preference>("key_about_more_apps")?.setOnPreferenceClickListener {
            startActivityOrNothing(
                IntentHelper.intent("https://play.google.com/store/apps/dev?id=7518578900930550082")
                    .newTask()
            )
            true
        }

        findPreference<Preference>("key_about_github")?.setOnPreferenceClickListener {
            context.openWebsiteWithWarning("https://github.com/DeweyReed")
            true
        }

        findPreference<Preference>("key_about_email")?.setOnPreferenceClickListener {
            startActivityOrNothing(IntentHelper.email(it.summary.toString()))
            true
        }

        var clickedTimes = 0
        findPreference<Preference>("key_about_me")?.setOnPreferenceClickListener {
            val fm = parentFragment?.childFragmentManager
                ?: return@setOnPreferenceClickListener false

            if (++clickedTimes == 9) {
                fm.beginTransaction()
                    .replace(R.id.fragmentContainer, Monika())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            }
            true
        }
    }
}
