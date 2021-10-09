package io.github.deweyreed.timer.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import xyz.aprildown.timer.app.tasker.TaskerEditActivity
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.repositories.TimerRepository
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class TaskerEditActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var timerRepo: TimerRepository

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun showList() {
        val timers =
            listOf(TestData.fakeTimerSimpleA, TestData.fakeTimerSimpleB, TestData.fakeTimerAdvanced)
        timers.forEach {
            runBlocking {
                timerRepo.add(it)
            }
        }
        ActivityScenario.launch(TaskerEditActivity::class.java)

        // Wait our data is load asynchronously.
        onView(withText(RBase.string.timer_pick_required)).perform(click())
        Thread.sleep(1000)
        timers.map { it.name }.forEach {
            onView(withText(it)).check(matches(isDisplayed()))
        }
    }
}
