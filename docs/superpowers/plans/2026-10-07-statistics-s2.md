# 安记账 S2 统计深化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 交付0.1.5/code7，使用户能通过月年趋势、分类结构、同期变化和逐笔来源回看账目。
**Architecture:** 沿用单Activity、Compose、现有LedgerViewModel/SQLite。纯Kotlin分析接收完整账目快照；统计视图独立于明细搜索，分类钻取和详情共享稳定ID操作。后台派生结果绑定请求，防止旧数据覆盖新期间。
**Tech Stack:** Kotlin2.1.20、Compose BOM2025.04.01、Material3、Lifecycle2.8.7、Coroutines1.10.2、JUnit/Robolectric/Compose UI Test；不增加依赖。
**日期/授权:** 2026-10-07（Asia/Shanghai）。作者已明确0.1.4“全部予以通过”，授权继续深化统计以帮助自查自省。基线c476eac220ba3f062200365671a674e5decfd06d（仅在最终S1交付4c452e1上记录验收）；新分支codex/ledger-statistics-v0.1.5。本计划扩充V1.0的S2只读统计，不进入S3。

## Global Constraints

- Android 8.0 / API 26 及以上；默认 CNY 人民币；单账本。
- 首版本地存储，不添加 INTERNET 权限、广告、遥测、第三方分析、云同步。
- 金额用整数分，禁止浮点数参与账目计算；校验输入、日期、金额边界。
- 单笔金额 0.01 至 999999999.99 元；分类修剪后 1 至 20 字符；备注最多 200 字符；记录年份 1900 至 9999。
- 保持schema1、format1、稳定UUID和Entry全部原字段；不修改DB/BackupCodec/VM操作协议、旧MonthlySummary接口、Money和LedgerRules。
- 所有统计使用完整state.entries；明细关键词及收支过滤不改变统计。分类身份为(kind, Kotlin trim后的名称)，大小写敏感；不借用子串搜索钻取。
- 旧81项JVM和35项常规UI原名称/行为/10秒上限保留。新测试单独取得行为RED再实现，不把编译失败或未执行当RED。
- 保存/删除只消费匹配operationId；原明细详情成功回明细、取消留详情、失败留草稿。统计来源单独回原统计或分类明细；busy/pending同步守卫保留。
- 新包0.1.5/code7，BuildConfig显示一致；旧已验收0.1.4/code6→新code7同签名升级，不清库、不卸载、不破坏迁移。
- 两处output旧物及作者主目录HANDOFF/未跟踪方案共107文件已冻结保护；主目录原字节不改。技术验证与新版本作者手机验收分开，手机框不预勾。
- 原深青配色、系统中文字体、浅深主题与native窗口底色保持；可点击区域至少48dp。360dp、fontScale1.0/1.3/2.0可读可滚动；图形有同源文字金额。
- 不加储蓄率/资产余额/预算/账户/分类库/AI评语/通知；分析仅根据已记录账目，不把无记录解释成实际无消费。

## 产品与统计合同

1. 月度/年度切换。月度默认沿用所选月份，保留既有月统计标题/上月下月本月和月份选择；年度默认该月所属年份，提供上年/下年/本年及1900–9999年份输入。上下界按钮禁用，不生成1899/10000导航。
2. 完整所选期间的收入/支出/结余，结余只表示记录收入减记录支出。总览包含期间内未来日期记录时明确提示“包含未来日期账目”；同期比较单独截断，不混用两个口径。
3. 收入/支出分类各自排行：金额降序、同额category升序；金额、占本类型总额比例（1位小数，HALF_UP）、记录笔数。没有该类型记录时显示空态，比例不除零。
4. 年度始终12个月的双系列柱形概览，并有各月收入、支出、结余文字；无记录月金额0且标“无记录”，未来月加“未到”。图形仅用于相对比较，不截断或回写金额；选择月文字行可进入该月月度分析。
5. 同期比较只比较上一周期：月对上月，年对上年。已结束期间比较两个完整周期，显示准确起止日期。当前月按相同前k天比较，k=min(今日日期数、上月天数)；双方从各自1日开始。当前年按相同前k天比较，k=min(今日在本年的天数、上年全年天数)，双方从1月1日开始。年内遇闰年日期可能相差1天；界面说明“按相同天数比较”，两范围完整展示。未来期间/1900边界没有可用对比则显示原因，不生成越界日期。
6. 每个类型的同期差额为当前比较窗口金额减上期窗口金额，Math.subtractExact。上期金额>0才给百分比，使用BigDecimal，不先做Long*100。双方无记录显示“暂无可比较记录”；上期无记录但本期有记录显示“对比期无此类型记录”，不报虚构100%增长。同期金额不同于完整总览时由范围文本明确区分。
7. 记录笔数、平均每笔（整数分HALF_UP后Money.format）、较大单笔前3笔随类型切换；同额按日期降序、再ID升序。标题和说明用“记录”而非消费次数；Top3不是异常检测。点击任一记录进入原详情。
8. 分类钻取限定完整所选月/年+类型+trim精确名称，按date降序、id升序。不改变主明细query/kind/月选择；返回保留统计模式、期间、类型及滚动位置。详情取消编辑回详情；保存/删除成功从统计来源回原钻取或统计；修改日期/类型/分类后不符合范围的条目立即移出，删除最后一条显示分类空态，不重建旧记录。
9. 统计计算和钻取使用不可变账目快照，不保存全账本到Bundle。所选期间、类型、钻取键、详情来源及滚动为少量saveable状态。重建/换页后不串用明细搜索。
10. 提供简短“如何看这份统计”：先看同期变化、再看分类金额/笔数、最后打开较大记录结合备注回顾。该说明是用户操作指引，不生成价值评判。

