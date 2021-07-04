package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

//    Create a fake data source to act as a double to the real data source
    var reminders: MutableList<ReminderDTO> = mutableListOf()
    var error: ErrorDto? = null

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return errorOr {
            reminders
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return errorOr {
            reminders.first { it.id == id }
        }
    }

    override suspend fun deleteAllReminders() {
        reminders = mutableListOf()
    }

    /**
     * Returns Result with error property if it's not null, else execute block and return success result
     */
    private fun <T : Any> errorOr(function: () -> T): Result<T> {
        val errorDto = error
        return if (errorDto != null) {
            Result.Error(errorDto.message, errorDto.statusCode)
        } else {
            Result.Success(function())
        }
    }

    data class ErrorDto(
        val message: String?,
        val statusCode: Int?
    )
}