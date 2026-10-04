# 安记账首版 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付中文安卓离线记账测试 APK，覆盖收支、分类、月统计、备份恢复。

**Architecture:** Kotlin + Compose Material3，单 Activity。纯 Kotlin domain 与 Android SQLite 数据层分离，ViewModel/StateFlow 驱动界面。JSON 备份通过系统文档选择器，以验证后事务合并方式恢复。

**Tech Stack:** Gradle8.11.1，AGP8.9.2，Kotlin/Compose plugin2.1.20，Compose BOM2025.04.01，activity-compose1.10.1，lifecycle2.8.7，coroutines1.10.2，JUnit4.13.2，Robolectric4.14.1。协程采用已验证发布的1.10.2补丁版本，并已通过Task1构建测试（如平台相容验证发现问题，以记录后最小兼容调整为准）。

## Global Constraints

- 完整遵循 agent.md 和 docs/REQUIREMENTS.md，首版不扩展多账户/转账/同步。
- minSdk26，compileSdk35，targetSdk35；applicationId/namespace 为 com.xiaoliumiaomiao.anjizhang；versionName0.1.0，versionCode1。
- 不添加 INTERNET/广泛存储权限，不接广告、遥测、自动云备份。
- 金额 Long 整数分：MAX_CENTS=99999999999L；分类1..20字符、备注<=200字符、日期年份1900..9999。
- 备份格式an-jizhang，version1，currencyCNY；UTF-8<=10MiB，<=50000记录；同ID相同跳过、冲突整批拒绝、事务内无部分写入。
- 第三方复用固定SHA、保留完整许可证并包含在APK；不执行未经审查的第三方脚本。
- 所有技术证据与作者实机验收分开；不提前标记完成。

---

### Task 1: 构建、金额规则与本地账本/备份

