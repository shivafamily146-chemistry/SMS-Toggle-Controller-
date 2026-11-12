package com.shivafamily.smstogglecontroller

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("controller_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "SMS Toggle Controller"
            textSize = 24f
        }
        val help = TextView(this).apply {
            text = "Enter the trusted sender number and secret SMS command. The first supported action will be FLASHLIGHT (LIGHT)."
            textSize = 15f
        }
        val phoneInput = EditText(this).apply {
            hint = "Trusted phone number (with country code)"
            setSingleLine(true)
            setText(prefs.getString(KEY_PHONE, ""))
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val commandInput = EditText(this).apply {
            hint = "Secret command, e.g. 4826 LIGHT"
            setSingleLine(true)
            setText(prefs.getString(KEY_COMMAND, ""))
        }
        val saveButton = Button(this).apply {
            text = "Save settings"
            setOnClickListener {
                val phone = phoneInput.text.toString().trim()
                val command = commandInput.text.toString().trim()
                if (phone.isBlank() || command.isBlank()) {
                    Toast.makeText(this@MainActivity, "Enter both fields.", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit()
                        .putString(KEY_PHONE, phone)
                        .putString(KEY_COMMAND, command)
                        .apply()
                    Toast.makeText(this@MainActivity, "Settings saved on this phone.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(title)
        layout.addView(help)
        layout.addView(phoneInput)
        layout.addView(commandInput)
        layout.addView(saveButton)
        setContentView(layout)

        val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.CAMERA)
        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    companion object {
        const val KEY_PHONE = "trusted_phone"
        const val KEY_COMMAND = "secret_command"
        const val REQUEST_PERMISSIONS = 1001
    }
}
