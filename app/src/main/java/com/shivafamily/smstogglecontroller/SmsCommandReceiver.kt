package com.shivafamily.smstogglecontroller

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.util.Log

class SmsCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("controller_settings", Context.MODE_PRIVATE)
        val trustedNumber = prefs.getString(MainActivity.KEY_PHONE, null) ?: return
        val expectedPin = prefs.getString(MainActivity.KEY_COMMAND, null) ?: return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }.trim()
        val parts = body.split(Regex("\\s+"), limit = 2)

        if (!PhoneNumberUtils.compare(sender, trustedNumber) || parts.size != 2 ||
            !parts[0].equals(expectedPin, ignoreCase = true)) {
            Log.i(TAG, "Ignored SMS: sender or PIN did not match.")
            return
        }

        when (parts[1].trim().uppercase()) {
            "LIGHT" -> toggleFlashlight(context)
            "BLUETOOTH" -> toggleBluetooth(context)
            "DND" -> toggleDoNotDisturb(context)
            "LOCATION" -> toggleLocation(context)
            else -> Log.i(TAG, "Ignored SMS: unknown action.")
        }
    }

    private fun toggleFlashlight(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission is missing.")
            return
        }
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val flashId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            val prefs = context.getSharedPreferences("controller_settings", Context.MODE_PRIVATE)
            val next = !prefs.getBoolean(KEY_FLASHLIGHT_ON, false)
            cameraManager.setTorchMode(flashId, next)
            prefs.edit().putBoolean(KEY_FLASHLIGHT_ON, next).apply()
            Log.i(TAG, "Flashlight toggle request completed.")
        } catch (error: Exception) {
            Log.e(TAG, "Flashlight toggle failed.", error)
        }
    }

    @Suppress("DEPRECATION")
    private fun toggleBluetooth(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Bluetooth Connect permission is missing.")
            return
        }
        try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: return
            val accepted = if (adapter.isEnabled) adapter.disable() else adapter.enable()
            if (!accepted) Log.w(TAG, "Bluetooth toggle rejected by Android or device firmware.")
        } catch (error: Exception) {
            Log.e(TAG, "Bluetooth toggle failed or is restricted by Android.", error)
        }
    }

    private fun toggleDoNotDisturb(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!manager.isNotificationPolicyAccessGranted) {
                Log.w(TAG, "Grant Do Not Disturb access in the app first.")
                return
            }
            val nextFilter = if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                NotificationManager.INTERRUPTION_FILTER_NONE
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            manager.setInterruptionFilter(nextFilter)
            Log.i(TAG, "Do Not Disturb filter change requested.")
        } catch (error: Exception) {
            Log.e(TAG, "Do Not Disturb toggle failed.", error)
        }
    }

    @Suppress("DEPRECATION")
    private fun toggleLocation(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location toggle needs one-time ADB grant: adb shell pm grant $PACKAGE_NAME android.permission.WRITE_SECURE_SETTINGS")
            return
        }
        try {
            val resolver = context.contentResolver
            val currentMode = Settings.Secure.getInt(
                resolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            val nextMode = if (currentMode == Settings.Secure.LOCATION_MODE_OFF) {
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            } else {
                Settings.Secure.LOCATION_MODE_OFF
            }
            val changed = Settings.Secure.putInt(resolver, Settings.Secure.LOCATION_MODE, nextMode)
            if (!changed) Log.w(TAG, "Android did not accept the Location mode change.")
            else Log.i(TAG, "Location mode changed to $nextMode.")
        } catch (error: Exception) {
            Log.e(TAG, "Location toggle failed or is restricted by this Android build.", error)
        }
    }

    companion object {
        private const val TAG = "SmsToggleController"
        private const val KEY_FLASHLIGHT_ON = "flashlight_on"
        private const val PACKAGE_NAME = "com.shivafamily.smstogglecontroller"
    }
}
