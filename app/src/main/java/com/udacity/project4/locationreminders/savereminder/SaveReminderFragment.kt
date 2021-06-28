package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.createChannel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber

const val GEOFENCE_DEFAULT_RADIUS = 20.0f
/*
Managing geofences according to tutorial:
https://www.raywenderlich.com/7372-geofencing-api-tutorial-for-android
 */

class SaveReminderFragment : BaseFragment() {
    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.selectedPOI.value = null
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            Timber.i("saveReminder fab clicked")
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val reminderItem = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminderItem)) {
                Timber.i("Trying to add geofence...")
                addGeofence(
                    reminderItem,
                    {
                        Timber.i("Geofence added successfully, saving reminder...")
                        _viewModel.saveReminder(reminderItem)
                    },
                    {
                        Timber.e(it)
                        Snackbar.make(view, it, Snackbar.LENGTH_LONG).show()
                    }
                )
            }
        }

        _viewModel.selectedPOI.observe(viewLifecycleOwner, Observer { })
        _viewModel.latitude.observe(viewLifecycleOwner, Observer { })
        _viewModel.longitude.observe(viewLifecycleOwner, Observer { })

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        // Create channel for notifications
        createChannel(requireContext())

        checkPermissionsAndRequestIfMissing()
    }

    override fun onDestroy() {
        super.onDestroy()
        // make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun addGeofence(
        reminder: ReminderDataItem,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        val geofence = buildGeofence(reminder)
        if (geofence != null &&
            foregroundAndBackgroundLocationPermissionApproved()
        ) {
            Timber.d("Have permissions, adding geofence...")
            geofencingClient
                .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)
                .addOnSuccessListener {
                    success()
                }
                .addOnFailureListener {
                    failure("Error")
                }
        } else {
            Timber.d("Permissions missing, requesting...")
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun buildGeofence(reminder: ReminderDataItem): Geofence? {
        Timber.i("buildGeofence, start, reminder = %s", reminder)
        val latitude = reminder.latitude
        val longitude = reminder.longitude
        val radius = GEOFENCE_DEFAULT_RADIUS

        if (latitude != null && longitude != null) {
            Timber.i("building geofence...")
            return Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                    latitude,
                    longitude,
                    radius
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }

        return null
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(listOf(geofence))
            .build()
    }

    private fun checkPermissionsAndRequestIfMissing() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundPermissionApproved = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundPermissionApproved = if (runningQOrLater)
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else
            true
        return foregroundPermissionApproved && backgroundPermissionApproved
    }

    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        val permissions = if (runningQOrLater)
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        else
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val requestCode = if (runningQOrLater)
            REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE
        else
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        ActivityCompat.requestPermissions(requireActivity(), permissions, requestCode)
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE = 101
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 102