## 视觉安排

沿用S1颜色：light primary176B5B/backgroundF5F7FB/text17212F/errorB33C38；dark primary8DD6C3/background101820/textE7EEF5/errorFFB4AB。系统sans，金额28–32sp、正文16sp，必要时换为纵向。

页面自上而下：月/年与期间控件 → 三项完整总览 → 收支类型切换 → 同期对比（明确范围）→ 笔数/平均每笔 → 分类排行 → 较大3笔 → 年度双柱与12行数值（年度模式）→ 阅读口径。以留白、标题和细分隔组织信息，避免重复大卡片；正负金额及收入/支出文字与颜色同时呈现。长分类换行，大字体纵排，无横向操作依赖。

## Task 1: 纯统计与比较口径

**Files:** 新增app/src/main/java/com/xiaoliumiaomiao/anjizhang/domain/StatisticsPeriod.kt、CategoryStatistics.kt、AnnualSummary.kt、PeriodAnalysis.kt、StatisticsNumbers.kt；新增对应app/src/test/java/com/xiaoliumiaomiao/anjizhang/StatisticsPeriodTest.kt、PeriodAnalysisTest.kt、StatisticsNumbersTest.kt。现有文件保持。
**Interfaces:** 以下接口是Task2消费合同，可在报告补充内部类型，但不能改名或改变语义而不通知ROOT。

~~~kotlin
data class StatisticsPeriod(val year: Int, val month: Int? = null) {
    val startDate: LocalDate
    val endDate: LocalDate
    fun contains(date: LocalDate): Boolean
    companion object {
        fun monthly(month: YearMonth): StatisticsPeriod
        fun annual(year: Int): StatisticsPeriod
    }
}
data class CategoryStatistics(val category: String, val amountCents: Long, val entryCount: Int)
data class MonthTotals(val month: YearMonth, val incomeCents: Long, val expenseCents: Long, val entryCount: Int) {
    val balanceCents: Long
}
data class AnnualSummary(val year: Int, val months: List<MonthTotals>) {
    val incomeCents: Long
    val expenseCents: Long
    val balanceCents: Long
    companion object { fun calculate(entries: List<Entry>, year: Int): AnnualSummary }
}
data class TypeAnalysis(val totalCents: Long, val entryCount: Int,
    val categories: List<CategoryStatistics>, val largestEntries: List<Entry>)
data class TypeComparison(val currentCents: Long, val previousCents: Long,
    val currentCount: Int, val previousCount: Int) { val deltaCents: Long }
data class PeriodComparison(val currentStart: LocalDate, val currentEnd: LocalDate,
    val previousStart: LocalDate, val previousEnd: LocalDate,
    val income: TypeComparison, val expense: TypeComparison)
data class PeriodAnalysis(val period: StatisticsPeriod, val income: TypeAnalysis,
    val expense: TypeAnalysis, val comparison: PeriodComparison?,
    val annual: AnnualSummary?, val hasFutureEntries: Boolean) {
    val balanceCents: Long
    companion object {
        fun calculate(entries: List<Entry>, period: StatisticsPeriod, today: LocalDate): PeriodAnalysis
        fun drillDown(entries: List<Entry>, period: StatisticsPeriod,
            kind: EntryKind, category: String): List<Entry>
    }
}
object StatisticsNumbers {
    fun percentage(numerator: Long, denominator: Long): String? // 1位小数，如37.0%；denominator<=0返回null
    fun averageCents(totalCents: Long, count: Int): Long? // count==0返回null；HALF_UP
}
~~~

