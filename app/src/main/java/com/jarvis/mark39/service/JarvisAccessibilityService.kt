package com.jarvis.mark39.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Full phone UI control via AccessibilityService.
 * Enable: Settings → Accessibility → JARVIS.
 */
class JarvisAccessibilityService : AccessibilityService() {

    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { _foregroundPackage.value = it }
        }
    }

    override fun onInterrupt() {}

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.let { startActivity(it); true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun clickAt(x: Float, y: Float, durationMs: Long = 50): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun scrollForward(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true
    }

    fun scrollBackward(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollable(child)
            if (found != null) return found
        }
        return null
    }

    /** Click the first on-screen node whose text or contentDescription contains [text] (case-insensitive). */
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, text) ?: return false
        if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        // Try parent clickable
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            parent = parent.parent
        }
        // Gesture at center of bounds
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return clickAt(rect.centerX().toFloat(), rect.centerY().toFloat())
    }

    fun longClickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, text) ?: return false
        if (node.isLongClickable) return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return clickAt(rect.centerX().toFloat(), rect.centerY().toFloat(), 800)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val q = query.lowercase()
        val t = node.text?.toString()?.lowercase().orEmpty()
        val d = node.contentDescription?.toString()?.lowercase().orEmpty()
        if (t.contains(q) || d.contains(q)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, query)
            if (found != null) return found
        }
        return null
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow
        val focused = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** Dump visible text from the active window (for "read screen"). */
    fun readScreenText(maxChars: Int = 4000): String {
        val root = rootInActiveWindow ?: return "No active window"
        val sb = StringBuilder()
        collectText(root, sb)
        val out = sb.toString().trim()
        return if (out.length > maxChars) out.take(maxChars) + "…" else out.ifBlank { "(no text found)" }
    }

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(it)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, sb) }
        }
    }

    fun performGlobalActionBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performGlobalActionHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performGlobalActionRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun performGlobalActionNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun performGlobalActionQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    fun performGlobalActionPowerDialog(): Boolean = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    fun performGlobalActionLockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else false
    }
    fun performGlobalActionTakeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else false
    }

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        fun require(): JarvisAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
