# 安记账 0.1.2 账目搜索与收支筛选 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已验收的 0.1.1 上增加当前月份的分类/备注关键词搜索及收支类型筛选，交付同本机签名的 0.1.2 测试 APK，并等待本轮独立手机验收。

**Architecture:** 新增纯函数 EntryFilter；LedgerScreen 顶层保存两个简单筛选状态，从完整 state.entries 派生列表。仅抽取无状态 EntryFilters 控件，原编辑/删除和操作完成 ID 留在原位置；月统计、导出、数据库和备份协议保持既有数据流。

**Tech Stack:** Kotlin 2.1.20、Compose BOM 2025.04.01（Material3 1.3.2 / UI 1.8.0）、现有 SQLiteOpenHelper、JUnit4/Robolectric/Compose Android 测试、现有 PowerShell 构建入口与 GitHub Actions API35 临时模拟器。

## Global Constraints

- 工作区为 `C:/Users/13765/.codex/worktrees/ledger-v0-1-1/an-jizhang`，分支 `feat/ledger-query-v0.1.2`；已验收 0.1.1 固定基线为 `976817290302e712519297cc5b810ce019d2d383`。实施前核对 Git；不覆盖作者改动。
- 不新增依赖或权限；不改 LedgerViewModel、LedgerDatabase、BackupCodec、EntryEditor 生产实现、SQLite schema 1、备份 format 1；不加入联网、遥测、Room、FTS、DataStore 或应用锁。
- `EntryFilter.apply(entries: List<Entry>, month: YearMonth, query: String = "", kind: EntryKind? = null): List<Entry>` 仅处理所选月；分类 OR 备注，关键词与类型 AND，`null` 为全部类型。
- 仅匹配时 `query.trim()`；字符串 `contains(ignoreCase = true)`，百分号、下划线、点号均按字面；不使用 SQL LIKE、Regex 或默认 Locale 转小写；保持输入顺序、对象与输入集合不变。
- query 和 kindName 必须在 LedgerScreen 顶层 `rememberSaveable`，导航、切月和 Activity 重建保留；清除筛选一次清空两条件，月份不变；不承诺强制停止后的长期偏好保存。
- 不新增查询 200 字符硬限制：200 是已有备注数据上限，分类上限 20；查询首尾空白在匹配时才 trim，截断原输入会改变语义。保留原 value/onValueChange 和单行输入；不引入新框架 API。
- 顶部统计、统计页始终显示完整所选月；导出始终为完整账本。筛选仅影响列表，展示“筛选仅影响列表，统计仍为全月”和“显示 N / 本月 M 条”。
- 空月显示“这个月还没有账目”；有本月记录但无匹配显示“没有符合条件的账目”；“清除筛选”可一键清空关键词及类型。
- 只抽无状态筛选控件，不迁移 editing/editingJson/deletingJson/pendingSaveId/pendingDeleteId 或完成消费 LaunchedEffect；不重构原增删改流程。
- 保留原 6 项 LedgerSmokeTest、业务断言及 10000ms 等待；新增 4 项真实 UI 行为测试。适应布局仅可真实滚动、真实关闭 IME、稳定节点选择；不删断言、不改测试为跳过、不延长等待、不直接改筛选状态使测试通过。
- 阶段 A 可新增 KeyboardTestSupport.kt，抽取共用 ComposeTestRule.closeKeyboard()，旧 smoke 仅替换重复 IME helper 与去除无用 imports；六项业务、断言和 10000ms 保持，不提前改生产 UI/版本。
- UI RED 必须在未接入筛选的 0.1.1 UI 上真实执行新测试，主 agent 实读因找不到搜索控件而失败的断言；编译错误、模拟器故障和故意破坏生产代码均不算 RED。RED 阶段保留旧版本和旧升级链，停于 UI 失败时不得声称升级已执行。
- 0.1.2 最终 versionName 为 `0.1.2`、versionCode 为 `3`；升级链固定已验收 0.1.1/code2 到 0.1.2/code3，沿用现有工作流，不另加工作流。
- 升级保留 `am instrument -w -r`、恰好一条 `OK (1 test)`、唯一 `INSTRUMENTATION_CODE: -1`、所有失败/崩溃检查、同证书三包、CI/单设备/QEMU/ci-ledger 检查和 timeout；seed 后至 verify 前禁止清库、卸载、重 seed、connected 测试和降级安装。
- 旧 0.1.0/0.1.1 两个本机 APK、真实账本及密钥不动；本机 debug keystore 不读出、不复制、不上传。仅公开证书用于比对，最终新 APK 用同本机签名；不同 CI 签名不能替代本机覆盖安装包。
- 工具沿用 `.tooling` junction 和 `scripts/build.ps1`；只改当前进程环境，不改系统 PATH，不安装新依赖。测试/证据只放 ignored `test-artifacts/sdd-v012` 或明确的本轮 CI 子目录。
- 实现 agent 只修改各任务明确文件，不做 Git 写操作；主 agent 安排原子提交、真实 CI、私有 PR 和交付。每任务由另一agent依次独立审查规格与质量；修复复审；完整分支最终再审。
- 技术验证与作者手机验收分开；本轮验收表保持未勾，不能继承 0.1.1 的通过结果。不得未经本轮作者验收合并或正式发布。

---

## 需求、验收与复用决定

用户已确认 0.1.1 全部通过并要求继续迭代；主 agent 给出偏好选择并留足回复时间后，已声明按推荐账目查询推进；作者随后补充“继续”。没有扩展金额区间、跨月搜索、排序、分页、导出筛选结果或持久化偏好。上线默认空关键词/全部类型，行为兼容原月列表。

本轮复用现有 Kotlin 标准库与 Material3；调研来源为 `test-artifacts/sdd-v012/reuse-research.md`，范围审查为 `scope-audit.md`。MoneyBook 固定 SHA `98c50341da262e839f68557e721eee38aca57067` 仅交互参考，MIT；不复制其 Room/LIKE 实现或新增许可证义务。Kotlin trim/contains 和官方 TextField/FilterChip API 的链接、固定版本及成本决定在 Task 3 落到 REUSE_V012.md；既有完整许可继续入 APK。

查询无额外硬上限是本次的明确取舍：长于任一字段的非空查询自然无结果；200 字符 UI 截断会将“不匹配的长查询”变成可匹配前缀，而且无法等同于 trim 后的长度。`rememberSaveable` 仍使用系统 Bundle；极端超长粘贴的保存大小不是本次承诺，实机需观察普通长备注搜索，不增设未经需求确认的产品限制。

