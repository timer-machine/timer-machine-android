package xyz.aprildown.timer.data.datas

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import xyz.aprildown.timer.domain.entities.FolderEntity

@Keep
@JsonClass(generateAdapter = true)
@Entity(tableName = "TimerItem")
internal data class TimerData(
    @Json(name = "id")
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,

    @Json(name = "name")
    @ColumnInfo(name = "name")
    val name: String,

    @Json(name = "loop")
    @ColumnInfo(name = "loop")
    val loop: Int,

    @Json(name = "steps")
    @ColumnInfo(name = "steps")
    val steps: List<StepData>,

    @Json(name = "startStep")
    @ColumnInfo(name = "startStep")
    val startStep: StepData.Step? = null,

    @Json(name = "endStep")
    @ColumnInfo(name = "endStep")
    val endStep: StepData.Step? = null,

    @Json(name = "more")
    @ColumnInfo(name = "more")
    val more: TimerMoreData = TimerMoreData(),

    @Json(name = "folderId")
    @ColumnInfo(name = "folderId")
    val folderId: Long = FolderEntity.FOLDER_DEFAULT,
)

@Keep
internal data class TimerInfoData(
    val id: Int,
    val name: String,
    val folderId: Long = FolderEntity.FOLDER_DEFAULT
)
