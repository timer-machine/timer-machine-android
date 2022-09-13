package xyz.aprildown.timer.app.timer.one

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.base.utils.ScreenWakeLock
import xyz.aprildown.timer.app.timer.one.databinding.ActivityOneBinding
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

    private lateinit var binding: ActivityOneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOneBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
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
        if (binding.fragmentOneHost.getFragment<NavHostFragment>().navController.navigateUp()) {
            return false
        }
        if (!isTaskRoot) {
            onBackPressed()
            return true
        }
        return super.onSupportNavigateUp()
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
