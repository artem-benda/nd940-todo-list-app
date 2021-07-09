package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeReminderDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue2
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    // provide testing to the SaveReminderView and its live data objects

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake data source to be injected into the view model.
    private lateinit var fakeDataSource: FakeReminderDataSource

    @Before
    fun setup() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        // Initialise the repository with no tasks.
        fakeDataSource = FakeReminderDataSource()

        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun saveReminder_fieldsSet_shouldShowSuccessToast() {

        val reminderItem = ReminderDataItem(
            "a title",
            "a description",
            "a poi",
            11.000,
            12.000
        )

        saveReminderViewModel.validateAndSaveReminder(reminderItem)

        assertThat(saveReminderViewModel.showToast.getOrAwaitValue2(), `is`("Reminder Saved !"))
    }

    @Test
    fun saveReminder_titleNotSet_shouldShowErrorSnackbar() {

        val reminderItem = ReminderDataItem(
            "",
            "a description",
            "a poi",
            11.000,
            12.000
        )

        saveReminderViewModel.validateAndSaveReminder(reminderItem)

        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue2(), `is`(R.string.err_enter_title))
    }

    @Test
    fun saveReminder_locationNotSet_shouldShowErrorSnackbar() {

        val reminderItem = ReminderDataItem(
            "a title",
            "a description",
            "",
            11.000,
            12.000
        )

        saveReminderViewModel.validateAndSaveReminder(reminderItem)

        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue2(), `is`(R.string.err_select_location))
    }
}
