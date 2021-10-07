package xyz.aprildown.timer.data.datas

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "Folder")
internal data class FolderData(
    @Json(name = "id")
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,

    @Json(name = "name")
    @ColumnInfo(name = "name")
    val name: String

)