验收映射：字面/月份/类型组合由 Task 1 的 14 个测试证明；查询、清除、全月统计、切页/切月/重建、增删改刷新和两种空态由 Task 2 的 4 个测试证明；原 6 回归和固定版本真实覆盖升级由 Task 3 最终 CI 证明；作者真实手机覆盖安装与交互单列验收。

## 文件职责与交接接口

| 任务 | 文件 | 职责 |
| --- | --- | --- |
| 1 | 新建 `app/src/main/java/com/xiaoliumiaomiao/anjizhang/domain/EntryFilter.kt` | 无 Android 依赖的纯列表筛选 |
| 1 | 新建 `app/src/test/java/com/xiaoliumiaomiao/anjizhang/EntryFilterTest.kt` | 14 个命名行为与输入不变回归 |
| 2 | 新建 `app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerQueryTest.kt` | 4 项真实 UI 集成行为 |
| 2 | 新建 `app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/KeyboardTestSupport.kt` | 两组 UI 测试共用原生 IME 关闭逻辑 |
| 2 | 新建 `app/src/main/java/com/xiaoliumiaomiao/anjizhang/ui/EntryFilters.kt` | 无状态输入、类型 chips 与清除控件 |
| 2 | 修改 `app/src/main/java/com/xiaoliumiaomiao/anjizhang/ui/LedgerScreen.kt` | 顶层查询状态、派生列表、计数/空态、行 tag、设置版本 |
| 2 | 修改 `app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerSmokeTest.kt` | 版本断言与必要真实滚动；经失败诊断增加三行提交观测，保留原六项业务/10秒/断言 |
| 2 | 修改 `app/build.gradle.kts` | 仅 versionName/code |
| 3 | 修改 `app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerUpgradeTest.kt` | 两个真实安装版本断言 |
| 3 | 修改 `.github/scripts/verify-upgrade.sh`、`.github/workflows/android.yml`、`.gitignore` | 固定 0.1.1 升级基线及目录，保留现有门禁 |
| 3 | 核对现有 `docs/REUSE_V012.md`；新建 `docs/VALIDATION_V012.md`、`docs/REVIEW_V012.md`、`docs/PHONE_ACCEPTANCE_V012.md` | 本轮来源、真实证据、独立审查与待验收记录 |
| 3 | 修改 `README.md`、`docs/HANDOFF.md`、`docs/DEVELOPMENT.md`、本计划 | 当前交付、操作路径与真实完成状态 |
| 3 | ignored `output/an-jizhang-0.1.2-debug.apk`、`output/SHA256SUMS.txt`、`test-artifacts/sdd-v012/` | 同本机签名新包、追加校验和与证据 |

共用接口：`EntryFilters(query: String, kind: EntryKind?, onQueryChange: (String) -> Unit, onKindChange: (EntryKind?) -> Unit, onClear: () -> Unit)`；UI 测试只依赖用户文案与 `entry-${entry.id}` 行 tag，不依赖内部筛选状态。实现、spec reviewer、quality reviewer 的任务说明必须包含上面 Global Constraints 的完整原文，不只附超链接。

## Task 1：纯 EntryFilter 与 JVM RED/GREEN

**Files:** 创建文件见上表 Task 1；其余生产/测试文件不改。

**Interfaces:** 消费已有 `Entry(id, kind, amountCents, category, date, note)`、`EntryKind.EXPENSE/INCOME`。输出 Global Constraints 中唯一 EntryFilter.apply 接口；无数据库、协程、UI 状态或新依赖。

- [x] **Step 1：写完整行为测试。** 新建 EntryFilterTest.kt，内容如下。

```kotlin
package com.xiaoliumiaomiao.anjizhang

import com.xiaoliumiaomiao.anjizhang.domain.*
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class EntryFilterTest {
    private val month = YearMonth.of(2026, 10)
    private fun entry(n: Int, category: String = "餐饮", note: String = "", kind: EntryKind = EntryKind.EXPENSE,
                      date: LocalDate = month.atDay(1)): Entry =
        Entry("00000000-0000-0000-0000-${n.toString().padStart(12, '0')}", kind, n.toLong(), category, date, note)
    private val expense = entry(1, "午餐", "团队 Lunch")
    private val income = entry(2, "奖金", "午餐报销", EntryKind.INCOME)
    private val other = entry(3, "交通", "地铁")
    private val entries = listOf(expense, income, other)

    @Test fun selectedMonthIncludesBothEdgesAndExcludesAdjacentDays() {
        val before = entry(4, date = month.atDay(1).minusDays(1))
        val first = entry(5, date = month.atDay(1))
        val last = entry(6, date = month.atEndOfMonth())
        val after = entry(7, date = month.plusMonths(1).atDay(1))
        assertEquals(listOf(first, last), EntryFilter.apply(listOf(before, first, last, after), month))
    }
    @Test fun emptyQueryAndNullKindReturnAllSelectedMonthEntries() {
        assertEquals(entries, EntryFilter.apply(entries, month))
    }
    @Test fun whitespaceOnlyQueryIsUnrestricted() {
        assertEquals(entries, EntryFilter.apply(entries, month, " \t\n "))
    }
    @Test fun queryTrimsOnlyEdgesAndPreservesInternalWhitespace() {
        val one = entry(4, note = "a  b")
        val two = entry(5, note = "a b")
        assertEquals(listOf(one), EntryFilter.apply(listOf(one, two), month, "  a  b\n"))
    }
    @Test fun categoryOrNoteCanMatchWithoutDuplicatingAnEntry() {
        val both = entry(4, "午餐", "午餐")
        assertEquals(listOf(expense, income, both), EntryFilter.apply(entries + both, month, "午餐"))
    }
    @Test fun typeOnlySupportsExpenseIncomeAndAll() {
        assertEquals(listOf(expense, other), EntryFilter.apply(entries, month, kind = EntryKind.EXPENSE))
        assertEquals(listOf(income), EntryFilter.apply(entries, month, kind = EntryKind.INCOME))
        assertEquals(entries, EntryFilter.apply(entries, month, kind = null))
    }
    @Test fun keywordAndTypeAreBothRequired() {
        assertEquals(listOf(expense), EntryFilter.apply(entries, month, "午餐", EntryKind.EXPENSE))
        assertEquals(listOf(income), EntryFilter.apply(entries, month, "午餐", EntryKind.INCOME))
        assertTrue(EntryFilter.apply(entries, month, "地铁", EntryKind.INCOME).isEmpty())
    }
    @Test fun asciiCaseIsIgnored() {
        assertEquals(listOf(expense), EntryFilter.apply(entries, month, "lUnCh"))
    }
    @Test fun chineseAndLineBreakAreLiteralSubstrings() {
        val target = entry(4, note = "中文\n备注")
        assertEquals(listOf(target), EntryFilter.apply(listOf(target, other), month, "文\n备"))
    }
    @Test fun percentUnderscoreAndDotAreLiteral() {
        val percent = entry(4, note = "50%")
        val underscore = entry(5, note = "a_b")
        val dot = entry(6, note = "a.b")
        val plain = entry(7, note = "axb")
        val all = listOf(percent, underscore, dot, plain)
        assertEquals(listOf(percent), EntryFilter.apply(all, month, "%"))
        assertEquals(listOf(underscore), EntryFilter.apply(all, month, "_"))
        assertEquals(listOf(dot), EntryFilter.apply(all, month, "."))
        assertTrue(EntryFilter.apply(all, month, ".*").isEmpty())
    }
    @Test fun noMatchReturnsEmptyList() {
        assertTrue(EntryFilter.apply(entries, month, "不存在").isEmpty())
    }
    @Test fun filteringPreservesOrderObjectsAndOriginalInput() {
        val source = mutableListOf(income, other, expense)
        val snapshot = source.toList()
        val result = EntryFilter.apply(source, month, "午餐")
        assertEquals(listOf(income, expense), result)
        assertSame(income, result[0])
        assertSame(expense, result[1])
        assertEquals(snapshot, source)
    }
    @Test fun unicodeCharacterCaseWorksWithoutNormalizationOrExpansion() {
        val accent = entry(4, note = "CAFÉ")
        val sharpS = entry(5, note = "straße")
        val all = listOf(accent, sharpS)
        assertEquals(listOf(accent), EntryFilter.apply(all, month, "café"))
        assertTrue(EntryFilter.apply(all, month, "cafe\u0301").isEmpty())
        assertTrue(EntryFilter.apply(all, month, "STRASSE").isEmpty())
    }
    @Test fun fullLengthNoteAndPaddedQueryRemainSearchable() {
        val full = entry(4, note = "中".repeat(200))
        assertEquals(listOf(full), EntryFilter.apply(listOf(full), month, "  ${full.note}  "))
        assertTrue(EntryFilter.apply(listOf(full), month, full.note + "中").isEmpty())
    }
}
```

