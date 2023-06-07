package xyz.aprildown.timer.data.repositories

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.aprildown.timer.data.R
import xyz.aprildown.timer.data.mappers.BehaviourMapper
import xyz.aprildown.timer.data.mappers.StepOnlyMapper
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.repositories.NotifierRepository

@RunWith(AndroidJUnit4::class)
@SmallTest
class NotifierRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var prefs: SharedPreferences

    private lateinit var notifierRepository: NotifierRepository

    @Before
    fun setUp() {
        prefs = context.getSharedPreferences("test", Context.MODE_PRIVATE)
        prefs.edit { clear() }
        notifierRepository =
            NotifierRepositoryImpl(context, prefs, StepOnlyMapper(BehaviourMapper()))
    }

    @After
    fun tearDown() {
        prefs.edit {
            clear()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.deleteSharedPreferences("test")
        }
    }

    @Test
    fun getAndSet() = runTest {
        // First get will get a default step
        val firstGet = notifierRepository.get()
        assertEquals(
            firstGet,
            NotifierRepositoryImpl.getDefaultNotifier()
                .copy(label = context.getString(R.string.edit_default_notifier_name))
        )

        // Save and get
        notifierRepository.set(TestData.fakeStepC)
        assertEquals(TestData.fakeStepC, notifierRepository.get())
    }
}
