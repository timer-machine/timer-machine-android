package xyz.aprildown.timer.data.datas

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "TimerStamp",
    indices = [Index("timerId")],
    foreignKeys = [
        ForeignKey(
            entity = TimerData::class,
            parentColumns = ["id"],
            childColumns = ["timerId"]
        )
    ]
)
internal data class TimerStampData(
    @Json(name = "id")
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,

    @Json(name = "timerId")
    @ColumnInfo(name = "timerId")
    val timerId: Int,

    @Json(name = "start")
    @ColumnInfo(name = "start")
    val start: Long = 0,

    @Json(name = "date")
    @ColumnInfo(name = "date")
    val end: Long
)