- [x] **Step 2：添加可编译的最小新接口，再记录行为 RED。** 新建 EntryFilter.kt。未接入 UI 的新接口先返回输入；这是新功能的起始实现，不改坏已有生产行为。

```kotlin
package com.xiaoliumiaomiao.anjizhang.domain

import java.time.YearMonth

object EntryFilter {
    fun apply(entries: List<Entry>, month: YearMonth, query: String = "", kind: EntryKind? = null): List<Entry> = entries
}
```

运行：

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', '--tests', 'com.xiaoliumiaomiao.anjizhang.EntryFilterTest'
```

预期：编译成功，JUnit 行为失败，例如 selectedMonthIncludesBothEdgesAndExcludesAdjacentDays 的集合不相等、noMatchReturnsEmptyList 为 false。保留 build 日志和 `app/build/test-results/testDebugUnitTest/TEST-com.xiaoliumiaomiao.anjizhang.EntryFilterTest.xml` 到 `test-artifacts/sdd-v012/task1-red/`，主 agent 实读失败原因；若为编译/环境错误先修复测试，不能记作 RED。

- [x] **Step 3：将 apply 函数体替换为最小实现。**

```kotlin
fun apply(entries: List<Entry>, month: YearMonth, query: String = "", kind: EntryKind? = null): List<Entry> {
    val needle = query.trim()
    return entries.filter { entry ->
        YearMonth.from(entry.date) == month &&
            (kind == null || entry.kind == kind) &&
            (needle.isEmpty() || entry.category.contains(needle, ignoreCase = true) ||
                entry.note.contains(needle, ignoreCase = true))
    }
}
```

- [x] **Step 4：运行新增测试及全 JVM。**

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', '--tests', 'com.xiaoliumiaomiao.anjizhang.EntryFilterTest'
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest'
git diff --check
```

预期新增 14 项通过，全量 50 项通过，0 失败/错误/跳过；读取 XML 确认实际计数，不把 UP-TO-DATE 说成重新执行。备份/数据库原测试不得删除。

- [x] **Step 5：独立规格审查，然后独立质量审查。** 原文交付 Global Constraints、此 Task、基线/当前 diff、RED/GREEN 日志路径。规格检查全部条件、稳定性和无修改；质量检查纯函数/无过度设计/测试可失败。阻塞问题修复、适量重跑并复审；主 agent 才提交。

主 agent 原子提交：

```powershell
git add app/src/main/java/com/xiaoliumiaomiao/anjizhang/domain/EntryFilter.kt app/src/test/java/com/xiaoliumiaomiao/anjizhang/EntryFilterTest.kt
git commit -m "feat: add pure monthly entry filter"
```

## Task 2：真实 UI RED、筛选接入及 UI GREEN

**Files:** 严格限表格 Task 2；升级 fixture/脚本/CI 不在此任务改。

**Interfaces:** 消费 Task 1 的 EntryFilter.apply；输出无状态 EntryFilters 接口、两个保存状态、文案及行 `entry-${entry.id}` tag。清除回调必须一次同时设置 query=""、kindName=""。

- [x] **Step 1：新增以下完整 LedgerQueryTest.kt，共用 IME helper；先不改任何生产 UI/版本。** 测试种子只包含合成数据，在临时 CI AVD setup 创建数据库。四个行为均通过真实 UI 输入/点击、滚动、重建；只读 VM 用于异步就绪和数据持久化断言，不直接操作筛选状态。