构造StatisticsPeriod校验year1900..9999/month1..12，使用LocalDate/YearMonth求闭区间，不用加一年上限法。金额聚合Math.addExact，结余Math.subtractExact；LedgerRules.validate用于参与分析/比较/钻取的账目，原输入不修改。类别排序compareByDescending(amount).thenBy(category)。年度按单次遍历累加12桶，不能12次调用MonthlySummary扫描全账本。Top3同额稳定。百分比用BigDecimal.valueOf(numerator).multiply(BigDecimal(100)).divide(BigDecimal.valueOf(denominator),1,HALF_UP).toPlainString()+"%"；不让Double参与金额/均值。

- [ ] 写实际行为测试，再加最小可编译未实现体，运行取得断言RED（不以缺类/编译错误作RED）。至少以下独立样本：
~~~kotlin
val p = StatisticsPeriod.monthly(YearMonth.of(2026, 10))
val rows = listOf(
    Entry("00000000-0000-0000-0000-000000000001", EntryKind.INCOME, 10000, "工资", LocalDate.of(2026,10,1), ""),
    Entry("00000000-0000-0000-0000-000000000002", EntryKind.EXPENSE, 1234, " 餐饮 ", LocalDate.of(2026,10,2), ""),
    Entry("00000000-0000-0000-0000-000000000003", EntryKind.EXPENSE, 1, "交通", LocalDate.of(2026,10,2), ""),
    Entry("00000000-0000-0000-0000-000000000004", EntryKind.EXPENSE, 100, "餐饮外卖", LocalDate.of(2026,10,3), ""),
    Entry("00000000-0000-0000-0000-000000000005", EntryKind.EXPENSE, 2000, "餐饮", LocalDate.of(2026,9,30), ""))
val result = PeriodAnalysis.calculate(rows,p,LocalDate.of(2026,11,1))
assertEquals(1335L,result.expense.totalCents)
assertEquals(8665L,result.balanceCents)
assertEquals(listOf(rows[1]), PeriodAnalysis.drillDown(rows,p,EntryKind.EXPENSE,"餐饮"))
val annual=AnnualSummary.calculate(rows,2026)
assertEquals(12,annual.months.size)
assertEquals(3335L,annual.expenseCents)
assertEquals(6665L,annual.balanceCents)
~~~
- [ ] 独立断言收入与支出分类各自之和、大小写/特殊字符/Unicode首尾空白精确归类、同额分类稳定、Top3稳定、输入字段不变、年边界/闰年/12个零月份、空类型/零比较基数、负差额/大Long百分比不溢出、1分钱平均舍入。
- [ ] 时间比较固定today：2026-03-31比较3月1..28与2月1..28，3月29..31只计完整总览；2024年闰年当前年按相同前k天；历史完整月/年；未来期间和1900边界无comparison；1900年2月可比1月；9999年完整表示。验证未来账目提示及不进入截至窗口。
- [ ] 运行PowerShell：& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest')，保存RED/GREEN日志/XML到test-artifacts/sdd-v1-s2/task1-{red,green}；旧81逐名保留。
- [ ] 以1k/10k/50k合成账目测分析/年汇总/钻取，记录本机纯逻辑时间和数据规模，不冒充Android帧率/手机500ms目标。若不合理重复扫描，修正后再测。
- [ ] git diff --check，只提交本任务文件；完整报告test-artifacts/sdd-v1-s2/task1-report.md，独立Spec/Quality Approved后进入Task2。

## Task 2: 统计页面、钻取与返回路径

**Files:** 新增ui/StatisticsPage.kt、LedgerCharts.kt、CategoryDrillPage.kt（可新建小型StatisticsState.kt）；从ui/LedgerPages.kt仅移走旧StatisticsPage，必要将EntryRow开放internal复用；修改ui/LedgerScreen.kt及app/build.gradle.kts；新增androidTest/.../LedgerStatisticsTest.kt与必要测试支持；最少适配旧Smoke版本文本、升级fixture/script与工作流。
**Consumes:** Task1全部接口及完整state.entries。
**Produces:** 月年统计/12月图和文字/分类精确钻取/Top3详情；可保存小型UI状态；0.1.5/code7。

