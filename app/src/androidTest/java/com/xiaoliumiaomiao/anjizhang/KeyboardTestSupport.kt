package com.xiaoliumiaomiao.anjizhang

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

internal fun ComposeTestRule.closeKeyboard() {
    // Close the real IME with a user's Back key; Compose idling does not wait for native windows.
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val automation = instrumentation.uiAutomation
    val info = automation.serviceInfo
    val previousFlags = info.flags
    info.flags = previousFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
    automation.serviceInfo = info
    try {
        waitUntil(10000) { automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        waitUntil(10000) { automation.windows.none { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } }
    } finally {
        info.flags = previousFlags
        automation.serviceInfo = info
    }
}
