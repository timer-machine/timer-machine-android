package xyz.aprildown.timer.app.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.widget.Spinner
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceViewHolder
import xyz.aprildown.timer.app.base.data.PreferenceData
import java.lang.ref.WeakReference

internal class PhoneCallPreference(
    context: Context,
    attrs: AttributeSet? = null
) : DropDownPreference(context, attrs), SharedPreferences.OnSharedPreferenceChangeListener {

    private var spinner: WeakReference<Spinner>? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        spinner = WeakReference(
            holder.findViewById(androidx.preference.R.id.spinner) as? Spinner
        )
    }

    override fun onAttached() {
        super.onAttached()
        sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetached() {
        super.onDetached()
        sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PreferenceData.KEY_PHONE_CALL) {
            val newValue = sharedPreferences?.getString(key, null)
            spinner?.get()?.setSelection(entryValues.indexOfFirst { it == newValue })
        }
    }
}
