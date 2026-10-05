package com.xiaoliumiaomiao.anjizhang

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry

/** Enter an amount through the visible application keypad, never through SetText/TextInput. */
internal fun ComposeTestRule.setAmountWithKeypad(value: String) {
    require(value.all { it in '0'..'9' || it == '.' })
    onNodeWithText("金额（元）").performScrollTo().assertIsDisplayed().performTouchInput { click() }
    onNodeWithTag("amount-keypad").assertExists()
    pressAmountKey("clear")
    value.forEach { pressAmountKey(if (it == '.') "dot" else it.toString()) }
    assertAmountDraft(if (value == ".") "0." else value)
}

internal fun ComposeTestRule.pressAmountKey(key: String) {
    require(key in setOf("dot", "backspace", "clear") || (key.length == 1 && key[0] in '0'..'9'))
    onNodeWithTag("amount-key-$key").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
    onNodeWithText("金额（元）").assertIsNotFocused()
}

internal fun ComposeTestRule.assertAmountDraft(value: String) {
    onNodeWithText("金额（元）").performScrollTo()
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value)))
}

/** Observe native IME windows, restoring the accessibility flags even when the bounded wait fails. */
internal fun ComposeTestRule.assertSystemImeVisible(expected: Boolean) {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val info = automation.serviceInfo
    val previousFlags = info.flags
    info.flags = previousFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
    automation.serviceInfo = info
    try {
        waitUntil(10000) {
            automation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } == expected
        }
    } finally {
        info.flags = previousFlags
        automation.serviceInfo = info
    }
}
