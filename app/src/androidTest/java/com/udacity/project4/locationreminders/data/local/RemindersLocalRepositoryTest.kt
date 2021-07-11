package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
// Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    Add testing implementation to the RemindersLocalRepository.kt

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutineMainRule = MainCoroutineRule()

    private lateinit var localRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setupDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localRepository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun tearDownDb() = database.close()

    @Test
    fun saveReminder_matches_retrievedById() = runBlockingTest {
        // GIVEN - A new reminder is saved in the database.
        val reminder = ReminderDTO(
            "reminder1",
            "just a test reminder",
            "Test Location",
            123.456789123,
            321.012345678
        )
        localRepository.saveReminder(reminder)

        // WHEN  - Reminder retrieved by ID.
        val result = localRepository.getReminder(reminder.id)

        // THEN - Same reminder is returned.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data, CoreMatchers.notNullValue())
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveReminder_matches_retrievedReminders() = runBlockingTest {
        // GIVEN - A new reminder saved in the database.
        val reminder = ReminderDTO(
            "reminder1",
            "just a test reminder",
            "Test Location",
            123.456789123,
            321.012345678
        )
        localRepository.saveReminder(reminder)

        // WHEN  - Get all reminders
        val result = localRepository.getReminders()

        // THEN - Size is 1 element and matched saved one.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success

        assertThat(result.data.size, `is`(1))

        val itemResult = result.data[0]
        assertThat(itemResult, CoreMatchers.notNullValue())
        assertThat(itemResult.id, `is`(reminder.id))
        assertThat(itemResult.title, `is`(reminder.title))
        assertThat(itemResult.description, `is`(reminder.description))
        assertThat(itemResult.location, `is`(reminder.location))
        assertThat(itemResult.latitude, `is`(reminder.latitude))
        assertThat(itemResult.longitude, `is`(reminder.longitude))
    }

    @Test
    fun deleteAllReminder_deletesAll() = runBlockingTest {
        // GIVEN - A new reminder saved in the database.
        val reminder = ReminderDTO(
            "reminder1",
            "just a test reminder",
            "Test Location",
            123.456789123,
            321.012345678
        )
        localRepository.saveReminder(reminder)

        // WHEN
        localRepository.deleteAllReminders()

        // THEN REMINDERS LIST SIZE IS 0
        val result = localRepository.getReminders()
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success

        assertThat(result.data.size, `is`(0))
    }

    @Test
    fun getReminderById_forNonExistentReminder_returnError() = runBlockingTest {
        // WHEN  - Reminder retrieved by ID.
        val result = localRepository.getReminder("NON_EXISTENT_ID")

        // THEN - Same reminder is returned.
        assertThat(result is Result.Error, `is`(true))
        result as Result.Error
        assertThat(result.message, CoreMatchers.notNullValue())
        assertThat(result.message, `is`("Reminder not found!"))
    }
}
