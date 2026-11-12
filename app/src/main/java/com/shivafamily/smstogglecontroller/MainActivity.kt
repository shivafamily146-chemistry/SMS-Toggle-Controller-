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
            text = "Set one trusted sender and the command text. Only matching messages will be considered."
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

        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION)
        }
    }

    companion object {
        const val KEY_PHONE = "trusted_phone"
        const val KEY_COMMAND = "secret_command"
        const val REQUEST_SMS_PERMISSION = 1001
    }
}
