package xyz.aprildown.timer.app.timer.one

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.base.utils.ScreenWakeLock
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.presentation.one.OneViewModel
import xyz.aprildown.timer.app.base.R as RBase

internal interface OneActivityInterface {
    fun setToolbarTitle(title: String)
}

@AndroidEntryPoint
class OneActivity : BaseActivity(), OneActivityInterface {

    private val viewModel: OneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        volumeControlStream = storedAudioTypeValue

        if (savedInstanceState == null) {
            viewModel.setTimerId(
                intent?.getIntExtra(Constants.EXTRA_TIMER_ID, TimerEntity.NULL_ID)
                    ?: TimerEntity.NULL_ID
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController =
            (supportFragmentManager.findFragmentById(R.id.fragmentOneHost) as NavHostFragment).navController
        val currentId = navController.currentDestination?.id
        return if (currentId == R.id.fragmentOne) {
            finish()
            true
        } else {
            navController.navigateUp()
            false
        }
    }

    override fun onResume() {
        super.onResume()
        ScreenWakeLock.acquireScreenWakeLock(
            context = this,
            screenTiming = getString(RBase.string.pref_screen_timing_value_timer)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
            ActivityCompat.recreate(this)
        }
    }

    override fun onPause() {
        super.onPause()
        ScreenWakeLock.releaseScreenLock(
            context = this,
            screenTiming = getString(RBase.string.pref_screen_timing_value_timer)
        )
    }

    override fun setToolbarTitle(title: String) {
        supportActionBar?.run {
            setDisplayShowTitleEnabled(true)
            this.title = title
        }
    }

    companion object {
        fun intent(context: Context, id: Int): Intent = Intent(context, OneActivity::class.java)
            .putExtra(Constants.EXTRA_TIMER_ID, id)
    }
}