```kotlin
package com.xiaoliumiaomiao.anjizhang

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.xiaoliumiaomiao.anjizhang.data.LedgerDatabase
import com.xiaoliumiaomiao.anjizhang.domain.*
import com.xiaoliumiaomiao.anjizhang.ui.LedgerViewModel
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LedgerQueryTest {
    private val month = YearMonth.now()
    private val seeds = listOf(
        Entry("01200000-0000-0000-0000-000000000001", EntryKind.EXPENSE, 1234L, "午餐", month.atDay(2), "团队午餐"),
        Entry("01200000-0000-0000-0000-000000000002", EntryKind.INCOME, 5000L, "奖金", month.atDay(3), "午餐报销"),
        Entry("01200000-0000-0000-0000-000000000003", EntryKind.EXPENSE, 100L, "交通", month.atDay(1), "地铁"),
        Entry("01200000-0000-0000-0000-000000000004", EntryKind.EXPENSE, 200L, "午餐", month.minusMonths(1).atEndOfMonth(), "上月午餐")
    )
    val compose = createAndroidComposeRule<MainActivity>()
    private val seedDatabase = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("ledger.db")
            val db = LedgerDatabase(context)
            try { seeds.forEach(db::save) } finally { db.close() }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(seedDatabase).around(compose)

    private fun model(): LedgerViewModel {
        var result: LedgerViewModel? = null
        compose.runOnIdle { result = ViewModelProvider(compose.activity)[LedgerViewModel::class.java] }
        return result!!
    }
    private fun ready() {
        val vm = model()
        compose.waitUntil(10000) { !vm.state.value.busy && vm.state.value.entries.size == seeds.size }
        assertNull(vm.state.value.error)
    }
    private fun reveal(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNode(hasScrollAction()).performScrollToNode(matcher)
        return compose.onNode(matcher).assertIsDisplayed()
    }
    private fun text(value: String) = reveal(hasText(value))
    private fun query(value: String) {
        text("搜索分类或备注").performClick().performTextReplacement(value)
        closeIme()
    }
    private fun closeIme() {
        compose.closeKeyboard()
    }
    private fun count(visible: Int, total: Int) { text("显示 $visible / 本月 $total 条") }
    private fun summary(income: String, expense: String, balance: String) {
        text("收入 ¥$income")
        text("支出 ¥$expense")
        text("本月结余 ¥$balance")
    }
    private fun assertQuery(value: String, kind: String) {
        text("搜索分类或备注").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value)))
        text(kind).assertIsSelected()
    }
    private fun row(id: String) = reveal(hasTestTag("entry-$id"))
    private fun rowButton(id: String, label: String) {
        row(id)
        compose.onNode(hasText(label) and hasAnyAncestor(hasTestTag("entry-$id")))
            .performScrollTo().assertIsDisplayed().performClick()
    }
    private fun saveEditor() {
        compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("金额（元）").fetchSemanticsNodes().isEmpty() }
        assertNull(model().state.value.error)
    }
    private fun add(category: String, amount: String) {
        compose.onAllNodes(hasText("记一笔") and hasClickAction() and isEnabled())[0].performClick()
        compose.onNodeWithText("金额（元）").performTextInput(amount)
        compose.onNodeWithText("分类（可自定义）").performScrollTo().performTextReplacement(category)
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().performTextInput("匹配词")
        closeIme()
        saveEditor()
    }

    @Test fun combinesKeywordAndTypeClearsBothAndKeepsFullMonthStatistics() {
        ready()
        query("午餐")
        count(2, 3)
        row(seeds[0].id)
        row(seeds[1].id)
        text("仅支出").performClick()
        count(1, 3)
        row(seeds[0].id)
        summary("50.00", "13.34", "36.66")
        compose.onNodeWithText("统计").performClick()
        text("午餐  ¥12.34")
        text("交通  ¥1.00")
        compose.onNodeWithText("账目").performClick()
        text("清除筛选").performClick()
        assertQuery("", "全部类型")
        count(3, 3)
        row(seeds[2].id)
        summary("50.00", "13.34", "36.66")
    }

    @Test fun retainsQueryAndKindAcrossNavigationRecreationAndMonthSwitch() {
        ready()
        query(" 午餐 ")
        text("仅支出").performClick()
        compose.onNodeWithText("设置").performClick()
        compose.activityRule.scenario.recreate()
        text("备份与恢复")
        compose.onNodeWithText("账目").performClick()
        assertQuery(" 午餐 ", "仅支出")
        count(1, 3)
        text("上月").performClick()
        assertQuery(" 午餐 ", "仅支出")
        count(1, 1)
        row(seeds[3].id)
        compose.activityRule.scenario.recreate()
        assertQuery(" 午餐 ", "仅支出")
        count(1, 1)
        text("本月").performClick()
        count(1, 3)
        row(seeds[0].id)
    }

    @Test fun addingEditingAndDeletingRefreshesMatchingListWithoutLosingQuery() {
        ready()
        query("匹配词")
        text("仅支出").performClick()
        count(0, 3)
        add("试验甲", "12.34")
        count(1, 4)
        add("试验乙", "2.00")
        count(2, 5)
        val first = model().state.value.entries.single { it.category == "试验甲" }
        val second = model().state.value.entries.single { it.category == "试验乙" }
        rowButton(first.id, "编辑")
        compose.onNodeWithText("备注（可选，最多200字）").performScrollTo().performClick().performTextReplacement("已改为其他备注")
        closeIme()
        saveEditor()
        count(1, 5)
        assertEquals("已改为其他备注", model().state.value.entries.single { it.id == first.id }.note)
        row(second.id)
        rowButton(second.id, "删除")
        compose.onNodeWithText("确认删除").assertIsDisplayed().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("删除账目？").fetchSemanticsNodes().isEmpty() }
        assertFalse(model().state.value.entries.any { it.id == second.id })
        count(0, 4)
        text("没有符合条件的账目")
        assertQuery("匹配词", "仅支出")
        summary("50.00", "25.68", "24.32")
        text("清除筛选").performClick()
        count(4, 4)
        row(first.id)
    }

    @Test fun distinguishesEmptyMonthFromNoMatchesAndClearKeepsMonth() {
        ready()
        query("不存在")
        text("仅收入").performClick()
        count(0, 3)
        text("没有符合条件的账目")
        text("下月").performClick()
        count(0, 0)
        text("这个月还没有账目")
        compose.onNodeWithText("没有符合条件的账目").assertDoesNotExist()
        text("清除筛选").performClick()
        assertQuery("", "全部类型")
        val next = month.plusMonths(1)
        text("${next.year}年${next.monthValue}月")
        count(0, 0)
        text("上月").performClick()
        count(3, 3)
        row(seeds[0].id)
    }
}
```

共用 test-only KeyboardTestSupport.kt 如下。LedgerSmokeTest 原 closeEditorKeyboard() 仅改为调用 compose.closeKeyboard()，删除相应无用 imports；原六项业务/断言/版本不变。初审重复逻辑 Important 与修后复审分别保存，不覆盖历史。

```kotlin
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
```

- [x] **Step 2：编译新测试，主 agent 单独提交并推送取得真实 UI RED。**

```powershell
.\scripts\build.ps1 -Tasks ':app:assembleDebugAndroidTest'
git diff --check
```

预期编译成功；生产版本仍 0.1.1/code2、没有搜索控件。主 agent 执行：

