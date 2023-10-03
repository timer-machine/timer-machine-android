package xyz.aprildown.timer.app.timer.run

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

internal class PhoneCallReceiver(
    private val context: Context,
    private val onListenFailed: () -> Unit,
) {
    interface ServiceActionCallback {
        fun pauseActionsForCalls()
        fun resumeActionsAfterCalls()
    }

    private val manager: TelephonyManager? by lazy { context.getSystemService() }
    private var listener: CallStateListener? = null

    fun register(callback: ServiceActionCallback) {
        if (this.listener != null) {
            unregister()
        }
        val manager = manager ?: return
        val listener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            CallStateListener.S(context = context, callback = callback)
        } else {
            CallStateListener.PreS(callback)
        }
        try {
            listener.register(manager)
            this.listener = listener
        } catch (_: SecurityException) {
            onListenFailed()
        }
    }

    fun unregister() {
        val listener = listener ?: return
        try {
            val manager = manager ?: return
            listener.unregister(manager)
        } catch (_: SecurityException) {
            onListenFailed()
        } finally {
            this.listener = null
        }
    }
}

private sealed interface CallStateListener {
    var isCalling: Boolean
    val callback: PhoneCallReceiver.ServiceActionCallback

    fun register(manager: TelephonyManager)
    fun unregister(manager: TelephonyManager)

    fun onStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isCalling) {
                    isCalling = false
                    callback.resumeActionsAfterCalls()
                }
            }
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!isCalling) {
                    isCalling = true
                    callback.pauseActionsForCalls()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    class PreS(
        override val callback: PhoneCallReceiver.ServiceActionCallback
    ) : android.telephony.PhoneStateListener(), CallStateListener {
        override var isCalling: Boolean = false

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            onStateChanged(state)
        }

        override fun register(manager: TelephonyManager) {
            manager.listen(this, LISTEN_CALL_STATE)
        }

        override fun unregister(manager: TelephonyManager) {
            manager.listen(this, LISTEN_NONE)
            isCalling = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    class S(
        private val context: Context,
        override val callback: PhoneCallReceiver.ServiceActionCallback
    ) : TelephonyCallback(), TelephonyCallback.CallStateListener, CallStateListener {
        override var isCalling: Boolean = false
        override fun onCallStateChanged(state: Int) {
            onStateChanged(state)
        }

        override fun register(manager: TelephonyManager) {
            manager.registerTelephonyCallback(ContextCompat.getMainExecutor(context), this)
        }

        override fun unregister(manager: TelephonyManager) {
            manager.unregisterTelephonyCallback(this)
            isCalling = false
        }
    }
}

