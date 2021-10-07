package xyz.aprildown.timer.app.base.media

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.CountDownTimer
import androidx.core.content.getSystemService

object Torch {
    private var cm: CameraManager? = null
    private var timer: CountDownTimer? = null

    fun start(context: Context, duration: Long, step: Long) {
        cm = context.getSystemService() ?: return

        timer?.cancel()

        var isOn = false
        timer = object : CountDownTimer(duration, step) {
            override fun onTick(millisUntilFinished: Long) {
                isOn = !isOn
                cm?.setTorchMode(isOn)
            }

            override fun onFinish() = Unit
        }
        timer?.start()
    }

    fun stop() {
        cm?.setTorchMode(false)
        cm = null
        timer?.cancel()
        timer = null
    }
}

private fun CameraManager.setTorchMode(enable: Boolean) {
    for (currentCameraId in cameraIdList) {
        try {
            setTorchMode(currentCameraId, enable)
        } catch (_: Exception) {
            // Ignore
        }
    }
}
