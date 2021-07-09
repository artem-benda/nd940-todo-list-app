package com.udacity.project4.locationreminders.data

import android.content.Context
import androidx.room.Room
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase

/**
 * Singleton class that is used to create a reminder db
 */
object LocalInMemoryDB {

    /**
     * static method that creates a reminder class and returns the DAO of the reminder
     */
    fun createRemindersDao(context: Context): RemindersDao {
        return Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java
        ).build().reminderDao()
    }

}