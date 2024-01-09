package xyz.aprildown.timer.app.timer.run.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.github.deweyreed.tools.arch.observeEvent
import com.github.deweyreed.tools.helper.startDrawableAnimation
import com.github.deweyreed.tools.helper.stopDrawableAnimation
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.base.utils.setTime
import xyz.aprildown.timer.app.timer.run.MachineService
import xyz.aprildown.timer.app.timer.run.databinding.ActivityScreenBinding
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.presentation.screen.ScreenViewModel
import xyz.aprildown.timer.presentation.stream.MachineContract
import java.io.File
import com.github.deweyreed.tools.R as RTools

@AndroidEntryPoint
class ScreenActivity : BaseActivity() {

    private lateinit var binding: ActivityScreenBinding

    private val viewModel: ScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawable(null)

        screen = this

        binding = ActivityScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        setUpFullscreen()
        setUpObservers()

        bindService(
            MachineService.bindIntent(this),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun init() {
        intent?.getStringExtra(EXTRA_NAME)?.let { timerName ->
            intent?.getStringExtra(EXTRA_STEP_NAME)?.let { stepName ->
                binding.textStepInfo.text = ScreenViewModel.formatStepInfo(
                    timerName = timerName,
                    loopString = "",
                    stepName = stepName
                )
            }
        }

        intent?.getStringExtra(EXTRA_IMAGE_PATH)?.let { imagePath ->
            if (imagePath.isNotEmpty()) {
                val image = File(imagePath)
                if (image.extension == "gif")
                    Glide.with(this).asGif().load(Uri.parse(imagePath)).into(binding.image)
                else
                    Glide.with(this).asBitmap().load(Uri.parse(imagePath)).into(binding.image)
//                binding.image.setImageURI(Uri.parse(imagePath))
            }
        }

        viewModel.setTimerId(
            intent?.getIntExtra(Constants.EXTRA_TIMER_ID, TimerEntity.NULL_ID)
                ?: TimerEntity.NULL_ID
        )
    }

    private fun setUpFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isLandscape = resources.getBoolean(RTools.bool.is_landscape)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val targetInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val isLtr = ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR
            if (!isLandscape) {
                binding.root.updatePadding(
                    left = targetInsets.left,
                    right = targetInsets.right,
                    top = targetInsets.top,
                )
                binding.btnStop.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomMargin = targetInsets.bottom
                }
            } else {
                if (isLtr) {
                    binding.root.updatePadding(
                        left = targetInsets.left,
                        top = targetInsets.top,
                    )
                    binding.btnStop.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        rightMargin = targetInsets.right
                    }
                } else {
                    binding.root.updatePadding(
                        top = targetInsets.top,
                        right = targetInsets.right,
                    )
                    binding.btnStop.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        leftMargin = targetInsets.left
                    }
                }
            }
            insets
        }

        @Suppress("DEPRECATION") // LOW_PROFILE only exists in old APIs.
        binding.root.systemUiVisibility =
            binding.root.systemUiVisibility or View.SYSTEM_UI_FLAG_LOW_PROFILE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        // Without the first two flags and only with the two functions above,
        // it won't work on some devices.
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Close dialogs and window shade, so this is fully visible
            @Suppress("MissingPermission", "DEPRECATION")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
    }

    private fun setUpObservers() {
        viewModel.timerStepInfo.observe(this) {
            binding.textStepInfo.text = it
        }
        viewModel.timerCurrentTime.observe(this) {
            binding.textTime.setTime(it ?: 0)
        }
        binding.btnAddOneMinute.setOnClickListener {
            viewModel.onAddOneMinute()
        }
        binding.btnStop.setOnClickListener {
            viewModel.onStop()
        }

        viewModel.intentEvent.observeEvent(this) {
            startService(it)
        }
        viewModel.stopEvent.observeEvent(this) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        unbindService(mConnection)
        viewModel.dropPresenter()
        screen = null
        super.onDestroy()
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            viewModel.setPresenter((service as MachineContract.PresenterProvider).getPresenter())
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }

    companion object {
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_STEP_NAME = "extra_step_name"
        private const val EXTRA_IMAGE_PATH = "extra_image_path"

        @SuppressLint("StaticFieldLeak")
        var screen: Activity? = null

        fun intent(context: Context, id: Int, timerName: String, stepName: String, imagePath: String): Intent {
            return Intent(context, ScreenActivity::class.java)
                .putExtra(Constants.EXTRA_TIMER_ID, id)
                .putExtra(EXTRA_NAME, timerName)
                .putExtra(EXTRA_STEP_NAME, stepName)
                .putExtra(EXTRA_IMAGE_PATH, imagePath)
        }
    }
}
