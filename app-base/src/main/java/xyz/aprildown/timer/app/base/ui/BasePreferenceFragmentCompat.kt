package xyz.aprildown.timer.app.base.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView

abstract class BasePreferenceFragmentCompat : PreferenceFragmentCompat() {
    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState)
            .also { recyclerView ->
                ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { view, insets ->
                    val target = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    view.updatePadding(bottom = target.bottom)
                    WindowInsetsCompat.CONSUMED
                }
            }
    }
}
