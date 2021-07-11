package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeRemindersDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
// UI Testing
@MediumTest
class ReminderListFragmentTest {

//     test the navigation of the fragments.
//     test the displayed data on the UI.
//     add testing for the error messages.
    private lateinit var dataSource: FakeRemindersDataSource

    @Before
    fun initRepository() {
        dataSource = FakeRemindersDataSource()

        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            //Declare singleton definitions to be later injected using by inject()
            single {
                //This view model is declared singleton to be used across multiple fragments
                SaveReminderViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            single { dataSource as ReminderDataSource }
            single { dataSource as RemindersLocalRepository }
            single { LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext()) }
        }

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(myModule))
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun clickFAB_navigateToSaveReminder() = runBlockingTest {

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navigationMock = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navigationMock)
        }

        onView(withId(R.id.addReminderFAB))
            .perform(click())

        verify(navigationMock).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun getRemindersError_showsErrorSnackBar() = runBlockingTest {

        dataSource.error = FakeRemindersDataSource.ErrorDto("An error", 500)

        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        scenario.onFragment {}

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("An error")))
    }

    @Test
    fun recyclerView_showsDataSourceItems() = runBlockingTest {
        dataSource.saveReminder(ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 11.0, 12.0))
        dataSource.saveReminder(ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 21.0, 22.0))

        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        val navigationMock = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navigationMock)
        }

        onView(withId(R.id.reminderssRecyclerView))
            .check(
                matches(hasDescendant(withText("TITLE1")))
            )

        onView(withId(R.id.reminderssRecyclerView))
            .check(
                matches(hasDescendant(withText("TITLE2")))
            )
    }
}