```powershell
git add app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerQueryTest.kt app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/KeyboardTestSupport.kt app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerSmokeTest.kt
git commit -m "test: specify ledger query UI behavior"
git push -u origin feat/ledger-query-v0.1.2
gh run list --branch feat/ledger-query-v0.1.2 --limit 5 --json databaseId,headSha,status,conclusion,url
```

选择 headSha 与本次 `git rev-parse HEAD` 相同的 run，后续使用本文“主 agent CI 取证”命令。预期 build 通过、原六 smoke 通过、新四 query 测试因 `搜索分类或备注` 控件不存在而在真实设备失败；逐项读取 XML 与栈。若测试发生早于该断言的 fixture/同步错误，修复测试再重新取得 RED。截图、XML 和原始日志保留；此时 connected 失败，后续升级链未执行，不能称为升级验证。主 agent 明确记录 RED 有效后才给 implementer 继续信号。

执行记录：tests-only 提交 `6c8c530b42d35dc000cd0c5e4dd82b65b668d9bb` 的 [CI 37217431685](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37217431685)，主 agent 实读 50 JVM 全通过、lint 0 Error / 9 Warning；设备 XML 为 10 tests / 4 failures / 0 errors / 0 skipped，原 6 smoke 通过，新 4 query 均以 `No node found…搜索分类或备注` 失败。有效 RED 已确认，升级未执行；原始日志、XML、ui-red-cases.json 和 ui-red-summary.json 保存在 ignored `test-artifacts/ci-v012-37217431685/`。初审的重复 IME 逻辑已提取为共享 test-only helper并独立复审通过，原 Changes requested 报告保留。

- [x] **Step 3：创建 EntryFilters.kt。**

```kotlin
package com.xiaoliumiaomiao.anjizhang.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xiaoliumiaomiao.anjizhang.domain.EntryKind

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryFilters(query: String, kind: EntryKind?, onQueryChange: (String) -> Unit,
                 onKindChange: (EntryKind?) -> Unit, onClear: () -> Unit) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = query, onValueChange = onQueryChange,
            label = { Text("搜索分类或备注") }, singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "全部类型", EntryKind.INCOME to "仅收入", EntryKind.EXPENSE to "仅支出").forEach { (value, label) ->
                FilterChip(selected = kind == value, onClick = { onKindChange(value) },
                    label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
            TextButton(onClick = { focus.clearFocus(); onClear() }, enabled = query.isNotEmpty() || kind != null) {
                Text("清除筛选")
            }
        }
        Text("筛选仅影响列表，统计仍为全月", style = MaterialTheme.typography.bodySmall)
    }
}
```

- [x] **Step 4：最小接入 LedgerScreen。** 增加 `import androidx.compose.ui.platform.testTag`；在 month 定义之后、编辑状态之前增加：

```kotlin
var query by rememberSaveable { mutableStateOf("") }
var kindName by rememberSaveable { mutableStateOf("") }
val kind = EntryKind.entries.firstOrNull { it.name == kindName }
val monthEntries = remember(state.entries, month) { EntryFilter.apply(state.entries, month) }
val visibleEntries = remember(state.entries, month, query, kindName) {
    EntryFilter.apply(state.entries, month, query, kind)
}
```

原 `summary = remember(state.entries, month)` 保持。LazyColumn modifier 在 `.padding(padding)` 后增加 `.imePadding()`，搜索时为真实键盘预留空间。`"账目" -> {` 内从 `val entries = ...` 到空月分支末尾替换为：

```kotlin
item {
    EntryFilters(query, kind, onQueryChange = { query = it },
        onKindChange = { kindName = it?.name.orEmpty() },
        onClear = { query = ""; kindName = "" })
}
item { Text("显示 ${visibleEntries.size} / 本月 ${monthEntries.size} 条") }
if (monthEntries.isEmpty()) item {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("这个月还没有账目", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { pendingSaveId = null; editingJson = null; editing = true }, enabled = !state.busy) { Text("记一笔") }
    }
} else if (visibleEntries.isEmpty()) item {
    Text("没有符合条件的账目", style = MaterialTheme.typography.titleMedium)
}
```

原 `items(entries, key = { it.id })` 替换为 `items(visibleEntries, key = { it.id })`，原行 Column modifier 的 `.fillMaxWidth()` 后增加 `.testTag("entry-${entry.id}")`；原行内部内容及编辑删除 onClick 保持。筛选控件中已始终提供清除按钮，无结果/空月时无需创建第二个同文案按钮。设置页仅将 `Text("版本 0.1.1")` 改成 `Text("版本 0.1.2")`。

- [x] **Step 5：更新版本及原 smoke 的必要滚动。** app/build.gradle.kts defaultConfig 仅两行：

```kotlin
versionCode = 3
versionName = "0.1.2"
```

LedgerSmokeTest 版本断言改为 `compose.onNodeWithText("版本 0.1.2").performScrollTo().assertIsDisplayed()`。原列表更长，新增 helper，保留已有 readyEntryAction、closeEditorKeyboard、全部异步/重建/失败断言：

```kotlin
private fun revealLedgerText(value: String): SemanticsNodeInteraction {
    compose.onNode(hasScrollAction()).performScrollToNode(hasText(value))
    return compose.onNodeWithText(value).assertIsDisplayed()
}
```

对原 emptyLedgerAndNavigation 及删除结束的 `这个月还没有账目` 可见断言改用 `revealLedgerText("这个月还没有账目")`。原 `-¥0.01` 保存等待不要依赖尚未进入组合的屏外行，替换为：

```kotlin
val savedModel = ledgerModel()
compose.waitUntil(10000) {
    savedModel.state.value.entries.any { it.amountCents == 1L && it.kind == com.xiaoliumiaomiao.anjizhang.domain.EntryKind.EXPENSE } &&
        compose.onAllNodesWithText("金额（元）").fetchSemanticsNodes().isEmpty()
}
revealLedgerText("-¥0.01")
revealLedgerText("支出 ¥0.01")
```

原重建保存完成后的 `compose.onNodeWithText("-¥12.34").assertIsDisplayed()` 改用 `revealLedgerText("-¥12.34")`。原等待均仍 10000ms，真实列表和统计仍实际显示；需要点击的屏外“删除”先 `revealLedgerText("删除")` 再 performClick。不得把可见性断言改成只检查 VM 或 assertExists。

