package xyz.aprildown.timer.app.settings

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.tools.helper.safeSharedPreference

internal class BakedCountPreference(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val imageView = holder?.findViewById(R.id.image) as? ImageView? ?: return
        imageView.isVisible =
            !context.safeSharedPreference.getBoolean(Constants.PREF_HAS_PRO, false)
    }
}
