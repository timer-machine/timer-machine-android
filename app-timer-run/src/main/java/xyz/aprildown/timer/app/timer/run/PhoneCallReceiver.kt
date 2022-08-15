package xyz.aprildown.timer.app.timer.run

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

internal class PhoneCallReceiver(
    private val context: Context,
    private val onListenFailed: () -> Unit,
) : PhoneStateListener() {

    interface ServiceActionCallback {
        fun pauseActionsForCalls()

        fun resumeActionsAfterCalls()
    }

    private var isCalling = false

    private var manager: TelephonyManager? = null
    private var callback: ServiceActionCallback? = null

    fun listen(callback: ServiceActionCallback) {
        this.callback = callback
        manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            manager?.listen(this, LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            onListenFailed()
        }
    }

    fun unListen() {
        isCalling = false
        callback = null
        try {
            manager?.listen(this, LISTEN_NONE)
        } catch (e: SecurityException) {
            onListenFailed()
        }
        manager = null
    }

    override fun onCallStateChanged(state: Int, incomingNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isCalling) {
                    isCalling = false
                    callback?.resumeActionsAfterCalls()
                }
            }
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!isCalling) {
                    isCalling = true
                    callback?.pauseActionsForCalls()
                }
            }
        }
    }
}
