package xyz.aprildown.timer.data.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import xyz.aprildown.timer.data.datas.TimerMoreData
import xyz.aprildown.timer.domain.entities.TimerEntity

internal class TimerMoreDataJsonAdapter {

    private val options: JsonReader.Options =
        JsonReader.Options.of("showNotif", "notifCount", "triggerTimerId")

    @FromJson
    fun fromJson(reader: JsonReader): TimerMoreData {
        var showNotif: Boolean? = null
        var notifCount: Boolean? = null
        var triggerTimerId: Int? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> showNotif = reader.nextBoolean()
                1 -> notifCount = reader.nextBoolean()
                2 -> triggerTimerId = reader.nextInt()
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        var result = TimerMoreData()
        result = result.copy(
            showNotif = showNotif ?: result.showNotif,
            notifCount = notifCount ?: result.notifCount,
            triggerTimerId = triggerTimerId ?: result.triggerTimerId
        )
        return result
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: TimerMoreData?) {
        if (value == null) {
            throw NullPointerException("value was null! Wrap in .nullSafe() to write nullable values.")
        }
        writer.beginObject()

        value.showNotif.let {
            if (!it) {
                writer.name("showNotif")
                writer.value(false)
            }
        }

        value.notifCount.let {
            if (!it) {
                writer.name("notifCount")
                writer.value(it)
            }
        }

        value.triggerTimerId.let {
            if (it != TimerEntity.NULL_ID) {
                writer.name("triggerTimerId")
                writer.value(it)
            }
        }

        writer.endObject()
    }
}
