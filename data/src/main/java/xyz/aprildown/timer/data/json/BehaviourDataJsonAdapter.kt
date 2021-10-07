package xyz.aprildown.timer.data.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import xyz.aprildown.timer.data.datas.BehaviourData
import xyz.aprildown.timer.domain.entities.BehaviourType

internal class BehaviourDataJsonAdapter {

    private val options: JsonReader.Options =
        JsonReader.Options.of("type", "label", "content", "loop")

    @FromJson
    fun fromJson(reader: JsonReader): BehaviourData {
        var type: BehaviourType? = null
        var label: String? = null
        var content: String? = null
        var loop: Boolean? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> type = BehaviourType.valueOf(reader.nextString())
                1 -> label = reader.nextString()
                    ?: throw JsonDataException("Non-null value 'label' was null at ${reader.path}")
                2 -> content = reader.nextString()
                    ?: throw JsonDataException("Non-null value 'content' was null at ${reader.path}")
                3 -> loop = reader.nextBoolean()
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        var result = BehaviourData(
            type = type
                ?: throw JsonDataException("Required property 'type' missing at ${reader.path}")
        )
        result = result.copy(
            label = label ?: result.label,
            content = content ?: result.content,
            loop = loop ?: if (type.hasBoolValue) type.defaultBoolValue else result.loop
        )
        return result
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: BehaviourData?) {
        if (value == null) {
            throw NullPointerException("value was null! Wrap in .nullSafe() to write nullable values.")
        }

        writer.beginObject()

        val type = value.type

        writer.name("type")
        writer.value(type.name)

        value.label.let {
            if (it.isNotEmpty()) {
                writer.name("label")
                writer.value(it)
            }
        }

        value.content.let {
            if (it.isNotEmpty()) {
                writer.name("content")
                writer.value(it)
            }
        }

        value.loop.let {
            if (type.hasBoolValue) {
                writer.name("loop")
                writer.value(it)
            }
        }

        writer.endObject()
    }
}
