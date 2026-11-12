package com.shivafamily.smstogglecontroller

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var cameraManager: CameraManager
    private var flashCameraId: String? = null
    private var torchEnabled = false
    private lateinit var flashButton: Button
    private lateinit var dndButton: Button

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashCameraId) {
                torchEnabled = enabled
                if (::flashButton.isInitialized) updateFlashLabel()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        flashCameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
        cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 32)
        }

        val title = TextView(this).apply {
            text = "Device Controller"
            textSize = 26f
        }
        val description = TextView(this).apply {
            text = "Control supported phone features from this screen. Android may open system settings for features that apps cannot change directly."
            textSize = 15f
            setPadding(0, 12, 0, 20)
        }

        flashButton = Button(this).apply {
            setOnClickListener { toggleFlashlight() }
        }
        dndButton = Button(this).apply {
            text = "Do Not Disturb"
            setOnClickListener { toggleDnd() }
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(flashButton)
        layout.addView(dndButton)
        layout.addView(makeSettingsButton("Bluetooth settings", Settings.ACTION_BLUETOOTH_SETTINGS))
        layout.addView(makeSettingsButton("Wi-Fi settings", Settings.ACTION_WIFI_SETTINGS))
        layout.addView(makeSettingsButton("Location settings", Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        layout.addView(makeSettingsButton("Airplane mode settings", Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        layout.addView(makeSettingsButton("Network / mobile data settings", Settings.ACTION_WIRELESS_SETTINGS))

        setContentView(layout)
        updateFlashLabel()
        updateDndLabel()

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::dndButton.isInitialized) updateDndLabel()
    }

    private fun makeSettingsButton(label: String, action: String): Button {
        return Button(this).apply {
            text = label
            setOnClickListener {
                try {
                    startActivity(Intent(action))
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "This settings page is not available on this phone.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleFlashlight() {
        val cameraId = flashCameraId
        if (cameraId == null) {
            Toast.makeText(this, "This phone does not report a camera flash.", Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
            return
        }
        try {
            cameraManager.setTorchMode(cameraId, !torchEnabled)
        } catch (error: Exception) {
            Toast.makeText(this, "Flashlight could not be changed: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateFlashLabel() {
        if (::flashButton.isInitialized) {
            flashButton.text = if (torchEnabled) "Flashlight: ON (tap to turn OFF)" else "Flashlight: OFF (tap to turn ON)"
        }
    }

    private fun toggleDnd() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(this, "Do Not Disturb control needs Android 6.0 or later.", Toast.LENGTH_LONG).show()
            return
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Allow Do Not Disturb access on the next screen, then return here.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        try {
            val next = if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                NotificationManager.INTERRUPTION_FILTER_NONE
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            manager.setInterruptionFilter(next)
            updateDndLabel()
        } catch (error: Exception) {
            Toast.makeText(this, "Do Not Disturb could not be changed: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateDndLabel() {
        if (!::dndButton.isInitialized) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            dndButton.text = "Do Not Disturb: not supported"
            return
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        dndButton.text = when {
            !manager.isNotificationPolicyAccessGranted -> "Do Not Disturb: grant access"
            manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL -> "Do Not Disturb: OFF (tap to turn ON)"
            else -> "Do Not Disturb: ON (tap to turn OFF)"
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
    }
}
