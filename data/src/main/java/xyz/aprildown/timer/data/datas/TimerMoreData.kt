package xyz.aprildown.timer.data.datas

import androidx.annotation.Keep
import com.squareup.moshi.Json
import xyz.aprildown.timer.domain.entities.TimerEntity

@Keep
internal data class TimerMoreData(
    @Json(name = "showNotif")
    val showNotif: Boolean = true,

    @Json(name = "notifCount")
    val notifCount: Boolean = true,

    @Json(name = "triggerTimerId")
    val triggerTimerId: Int = TimerEntity.NULL_ID
)
