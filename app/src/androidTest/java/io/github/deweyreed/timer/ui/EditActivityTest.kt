package io.github.deweyreed.timer.ui

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.mikepenz.fastadapter.FastAdapter
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.deweyreed.timer.TestDataModule
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.edit.EditActivity
import xyz.aprildown.timer.app.timer.edit.EditableStep
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.utils.Constants
import javax.inject.Inject
import com.google.android.material.R as RMaterial
import xyz.aprildown.timer.app.base.R as RBase
import xyz.aprildown.timer.app.intro.R as RIntro
import xyz.aprildown.timer.app.timer.edit.R as RTimerEdit
import xyz.aprildown.timer.component.key.R as RComponentKey

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class EditActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var notifierRepo: NotifierRepository

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sharedPreferences = TestDataModule.provideSharedPreferences(context)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun snackbar() {
        launchEditTimerActivity()
        onView(withId(RComponentKey.id.editNameLoopName)).perform(replaceText(""))
        onView(withId(RIntro.id.action_save_timer)).perform(click())
        Thread.sleep(500)
        onView(
            allOf(
                withId(RMaterial.id.snackbar_text),
                withText(RBase.string.edit_wrong_empty_name)
            )
        )
            .check(matches(isDisplayed()))
    }

    @Test
    fun start_end() {
        val scenario = launchEditTimerActivity()
        onView(withText(RBase.string.edit_add_start)).check(matches(isDisplayed()))
        scenario.onActivity { it.addStartStep() }
        Thread.sleep(2000)
        onView(withText(RBase.string.edit_start_step)).check(matches(isDisplayed()))

        onView(isRoot()).perform(ViewActions.swipeUp())

        onView(withText(RBase.string.edit_add_end)).check(matches(isDisplayed()))
        scenario.onActivity { it.addEndStep() }
        Thread.sleep(2000)
        onView(withText(RBase.string.edit_end_step)).check(matches(isDisplayed()))
    }

    @Test
    fun new_notifier() {
        val scenario = launchEditTimerActivity()
        Thread.sleep(1000)
        scenario.onActivity { it.addNotifierStep() }
        Thread.sleep(1000)
        onView(
            allOf(
                withClassName(endsWith("EditText")),
                withText(RBase.string.edit_default_notifier_name)
            )
        ).check(matches(isDisplayed()))

        val new = TestData.fakeStepC
        // edit notifier
        onView(
            allOf(
                withClassName(endsWith("EditText")),
                withText(`is`(context.getString(RBase.string.edit_default_notifier_name)))
            )
        ).perform(replaceText(new.label))

        onView(withId(RTimerEdit.id.listEditSteps)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                object : ViewAction {
                    override fun getDescription(): String = ""
                    override fun getConstraints(): Matcher<View> = allOf()
                    override fun perform(uiController: UiController, view: View) {
                        view.findViewById<View>(RComponentKey.id.btnBehaviourAdd).performClick()
                    }
                }
            )
        )
        Thread.sleep(500)
        onView(withText(containsString(context.getString(RBase.string.behaviour_voice))))
            .perform(click())

        // new added notifier should be same as the last one
        scenario.onActivity { it.addNotifierStep() }
        onView(withId(RTimerEdit.id.listEditSteps)).check { view, _ ->
            ((view as RecyclerView).adapter as FastAdapter<*>).run {
                val step1 = getItem(1) as EditableStep
                val step2 = getItem(2) as EditableStep
                assertEquals(step1.label, step2.label)
                assertEquals(step1.length, step2.length)
                assertEquals(step1.behaviour, step2.behaviour)
                assertEquals(step1.stepType, step2.stepType)
            }
        }

        onView(withId(RIntro.id.action_save_timer)).perform(click())
        val stored = runBlocking { notifierRepo.get() }
        assertEquals(new.label, stored.label)
        assertEquals(StepType.NOTIFIER, stored.type)
    }

    @Test
    fun old_notifier() {
        val old = TestData.fakeStepC
        runBlocking { notifierRepo.set(old) }

        val scenario = launchEditTimerActivity()
        Thread.sleep(1000)
        scenario.onActivity { it.addNotifierStep() }
        Thread.sleep(2000)
        onView(withText(old.label)).check(matches(isDisplayed()))
        onView(withText(old.length.produceTime())).check(matches(isDisplayed()))

        onView(withId(RTimerEdit.id.listEditSteps)).check { view, _ ->
            ((view as RecyclerView).adapter as FastAdapter<*>).run {
                assertEquals(old.behaviour, (getItem(1) as EditableStep).behaviour)
            }
        }
    }

    private fun launchEditTimerActivity(id: Int = TimerEntity.NULL_ID): ActivityScenario<EditActivity> {
        return ActivityScenario.launch(
            Intent(
                context,
                EditActivity::class.java
            ).apply {
                if (id != TimerEntity.NULL_ID) {
                    putExtra(Constants.EXTRA_TIMER_ID, id)
                }
            }
        )
    }
}