- [x] **Step 6：本机编译、完整 JVM、lint；设备 UI GREEN 留待 Task 3。**

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest'
git diff --check
```

预期 50 JVM、lint 0 Error、两 APK 编译；警告据真实输出记录。主 agent 审查后仅在本地提交本任务 UI 接入（不包含 Task 3 文件），暂不推送：

```powershell
git add app/src/main/java/com/xiaoliumiaomiao/anjizhang/ui/EntryFilters.kt app/src/main/java/com/xiaoliumiaomiao/anjizhang/ui/LedgerScreen.kt app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerSmokeTest.kt app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerQueryTest.kt app/build.gradle.kts
git commit -m "feat: search and filter monthly ledger entries"
```

此时只记录本机 JVM/lint/两APK编译 GREEN，Task 2 的设备 GREEN 明确保持待执行。版本已0.1.2，旧升级脚本尚期待0.1.1，不能推送制造必然的中间版本失败CI，也不能跳过升级门禁。Task 2代码先完成独立规格/质量审查和本地原子提交；Task 3同步新升级链并通过独立审查后，由根agent一次推送当前全部提交取得完整CI GREEN。该运行中实际10项UI全过，才完成Task 2设备验收并勾选对应步骤；代码审查通过不代替设备通过。
- [x] **Step 7：独立规格审查、质量审查。** 审查状态在分页条件外；统计/export 的全账本边界；清除两条件；默认行为；四测试真实交互、6 原测试完整；无操作 ID 迁移；IME 与 LazyColumn 使用现有版本 API。遇阻塞先修复再相应重跑。必要的测试同步修复不能改变 RED 的需求契约。

## Task 3：固定升级链、完整验证、APK 与私有 PR 交付

**Files:** 表格 Task 3；不改变已验收 Task 1/2 功能，若发现问题回到所属任务修复并复审。

**Interfaces:** 消费 0.1.2/code3 应用及测试 APK；旧 APK 必须由固定已验收 `976817290302e712519297cc5b810ce019d2d383` 在同 CI job 构建。同一 fixture 两方法分别在真实 0.1.1/code2 与 0.1.2/code3 进程执行，不得假造版本或重建旧数据库。

- [x] **Step 1：替换 fixture/脚本的版本常量，保留所有断言。** LedgerUpgradeTest 两行分别为：

```kotlin
val context = installedVersion("0.1.1", 2) // seedOldVersionLedger
val context = installedVersion("0.1.2", 3) // verifyUpgradedLedger
```

三条原合成记录、完整 Entry 相等、schema1、开库/重开、10月和9月统计全部保持。`.github/scripts/verify-upgrade.sh` 只替换下列三行：

```bash
check_apk "$baseline_apk" baseline "$app" 2 0.1.1
check_apk "$current_apk" current "$app" 3 0.1.2
printf 'PASS: synthetic ledger survived 0.1.1/code2 -> 0.1.2/code3 replacement\n' | tee "$artifacts/summary.txt"
```

- [x] **Step 2：同步现有 CI 基线，保留完整工作流。** `.github/workflows/android.yml` 所有 `baseline-v010` 改 `baseline-v011`；注释/step name 中的 `fixed 0.1.0` 改 `fixed 0.1.1`；第二 checkout 的 ref 改为下列完整块。不要修改 action pins、notClass、设备和证据步骤。

```yaml
      - name: Checkout fixed 0.1.1 baseline
        uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262
        with:
          ref: 976817290302e712519297cc5b810ce019d2d383
          path: baseline-v011
          persist-credentials: false
```

`.gitignore` 保留 `baseline-v010/` 并追加 `baseline-v011/`，不取消历史忽略、不纳入 APK/证据。

- [x] **Step 3：重新编译受影响 fixture，做静态门禁，再提交真实 CI。**

```powershell
.\scripts\build.ps1 -Tasks ':app:assembleDebugAndroidTest'
git diff --check
git diff -- .github/scripts/verify-upgrade.sh .github/workflows/android.yml app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerUpgradeTest.kt .gitignore
```

预期变更只有上述版本/固定基线/目录；需要运行 shell 语法时用本机已有 Git Bash（先只读 `Get-Command bash` 验证存在）或 CI Linux 的 `bash -n .github/scripts/verify-upgrade.sh`，不得为了语法检查安装新工具。根 agent 核对 diff 保留 `-w -r`、恰 1 OK/唯一 -1、证书/单设备/超时/禁中途清库全部原码；既有 parser 无变更无需额外另造 mock 取代真实升级。

主 agent 在独立 spec/quality 审查通过后提交并推送：

```powershell
git add .github/scripts/verify-upgrade.sh .github/workflows/android.yml .gitignore app/src/androidTest/java/com/xiaoliumiaomiao/anjizhang/LedgerUpgradeTest.kt
git commit -m "test: verify accepted 0.1.1 to 0.1.2 upgrade"
git push
```

- [x] **Step 4：主 agent 真实 CI 取证，所有阶段必须通过。** 以下 PowerShell 获取当前提交运行并保存原始资料，不从 GitHub 绿色图标推断业务正确：

```powershell
$expectedHead = (git rev-parse HEAD).Trim()
$runs = gh run list --repo xiaoliumiaomiao/an-jizhang --branch feat/ledger-query-v0.1.2 --limit 10 --json databaseId,headSha,status,conclusion,url | ConvertFrom-Json
$selectedRun = $runs | Where-Object { $_.headSha -eq $expectedHead } | Select-Object -First 1
if (-not $selectedRun) { throw '本次提交尚未出现 CI；稍后重查，不选择旧运行' }
$runId = $selectedRun.databaseId
$evidence = Join-Path 'test-artifacts' "ci-v012-$runId"
New-Item -ItemType Directory -Path $evidence -Force | Out-Null
gh run view $runId --repo xiaoliumiaomiao/an-jizhang --json databaseId,headSha,status,conclusion,url,jobs | Set-Content -Encoding UTF8 (Join-Path $evidence 'run.json')
```

使用短时状态查询间隔，持续向用户报告有意义进展；不要单次阻塞超过60秒。完成后：

```powershell
gh run view $runId --repo xiaoliumiaomiao/an-jizhang --log | Set-Content -Encoding UTF8 (Join-Path $evidence 'run.log')
gh run download $runId --repo xiaoliumiaomiao/an-jizhang --dir $evidence
Get-ChildItem -LiteralPath $evidence -Recurse -Filter '*.xml' | Select-Object FullName
Get-ChildItem -LiteralPath $evidence -Recurse -Filter '*instrumentation.txt' | ForEach-Object { Get-Content -LiteralPath $_.FullName }
```

只对 test-results 与 androidTest-results 的 `testsuite` XML求和，避免同时把 HTML 报告副本计数。下面按文件路径筛选打印每 suite，主 agent 确认6个原 JVM suite 加1个 EntryFilter suite共50、原 LedgerSmoke 6 加 LedgerQuery 4共10：

```powershell
$xmlFiles = Get-ChildItem -LiteralPath $evidence -Recurse -Filter '*.xml' | Where-Object { $_.FullName -match 'test-results|androidTest-results' }
$suites = foreach ($file in $xmlFiles) {
    [xml]$document = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($suite in $document.SelectNodes('//testsuite')) {
        [pscustomobject]@{ File=$file.FullName; Name=$suite.name; Tests=[int]$suite.tests; Failures=[int]$suite.failures; Errors=[int]$suite.errors; Skipped=[int]$suite.skipped }
    }
}
$suites | Format-Table -AutoSize
$suites | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 (Join-Path $evidence 'suite-summary.json')
```

最终必须：两个 job success，50 JVM/10 UI 0失败0错误0跳过；lint 0 Error（warning如实）；seed/verify各恰1通过及唯一 completion=-1；安装旧版0.1.1/code2、新版0.1.2/code3；三包同公开证书；旧账目所有字段及两个月统计正确；replacement Success；launch Status: ok；Upgrade/Device exit0。打开 device-screen.png 检查中文界面真实启动。保存失败历史，不用后来通过抹除 RED 或失败；任何最终失败修复后重新在同一工作流取得通过。

- [x] **Step 5：本机最终构建并核验新旧公开证书、包信息与许可。**

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest'
$config = Get-Content -LiteralPath '.tooling/toolchain.json' -Raw | ConvertFrom-Json
$toolsRoot = Join-Path $config.sdkRoot 'build-tools/35.0.0'
$newApk = (Resolve-Path 'app/build/outputs/apk/debug/app-debug.apk').Path
& (Join-Path $toolsRoot 'aapt.exe') dump badging $newApk
& (Join-Path $toolsRoot 'aapt.exe') dump permissions $newApk
$previousJava = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $config.javaHome
    & (Join-Path $toolsRoot 'apksigner.bat') verify --verbose --print-certs $newApk
    if ($LASTEXITCODE -ne 0) { throw '新包签名验证失败' }
    & (Join-Path $toolsRoot 'apksigner.bat') verify --print-certs 'D:\AIproject\an-jizhang\output\an-jizhang-0.1.1-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw '旧包签名验证失败' }
} finally { $env:JAVA_HOME = $previousJava }
& (Join-Path $toolsRoot 'aapt.exe') dump xmltree $newApk AndroidManifest.xml
```

