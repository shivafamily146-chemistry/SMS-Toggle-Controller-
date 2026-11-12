package com.shivafamily.smstogglecontroller

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.telephony.PhoneNumberUtils
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
            text = "Set one trusted sender and a secret PIN. Send: PIN LIGHT, PIN BLUETOOTH, or PIN DND. Example: 4826 LIGHT"
            textSize = 15f
        }
        val phoneInput = EditText(this).apply {
            hint = "Trusted phone number (with country code)"
            setSingleLine(true)
            setText(prefs.getString(KEY_PHONE, ""))
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val pinInput = EditText(this).apply {
            hint = "Secret PIN (4 to 12 digits)"
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(12))
            setText(prefs.getString(KEY_COMMAND, ""))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val saveButton = Button(this).apply {
            text = "Save settings"
            setOnClickListener {
                val phone = phoneInput.text.toString().trim()
                val pin = pinInput.text.toString().trim()
                val validPhone = PhoneNumberUtils.isGlobalPhoneNumber(phone) &&
                    phone.count { it.isDigit() } >= 7
                val validPin = pin.length in 4..12 && pin.all { it in '0'..'9' }
                if (!validPhone || !validPin) {
                    Toast.makeText(
                        this@MainActivity,
                        "Enter a valid phone number and a PIN containing 4 to 12 digits.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    prefs.edit().putString(KEY_PHONE, phone).putString(KEY_COMMAND, pin).apply()
                    Toast.makeText(this@MainActivity, "Settings saved on this phone.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val dndAccessButton = Button(this).apply {
            text = "Grant Do Not Disturb access"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
            }
        }

        layout.addView(title)
        layout.addView(help)
        layout.addView(phoneInput)
        layout.addView(pinInput)
        layout.addView(saveButton)
        layout.addView(dndAccessButton)
        setContentView(layout)

        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        val missing = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), REQUEST_PERMISSIONS)
    }

    companion object {
        const val KEY_PHONE = "trusted_phone"
        const val KEY_COMMAND = "secret_command"
        const val REQUEST_PERMISSIONS = 1001
    }
}
