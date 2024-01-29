package xyz.aprildown.timer.data.db

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import xyz.aprildown.timer.data.datas.StepData
import xyz.aprildown.timer.data.datas.TimerMoreData
import xyz.aprildown.timer.data.json.BehaviourDataJsonAdapter
import xyz.aprildown.timer.data.json.PolymorphicJsonAdapterFactory
import xyz.aprildown.timer.data.json.TimerMoreDataJsonAdapter
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode

internal class StepConverters {

    private val stepsAdapter: JsonAdapter<List<StepData>>
    private val stepAdapter: JsonAdapter<StepData.Step>

    init {
        val behaviourDataJsonAdapter = BehaviourDataJsonAdapter()

        stepsAdapter = Moshi.Builder()
            .add(behaviourDataJsonAdapter)
            .add(getStepDataJsonAdapter())
            .build()
            .adapter(
                Types.newParameterizedType(List::class.java, StepData::class.java)
            )

        stepAdapter = Moshi.Builder()
            .add(behaviourDataJsonAdapter)
            .build()
            .adapter(StepData.Step::class.java)
    }

    @TypeConverter
    fun stepsToJson(steps: List<StepData>): String = stepsAdapter.toJson(steps)

    @TypeConverter
    fun jsonToSteps(json: String): List<StepData> = stepsAdapter.fromJson(json) ?: listOf()

    @TypeConverter
    fun stepToJson(step: StepData.Step?): String =
        if (step == null) "" else stepAdapter.toJson(step)

    @TypeConverter
    fun jsonToStep(json: String): StepData.Step? = if (json.isEmpty()) {
        null
    } else {
        stepAdapter.fromJson(json)
    }

    companion object {
        fun getStepDataJsonAdapter(): PolymorphicJsonAdapterFactory<StepData> =
            PolymorphicJsonAdapterFactory.of(StepData::class.java, "step_type")
                .withSubtype(StepData.Step::class.java, "step")
                .withSubtype(StepData.Group::class.java, "group")
                .withDefaultSubType(StepData.Step::class.java)
    }
}

internal class TimerMoreConverters {
    private val moshi = Moshi.Builder()
        .add(TimerMoreDataJsonAdapter())
        .build()
        .adapter(TimerMoreData::class.java)

    @TypeConverter
    fun moreToJson(more: TimerMoreData): String = moshi.toJson(more)

    @TypeConverter
    fun jsonToMore(json: String): TimerMoreData {
        return if (json.isBlank()) {
            TimerMoreData()
        } else {
            moshi.fromJson(json) ?: TimerMoreData()
        }
    }
}

internal class BooleanConverters {
    private val moshi = Moshi.Builder()
        .build()
        .adapter<List<Boolean>>(List::class.java)

    @TypeConverter
    fun booleanListToString(booleans: List<Boolean>): String = moshi.toJson(booleans)

    @TypeConverter
    fun stringToBooleanList(string: String): List<Boolean> = moshi.fromJson(string)
        ?: List(7) { false }
}

internal class SchedulerRepeatModeConverter {

    @TypeConverter
    fun toJson(mode: SchedulerRepeatMode): String = mode.name

    @TypeConverter
    fun fromJson(json: String?): SchedulerRepeatMode {
        return SchedulerRepeatMode.entries.find { it.name == json }
            ?: SchedulerRepeatMode.ONCE
    }
}
