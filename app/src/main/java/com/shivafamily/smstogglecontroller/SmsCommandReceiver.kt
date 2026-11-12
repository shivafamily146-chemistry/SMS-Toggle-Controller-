package com.shivafamily.smstogglecontroller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        val senderMatches = PhoneNumberUtils.compare(sender, trustedNumber)
        if (!senderMatches || !body.equals(expectedCommand, ignoreCase = true)) {
            Log.i(TAG, "Ignored SMS: sender or command did not match.")
            return
        }

        // No device setting is changed yet. Permission-aware action handling
        // will be added in the next implementation step.
        Log.i(TAG, "Authorized SMS command received.")
    }

    companion object {
        private const val TAG = "SmsToggleController"
    }
}
