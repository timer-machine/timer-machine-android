package xyz.aprildown.timer.app.base.ui

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import xyz.aprildown.timer.app.base.utils.AppThemeUtils
import xyz.aprildown.tools.helper.isDarkTheme

abstract class BaseActivity : AppCompatActivity() {

    override fun setContentView(view: View?) {
        super.setContentView(view)
        newDynamicTheme.tintSystemUi(this)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        newDynamicTheme.tintSystemUi(this)
    }

    override fun onNightModeChanged(mode: Int) {
        AppThemeUtils.configThemeForDark(
            this,
            isDark = when (mode) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> resources.isDarkTheme
            }
        )
    }
}
