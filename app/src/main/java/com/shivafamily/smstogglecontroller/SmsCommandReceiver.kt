package com.shivafamily.smstogglecontroller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.util.Log

class SmsCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("controller_settings", Context.MODE_PRIVATE)
        val trustedNumber = prefs.getString(MainActivity.KEY_PHONE, null) ?: return
        val expectedCommand = prefs.getString(MainActivity.KEY_COMMAND, null) ?: return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }.trim()

        if (!PhoneNumberUtils.compare(sender, trustedNumber) ||
            !body.equals(expectedCommand, ignoreCase = true)) {
            Log.i(TAG, "Ignored SMS: sender or command did not match.")
            return
        }

        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

            val isCurrentlyOn = prefs.getBoolean(KEY_FLASHLIGHT_ON, false)
            cameraManager.setTorchMode(flashCameraId, !isCurrentlyOn)
            prefs.edit().putBoolean(KEY_FLASHLIGHT_ON, !isCurrentlyOn).apply()
            Log.i(TAG, "Authorized flashlight toggle command completed.")
        } catch (error: Exception) {
            Log.e(TAG, "Flashlight toggle failed.", error)
        }
    }

    companion object {
        private const val TAG = "SmsToggleController"
        private const val KEY_FLASHLIGHT_ON = "flashlight_on"
    }
}
