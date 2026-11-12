package com.shivafamily.smstogglecontroller

import android.Manifest
import android.app.NotificationManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Telephony
import android.provider.Settings
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
            "LOCATION" -> openLocationSettingsNotice(context)
            else -> Log.i(TAG, "Ignored SMS: unknown action.")
        }
    }


    private fun openLocationSettingsNotice(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Notification permission is missing; open the app and allow notifications.")
            return
        }
        try {
            val channelId = "location_settings"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(channelId, "Location settings requests", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(context, 2001, settingsIntent, pendingFlags)
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, channelId)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
            val notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Location settings")
                .setContentText("Tap here to open Location settings and turn Location on.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(2001, notification)
        } catch (error: Exception) {
            Log.e(TAG, "Could not show Location settings notification.", error)
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

    companion object {
        private const val TAG = "SmsToggleController"
        private const val KEY_FLASHLIGHT_ON = "flashlight_on"
    }
}