把命令实际输出保存在 ignored 本轮 audit 文件。明确核对包名 `com.xiaoliumiaomiao.anjizhang`、version3/0.1.2、min26/target35，无 INTERNET/广泛存储权限，allowBackup=false；公开证书新旧相等，当前已验收旧包历史公开证书为 `209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`，必须现场重验，不以文档代替。用现有 Zip API 读取 APK assets/licenses 三文件，与源码逐文件比较 SHA256（不读取密钥）：

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($newApk)
try {
    Get-ChildItem -LiteralPath 'app/src/main/assets/licenses' -File | ForEach-Object {
        $entry = $zip.GetEntry('assets/licenses/' + $_.Name)
        if (-not $entry) { throw "APK缺许可：$($_.Name)" }
        $stream = $entry.Open()
        $sha = [Security.Cryptography.SHA256]::Create()
        try { $actual = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-', '').ToLowerInvariant() }
        finally { $sha.Dispose(); $stream.Dispose() }
        $expected = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($actual -ne $expected) { throw "许可内容不一致：$($_.Name)" }
        "$($_.Name): $actual"
    }
} finally { $zip.Dispose() }
```

- [x] **Step 6：以新文件名交付，拒绝覆盖异哈希目标，保留旧包和旧校验行。** 先完成证书相同门禁，再运行：

```powershell
$source = (Resolve-Path 'app/build/outputs/apk/debug/app-debug.apk').Path
$newHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
$destinations = @((Join-Path (Get-Location).Path 'output'), 'D:\AIproject\an-jizhang\output')
foreach ($directory in $destinations) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $target = Join-Path $directory 'an-jizhang-0.1.2-debug.apk'
    if (Test-Path -LiteralPath $target) {
        if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $newHash) { throw "拒绝覆盖异哈希APK：$target" }
    } else { Copy-Item -LiteralPath $source -Destination $target }
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $newHash) { throw '复制后哈希不一致' }
    $sums = Join-Path $directory 'SHA256SUMS.txt'
    $line = "$newHash  an-jizhang-0.1.2-debug.apk"
    $existing = if (Test-Path -LiteralPath $sums) { @(Get-Content -LiteralPath $sums) } else { @() }
    $oldNewVersionLine = $existing | Where-Object { $_ -match '  an-jizhang-0\.1\.2-debug\.apk$' }
    if ($oldNewVersionLine -and ($oldNewVersionLine -ne $line)) { throw '校验文件已有不同0.1.2记录，保留并调查' }
    if ($existing -notcontains $line) { Add-Content -LiteralPath $sums -Value $line -Encoding ASCII }
}
```

复制前后分别记录两个 output 中 0.1.0/0.1.1 的 SHA256，确认历史文件未变；不自动安装到作者手机，不更换签名，不触碰真实账本。回退保留旧 APK/基线，但 Android 拒绝降版本时不得指导卸载或 `-d` 强行覆盖；作者先导出备份并保留现装版本，修复后以更高版本继续。

- [x] **Step 7：写本轮证据文档和未勾手机验收。** 主agent已创建REUSE_V012.md，本任务先核对并最小补充，不覆盖已有内容；本轮真实调研归档包含：固定版本、MoneyBook SHA/MIT/不复制、官方 API 链接、无新依赖/权限、采用成本；保留既有授权。VALIDATION_V012.md 记录已执行命令及日志路径、Task1 RED断言、UI RED实际run/commit、最终GREEN实际run/commit、真实 suite/runner计数、lint、APK hash/公开证书、截图观察、未解决限制。REVIEW_V012.md 逐项列实际 reviewer、范围、结论、修复与复审证据；报告未返回前不得写 Approved。

PHONE_ACCEPTANCE_V012.md 的初始完整正文：

```markdown
# 0.1.2 手机覆盖升级验收（待作者执行）

0.1.1 已由作者确认全部通过；本表只记录新一轮 0.1.2，尚无手机结果。
先在现有 0.1.1 中导出备份，再直接安装本机同签名 output/an-jizhang-0.1.2-debug.apk。
如安装报错，保留原应用和账本，记录提示；不要卸载。备份为明文，请保存到可信位置。

