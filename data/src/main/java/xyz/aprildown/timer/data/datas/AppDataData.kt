package xyz.aprildown.timer.data.datas

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class AppDataData(
    @Json(name = "folders")
    val folders: List<FolderData> = emptyList(),

    @Json(name = "timers")
    val timers: List<TimerData> = emptyList(),

    @Json(name = "notifier")
    val notifier: StepData.Step? = null,

    @Json(name = "timerStamps")
    val timerStamps: List<TimerStampData> = emptyList(),

    @Json(name = "schedulers")
    val schedulers: List<SchedulerData> = emptyList(),

    @Json(name = "prefs")
    val prefs: Map<String, String> = emptyMap()
)
