package com.ankit.mediapicker

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Description: This util class is used for the purpose of checking and asking user permissions.
 *
 * [NOTE: Use setConfiguration to configure the permissions.]
 * @author Ankit Mishra
 * @since 16/11/21
 *
 * @param activity Provide the activity in which this class instance will be used.
 *
 * @see setConfiguration
 */
class AppPermissions(
    private val activity: ComponentActivity
) {
    /**
     * For configuring the AppPermissions class.
     */
    data class Configuration(
        val isLocationPermissionRequired: Boolean = false,
        val isBluetoothScanPermissionRequired: Boolean = false,
        val isBluetoothConnectPermissionRequired: Boolean = false,
        val isBackgroundLocationRequired: Boolean = false,
        val isWriteExternalStoragePermissionRequired: Boolean = false,//This won't work for Android OS 11 and above.
        val isReadAllStoragePermissionRequired: Boolean = false,
        val isReadImagePermissionRequired: Boolean = false,
        val isReadVideoPermissionRequired: Boolean = false,
        val isReadAudioPermissionRequired: Boolean = false,
        val isCameraPermissionRequired: Boolean = false,
    )

    //Permission codes for asking permissions.
    private val isOsAndroid13OrAbove get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val isOsAndroid12OrAbove get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    private val isOsAndroid10OrAbove get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * This is configuration object which we be set by user according to his/her permission requirement.
     */
    private var configuration: Configuration? = null

    /**
     * Create an empty list of required permissions to request runtime
     */
    private val list = arrayListOf<String>()


    /**
     * This will contain the dialog for showing location information to user.
     */
    private var dialogShowLocationInformation: Dialog? = null


    /**
     * This will contain the title for the showing location information dialog.
     */
    private var showLocationInformationDialogTitle = "Background Location Required"

    /**
     * This will contain the Message for the showing location information dialog.
     */
    private var showLocationInformationDialogMessage =
        "Please \"Allow all the time\" permission for location access from the settings which is necessary for the search beacon functionality."

    /**
     * This will contain the positive button text for the showing location information dialog.
     */
    private var showLocationInformationDialogPositiveButtonText = "Yes"

    /**
     * This will contain the negative button text for the showing location information dialog.
     */
    private var showLocationInformationDialogNegativeButtonText = "No"

    /**
     * This is permission denied dialog title.
     */
    private var permissionDeniedDialogTitle = "Permission Denied"

    /**
     * If permission denied message is not found in the permissionDeniedMessages map then this will be shown.
     */
    private val permissionDeniedDefaultMessage =
        "It seems like you have denied some permission which is necessary for the application. Please allow all the permissions from the settings."

    /**
     * This map contains all the permission denied messages for all different permissions.
     */
    @SuppressLint("InlinedApi")
    private val permissionDeniedMessages: MutableMap<String, String> =
        mutableMapOf(
            Manifest.permission.READ_EXTERNAL_STORAGE to "It seems like you have denied the storage permission which is necessary for the application. Please allow permission for storage from the settings.",
            Manifest.permission.READ_MEDIA_IMAGES to "It seems like you have denied the image access permission which is necessary for the application. Please allow permission for image access from the settings.",
            Manifest.permission.READ_MEDIA_VIDEO to "It seems like you have denied the video access permission which is necessary for the application. Please allow permission for video access from the settings.",
            Manifest.permission.READ_MEDIA_AUDIO to "It seems like you have denied the audio access permission which is necessary for the application. Please allow permission for audio access from the settings.",
            Manifest.permission.WRITE_EXTERNAL_STORAGE to "It seems like you have denied the storage permission which is necessary for the application. Please allow permission for storage from the settings.",
            Manifest.permission.CAMERA to "It seems like you have denied the camera permission which is necessary for the application. Please allow permission for camera from the settings.",
            Manifest.permission.ACCESS_FINE_LOCATION to "It seems like you have denied the location permission which is necessary for the application. Please allow permission for location access from the settings.",
            Manifest.permission.ACCESS_COARSE_LOCATION to "It seems like you have denied the location permission which is necessary for the application. Please allow permission for location access from the settings.",
            Manifest.permission.BLUETOOTH to "It seems like you have denied the bluetooth permission which is necessary for the application. Please allow permission for bluetooth from the settings.",
            Manifest.permission.BLUETOOTH_ADMIN to "It seems like you have denied the bluetooth permission which is necessary for the application. Please allow permission for bluetooth from the settings.",
            Manifest.permission.BLUETOOTH_SCAN to "It seems like you have denied the bluetooth permission which is necessary for the application. Please allow permission for bluetooth from the settings.",
            Manifest.permission.BLUETOOTH_CONNECT to "It seems like you have denied the bluetooth permission which is necessary for the application. Please allow permission for bluetooth from the settings."
        )
    private val multiPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var onMultiPermissionsResult: ((Map<String, Boolean>) -> Unit)? = null
    private var onCheckPermissionsSuccess: (() -> Unit)? = null


    fun setOnMultiPermissionsResult(onPermissionsResult: (Map<String, Boolean>) -> Unit) {
        onMultiPermissionsResult = onPermissionsResult
    }

    init {
        /**
         * Adding Background Location Permission Denied message in the map.
         */
        if (isOsAndroid10OrAbove) {
            permissionDeniedMessages[Manifest.permission.ACCESS_BACKGROUND_LOCATION] =
                "Please \"Allow all the time\" permission for location access from the settings which is necessary for the application."
        }

        multiPermissionsLauncher =
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                permissions.entries.forEach {
                    Log.d("AppPermissions", "${it.key} =>>> ${it.value}")
                }

                if (configuration?.isBackgroundLocationRequired == true
                    && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    && permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    && isOsAndroid10OrAbove
                ) {
                    showLocationInformationPopup()
                }

                //Returning result for user who want to handle it differently
                onMultiPermissionsResult?.invoke(permissions)
                if (permissions.isNotEmpty()) {
                    if(permissions.values.all { it }) {
                        onCheckPermissionsSuccess?.invoke()
                        onCheckPermissionsSuccess = null
                    }
                }
            }
    }

    /**
     * Description: This method is used for the purpose of setting up which permission are required by the app.
     * When user will call checkPermissions() method then only those permissions will be checked which was set true using this function.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     *
     * @param configuration Provide configuration instance.
     */
    fun setConfiguration(
        configuration: Configuration
    ) {
        /**
         * Setting up the configuration
         */
        this.configuration = configuration
        /**
         * Resetting the permissions list
         */
        resetPermissionList()
    }

    /**
     * Description: This method is used for the purpose of resetting permissions list according to the new booleans.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     */
    private fun resetPermissionList() {
        /**
         * Removing Previous Permissions.
         */
        list.clear()
        configuration?.let { config ->


            /**
             * Adding new Permissions.
             */
            if (config.isWriteExternalStoragePermissionRequired) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            if (config.isReadAllStoragePermissionRequired) {
                if (isOsAndroid13OrAbove) {
                    list.add(Manifest.permission.READ_MEDIA_IMAGES)
                    list.add(Manifest.permission.READ_MEDIA_VIDEO)
                    list.add(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

            if (config.isReadImagePermissionRequired) {
                list.add(if (isOsAndroid13OrAbove) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (config.isReadVideoPermissionRequired) {
                list.add(if (isOsAndroid13OrAbove) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (config.isReadAudioPermissionRequired) {
                list.add(if (isOsAndroid13OrAbove) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (config.isCameraPermissionRequired) {
                list.add(Manifest.permission.CAMERA)
            }

            if (config.isBluetoothScanPermissionRequired || config.isBluetoothConnectPermissionRequired) {
                list.add(Manifest.permission.BLUETOOTH)
                list.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (isOsAndroid12OrAbove) {
                if (config.isBluetoothScanPermissionRequired) {
                    list.add(Manifest.permission.BLUETOOTH_SCAN)
                }
                if (config.isBluetoothConnectPermissionRequired) {
                    list.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }

            if (config.isLocationPermissionRequired) {
                list.add(Manifest.permission.ACCESS_FINE_LOCATION)
                list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            if (config.isBackgroundLocationRequired && isOsAndroid10OrAbove) {
                list.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }



            list.removeDuplicateElements()

        }
    }


    /**
     * Description: This method checks if all the permissions are granted by the user or not.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     *
     * @param listOfPermissions Provide the list of permissions which are required to be checked. By default, the permissions set by configuration method will be checked.
     */
    private fun isPermissionsGranted(listOfPermissions: ArrayList<String> = list): Boolean {
        // PERMISSION_GRANTED : Constant Value: 0
        // PERMISSION_DENIED : Constant Value: -1
        var counter = 0
        for (permission in listOfPermissions) {
            counter += ContextCompat.checkSelfPermission(activity, permission)
        }
        return counter == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Description: This method is used for the purpose of checking if a single permission is granted or not.
     *
     * @author Ankit Mishra
     * @since 18/11/21
     */
    fun isPermissionGranted(permission: String) =
        isPermissionsGranted(arrayListOf(permission))


    /**
     * Description: This method is used for the purpose of finding out if the permission denied dialog should be shown.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     *
     * @return true if the permission denied dialog is required to be shown else false.
     */
    private fun shouldShowPermissionDeniedDialog(permission: String) =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    /**
     * Description: This method is used for the purpose of checking if the permission is denied.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     *
     * @return true if the permission is not granted yet else false.
     */
    private fun isPermissionNotGranted(permission: String) = ContextCompat.checkSelfPermission(
        activity,
        permission
    ) != PackageManager.PERMISSION_GRANTED


    fun checkIfPermissionsGranted(permissions: List<String>, onSuccess: () -> Unit) {
        onCheckPermissionsSuccess = onSuccess
        var isAllPermissionsGranted = true
        for (permission in permissions) {
            /**
             * Checking if the permission is granted or not.
             */
            if (isPermissionNotGranted(permission)) {
                isAllPermissionsGranted = false
                /**
                 * Checking if the permission is denied in that case should we show the dialog.
                 */
                if (shouldShowPermissionDeniedDialog(permission)) {
                    /**
                     * Showing permission denied dialog.
                     */
                    showPermissionDeniedDialog(permission)
                } else {
                    val listWithoutBackgroundPermissions = permissions.toMutableList()
                    /**
                     * Removing background permission from the list as it has to be asked separately.
                     */
                    if (isOsAndroid10OrAbove) {
                        listWithoutBackgroundPermissions.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    /**
                     * Asking permissions to the user.
                     */
                    askPermissionsToUser(ArrayList(listWithoutBackgroundPermissions))
                }
                break
            }
        }
        if (isAllPermissionsGranted) {
            onSuccess()
            onCheckPermissionsSuccess = null
        }
    }

    /**
     * Description: This method will check the permissions required by the application.
     *
     * [NOTE: Only those permissions are checked which are set required using whichPermissionsAreRequired function.]
     *
     * @see setConfiguration
     *
     *
     * @author Ankit Mishra
     * @since 16/11/21
     */
    fun checkPermissions() {
        for (permission in list) {
            /**
             * Checking if the permission is granted or not.
             */
            if (isPermissionNotGranted(permission)) {
                /**
                 * Checking if the permission is denied in that case should we show the dialog.
                 */
                if (shouldShowPermissionDeniedDialog(permission)) {
                    /**
                     * Showing permission denied dialog.
                     */
                    showPermissionDeniedDialog(permission)
                } else {
                    val listWithoutBackgroundPermissions = list.toMutableList()
                    /**
                     * Removing background permission from the list as it has to be asked separately.
                     */
                    if (isOsAndroid10OrAbove) {
                        listWithoutBackgroundPermissions.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    /**
                     * Asking permissions to the user.
                     */
                    askPermissionsToUser(ArrayList(listWithoutBackgroundPermissions))
                }
                break
            }
        }
    }

    /**
     * Description: This method is used for the purpose of asking single permission.
     *
     * @author Ankit Mishra
     * @since 18/11/21
     *
     * @param permission Provide the permission which needed to be checked and asked.
     * @param permissionDeniedTitle Provide the permission denied title which will be shown in the permission denied dialog in the case of permission denied.
     * @param permissionDeniedMessage Provide the permission Denied message which will be shown in case of permission denied.
     */
    fun askSinglePermission(
        permission: String,
        permissionDeniedTitle: String? = null,
        permissionDeniedMessage: String? = null
    ) {
        /**
         * Setting up the permission denied title if provided.
         */
        permissionDeniedTitle?.let { title ->
            permissionDeniedDialogTitle = title
        }
        /**
         * Setting up the permission denied message if provided.
         */
        permissionDeniedMessage?.let { message ->
            permissionDeniedMessages[permission] = message
        }
        /**
         * Checking if the permission is already granted.
         */
        if (isPermissionNotGranted(permission)) {
            /**
             * Checking if the permission is denied in that case should we show the dialog.
             */
            if (shouldShowPermissionDeniedDialog(permission)) {
                /**
                 * Showing Permission Denied Dialog.
                 */
                showPermissionDeniedDialog(permission)
            } else {
                /**
                 * Asking permission to user
                 */
                if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
                    if (isOsAndroid10OrAbove) {
                        showLocationInformationPopup()
                    }
                } else {
                    askPermissionsToUser(permissions = arrayListOf(permission))
                }
            }
        }

    }

    /**
     * Description: This method is used for the purpose of asking permissions to the Android User.
     *
     * @author Ankit Mishra
     * @since 18/11/21
     *
     * @param permissions Provide the array list of permissions which needed to asked.
     * @param permissionsRequestCode Provide permission request code. It is not mandatory to provide.
     */
    private fun askPermissionsToUser(
        permissions: ArrayList<String>
    ) {
        multiPermissionsLauncher.launch(permissions.toTypedArray())
    }


    /**
     * Description: This method is used for the purpose of setting location information dialog strings.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     *
     * @param title Provide the text which should be displayed on title.
     * @param message Provide the text which should be displayed on dialog message.
     * @param positiveButtonText Provide the text which should be displayed on the positive button.
     * @param negativeButtonText Provide the text which should be displayed on the negative button.
     */
    fun setLocationInformationDialog(
        title: String? = null,
        message: String? = null,
        positiveButtonText: String? = null,
        negativeButtonText: String? = null
    ) {
        title?.let { showLocationInformationDialogTitle = title }
        message?.let { showLocationInformationDialogMessage = message }
        positiveButtonText?.let {
            showLocationInformationDialogPositiveButtonText = positiveButtonText
        }
        negativeButtonText?.let {
            showLocationInformationDialogNegativeButtonText = negativeButtonText
        }
    }


    /**
     * Description: This method is used for the purpose of showing location information popup which will describe user the usage of background permission.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showLocationInformationPopup() {
        if (dialogShowLocationInformation == null) {
            dialogShowLocationInformation = AlertDialog.Builder(activity).apply {
                setTitle(showLocationInformationDialogTitle)
                setCancelable(false)
                setMessage(showLocationInformationDialogMessage)
                setPositiveButton(showLocationInformationDialogPositiveButtonText) { dialog, _ ->
                    dialog.dismiss()
                    /**
                     * Asking for the background location permission.
                     */
                    askPermissionsToUser(
                        arrayListOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    )
                }
                setNegativeButton(showLocationInformationDialogNegativeButtonText) { dialog, _ ->
                    dialog.dismiss()
                }
            }.create()
        }
        if (!activity.isFinishing && !activity.isDestroyed && dialogShowLocationInformation?.isShowing == false) {
            dialogShowLocationInformation?.show()
        }
    }

    /**
     * Description: This method is used for the purpose of setting permission denied message for provided permission.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     *
     * @param permission Provide permission associated with the denied message
     * @param permissionDeniedMessage Provide permission denied message which will be shown in the dialog if that permission is denied by the user.
     */
    fun setPermissionDeniedMessage(permission: String, permissionDeniedMessage: String) {
        permissionDeniedMessages[permission] = permissionDeniedMessage
    }

    /**
     * Description: This method is used for the purpose of setting up the permission denied dialog title.
     *
     * @author Ankit Mishra
     * @since 18/11/21
     *
     * @param title Provide the Permission Denied Dialog Title.
     */
    fun setPermissionDeniedTitle(title: String) {
        permissionDeniedDialogTitle = title
    }

    /**
     * Description: This method is used for the purpose of showing permission denied dialog.
     *
     * @author Ankit Mishra
     * @since 16/11/21
     *
     * @param permission Provide permission which is denied by the user.
     */
    private fun showPermissionDeniedDialog(permission: String) {
        val permissionDeniedDialog = AlertDialog.Builder(activity).apply {
            setTitle(permissionDeniedDialogTitle)
            setCancelable(false)
            setMessage(permissionDeniedMessages[permission] ?: permissionDeniedDefaultMessage)
            setPositiveButton(showLocationInformationDialogPositiveButtonText) { dialog, _ ->
                dialog.dismiss()
                //Going to settings menu to approve the permission
                navigateToAppSettings()
            }
            setNegativeButton(showLocationInformationDialogNegativeButtonText) { dialog, _ ->
                dialog.dismiss()
            }
        }.create()

        if (!activity.isFinishing && !activity.isDestroyed) {
            permissionDeniedDialog.show()
        }
    }

    /**
     * Description: This method is used for the purpose of navigating to the App Settings.
     *
     * @author Ankit Mishra
     * @since 18/11/21
     */
    fun navigateToAppSettings() {
        //Going to settings menu to approve the permission
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }


    private fun <T> ArrayList<T>.removeDuplicateElements() {
        val newList = distinct()
        clear()
        addAll(newList)
    }
}