- [ ] 覆盖安装成功，设置显示 0.1.2；旧条数、金额、分类、日期、备注与跨月统计保留。
- [ ] 本月搜索分类或备注立即更新；空格首尾忽略，百分号/下划线/点号按字面匹配。
- [ ] 全部类型/仅收入/仅支出与关键词组合正确；清除筛选同时清除两条件，月份不变。
- [ ] 筛选后顶部和统计页仍是完整所选月，页面说明清楚。
- [ ] 切换账目/统计/设置、切月再返回及系统重建后，查询和类型按约定保留。
- [ ] 搜索后新增匹配账目会出现，编辑为不匹配后移出，删除后结果刷新；清除可看到其他账目。
- [ ] 空月与无匹配提示不同；键盘关闭、滚动、字号下的控件和底部导航可用。
- [ ] 筛选开启时导出仍是完整账本，重复恢复不重复；重开账本正常。

手机型号、Android版本、验收日期和作者反馈尚未提供。技术CI与本表手机结果分别记录。
```

README/HANDOFF 当前段写清 0.1.2 功能/包名/实际证据链接和“待作者手机验收”；0.1.0/0.1.1历史验收记录保留。DEVELOPMENT 的本轮路径改为 10 UI、固定新 SHA、0.1.1→0.1.2，并链接本轮验收表。长期50000条/10MiB整本备份上限、debug签名和不保证CI覆盖安装的限制继续保留。此计划仅对实际完成步骤勾选，不预填事实。文档里运行 ID、hash 等必须从本次工具输出抄录，不能用旧值凑齐。

- [ ] **Step 8：完整分支独立审查，根 agent 私有 PR 及同步验证。** 审查 `git diff 976817290302e712519297cc5b810ce019d2d383...HEAD` 加当前未提交改动；独立 spec/quality reviewer 都读取 Global Constraints、3任务、原始证据。重点关注统计/导出边界、savedState、操作ID、UI测试真失败/真通过、升级不清库、版本/签名与作者验收未预勾。修复阻塞后复审。源码若变，重跑相关测试且最终全CI需覆盖修改后的代码；只有文档改动可引用已测代码提交并说明差异。

主 agent 按实际完成文档列表显式 git add、diff check、commit/push；不要 `git add .`。PR保持 draft，repo先确认private：

```powershell
gh repo view xiaoliumiaomiao/an-jizhang --json isPrivate,url
git status --short
git diff --check
```

写入 ignored PR正文文件（最终加入真实验证链接后由主 agent 提交）：

```powershell
@'
本月账目增加分类或备注的字面关键词搜索，并可组合全部/收入/支出筛选。清除一次复原两个条件；导航、切月和Activity重建保留条件，统计与整本备份继续基于完整账本。

新增纯筛选测试和四项真实UI行为测试，保留原六项回归。固定已验收0.1.1到0.1.2的同证书覆盖升级，检查全部账目字段与跨月统计。详细RED/GREEN运行、APK SHA256、公开证书与审查结论见docs/VALIDATION_V012.md和docs/REVIEW_V012.md。

本次为debug测试交付，新的手机验收表保持待作者执行；正式签名与商店发布不在范围。
'@ | Set-Content -LiteralPath 'test-artifacts/sdd-v012/pr-body.md' -Encoding UTF8
gh pr create --repo xiaoliumiaomiao/an-jizhang --base main --head feat/ledger-query-v0.1.2 --draft --title 'feat: add monthly ledger search and type filter (0.1.2)' --body-file test-artifacts/sdd-v012/pr-body.md
```

若当前分支已有PR，用 `gh pr view --json url` 找到并更新，不重复建；创建后调用 `mcp__codex_app__attach_artifact`，artifact_type=pull_request、url为工具真实返回。最后 `gh pr view --json url,headRefOid,isDraft`、`git rev-parse HEAD`、`git ls-remote origin refs/heads/feat/ledger-query-v0.1.2` 三方HEAD一致，repo仍private，工作区无未处理改动，新旧APK hash再次一致。向作者给出新APK绝对路径、hash、测试结果、升级第一步和新验收表；到此停止，等待作者本轮手机结果。

## 计划自检

- [ ] 需求映射齐全：14个纯函数行为、4个设备行为、6原回归、真实升级、包核验、作者门禁。
- [ ] 接口签名和文案逐字一致；测试不使用未定义 helper；固定依赖无新API。
- [ ] Task 2 RED 与最终 GREEN 证据分别保存；Task2本地提交不推送，Task3同步升级链后一次获取完整GREEN。
- [ ] 每个 implementer/reviewer brief 均复制完整 Global Constraints 并明确文件范围；Git/CI/交付由根 agent执行。
- [ ] 本文代码块与实际实现按当前源码验证；方案中不存在替代行为测试的编译错误或虚假证据。

计划编写本身不表示上述构建、测试、审查或交付已经执行。

## 完整CI失败后的最小诊断补充（2026-10-05）

首次完整CI37218934264对应8f1c4c9：50 JVM通过，10 UI中4 Query/5 Smoke通过；唯一原negative case等待错误文字10000ms超时，升级未执行。只读Phase1/2尚未证明根因，原日志/失败报告保留。

ROOT依照Step7已有“必要测试同步修复不能改变需求契约”授权单变量增加原negative方法三行：只读model、持锁后既有Jobs快照、真实点击后重建前复用awaitSubmitted。原DROP/recreate/unlock/错误与输入/取消断言逐字保留，不加IME关闭/重试/扩时/skip/生产改动。11秒编译4执行/40缓存，独立/root/v012_final_review Spec/Quality Approved；ROOT核验tree822835c9c336043fead7447d93850e2e865babe2后提交0076b6b11de015a81f7c2f4e2337f9408ccd9a41并启动CI37220141121。它是更明确的提交观测，不等于根因已修好；即使GREEN，也不从单次结果反推原失败必定未dispatch。新运行的完整实测结果及余下交付步骤仍按真实证据收口。
实际收口证据：CI37220141121 / 0076b6b 两job成功，50 JVM/10 UI/seed1/verify1全0；negative新增提交观测日志jobs=1与原业务通过，不反推旧根因。两处output新SHA匹配审计8c82...，三旧APK不变；SHA256SUMS原94/188bytes前缀SHA与独立交付前快照一致。首次PowerShell报告元数据膨胀保留raw，compact基本类型记录实际复核，无APK变化；ReadAllLines单行报告修正未重新执行复制。最终draft PR同步与delivery审查待完成，作者八项仍未勾。
