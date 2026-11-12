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
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var cameraManager: CameraManager
    private var flashCameraId: String? = null
    private var torchEnabled = false
    private lateinit var flashButton: Button
    private lateinit var dndButton: Button
    private lateinit var shizukuStatus: TextView
    private val executor = Executors.newSingleThreadExecutor()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashCameraId) {
                torchEnabled = enabled
                if (::flashButton.isInitialized) updateFlashLabel()
            }
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener { updateShizukuStatus() }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST) {
            updateShizukuStatus()
            toast(if (grantResult == PackageManager.PERMISSION_GRANTED) "Shizuku permission granted." else "Shizuku permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addBinderReceivedListener(binderListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        flashCameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) { null }
        cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 32)
        }
        layout.addView(TextView(this).apply { text = "Device Controller"; textSize = 26f })
        layout.addView(TextView(this).apply {
            text = "Shizuku must be running. On Android 11+, start it using Wireless debugging. After reboot, start Shizuku again."
            textSize = 14f
            setPadding(0, 12, 0, 12)
        })
        shizukuStatus = TextView(this).apply { textSize = 16f; setPadding(0, 8, 0, 8) }
        layout.addView(shizukuStatus)
        layout.addView(Button(this).apply {
            text = "Grant Shizuku permission"
            setOnClickListener { requestShizukuPermission() }
        })

        flashButton = Button(this).apply { setOnClickListener { toggleFlashlight() } }
        dndButton = Button(this).apply { text = "Do Not Disturb"; setOnClickListener { toggleDnd() } }
        layout.addView(flashButton)
        layout.addView(dndButton)
        layout.addView(makeShizukuButton("Bluetooth", "settings get global bluetooth_on", "svc bluetooth"))
        layout.addView(makeShizukuButton("Wi-Fi", "settings get global wifi_on", "svc wifi"))
        layout.addView(makeShizukuButton("Mobile Data", "settings get global mobile_data", "svc data"))
        layout.addView(makeShizukuButton("Location", "cmd location is-location-enabled", "cmd location set-location-enabled"))
        layout.addView(makeShizukuButton("Airplane Mode", "cmd connectivity airplane-mode", "cmd connectivity airplane-mode"))

        setContentView(layout)
        updateFlashLabel()
        updateDndLabel()
        updateShizukuStatus()

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::dndButton.isInitialized) updateDndLabel()
        if (::shizukuStatus.isInitialized) updateShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        executor.shutdown()
    }

    private fun makeShizukuButton(label: String, readCommand: String, toggleCommand: String): Button {
        return Button(this).apply {
            text = "$label: tap to toggle"
            setOnClickListener { runToggle(label, readCommand, toggleCommand) }
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                toast("Start Shizuku first, then return to this app.")
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://shizuku.rikka.app/")))
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                toast("Shizuku permission is already granted.")
            } else {
                Shizuku.requestPermission(SHIZUKU_REQUEST)
            }
            updateShizukuStatus()
        } catch (error: Exception) {
            toast("Shizuku is not ready: ${error.message}")
        }
    }

    private fun updateShizukuStatus() {
        if (!::shizukuStatus.isInitialized) return
        val status = try {
            when {
                !Shizuku.pingBinder() -> "Shizuku: not running"
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "Shizuku: connected and permission granted"
                else -> "Shizuku: connected; permission needed"
            }
        } catch (_: Exception) { "Shizuku: not connected" }
        runOnUiThread { shizukuStatus.text = status }
    }

    private fun runToggle(label: String, readCommand: String, toggleCommand: String) {
        if (!hasShizukuPermission()) {
            toast("Start Shizuku and grant permission first.")
            return
        }
        executor.execute {
            try {
                val command = when (label) {
                    "Location" -> "if cmd location is-location-enabled | grep -qi true; then cmd location set-location-enabled false; else cmd location set-location-enabled true; fi"
                    "Airplane Mode" -> "if [ \"$(cmd connectivity airplane-mode)\" = \"enabled\" ]; then cmd connectivity airplane-mode disable; else cmd connectivity airplane-mode enable; fi"
                    else -> "if [ \"$(${readCommand})\" = \"1\" ]; then ${toggleCommand} disable; else ${toggleCommand} enable; fi"
                }
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                val exit = process.waitFor()
                runOnUiThread {
                    val detail = if (error.isNotBlank()) error.trim() else output.trim()
                    Toast.makeText(this, if (exit == 0) "$label command sent." else "$label failed: $detail", Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                runOnUiThread { Toast.makeText(this, "$label failed: ${error.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun hasShizukuPermission(): Boolean {
        return try { Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
        catch (_: Exception) { false }
    }

    private fun toast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    private fun toggleFlashlight() {
        val cameraId = flashCameraId
        if (cameraId == null) { toast("This phone does not report a camera flash."); return }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA); return
        }
        try { cameraManager.setTorchMode(cameraId, !torchEnabled) }
        catch (error: Exception) { toast("Flashlight could not be changed: ${error.message}") }
    }

    private fun updateFlashLabel() {
        if (::flashButton.isInitialized) flashButton.text =
            if (torchEnabled) "Flashlight: ON (tap to turn OFF)" else "Flashlight: OFF (tap to turn ON)"
    }

    private fun toggleDnd() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); return
        }
        try {
            val next = if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL)
                NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            manager.setInterruptionFilter(next)
            updateDndLabel()
        } catch (error: Exception) { toast("DND failed: ${error.message}") }
    }

    private fun updateDndLabel() {
        if (!::dndButton.isInitialized) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        dndButton.text = when {
            !manager.isNotificationPolicyAccessGranted -> "Do Not Disturb: grant access"
            manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL -> "Do Not Disturb: OFF (tap to turn ON)"
            else -> "Do Not Disturb: ON (tap to turn OFF)"
        }
    }

    companion object {
        private const val REQUEST_CAMERA = 1001
        private const val SHIZUKU_REQUEST = 2001
    }
}