**Files:**
- Create: build.gradle.kts, settings.gradle.kts, gradle.properties, gradle/wrapper/*, gradlew, gradlew.bat, app/build.gradle.kts, app/src/main/AndroidManifest.xml。
- Create: app/src/main/java/com/xiaoliumiaomiao/anjizhang/domain/{Entry,Money,LedgerRules,MonthlySummary}.kt。
- Create: app/src/main/java/com/xiaoliumiaomiao/anjizhang/data/{LedgerDatabase,BackupCodec}.kt。
- Create: app/src/main/assets/licenses/*, third_party/*, docs/REUSE_AUDIT.md。
- Test: app/src/test/java/com/xiaoliumiaomiao/anjizhang/{MoneyTest,SummaryTest,LedgerDatabaseTest,BackupCodecTest}.kt。

**Interfaces:**
- `enum class EntryKind { EXPENSE, INCOME }`。
- `data class Entry(val id:String, val kind:EntryKind, val amountCents:Long, val category:String, val date:LocalDate, val note:String="")`。
- `object Money { const val MAX_CENTS:Long=99999999999L; fun parse(text:String):Long?; fun format(cents:Long):String; fun plain(cents:Long):String }`。
- `object LedgerRules { fun validate(entry:Entry):Unit }` throws IllegalArgumentException for invalid record。
- `data class MonthlySummary(val incomeCents:Long,val expenseCents:Long,val expensesByCategory:Map<String,Long>)` with `val balanceCents:Long` and companion `fun calculate(entries:List<Entry>,month:YearMonth):MonthlySummary`。
- `class LedgerDatabase(context:Context):SQLiteOpenHelper` exposes `fun listEntries():List<Entry>`, `fun save(entry:Entry):Unit`, `fun delete(id:String):Unit`, `fun merge(entries:List<Entry>):ImportResult`；`data class ImportResult(val imported:Int,val skipped:Int)`。
- `object BackupCodec { const val MAX_BYTES:Int=10485760; fun encode(entries:List<Entry>):String; fun decode(json:String):List<Entry> }`。

- [x] Step 1: 配置固定版本构建并从审查过的固定SHA获取官方wrapper和许可证；只创建无业务实现的API骨架。添加能验证行为的测试：
```kotlin
@Test fun preciseCents() { assertEquals(1234L, Money.parse("12.34")) }
@Test fun invalidAmount() { for (x in listOf("0","-1","1.234","1e2","1000000000")) assertNull(Money.parse(x)) }
@Test fun monthIsolated() {
  val x = listOf(Entry("00000000-0000-0000-0000-000000000001",EntryKind.INCOME,10000,"工资",LocalDate.of(2026,10,1)))
  assertEquals(10000L, MonthlySummary.calculate(x,YearMonth.of(2026,10)).incomeCents)
  assertEquals(0L, MonthlySummary.calculate(x,YearMonth.of(2026,9)).incomeCents)
}
```
- [x] Step 2: `./gradlew.bat :app:testDebugUnitTest --no-daemon`，记录预期断言失败（非拼写/环境故障），保存在任务报告。
- [x] Step 3: 实现规则与统计；Money.parse适配MoneyBook的BigDecimal片段并添加regex和上限，format不转double。数据库SQL列为id TEXT PRIMARY KEY, kind TEXT, amount_cents INTEGER, category TEXT, date TEXT, note TEXT。保存前validate，查询稳定排序。merge先验证整批，再在单一事务内对比已有记录；冲突抛异常回滚。BackupCodec严格验证根格式、version、currency、记录类型/范围/重复ID/UTF-8大小；不得用JSONObject.getLong静默截断小数。
- [x] Step 4: 通过真实SQLite/Robolectric测试插入编辑删除、关闭重开、全量roundtrip、重复导入、冲突回滚（冲突记录放批次末尾）、损坏JSON、小数金额、超限与未知版本。执行 `./gradlew.bat :app:testDebugUnitTest --no-daemon` 和 `git diff --check`，记录实际输出。
- [x] Step 5: 自检并显式add相关文件，提交 `feat: add exact money rules and transactional local ledger`；独立审查规格及代码，修复阻塞发现后复审。

### Task 2: 中文界面与系统备份/恢复

**Files:**
- Create: app/src/main/java/com/xiaoliumiaomiao/anjizhang/{MainActivity}.kt。
- Create: app/src/main/java/com/xiaoliumiaomiao/anjizhang/ui/{LedgerViewModel,LedgerScreen,EntryEditor,Theme}.kt。
- Create: app/src/main/res/values/{strings,styles,colors}.xml, app/src/main/res/drawable/ic_launcher.xml, app/src/main/res/xml/{backup_rules,data_extraction_rules}.xml。
- Test: app/src/test/java/com/xiaoliumiaomiao/anjizhang/{LedgerViewModelTest,MainActivityTest}.kt。

**Interfaces:**
- Consumes Task1 Entry/Money/LedgerRules/MonthlySummary/LedgerDatabase/BackupCodec。
- `class LedgerViewModel(application:Application):AndroidViewModel` exposes a StateFlow UI状态（entries,busy,error），后台执行save/delete/load，成功后刷新；保存失败不关闭编辑器且明确错误。数据库owner仅ViewModel。
- `@Composable fun LedgerScreen(viewModel:LedgerViewModel,onExport:()->Unit,onImport:()->Unit,onLicenses:()->Unit)`。
- MainActivity注册CreateDocument("application/json")/OpenDocument()；导出前提示明文；导入限流读取最多MAX_BYTES+1字节并在后台调用decode+merge，返回新增/跳过数量。

- [x] Step 1: 添加可执行行为测试：启动后显示“安记账”、空状态、记账入口；数据库失败能够呈现错误；不在启动时添加演示账目。先运行测试记录因页面尚未实现而失败。
- [x] Step 2: 实现温暖米白/深青配色Material3，账目/统计/设置三个导航项；月切换和本月按钮，汇总卡，分类条形统计，列表编辑与确认删除。Editor提供支出/收入、金额、日期选择器、分类建议+自定义、备注、验证错误。使用rememberSaveable保留输入状态，长内容可滚动、点击区>=48dp，系统Insets不会遮挡。
- [x] Step 3: 实现备份选择器与后台I/O，取消选择不改变数据；read失败/解析失败不执行merge；恢复先提示合并方式；设置内显示隐私/卸载提醒/版本及第三方许可。
- [x] Step 4: `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon`，修正真实失败。检查merged manifest权限/backup属性；如果未能运行安卓UI，明确测试层级并保留作者验收。
- [x] Step 5: 自检提交 `feat: add offline bookkeeping screens and document backups`，独立审查并修复后复审。

### Task 3: 验证、交付与私有远端同步

**Files:**
- Modify: README.md, docs/HANDOFF.md。
- Create: docs/{VALIDATION,PHONE_ACCEPTANCE,DEVELOPMENT}.md, scripts/build.ps1, .github/workflows/android.yml。
- Output ignored: output/an-jizhang-0.1.0-debug.apk, output/SHA256SUMS.txt, test-artifacts/。

**Interfaces:**
- Consumes完整应用；交付测试APK，正式签名/商店发布不在范围内。

- [x] Step 1: 使用实际JDK/SDK执行测试、lint、debug构建并保存日志；独立全分支审查，修复阻塞项后重跑受影响验证。
- [x] Step 2: `git diff --check`；APK使用aapt分析无INTERNET权限且apksigner verify成功。复制生成APK到output，不覆盖已有同名异哈希文件；Get-FileHash记录SHA256。
- [x] Step 3: 如无本机模拟器可用，配置GitHub Actions的Android模拟器instrumentation smoke测试并保留报告；无法运行时如实记录，不伪造通过。最终37191001385实际6项/0失败/0错误/0跳过。
- [x] Step 4: 写清楚一条构建路径（项目工具路径+scripts/build.ps1）、每步成功信号、失败处理、回退界限；手机验收先安装测试APK，然后新增一笔0.01元支出，作者检查界面及金额；后续勾选验收表。
- [ ] Step 5: 提交并推送feature分支/建立可审查PR，attach PR；作者验收记录保持pending。核验GitHub private、远端HEAD、工作区状态并交付。
