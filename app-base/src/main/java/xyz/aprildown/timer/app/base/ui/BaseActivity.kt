package xyz.aprildown.timer.app.base.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.github.deweyreed.tools.helper.isDarkTheme
import com.github.deweyreed.tools.helper.themeColor
import com.github.deweyreed.tools.utils.ThemeColorUtils
import com.google.android.material.R as RMaterial

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        tintSystemUi()
    }

    private fun tintSystemUi() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isLight = ThemeColorUtils.isLightColor(
            if (resources.isDarkTheme) {
                themeColor(RMaterial.attr.colorSurface)
            } else {
                newDynamicTheme.colorPrimary
            }
        )
        controller.isAppearanceLightStatusBars = isLight
    }
}