- [ ] 先只写新设备行为用例，沿用既有临时CI guard、真实点击/IME就绪与原10秒预算，旧35不删除/改名/弱化。提交test-only并push新功能分支取得真实设备RED后才能修改生产统计UI；等待CI期间可审查测试，不写最终实现。至少覆盖：月分类收入/支出及明细筛选独立；年度12月/总额/1900/9999导航；精确trim钻取不混餐饮外卖/另类型/另期间；钻取旋转和返回原query/scroll；来源详情取消及编辑移出/删除末项；同期范围与无基数文案/Top3；font1/1.3/2统计和图表文字/操作可达；浅深/空态。原日志及初次失败图保存。
- [ ] 主统计保持stats-list测试tag，月模式标题“月统计”和“支出分类”；新tag stats-mode-month/year、stats-kind-income/expense、stats-year-select/prev/next、stats-comparison、stats-metrics、stats-annual-chart、stats-month-1..12、stats-drill-list、stats-drill-back、stats-big-entry-ID及明确category行tag。旧正常月份/category金额文字断言保持。
- [ ] 新统计派生在Dispatchers.Default执行，使用produceState/LaunchedEffect取消旧任务；结果绑定entries对象快照、period、today，渲染前检查仍对应本次请求。等待时不展示上一期间数值，错误用固定无账目内容文案和重试读取；CancellationException必须重抛。日期在页面访问/Activity恢复时更新，不要求后台常驻午夜计时。图形float只从已计算整数金额求比例。
- [ ] monthText继续承接已验收月切换上下文，年度保存独立年份，模式/类型saveable；分类钻取键保存完整period/kind/category，不改query或kindName。statsScroll/drillScroll独立保留，改统计期间时可回顶部，返回/旋转不强制回顶部。
- [ ] LedgerScreen使用detailOrigin区分ledger/stats/drill。现有结果消费仅增加来源分流，busy/current pending guard、操作ID、新VM恢复和草稿合同照旧。统计来源保存/删除回原来源，明细来源回明细；编辑取消仍回详情。分类钻取Back及顶部返回回统计，详情Back回来源。钻取/详情隐藏全局新增按钮，避免重入；缺失/读取中与真正空分类分开。Top3进入详情再返回保持统计上下文。
- [ ] 采用既有Material3色板和FlowRow/纵向回退布局，Canvas只画12月双柱且附可访问文字清单；所有金额文字来自同一个analysis。分类显示金额、占比、笔数，可整行点击；Top3显示日期/分类/金额/备注摘要。提供具体范围和“仅根据已记录账目，无记录不代表无消费”。
- [ ] app版本0.1.5/code7；原旧版本断言只更新版本值。固定升级基线为本次已验收c476eac220ba3f062200365671a674e5decfd06d/code6，新code7；workflow/verify-upgrade.sh/LedgerUpgradeTest三个位置一致。保留seed→install-r→verify之间不清库、不卸载、不重新connected的链。基线文件夹baseline-v014；CI仅临时QEMU/ci-ledger。
- [ ] 完整本机4任务：& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')。记录实际执行与UP-TO-DATE。push后真实设备CI，全部原35+新增场景和升级全部通过；原生截图覆盖月/年/收入/钻取/比较无基数/三字号/深色。失败按现场修复，不靠删例或延长等待。
- [ ] 文件范围内小步提交，task2-report.md记录完整RED/GREEN、冻结SHA、测试名称/结果和限制，独立Spec/Quality审查通过后技术交付。

## Task 3: 整体审查、测试包与交接

**Files:** README.md、docs/HANDOFF.md、VALIDATION_V015.md、REVIEW_V015.md、PHONE_ACCEPTANCE_V015.md、DEVELOPMENT.md、REUSE_V015.md；新output产物独立文件名；忽略test-artifacts/sdd-v1-s2。
- [ ] ROOT实读最终SHA的JVM/UI XML、原始runner/升级全部字段和签名、lint及新原图。原81/35逐名保留，新增各场景不是只编译；图中文字/分类/比较与合成数据相等。完整分支最高能力独审，源码/运行/视觉/实物分别有实际结论。
- [ ] 从clean精确受测HEAD构建并原生审计applicationId、0.1.5/code7/min26/target35、公开旧本机debug证书、原权限/自动备份排除和完整三许可证；APK bytes/hash绑定源码和构建日志。不暴露/读取私钥。
- [ ] 新文件an-jizhang-0.1.5-debug.apk、SHA256SUMS-v0.1.5.txt及v0.1.5-screenshots复制两output，每份hash读回；107原件及16既有安全/数据合同保持。不覆盖旧校验。
- [ ] 准备新19项左右手机清单且未勾，明确先导出备份再同签名正常覆盖安装；至少两个有记录月核对总额/分类/同期范围/年12月/钻取修改返回/字体主题，真实手机结果另记。
- [ ] 更新新private draft PR描述/附到任务，文档commit/push后核HEAD/localremotePR一致、clean、private/draft、主目录原件保持。S2作者验收前不合S2；S3另行确认。新技术FullDelivery实际审查后再写回结论。

## 审查准备与完成定义

计划没有与新用户授权冲突的范围：S1已由作者通过；S2扩充仅利用原账目派生，储蓄率/预算等仍未纳入。复用审查见REUSE_V015.md。本机基线build SUCCESSFUL 23s/28 UP-TO-DATE，读取81项原XML全0；不称重跑81。最新4c452e1远端完整CI37305151507两job均success，旧受测3bf完整原证据保留。所有新增检查与作者反馈分别记录。

