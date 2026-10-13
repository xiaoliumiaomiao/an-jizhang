package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.LedgerUiState
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Wait for the actual data list, not just its temporary loading shell. */
internal fun ComposeTestRule.awaitStatisticsReady() {
    waitUntil(10000) {
        onNodeWithTag("stats-error").assertDoesNotExist()
        onAllNodesWithTag("stats-list").fetchSemanticsNodes().size == 1 &&
            onAllNodesWithTag("stats-loading").fetchSemanticsNodes().isEmpty() &&
            onAllNodesWithTag("stats-reading").fetchSemanticsNodes().isEmpty()
    }
}

/** Only synthetic ci-ledger data. Expected amounts are literals in the tests. */
internal fun statsId(number: Int) = "02500000-0000-0000-0000-${number.toString().padStart(12, '0')}"
internal val statisticsFixture = listOf(
    Entry(statsId(1), EntryKind.INCOME, 10000, "工资", LocalDate.parse("2025-05-01"), "keep"),
    Entry(statsId(2), EntryKind.INCOME, 1, " 餐饮 ", LocalDate.parse("2025-05-02"), "合成-2"),
    Entry(statsId(3), EntryKind.EXPENSE, 1234, " 餐饮 ", LocalDate.parse("2025-05-02"), "合成-3"),
    Entry(statsId(4), EntryKind.EXPENSE, 1, "餐饮", LocalDate.parse("2025-05-02"), "合成-4"),
    Entry(statsId(5), EntryKind.EXPENSE, 100, "餐饮外卖", LocalDate.parse("2025-05-03"), "合成-5"),
    Entry(statsId(6), EntryKind.EXPENSE, 100, "交通", LocalDate.parse("2025-05-04"), "合成-6"),
    Entry(statsId(7), EntryKind.EXPENSE, 100, "交通", LocalDate.parse("2025-05-04"), "合成-7"),
    Entry(statsId(8), EntryKind.EXPENSE, 50, "Dining", LocalDate.parse("2025-05-05"), "合成-8"),
    Entry(statsId(9), EntryKind.EXPENSE, 50, "dining", LocalDate.parse("2025-05-05"), "合成-9"),
    Entry(statsId(10), EntryKind.EXPENSE, 7, "末项", LocalDate.parse("2025-05-06"), "合成-10"),
    Entry(statsId(11), EntryKind.EXPENSE, 2000, "餐饮", LocalDate.parse("2025-04-30"), "合成-11"),
    Entry(statsId(12), EntryKind.INCOME, 5000, "工资", LocalDate.parse("2025-04-01"), "合成-12"),
    Entry(statsId(13), EntryKind.EXPENSE, 900, "餐饮", LocalDate.parse("2025-06-01"), "合成-13"),
    Entry(statsId(14), EntryKind.INCOME, 20000, "工资", LocalDate.parse("2025-06-01"), "合成-14"),
    Entry(statsId(15), EntryKind.EXPENSE, 888, "餐饮", LocalDate.parse("2024-05-01"), "合成-15"),
    Entry(statsId(16), EntryKind.INCOME, 4000, "工资", LocalDate.parse("2024-05-01"), "合成-16")
)

