package xyz.aprildown.timer.data.datas

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "TimerScheduler",
    indices = [(Index("timerId"))],
    foreignKeys = [
        (ForeignKey(
            entity = TimerData::class,
            parentColumns = ["id"],
            childColumns = ["timerId"]
        ))]
)
internal data class SchedulerData(
    @Json(name = "id")
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,

    @Json(name = "timerId")
    @ColumnInfo(name = "timerId")
    val timerId: Int,

    @Json(name = "label")
    @ColumnInfo(name = "label")
    val label: String,

    // 0 for start, 1 for end
    @Json(name = "action")
    @ColumnInfo(name = "action")
    val action: Int,

    @Json(name = "hour")
    @ColumnInfo(name = "hour")
    val hour: Int,

    @Json(name = "minute")
    @ColumnInfo(name = "minute")
    val minute: Int,

    @Json(name = "repeatMode")
    @ColumnInfo(name = "repeatMode")
    var repeatMode: SchedulerRepeatMode? = null,

    @Json(name = "days")
    @ColumnInfo(name = "days")
    val days: List<Boolean>,

    @Json(name = "enable")
    @ColumnInfo(name = "enable")
    val enable: Int
) {
    init {
        if (repeatMode == null) {
            repeatMode = if (days.any { it }) {
                SchedulerRepeatMode.EVERY_WEEK
            } else {
                SchedulerRepeatMode.ONCE
            }
        }
    }
}
