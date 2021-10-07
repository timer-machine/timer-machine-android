package xyz.aprildown.timer.presentation

import android.content.Intent
import xyz.aprildown.timer.presentation.stream.TimerIndex

interface StreamMachineIntentProvider {
    fun bindIntent(): Intent
    fun startIntent(id: Int, index: TimerIndex? = null): Intent
    fun pauseIntent(id: Int): Intent
    fun decreIntent(id: Int): Intent
    fun increIntent(id: Int): Intent
    fun moveIntent(id: Int, index: TimerIndex): Intent
    fun resetIntent(id: Int): Intent
    fun adjustTimeIntent(id: Int, amount: Long): Intent
}