abstract class StatisticsDeviceScenario {
    val compose = createAndroidComposeRule<MainActivity>()
    protected fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
        .bufferedReader().use { it.readText().trim() }
    protected fun emulatorOnly() {
        assertEquals("true", InstrumentationRegistry.getArguments().getString("ledgerCi"))
        assertEquals("1", shell("getprop ro.kernel.qemu"))
        assertEquals("ci-ledger", shell("getprop ro.boot.qemu.avd_name").ifEmpty { shell("getprop ro.kernel.qemu.avd_name") })
    }
    private val seed = object : ExternalResource() {
        override fun before() {
            emulatorOnly()
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("ledger.db")
            LedgerDatabase(context).use { db -> db.seedLegacyFixture(statisticsFixture) }
        }
    }
    private val failureImage = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            val name = "s2-failed-${description.testClass.simpleName}-${description.methodName}"
                .lowercase(Locale.ROOT).replace(Regex("[^a-z0-9-]+"), "-")
            runCatching { captureLedgerScreen(name) }.exceptionOrNull()?.let(error::addSuppressed)
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(UiPreferencesIsolation()).around(seed).around(compose).around(failureImage)
    protected fun model() = compose.runOnIdle { ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
    protected fun ready() {
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == statisticsFixture.size }
        assertNull(vm.state.value.error)
    }
    protected fun listTag() = compose.displayedOrdinaryListTag()
    protected fun reveal(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        // Async analysis has an explicit pending surface, not the previous period's list.
        awaitAnalysis()
        compose.onNodeWithTag(listTag()).performScrollToNode(matcher)
        // Compose 1.8 may stop at a nested target's semantics container; finish on the target.
        return compose.onNode(matcher).performScrollTo().assertIsDisplayed()
    }
    protected fun tag(value: String) = reveal(hasTestTag(value))
    protected fun text(value: String) = reveal(hasText(value))
    protected fun awaitAnalysis() {
        compose.waitForIdle()
        compose.waitUntil(10000) {
            compose.onNodeWithTag("stats-error").assertDoesNotExist()
            compose.onAllNodesWithTag("stats-loading").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("stats-reading").fetchSemanticsNodes().isEmpty() &&
                ordinaryListTags.sumOf {
                    compose.onAllNodesWithTag(it).fetchSemanticsNodes().size
                } == 1
        }
    }
    protected fun month(year: Int, month: Int) {
        tag("month-select").performClick()
        compose.onNodeWithText("年份（1900–9999）").performTextReplacement(year.toString())
        compose.closeKeyboard()
        compose.onNodeWithTag("month-$month").performScrollTo().performClick()
        compose.onNodeWithText("确定").performClick()
        tag("month-select").assertTextContains("${year}年${month}月")
        if (listTag() == "stats-list") awaitAnalysis()
    }
    protected fun openStats() {
        ready(); month(2025, 5); compose.onNodeWithTag("nav-stats").performClick()
        // Explicit new behavior: this must be absent on old UI for a meaningful RED.
        tag("stats-kind-income").assertExists()
        awaitAnalysis()
    }
    protected fun modeYear() { tag("stats-mode-year").performClick(); tag("stats-year-select").assertExists(); awaitAnalysis() }
    protected fun year(value: Int) {
        tag("stats-year-select").performClick()
        compose.onNodeWithTag("stats-year-input").performTextReplacement(value.toString()); compose.closeKeyboard()
        compose.onNodeWithText("确定").performClick()
        tag("stats-year-select").assertTextContains("${value}年")
        awaitAnalysis()
    }
    protected fun kind(kind: EntryKind) { tag("stats-kind-${kind.name.lowercase()}").performClick() }
    protected fun category(name: String, kind: EntryKind = EntryKind.EXPENSE) = tag("stats-category-${kind.name}-$name")
    protected fun categoryOrder(expected: List<String>, kind: EntryKind = EntryKind.EXPENSE) {
        val prefix = "stats-category-${kind.name}-"
        val observed = linkedSetOf<String>()
        val list = compose.onNodeWithTag("stats-list")
        val matcher = SemanticsMatcher("category row") {
            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith(prefix)
        }
        val samples = mutableListOf<String>()
        awaitAnalysis()
        list.performScrollToIndex(0)
        // Start at the first actual category, independent of expected names/order.
        list.performScrollToNode(matcher)
        var reachedEnd = false
        for (step in 0 until 20) {
            val nodes = compose.onAllNodes(matcher).fetchSemanticsNodes()
            val viewport = list.fetchSemanticsNode()
            val (distance, atEnd) = compose.runOnIdle {
                assertTrue(viewport.layoutInfo.isAttached && viewport.layoutInfo.isPlaced)
                val area = viewport.boundsInRoot
                val axis = viewport.config[SemanticsProperties.VerticalScrollAxisRange]
                val visible = nodes.filter { it.layoutInfo.isAttached && it.layoutInfo.isPlaced }
                    .filter { it.boundsInRoot.bottom > area.top && it.boundsInRoot.top < area.bottom }
                    .sortedBy { it.positionInRoot.y }
                    .map { it.config[SemanticsProperties.TestTag].removePrefix(prefix) }
                observed.addAll(visible)
                samples.add("step=$step scroll=${axis.value()}/${axis.maxValue()} viewport=$area raw=" +
                    nodes.joinToString { "${it.config[SemanticsProperties.TestTag]}:attached=${it.layoutInfo.isAttached},placed=${it.layoutInfo.isPlaced}" } +
                    " visible=$visible observed=$observed")
                area.height / 2f to (axis.value() >= axis.maxValue())
            }
            if (atEnd) { reachedEnd = true; break }
            assertTrue("Positive statistics viewport: $samples", distance > 0f)
            // Overlapping, non-flinging samples cannot skip an entire category section.
            list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, distance) }
        }
        assertTrue("Category traversal reached list end: ${samples.joinToString("\n")}", reachedEnd)
        assertEquals("Actual category ranking: ${samples.joinToString("\n")}", expected, observed.toList())
    }
    protected fun drill(name: String, kind: EntryKind = EntryKind.EXPENSE) {
        category(name, kind).performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("stats-loading").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("stats-drill-list").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("stats-drill-list").assertExists()
        compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled()
    }
    protected fun drillBack() { compose.onNodeWithTag("stats-drill-back").performClick() }
    protected fun back() {
        // Compose removal may precede the native Dialog window returning input focus.
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty() &&
                compose.runOnIdle { compose.activity.window.decorView.hasWindowFocus() }
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }
    protected fun overview(income: String, expense: String, balance: String, annual: Boolean = false) {
        awaitAnalysis()
        sectionText("stats-overview", "收入 ¥$income"); sectionText("stats-overview", "支出 ¥$expense")
        sectionText("stats-overview", "${if (annual) "本年" else "本月"}结余 ¥$balance")
    }
    protected fun sectionText(section: String, value: String) = reveal(hasText(value) and hasAnyAncestor(hasTestTag(section)))
    protected fun metrics(count: Int, average: String) { tag("stats-metrics").assertTextContains("记录 $count 笔").assertTextContains("平均每笔 ¥$average") }
    protected fun entries(): List<Entry> = LedgerDatabase(ApplicationProvider.getApplicationContext<Context>()).use { it.listEntries() }
    protected fun replaceFixture(records: List<Entry>) {
        val vm = model(); compose.waitUntil(10000) { !vm.state.value.busy }
        LedgerDatabase(ApplicationProvider.getApplicationContext<Context>()).use { db -> db.listEntries().forEach { db.delete(it.id) }; db.seedLegacyFixture(records) }
        vm.load(); compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.toSet() == records.toSet() }
        awaitAnalysis()
    }
    @Suppress("UNCHECKED_CAST")
    protected fun mutableState(): MutableStateFlow<LedgerUiState> = LedgerViewModel::class.java.getDeclaredField("mutable")
        .apply { isAccessible = true }.get(model()) as MutableStateFlow<LedgerUiState>
    protected fun detail(id: Int) { tag("entry-${statsId(id)}").performClick(); compose.onNodeWithTag("entry-detail").assertIsDisplayed(); compose.onNodeWithTag("add-entry").assertIsDisplayed().assertIsEnabled() }
    protected fun edit() { compose.onNodeWithText("编辑").performScrollTo().performClick() }
    protected fun field(label: String, value: String) { when(label) { "editor-category-current" -> compose.setCategoryThroughDirectory(value); "editor-date-select" -> compose.selectEditorDate(value, compose.activity); else -> { compose.onNodeWithText(label).performScrollTo().performTextReplacement(value); compose.closeKeyboard() } } }
    protected fun save() {
        compose.onNodeWithText("保存").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("editor-form").fetchSemanticsNodes().isEmpty() }
        assertNull(model().state.value.error)
    }
    protected fun delete() {
        compose.onNodeWithText("删除").performScrollTo().performClick(); compose.onNodeWithText("确认删除").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("删除账目？").fetchSemanticsNodes().isEmpty() }
    }
    /** Enumerate every Lazy row, so absence does not merely mean off-screen. */
    protected fun allDrillIds(expected: List<Int>) {
        text("共 ${expected.size} 条记录")
        val observed = linkedSetOf<String>()
        val list = compose.onNodeWithTag("stats-drill-list")
        val matcher = SemanticsMatcher("entry tag") {
            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("entry-")
        }
        val samples = mutableListOf<String>()
        list.performScrollToIndex(0)
        var reachedEnd = false
        for (step in 0 until 20) {
            compose.onAllNodes(hasTestTag("stats-drill-list")).assertCountEquals(1)
            val nodes = compose.onAllNodes(matcher).fetchSemanticsNodes()
            val viewport = list.fetchSemanticsNode()
            val (distance, atEnd) = compose.runOnIdle {
                assertTrue(viewport.layoutInfo.isAttached && viewport.layoutInfo.isPlaced)
                val area = viewport.boundsInRoot
                val axis = viewport.config[SemanticsProperties.VerticalScrollAxisRange]
                val visible = nodes.filter { it.layoutInfo.isAttached && it.layoutInfo.isPlaced }
                    .filter {
                        val bounds = it.boundsInRoot
                        bounds.width > 0f && bounds.height > 0f &&
                            bounds.bottom > area.top && bounds.top < area.bottom &&
                            bounds.right > area.left && bounds.left < area.right
                    }
                    .sortedBy { it.positionInRoot.y }
                    .map { it.config[SemanticsProperties.TestTag].removePrefix("entry-") }
                observed.addAll(visible)
                samples.add("step=$step scroll=${axis.value()}/${axis.maxValue()} viewport=$area raw=" +
                    nodes.joinToString { "${it.config[SemanticsProperties.TestTag]}:attached=${it.layoutInfo.isAttached},placed=${it.layoutInfo.isPlaced}" } +
                    " visible=$visible observed=$observed")
                area.height / 2f to (axis.value() >= axis.maxValue())
            }
            if (atEnd) { reachedEnd = true; break }
            assertTrue("Positive drill viewport: $samples", distance > 0f)
            // Overlapping, non-flinging samples preserve the order of actual visible rows.
            list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, distance) }
        }
        assertTrue("Drill traversal reached list end: ${samples.joinToString("\n")}", reachedEnd)
        assertEquals("Actual drill order: ${samples.joinToString("\n")}", expected.map(::statsId), observed.toList())
    }
    protected fun anchor(tag: String): Float {
        awaitAnalysis()
        val node = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
        val viewport = compose.onNodeWithTag(listTag()).fetchSemanticsNode()
        return compose.runOnIdle { assertTrue(node.layoutInfo.isAttached && node.layoutInfo.isPlaced); node.boundsInRoot.top - viewport.boundsInRoot.top }
    }
    protected fun assertAnchor(tag: String, expected: Float) { assertEquals(expected, anchor(tag), 2f * compose.activity.resources.displayMetrics.density) }
    protected fun readable(value: String, categoryTag: String? = null) {
        if (categoryTag == null) text(value) else tag(categoryTag)
        val matcher = if (categoryTag == null) hasText(value) else hasText(value) and hasAnyAncestor(hasTestTag(categoryTag))
        // Always inspect the unique actual Text, not a clickable parent's merged result.
        val node = compose.onNode(matcher, useUnmergedTree = true).assertIsDisplayed()
        val semantic = node.fetchSemanticsNode()
        val viewport = compose.onNodeWithTag(listTag()).fetchSemanticsNode()
        compose.runOnIdle {
            listOf(semantic, viewport).forEach { assertTrue(it.layoutInfo.isAttached && it.layoutInfo.isPlaced) }
            assertEquals(listOf(value), semantic.config[SemanticsProperties.Text].map { it.text })
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(requireNotNull(semantic.config[SemanticsActions.GetTextLayoutResult].action)(results))
            val config = compose.activity.resources.configuration
            val density = compose.activity.resources.displayMetrics.density
            val geometry = "fontScale=${config.fontScale} widthDp=${config.screenWidthDp} density=$density " +
                "nodeSize=${semantic.size} position=${semantic.positionInRoot} bounds=${semantic.boundsInRoot} viewport=${viewport.boundsInRoot}"
            val ordinary = results.isNotEmpty() && results.none { it.hasVisualOverflow }
            val naturalLine = !ordinary && results.size == 1 && describesUnclippedNaturalWidthLine(
                results.single(), semantic.size, value, density, config.fontScale)
            val layouts = results.joinToString("\n") { result ->
                "size=${result.size} paragraph=${result.multiParagraph.width}x${result.multiParagraph.height} " +
                    "exceedMaxLines=${result.multiParagraph.didExceedMaxLines} widthOverflow=${result.didOverflowWidth} heightOverflow=${result.didOverflowHeight} " +
                    "input=${result.layoutInput} lineCount=${result.lineCount} lines=" +
                    (0 until result.lineCount).joinToString { line ->
                        "$line:[${result.getLineLeft(line)},${result.getLineTop(line)},${result.getLineRight(line)},${result.getLineBottom(line)}] " +
                            "range=${result.getLineStart(line)}..${result.getLineEnd(line)} ellipsis=${result.isLineEllipsized(line)}"
                    }
            }
            assertTrue("Text is not ellipsized or clipped: $value; resultCount=${results.size}; $geometry\n$layouts", ordinary || naturalLine)
            if (naturalLine) android.util.Log.i("LedgerTextGeometry", "Verified natural-width single line: text=$value; $geometry; " +
                "rawWidthOverflow=true rawHeightOverflow=false parentWidth=${results.single().multiParagraph.width}; " +
                "line=${results.single().getLineRight(0)}x${results.single().getLineBottom(0)}; boundedCharacters=" +
                value.indices.joinToString { "$it:${results.single().getBoundingBox(it)}" })
        }
        fullyVisible(node)
    }
    /** Require the actual drill Text to render every amount character on one visible line. */
    protected fun drillAmountSingleLine(entryId: String, value: String) {
        readable(value) // Retain its exact text, clipping, and viewport checks.
        val amount = compose.onNode(hasText(value) and hasAnyAncestor(hasTestTag("entry-$entryId")),
            useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode()
        compose.runOnIdle {
            assertEquals(listOf(value), amount.config[SemanticsProperties.Text].map { it.text })
            val layouts = mutableListOf<TextLayoutResult>()
            assertTrue(requireNotNull(amount.config[SemanticsActions.GetTextLayoutResult].action)(layouts))
            assertEquals("One Text layout for drill amount", 1, layouts.size)
            val layout = layouts.single()
            assertEquals("Drill amount must be one line: $value", 1, layout.lineCount)
            assertEquals(0, layout.getLineStart(0))
            assertEquals(value.length, layout.getLineEnd(0))
            assertFalse(layout.isLineEllipsized(0))
            assertFalse(layout.multiParagraph.didExceedMaxLines)
            assertFalse(layout.hasVisualOverflow)
        }
    }
    /** Compose 1.8 String semantics rebuild at parent maxWidth while keeping the natural node size. */
    private fun describesUnclippedNaturalWidthLine(result: TextLayoutResult, size: IntSize, text: String,
        density: Float, fontScale: Float): Boolean {
        val input = result.layoutInput
        val style = input.style
        if (text.isEmpty() || result.size != size || input.text.text != text ||
            input.density.density != density || input.density.fontScale != fontScale ||
            !result.didOverflowWidth || result.didOverflowHeight || result.multiParagraph.didExceedMaxLines ||
            !input.constraints.hasBoundedWidth || input.constraints.minWidth != 0 || input.constraints.minHeight != 0 ||
            result.multiParagraph.width != input.constraints.maxWidth.toFloat() || result.multiParagraph.width <= size.width ||
            !input.softWrap || input.overflow != TextOverflow.Clip || input.maxLines != Int.MAX_VALUE ||
            input.layoutDirection != LayoutDirection.Ltr ||
            style.textAlign !in listOf(TextAlign.Unspecified, TextAlign.Start, TextAlign.Left) ||
            style.textDirection !in listOf(TextDirection.Unspecified, TextDirection.Ltr) ||
            input.text.spanStyles.isNotEmpty() || input.text.paragraphStyles.isNotEmpty() || input.placeholders.isNotEmpty() ||
            style.textIndent != null || style.textGeometricTransform != null || style.baselineShift != null ||
            style.shadow != null || style.drawStyle != null || style.fontStyle !in listOf(null, FontStyle.Normal) ||
            text.any { it.isISOControl() || it.isSurrogate() || Character.getType(it) == Character.FORMAT.toInt() } ||
            result.lineCount != 1 || result.isLineEllipsized(0) || result.getLineStart(0) != 0 || result.getLineEnd(0) != text.length ||
            result.getParagraphDirection(0) != ResolvedTextDirection.Ltr) return false
        val epsilon = .01f // pixel roundoff only, never the viewport's 2dp allowance
        fun fits(rect: Rect) = listOf(rect.left, rect.top, rect.right, rect.bottom).all { it.isFinite() } &&
            rect.left >= -epsilon && rect.top >= -epsilon && rect.right >= rect.left && rect.bottom >= rect.top &&
            rect.right <= size.width + epsilon && rect.bottom <= size.height + epsilon
        val line = Rect(result.getLineLeft(0), result.getLineTop(0), result.getLineRight(0), result.getLineBottom(0))
        val heightRounding = size.height - result.multiParagraph.height
        val widthRounding = size.width - line.right
        return fits(line) && kotlin.math.abs(line.left) <= epsilon && kotlin.math.abs(line.top) <= epsilon &&
            heightRounding >= -epsilon && heightRounding < 1f && widthRounding >= -epsilon && widthRounding < 1f &&
            text.indices.all { result.getBidiRunDirection(it) == ResolvedTextDirection.Ltr && fits(result.getBoundingBox(it)) }
    }
    private fun fullyVisible(interaction: SemanticsNodeInteraction) {
        val node = interaction.assertIsDisplayed().fetchSemanticsNode()
        val activeLists = ordinaryListTags.flatMap { compose.onAllNodesWithTag(it).fetchSemanticsNodes() }
        val enclosingList = activeLists.firstOrNull { list ->
            generateSequence(node.parent) { it.parent }.any { it.id == list.id }
        }
        val viewport = enclosingList ?: compose.onRoot().fetchSemanticsNode()
        val (unclipped, visibleArea) = compose.runOnIdle {
            listOf(node, viewport).forEach { assertTrue(it.layoutInfo.isAttached && it.layoutInfo.isPlaced) }
            Rect(node.positionInRoot, Size(node.size.width.toFloat(), node.size.height.toFloat())) to viewport.boundsInRoot
        }
        val tolerance = 2f * compose.activity.resources.displayMetrics.density
        assertTrue("Full text/touch bounds must fit current viewport: $unclipped / $visibleArea",
            unclipped.left >= visibleArea.left - tolerance && unclipped.right <= visibleArea.right + tolerance &&
                unclipped.top >= visibleArea.top - tolerance && unclipped.bottom <= visibleArea.bottom + tolerance)
    }
    protected fun touchTarget(node: SemanticsNodeInteraction) {
        val bounds = node.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val min = 48f * compose.activity.resources.displayMetrics.density
        assertTrue("48dp touch target: $bounds", bounds.width >= min - 1 && bounds.height >= min - 1)
        fullyVisible(node)
    }
}
