package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

//    Add testing implementation to the RemindersDao.kt
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun setupDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun tearDownDb() = database.close()

    @Test
    @ExperimentalCoroutinesApi
    fun insertReminderAndGetById() = runBlockingTest {
        val reminder = ReminderDTO(
            "reminder1",
            "just a test reminder",
            "Test Location",
            123.456789123,
            321.012345678
        )
        database.reminderDao().saveReminder(reminder)

        val loaded = database.reminderDao().getReminderById(reminder.id)

        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun insertReminderAndGetAll_returnsInserted() = runBlockingTest {
        val reminder = ReminderDTO(
            "reminder1",
            "just a test reminder",
            "Test Location",
            123.456789123,
            321.012345678
        )
        database.reminderDao().saveReminder(reminder)

        val allReminders = database.reminderDao().getReminders()

        assertThat(allReminders, notNullValue())
        assertThat(allReminders.size, `is`(1))

        val loaded = allReminders[0]

        assertThat(loaded, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun insertReminderAndRemoveAll() = runBlockingTest {
        val reminder = ReminderDTO(
            "reminder1",
            "just a test reminder",
            "Test Location",
            123.456789123,
            321.012345678
        )
        database.reminderDao().saveReminder(reminder)

        var allReminders = database.reminderDao().getReminders()

        assertThat(allReminders, notNullValue())
        assertThat(allReminders.size, `is`(1))

        database.reminderDao().deleteAllReminders()
        allReminders = database.reminderDao().getReminders()

        assertThat(allReminders, notNullValue())
        assertThat(allReminders.size, `is`(0))
    }

}