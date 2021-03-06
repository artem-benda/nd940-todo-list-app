package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceErrorMessages
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.createChannel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import timber.log.Timber

const val GEOFENCE_DEFAULT_RADIUS = 100.0f
const val GEOFENCE_EXPIRE_MILLIS = 86400000L
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
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            requireActivity(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val onSaveClickListener = View.OnClickListener {
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
                    _viewModel.showErrorMessage.value = it
                }
            )
        }
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

        binding.saveReminder.setOnClickListener(onSaveClickListener)

        _viewModel.selectedPOI.observe(viewLifecycleOwner, Observer { })
        _viewModel.latitude.observe(viewLifecycleOwner, Observer { })
        _viewModel.longitude.observe(viewLifecycleOwner, Observer { })

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        // Create channel for notifications
        createChannel(requireContext())
    }

    override fun onDestroy() {
        super.onDestroy()
        // make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(
        reminder: ReminderDataItem,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        val geofence = buildGeofence(reminder)
        if (geofence != null &&
            foregroundAndBackgroundLocationPermissionApproved()
        ) {
            checkDeviceLocationSettings {
                Timber.d("Have permissions, adding geofence...")
                geofencingClient
                    .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)
                    .addOnSuccessListener {
                        success()
                    }
                    .addOnFailureListener {
                        failure(GeofenceErrorMessages.getErrorString(requireContext(), it))
                    }
            }
        } else if (geofence == null) {
            _viewModel.showErrorMessage.value = getString(R.string.err_select_location)
        } else {
            Timber.d("Permissions missing, requesting...")
            _viewModel.showErrorMessage.value = getString(R.string.location_required_error)
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
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }

        return null
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(listOf(geofence))
            .build()
    }

    private fun checkPermissionsAndRequestIfMissing() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @SuppressLint("InlinedApi")
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        Timber.d("foregroundAndBackgroundLocationPermissionApproved, start, runningQOrLater = %b", runningQOrLater)
        val foregroundPermissionApproved = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundPermissionApproved = if (runningQOrLater)
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else
            true
        Timber.d("foregroundAndBackgroundLocationPermissionApproved, result is ${foregroundPermissionApproved && backgroundPermissionApproved}")
        return foregroundPermissionApproved && backgroundPermissionApproved
    }

    @SuppressLint("InlinedApi")
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
        requestPermissions(permissions, requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE) {
            if (
                grantResults.isNotEmpty() &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                (grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                Timber.i("FOREGROUND_AND_BACKGROUND_PERMISSION granted, proceeding saving...")
                onSaveClickListener.onClick(binding.saveReminder)
            } else {
                Timber.w("FOREGROUND_AND_BACKGROUND_PERMISSION NOT granted")
                _viewModel.showErrorMessage.value = getString(R.string.location_required_error)
            }
        }
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            if (
                grantResults.isNotEmpty() &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            ) {
                Timber.i("FOREGROUND_ONLY_PERMISSION granted, proceeding saving...")
                onSaveClickListener.onClick(binding.saveReminder)
            } else {
                Timber.w("FOREGROUND_ONLY_PERMISSION NOT granted")
                _viewModel.showErrorMessage.value = getString(R.string.location_required_error)
            }
        }
    }

    /* This function is similar to the previous excercise - Geofences */
    private fun checkDeviceLocationSettings(resolve: Boolean = true, onSuccessAction: () -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    Timber.w("Resolvable error, starting resolution for result")
                    startIntentSenderForResult(exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.w(sendEx, "Error getting location settings resolution: %s", sendEx.message)
                }
            } else {
                Timber.w("Non resolvable error")
                Snackbar.make(
                    binding.layout, R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings(onSuccessAction = onSuccessAction)
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Timber.i("Executing on success action...")
                onSuccessAction()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // Check location settings and try to proceed saving reminder on success. We don't ask user if he refused turning locations on
            checkDeviceLocationSettings(false) {
                onSaveClickListener.onClick(binding.saveReminder)
            }
        }
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_REQUEST_CODE = 101
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 102
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 103
