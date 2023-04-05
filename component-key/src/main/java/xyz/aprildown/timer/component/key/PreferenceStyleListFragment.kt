package xyz.aprildown.timer.component.key

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.deweyreed.tools.compat.getParcelableArrayCompat
import com.github.deweyreed.tools.helper.findCallback
import kotlinx.parcelize.Parcelize

class PreferenceStyleListFragment : PreferenceFragmentCompat() {

    interface Callback {
        fun onEntryClicked(entry: Entry)
    }

    @Parcelize
    data class Entry(
        @DrawableRes val iconRes: Int = 0,
        @StringRes val titleRes: Int,
        @StringRes val summaryRes: Int = 0,
    ) : Parcelable

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        val screen = preferenceManager.createPreferenceScreen(context)

        val callback = findCallback<Callback>()

        arguments?.getParcelableArrayCompat<Entry>(EXTRA_ENTRIES)?.forEach { entry ->
            screen.addPreference(
                Preference(context).apply {
                    isPersistent = false
                    if (entry.iconRes != 0) {
                        setIcon(entry.iconRes)
                    }
                    setTitle(entry.titleRes)
                    if (entry.summaryRes != 0) {
                        setSummary(entry.summaryRes)
                    }
                    if (callback != null) {
                        setOnPreferenceClickListener {
                            callback.onEntryClicked(entry)
                            true
                        }
                    }
                }
            )
        }

        preferenceScreen = screen
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.run {
            overScrollMode = View.OVER_SCROLL_NEVER
            isNestedScrollingEnabled = false
            isTransitionGroup = true
        }
    }

    companion object {
        private const val EXTRA_ENTRIES = "entries"

        fun newInstance(entries: List<Entry>): PreferenceStyleListFragment {
            return PreferenceStyleListFragment().apply {
                arguments = Bundle().also {
                    it.putParcelableArray(EXTRA_ENTRIES, entries.toTypedArray())
                }
            }
        }
    }
}
