package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.*
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.core.os.ConfigurationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.data.UiPreferencesStore
import com.xiaoliumiaomiao.anjizhang.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.text.NumberFormat
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** New ordinary scenarios use only guarded synthetic ci-ledger data and restore preferences. */
abstract class V018DeviceScenario {
    val compose = createAndroidComposeRule<MainActivity>()
    protected open val initialPreferences = UiPreferences()
    protected open val systemNight: Boolean? = null
    protected fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
        .bufferedReader().use { it.readText().trim() }
    private var oldPreferences = UiPreferences()
    private var oldNight = ""
    private val synthetic = object : ExternalResource() {
        override fun before() {
            assertEquals("true", InstrumentationRegistry.getArguments().getString("ledgerCi"))
            assertEquals("1", shell("getprop ro.kernel.qemu"))
            assertEquals("ci-ledger", shell("getprop ro.boot.qemu.avd_name").ifEmpty {
                shell("getprop ro.kernel.qemu.avd_name") })
            val context = ApplicationProvider.getApplicationContext<Context>()
            val store = UiPreferencesStore.getInstance(context)
            oldPreferences = store.state.value
            oldNight = shell("cmd uimode night")
            runBlocking {
                store.setTheme(initialPreferences.theme).getOrThrow()
                store.setHand(initialPreferences.hand).getOrThrow()
            }
            systemNight?.let { shell("cmd uimode night ${if (it) "yes" else "no"}") }
            context.deleteDatabase("ledger.db")
        }
        override fun after() {
            val store = UiPreferencesStore.getInstance(ApplicationProvider.getApplicationContext<Context>())
            try {
                runBlocking {
                    store.setTheme(oldPreferences.theme).getOrThrow()
                    store.setHand(oldPreferences.hand).getOrThrow()
                }
            } finally {
                val mode = oldNight.substringAfter(": ").lowercase()
                check(mode in listOf("auto", "yes", "no")) { "Unknown original system night mode" }
                shell("cmd uimode night $mode")
            }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(synthetic).around(compose)
    protected fun model(): LedgerViewModel = compose.runOnIdle {
        ViewModelProvider(compose.activity)[LedgerViewModel::class.java]
    }
    protected fun ready() {
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy }
        assertNull(vm.state.value.error)
    }
    protected fun openGlobalEditor() {
        ready()
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithTag("editor-shell").assertIsDisplayed()
    }
}

internal class UiPreferencesIsolation : ExternalResource() {
    private var previous = UiPreferences()
    override fun before() {
        val store = UiPreferencesStore.getInstance(ApplicationProvider.getApplicationContext<Context>())
        previous = store.state.value
        runBlocking { store.setTheme(ThemeMode.SYSTEM).getOrThrow(); store.setHand(Handedness.RIGHT).getOrThrow() }
    }
    override fun after() {
        val store = UiPreferencesStore.getInstance(ApplicationProvider.getApplicationContext<Context>())
        runBlocking { store.setTheme(previous.theme).getOrThrow(); store.setHand(previous.hand).getOrThrow() }
    }
}

internal val ordinaryListTags = listOf("ledger-list", "stats-list", "stats-drill-list", "accounts-list", "account-detail-list", "settings-list") +
    listOf("appearance", "categories", "backup", "privacy", "about").map { "settings-$it-list" }

internal fun ComposeTestRule.displayedOrdinaryListTag(): String = ordinaryListTags.single { tag ->
    onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1 && runCatching { onNodeWithTag(tag).assertIsDisplayed() }.isSuccess
}

internal fun LedgerDatabase.seedLegacyFixture(records: List<Entry>) {
    require(records.all { it.accountId == null })
    val before = snapshot().categories.associateBy { it.id }
    merge(records)
    val after = snapshot()
    before.forEach { (id, value) -> assertEquals(value, after.categories.single { it.id == id }) }
    records.forEach { expected -> assertEquals(expected, after.entries.single { it.id == expected.id }) }
}

/** Root-page fields must belong to this Activity, never to an unrelated focused window. */
internal fun ComposeTestRule.awaitNativeActivityFocus(activity: Activity, node: SemanticsNodeInteraction): Window {
    val semantics = node.assertIsDisplayed().fetchSemanticsNode()
    val view = (semantics.root as ViewRootForTest).view
    val window = runOnUiThread { activity.window }
    require(runOnUiThread { view.rootView === window.decorView }) {
        "The target semantics node must belong to the actual Activity decor"
    }
    waitUntil(10000) {
        runOnUiThread {
            view.isAttachedToWindow && view.isShown && view.rootView === window.decorView &&
                view.hasWindowFocus() && window.decorView.isAttachedToWindow && window.decorView.isShown &&
                window.decorView.hasWindowFocus() &&
                WindowInspector.getGlobalWindowViews().any { it === window.decorView }
        }
    }
    return window
}

/** Compose can be idle before its target Dialog receives native window focus. */
internal fun ComposeTestRule.awaitNativeDialogFocus(node: SemanticsNodeInteraction): Window {
    val semantics = node.assertIsDisplayed().fetchSemanticsNode()
    val view = (semantics.root as ViewRootForTest).view
    val window = requireNotNull(runOnUiThread {
        generateSequence(view) { it.parent as? View }
            .filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
    }) { "The target semantics node must belong to a real Dialog" }
    try {
        waitUntil(10000) {
            runOnUiThread {
                view.isAttachedToWindow && view.isShown && view.rootView === window.decorView &&
                    window.decorView.isAttachedToWindow && window.decorView.isShown &&
                    window.decorView.hasWindowFocus() &&
                    WindowInspector.getGlobalWindowViews().any { it === window.decorView }
            }
        }
    } catch (failure: Throwable) {
        // Diagnostic only: keep the predicate, timeout and original failure unchanged.
        try {
            val registry = InstrumentationRegistry.getInstrumentation()
            val automation = registry.uiAutomation
            fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
                .bufferedReader().use { it.readText().trim() }
            check(InstrumentationRegistry.getArguments().getString("ledgerCi") == "true")
            check(shell("getprop ro.kernel.qemu") == "1")
            check(shell("getprop ro.boot.qemu.avd_name").ifEmpty { shell("getprop ro.kernel.qemu.avd_name") } == "ci-ledger")
            val tag = semantics.config.getOrNull(SemanticsProperties.TestTag) ?: "untagged"
            val name = "task6-native-focus-diag-${tag.lowercase(Locale.ROOT)}-${SystemClock.uptimeMillis()}"
            check(name.matches(Regex("[a-z0-9-]+")))
            fun identity(value: Any?) = value?.let { "${it.javaClass.name}@${System.identityHashCode(it).toString(16)}" } ?: "null"
            val currentOwner = runCatching { (node.fetchSemanticsNode().root as? ViewRootForTest)?.view }
            val diagnostic = runOnUiThread {
                fun viewState(current: View): JSONObject {
                    val insets = current.rootWindowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it) }
                    return JSONObject().put("identity", identity(current)).put("rootIdentity", identity(current.rootView))
                        .put("attached", current.isAttachedToWindow).put("shown", current.isShown)
                        .put("windowFocus", current.hasWindowFocus()).put("viewFocus", current.hasFocus())
                        .put("windowToken", identity(current.windowToken)).put("width", current.width).put("height", current.height)
                        .put("displayId", current.display?.displayId ?: JSONObject.NULL)
                        .put("imeInsetsAvailable", insets != null)
                        .put("imeVisible", insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: JSONObject.NULL)
                        .put("imeBottom", insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: JSONObject.NULL)
                }
                fun windowState(current: Window): JSONObject {
                    val attributes = current.attributes
                    return JSONObject().put("identity", identity(current)).put("decor", viewState(current.decorView))
                        .put("flags", attributes.flags).put("type", attributes.type).put("softInputMode", attributes.softInputMode)
                        .put("notFocusable", attributes.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
                        .put("altFocusableIme", attributes.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM != 0)
                        .put("notTouchable", attributes.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
                }
                val parents = JSONArray()
                generateSequence(view) { it.parent as? View }.forEach { parent ->
                    parents.put(viewState(parent).put("providerWindow", if (parent is DialogWindowProvider) identity(parent.window) else JSONObject.NULL))
                }
                var context = view.context
                val contexts = mutableSetOf<Context>()
                while (context is ContextWrapper && context !is Activity && contexts.add(context)) context = context.baseContext
                val activity = context as? Activity
                val roots = WindowInspector.getGlobalWindowViews()
                JSONObject().put("diagnosticId", name).put("diagnosticOnly", true).put("frameCommitVerified", false)
                    .put("uptimeMillis", SystemClock.uptimeMillis()).put("targetTag", tag).put("semanticsId", semantics.id)
                    .put("originalFailure", failure.javaClass.name).put("ownerView", viewState(view))
                    .put("currentOwnerView", currentOwner.getOrNull()?.let(::viewState) ?: JSONObject.NULL)
                    .put("currentOwnerIsCachedOwner", currentOwner.getOrNull() === view)
                    .put("currentOwnerFetchError", currentOwner.exceptionOrNull()?.javaClass?.name ?: JSONObject.NULL)
                    .put("ownerRoot", viewState(view.rootView)).put("targetWindow", windowState(window)).put("parents", parents)
                    .put("activityWindow", activity?.window?.let(::windowState) ?: JSONObject.NULL)
                    .put("configuration", JSONObject().put("keyboard", view.resources.configuration.keyboard)
                        .put("hardKeyboardHidden", view.resources.configuration.hardKeyboardHidden))
                    .put("inspectorRoots", JSONArray().also { array -> roots.forEach { array.put(viewState(it)) } })
                    .put("guards", JSONObject().put("ownerAttached", view.isAttachedToWindow).put("ownerShown", view.isShown)
                        .put("ownerRootIsTargetDecor", view.rootView === window.decorView)
                        .put("decorAttached", window.decorView.isAttachedToWindow).put("decorShown", window.decorView.isShown)
                        .put("decorHasWindowFocus", window.decorView.hasWindowFocus())
                        .put("inspectorContainsTargetDecor", roots.any { it === window.decorView }))
            }
            val info = automation.serviceInfo
            val previousFlags = info.flags
            try {
                info.flags = previousFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                automation.serviceInfo = info
                diagnostic.put("accessibilityFlagsBefore", previousFlags).put("accessibilityFlagsObserved", automation.serviceInfo.flags)
                diagnostic.put("accessibilityWindows", JSONArray().also { array -> automation.windows.forEach {
                    array.put(JSONObject().put("id", it.id).put("type", it.type).put("focused", it.isFocused)
                        .put("active", it.isActive).put("displayId", it.displayId)
                        .put("bounds", android.graphics.Rect().also(it::getBoundsInScreen).toShortString())
                        .put("title", it.title?.toString() ?: JSONObject.NULL))
                } })
                diagnostic.put("accessibilityWindowsOnAllDisplays", JSONArray().also { array ->
                    val displays = automation.windowsOnAllDisplays
                    for (index in 0 until displays.size()) displays.valueAt(index).forEach {
                        array.put(JSONObject().put("displayId", displays.keyAt(index)).put("id", it.id)
                            .put("type", it.type).put("active", it.isActive).put("focused", it.isFocused))
                    }
                })
            } finally {
                info.flags = previousFlags
                automation.serviceInfo = info
            }
            diagnostic.put("readOnlySettings", JSONObject()
                .put("showImeWithHardKeyboard", shell("settings get secure show_ime_with_hard_keyboard"))
                .put("defaultInputMethod", shell("settings get secure default_input_method"))
                .put("fontScale", shell("settings get system font_scale"))
                .put("viewport", shell("wm size")).put("density", shell("wm density")).put("night", shell("cmd uimode night")))
            val directory = File(requireNotNull(registry.targetContext.getExternalFilesDir(null)), "v013-screens")
            check(directory.isDirectory || directory.mkdirs())
            val screenshot = File(directory, "$name.png")
            val jsonFile = File(directory, "$name.json")
            check(!screenshot.exists() && !jsonFile.exists())
            diagnostic.put("rawScreenPath", screenshot.absolutePath).put("jsonPath", jsonFile.absolutePath)
            val json = diagnostic.toString()
            jsonFile.writeText(json, Charsets.UTF_8)
            json.chunked(2000).forEachIndexed { index, chunk ->
                android.util.Log.e("NativeFocusDiagnostic", JSONObject().put("id", name).put("part", index)
                    .put("parts", (json.length + 1999) / 2000).put("jsonChunk", chunk).toString())
            }
            val bitmap = requireNotNull(automation.takeScreenshot())
            try { screenshot.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { bitmap.recycle() }
            check(screenshot.length() > 0)
            shell("mkdir -p /data/local/tmp/ledger-v013-screens")
            listOf(jsonFile, screenshot).forEach { file ->
                check(file.absolutePath.matches(Regex("[a-zA-Z0-9/._-]+")))
                shell("cp ${file.absolutePath} /data/local/tmp/ledger-v013-screens/${file.name}")
                check(shell("stat -c %s /data/local/tmp/ledger-v013-screens/${file.name}").toLong() == file.length())
            }
        } catch (diagnosticFailure: Throwable) {
            failure.addSuppressed(diagnosticFailure)
            android.util.Log.e("NativeFocusDiagnostic", "Diagnostic capture failed; original test failure retained", diagnosticFailure)
        }
        throw failure
    }
    return window
}

internal fun ComposeTestRule.assertSelectedCategory(name: String) = onNodeWithTag("editor-category-current")
    .performScrollTo().assertIsDisplayed()
    .assert(hasText("当前选择：$name") or hasText("原分类（保留）：$name"))

internal fun ComposeTestRule.selectCategory(name: String) {
    val chip = hasText(name) and isSelectable() and hasAnyAncestor(hasTestTag("editor-form"))
    if (onAllNodes(chip).fetchSemanticsNodes().isNotEmpty()) onNode(chip).performScrollTo().assertIsDisplayed().performClick()
    else {
        onNodeWithTag("editor-category-all").performScrollTo().assertIsDisplayed().performClick()
        val search = onNodeWithTag("editor-category-search")
        awaitNativeDialogFocus(search)
        search.performClick().performTextReplacement(name)
        closeKeyboard()
        val row = (hasText(name) or hasText("✓ $name")) and hasAnyAncestor(hasTestTag("editor-category-list")) and hasClickAction()
        onNodeWithTag("editor-category-list").performScrollToNode(row)
        onNode(row).assertIsDisplayed().performClick()
        onNodeWithTag("editor-category-list").assertDoesNotExist()
    }
    assertSelectedCategory(name)
}

internal fun ComposeTestRule.addAndSelectCategory(name: String) {
    onNodeWithTag("editor-category-add").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
    onNodeWithTag("category-editor").assertIsDisplayed()
    val field = onNodeWithTag("category-name")
    awaitNativeDialogFocus(field)
    field.performClick().performTextReplacement(name)
    closeKeyboard()
    onNodeWithTag("category-editor-save").assertIsDisplayed().assertIsEnabled().performClick()
    waitUntil(10000) { onAllNodesWithTag("category-editor").fetchSemanticsNodes().isEmpty() }
    assertSelectedCategory(name)
}

internal fun ComposeTestRule.setCategoryThroughDirectory(name: String) {
    onNodeWithTag("editor-category-all").performScrollTo().assertIsDisplayed().performClick()
    val search = onNodeWithTag("editor-category-search")
    awaitNativeDialogFocus(search)
    search.performClick().performTextReplacement(name)
    closeKeyboard()
    val row = (hasText(name) or hasText("✓ $name")) and hasAnyAncestor(hasTestTag("editor-category-list")) and hasClickAction()
    if (onAllNodes(row).fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("editor-category-list").performScrollToNode(row)
        onNode(row).assertIsDisplayed().performClick(); assertSelectedCategory(name)
    } else {
        onNode(hasText("返回") and hasAnyAncestor(isDialog() and hasAnyDescendant(hasTestTag("editor-category-list")))).assertIsDisplayed().performClick()
        addAndSelectCategory(name)
    }
}

internal fun ComposeTestRule.assertEditorDate(raw: String) {
    val date = LocalDate.parse(raw)
    onNodeWithTag("editor-date-select").performScrollTo().assertIsDisplayed()
        .assertContentDescriptionEquals("选择日期，${date.year}年${date.monthValue}月${date.dayOfMonth}日")
}

/** Fixed Material3 1.3.2 uses Text for the full day/year accessibility label. */
@OptIn(ExperimentalMaterial3Api::class)
internal fun ComposeTestRule.selectEditorDate(raw: String, context: Context = InstrumentationRegistry.getInstrumentation().targetContext,
    rowTag: String = "editor-date-select", confirm: Boolean = true) {
    val target = LocalDate.parse(raw)
    require(target.year in 1900..9999)
    fun resource(name: String, vararg arguments: Any): String {
        val id = context.resources.getIdentifier("m3c_date_picker_$name", "string", context.packageName)
        check(id != 0) { "Missing actual Material3 calendar resource: $name" }
        return context.resources.getString(id, *arguments)
    }
    fun millis(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun calendar(matcher: SemanticsMatcher) = matcher and hasAnyAncestor(isDialog() and hasAnyDescendant(hasTestTag("calendar-confirm")))
    val row = onNodeWithTag(rowTag).performScrollTo().assertIsDisplayed().assertIsEnabled()
    val before = row.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].toList()
    val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
    val formatter = DatePickerDefaults.dateFormatter()
    val switch = resource("switch_to_year_selection")
    row.performClick(); onNodeWithTag("calendar-confirm").assertIsDisplayed()
    assertSystemImeVisible(false); onNodeWithTag("amount-keypad-panel").assertDoesNotExist()
    onNode(calendar(hasContentDescription(switch) and hasClickAction())).assertIsDisplayed().performClick()
    val grid = calendar(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
    waitUntil(10000) { onAllNodes(grid, useUnmergedTree = true).fetchSemanticsNodes().size == 1 }
    onNode(grid, useUnmergedTree = true).performScrollToIndex(target.year - 1900)
    val digits = NumberFormat.getIntegerInstance().apply { isGroupingUsed = false }.format(target.year)
    onNode(calendar(hasText(resource("navigate_to_year_description", digits)) and hasClickAction())).assertIsDisplayed().assertIsEnabled().performClick()
    val labels = (1..12).associateWith { requireNotNull(formatter.formatMonthYear(millis(LocalDate.of(target.year, it, 1)), locale)) }
    val header = calendar(hasContentDescription(switch) and hasClickAction())
    var displayed: Int? = null
    waitUntil(10000) { displayed = (1..12).singleOrNull { onAllNodes(header and hasText(labels.getValue(it))).fetchSemanticsNodes().size == 1 }; displayed != null }
    var month = requireNotNull(displayed)
    val difference = target.monthValue - month
    val step = if (difference > 0) 1 else -1
    val arrow = resource(if (difference > 0) "switch_to_next_month" else "switch_to_previous_month")
    repeat(kotlin.math.abs(difference)) {
        onNode(calendar(hasContentDescription(arrow) and hasClickAction())).assertIsDisplayed().assertIsEnabled().performClick()
        month += step
        waitUntil(10000) { onAllNodes(header and hasText(labels.getValue(month))).fetchSemanticsNodes().size == 1 }
    }
    val full = requireNotNull(formatter.formatDate(millis(target), locale, forContentDescription = true))
    val day = calendar(hasClickAction() and (hasText(full) or hasText("${resource("today_description")}, $full")))
    onNode(day).assertIsDisplayed().assertIsEnabled().performClick().assertIsSelected()
    onNodeWithTag(if (confirm) "calendar-confirm" else "calendar-cancel").assertIsDisplayed().assertIsEnabled().performClick()
    onNodeWithTag("calendar-confirm").assertDoesNotExist()
    if (confirm) onNodeWithTag(rowTag).performScrollTo().assertContentDescriptionEquals("选择日期，${target.year}年${target.monthValue}月${target.dayOfMonth}日")
    else onNodeWithTag(rowTag).performScrollTo().assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, before))
}
