package com.shivafamily.smstogglecontroller

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
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
        if (!PhoneNumberUtils.compare(sender, trustedNumber) ||
            parts.size != 2 ||
            !parts[0].equals(expectedPin, ignoreCase = true)) {
            Log.i(TAG, "Ignored SMS: sender or PIN did not match.")
            return
        }

        when (parts[1].trim().uppercase()) {
            "LIGHT" -> toggleFlashlight(context)
            "BLUETOOTH" -> toggleBluetooth(context)
            else -> Log.i(TAG, "Ignored SMS: unknown action.")
        }
    }

    private fun toggleFlashlight(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission is missing; flashlight command was not run.")
            return
        }
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val flashCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: run {
                Log.w(TAG, "No flash unit is available on this device.")
                return
            }

            val prefs = context.getSharedPreferences("controller_settings", Context.MODE_PRIVATE)
            val isCurrentlyOn = prefs.getBoolean(KEY_FLASHLIGHT_ON, false)
            cameraManager.setTorchMode(flashCameraId, !isCurrentlyOn)
            prefs.edit().putBoolean(KEY_FLASHLIGHT_ON, !isCurrentlyOn).apply()
            Log.i(TAG, "Authorized flashlight toggle command completed.")
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
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter ?: run {
                Log.w(TAG, "Bluetooth is not supported on this device.")
                return
            }
            val accepted = if (adapter.isEnabled) adapter.disable() else adapter.enable()
            if (accepted) {
                Log.i(TAG, "Bluetooth toggle request accepted.")
            } else {
                Log.w(TAG, "Android or the device manufacturer rejected the Bluetooth toggle request.")
            }
        } catch (error: SecurityException) {
            Log.e(TAG, "Bluetooth permission or OS restriction prevented the toggle.", error)
        } catch (error: Exception) {
            Log.e(TAG, "Bluetooth toggle failed.", error)
        }
    }

    companion object {
        private const val TAG = "SmsToggleController"
        private const val KEY_FLASHLIGHT_ON = "flashlight_on"
    }
}
