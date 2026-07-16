package com.sync.xxx

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject

class NotificationInterceptorService : NotificationListenerService() {

    companion object {
        const val TAG = "NotifInterceptor"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        
        try {
            val packageName = sbn.packageName ?: "unknown"
            
            // Ignore our own notifications
            if (packageName == "com.sync.xxx") return
            
            val notification = sbn.notification ?: return
            val extras = notification.extras
            
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            
            // Get app name
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
            
            // Only forward meaningful notifications
            if (title.isNotBlank() || bigText.isNotBlank()) {
                forwardNotificationToService(
                    packageName,
                    appName,
                    title,
                    bigText,
                    subText,
                    sbn.postTime
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: track when notifications are dismissed
    }

    private fun forwardNotificationToService(
        pkg: String,
        appName: String,
        title: String,
        message: String,
        subText: String,
        time: Long
    ) {
        try {
            val intent = Intent("com.sync.xxx.NOTIF_INTERCEPT")
            intent.setPackage(packageName)
            intent.putExtra("package", pkg)
            intent.putExtra("appName", appName)
            intent.putExtra("title", title)
            intent.putExtra("message", message)
            intent.putExtra("subText", subText)
            intent.putExtra("time", time)
            sendBroadcast(intent)
            
            Log.d(TAG, "Notification forwarded: $appName - $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward notification: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }
}
