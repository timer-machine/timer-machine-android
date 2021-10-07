package xyz.aprildown.timer.data.datas

import androidx.annotation.Keep
import com.squareup.moshi.Json
import xyz.aprildown.timer.domain.entities.BehaviourType

@Keep
internal data class BehaviourData(
    @Json(name = "type")
    val type: BehaviourType,

    @Json(name = "label")
    val label: String = "",

    @Json(name = "content")
    val content: String = "",

    @Json(name = "loop")
    val loop: Boolean = true
)
