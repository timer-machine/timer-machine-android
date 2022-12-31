package xyz.aprildown.timer.data.repositories

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.squareup.moshi.Moshi
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.data.R
import xyz.aprildown.timer.data.datas.StepData
import xyz.aprildown.timer.data.json.BehaviourDataJsonAdapter
import xyz.aprildown.timer.data.mappers.StepOnlyMapper
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import javax.inject.Inject

@Reusable
internal class NotifierRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val stepOnlyMapper: StepOnlyMapper,
) : NotifierRepository {

    override suspend fun get(): StepEntity.Step {
        val storedString: String? = sharedPreferences.getString(PREF_STEP_NOTIFIER, null)
        return if (storedString == null) {
            translatedDefaultNotifier()
        } else {
            try {
                val step: StepData.Step? = fromJson(storedString)
                return if (step == null) {
                    translatedDefaultNotifier()
                } else {
                    stepOnlyMapper.mapFrom(step)
                }
            } catch (_: Exception) {
                translatedDefaultNotifier()
            }
        }
    }

    private fun fromJson(storedString: String): StepData.Step? {
        return Moshi.Builder()
            .add(BehaviourDataJsonAdapter())
            .build()
            .adapter(StepData.Step::class.java)
            .fromJson(storedString)
    }

    override suspend fun set(item: StepEntity.Step?): Boolean {
        val editor = sharedPreferences.edit()
        if (item != null) {
            editor.putString(
                PREF_STEP_NOTIFIER,
                Moshi.Builder()
                    .add(BehaviourDataJsonAdapter())
                    .build()
                    .adapter(StepData.Step::class.java)
                    .toJson(stepOnlyMapper.mapTo(item))
            )
        } else {
            editor.remove(PREF_STEP_NOTIFIER)
        }
        editor.apply()
        return true
    }

    private fun translatedDefaultNotifier(): StepEntity.Step {
        return getDefaultNotifier().copy(label = context.getString(R.string.edit_default_notifier_name))
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun getDefaultNotifier(): StepEntity.Step = StepEntity.Step(
            label = "Notifier",
            length = 10_000,
            behaviour = listOf(
                BehaviourEntity(BehaviourType.MUSIC),
                BehaviourEntity(BehaviourType.VIBRATION),
                BehaviourEntity(BehaviourType.SCREEN)
            ),
            type = StepType.NOTIFIER
        )
    }
}

private const val PREF_STEP_NOTIFIER = "pref_step_notifier"
