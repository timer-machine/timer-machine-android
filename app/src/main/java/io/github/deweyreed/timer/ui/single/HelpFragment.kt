package io.github.deweyreed.timer.ui.single

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.view.postDelayed
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.deweyreed.timer.component.tts.TtsSpeaker
import com.github.deweyreed.tools.helper.IntentHelper
import com.github.deweyreed.tools.helper.createChooserIntentIfDead
import com.github.deweyreed.tools.helper.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.deweyreed.timer.R
import io.github.deweyreed.timer.databinding.DialogTtsTestBinding
import xyz.aprildown.timer.app.base.data.FlavorData
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.utils.NavigationUtils.subLevelNavigate
import xyz.aprildown.timer.app.base.utils.openLink
import xyz.aprildown.timer.domain.utils.Constants
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class HelpFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var appNavigator: AppNavigator

    @Inject
    lateinit var flavorData: FlavorData

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()

        setPreferencesFromResource(R.xml.pref_help, rootKey)

        findPreference<Preference>(getString(RBase.string.help_key_tutorial))?.setOnPreferenceClickListener {
            startActivity(appNavigator.getIntroIntent())
            true
        }
        findPreference<Preference>(getString(RBase.string.help_key_tips))?.setOnPreferenceClickListener {
            context.openLink(Constants.getTipsAndTricksLink())
            true
        }
        findPreference<Preference>(getString(RBase.string.help_key_qa))?.setOnPreferenceClickListener {
            context.openLink(Constants.getQaLink())
            true
        }
        findPreference<Preference>(getString(RBase.string.help_key_tts))?.setOnPreferenceClickListener {
            TtsTestDialog().show(childFragmentManager, null)
            true
        }
        findPreference<Preference>(getString(RBase.string.help_key_whitelist))?.setOnPreferenceClickListener {
            NavHostFragment.findNavController(this)
                .subLevelNavigate(RBase.id.dest_whitelist)
            true
        }
        findPreference<Preference>(getString(RBase.string.help_key_feedback))?.setOnPreferenceClickListener {
            context.startActivity(
                IntentHelper.email(
                    flavorData.email,
                    context.getString(RBase.string.help_email_title)
                ).createChooserIntentIfDead(context)
            )
            true
        }
    }

    class TtsTestDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val context = requireContext()
            val binding = DialogTtsTestBinding.inflate(layoutInflater)
            return MaterialAlertDialogBuilder(context)
                .setView(binding.root)
                .create()
                .apply {
                    setOnShowListener {
                        setUpViews(binding)
                    }
                }
        }

        private fun setUpViews(binding: DialogTtsTestBinding) {
            val context = requireContext()

            binding.btnClickInstruction.setOnClickListener {
                TtsSpeaker.clean()
                TtsSpeaker.speak(
                    context = context,
                    text = context.getString(RBase.string.help_tts_test_read_content),
                    oneShot = true,
                    onDone = { TtsSpeaker.clean() }
                )
                it.postDelayed(1000) {
                    binding.layoutActions.show()
                }
            }

            binding.btnReadInstructions.setOnClickListener {
                context.openLink(Constants.getConfigureTtsLink())
            }
            binding.btnSystemSettings.setOnClickListener {
                try {
                    context.startActivity(
                        Intent().apply {
                            action = "com.android.settings.TTS_SETTINGS"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                } catch (_: ActivityNotFoundException) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } catch (_: ActivityNotFoundException) {
                        binding.textSystemSettingsManual.show()
                    }
                }
            }
            binding.btnDone.setOnClickListener {
                dialog?.dismiss()
            }
        }

        override fun onResume() {
            super.onResume()
            TtsSpeaker.clean()
        }

        override fun onPause() {
            super.onPause()
            TtsSpeaker.clean()
        }
    }
}
