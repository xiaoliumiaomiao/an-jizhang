# 安记账 V1.0 S1 补齐实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 在现有0.1.3修复版上补齐最新V1.0的S1，交付可覆盖安装的0.1.4/code6测试包。

**Architecture:** 保持单Activity、现有ViewModel和SQLite；界面保存少量原始字段及ID，编辑草稿独立于BackupCodec。月选择、最近分类、放弃草稿和系统浅深主题使用已有Compose组件。

**Tech Stack:** Kotlin 2.1.20、Compose BOM 2025.04.01、Material3、JUnit/Robolectric、Compose UI Test；无新增依赖。

## Global Constraints

- Android 8.0 / API 26 及以上；默认 CNY 人民币；单账本。
- 首版本地存储，不添加 INTERNET 权限、广告、遥测、第三方分析、云同步。
- 金额用整数分，禁止浮点数参与账目计算；校验输入、日期、金额边界。
- 单笔金额 0.01 至 999999999.99 元；分类修剪后 1 至 20 字符；备注最多 200 字符；记录年份 1900 至 9999。
- 保持schema1、format1、已有UUID、操作ID匹配和保存失败草稿。按作者最新反馈改为点击金额后按需从底部弹出的应用键盘，备注使用系统IME。
- 本轮只补齐S1；S2及分类库/预算/format2均等作者相应阶段确认。手机验收不预勾；不合main、不正式发布。
- 主目录现有方案、HANDOFF和旧APK/截图/校验文件原字节保留；使用现有隔离分支feat/ledger-ui-v0.1.3。

## 当前基线和复用决定

起点2257c68，应用0.1.3/code5。主目录main为ff8479f（已验收0.1.2）。本机基线任务成功，60 JVM XML失败0；最新方案文件复制到本工作区归档，主目录原文件不编辑。

本轮已实读[Android状态保存](https://developer.android.com/develop/ui/compose/state-saving)、[日期选择器](https://developer.android.com/develop/ui/compose/components/datepickers)、[Material3主题](https://developer.android.com/develop/ui/compose/designsystems/material3)官方文档。仅使用固定的现有依赖API，不复制示例代码、不下载图标、不升级依赖。保留原创Canvas导航图标，第三方许可不变。

视觉采用最新方案深青主色：primary #176B5B、浅色background #F5F7FB、surface #FFFFFF、正文 #17212F、错误 #B33C38；深色surface #17212F、background #101820、primary #8DD6C3、正文 #E7EEF5、错误 #FFB4AB。系统中文sans，不远程字体。金额28–32sp，正文16sp、辅助14sp。账目行与数字右对齐；字体放大时换行/滚动。记一笔放在导航上方右侧的保留区域，Scaffold将其真实高度纳入内容padding，避免遮挡。

### Task 1: 编辑草稿与最近分类的独立状态规则

**Files:** 新增ui/EntryEditorState.kt、ui/RecentCategories.kt及对应JVM测试。测试和生产位置均为app/src/{test,main}/java/com/xiaoliumiaomiao/anjizhang/。

**Interfaces:**
- `data class EntryEditorState(val id:String, val kind:EntryKind, val amount:String, val category:String, val date:String, val note:String)`，原始输入字符串不会因旋转而重新校验或修剪。
- `EntryEditorState.from(entry:Entry?, today:LocalDate)`：新增为EXPENSE、空金额、空分类、today、空备注且稳定UUID；编辑保留全部原始字段。
- `hasChangesFrom(initial:EntryEditorState):Boolean`：仅比较kind/amount/category/date/note五项可编辑内容，ID不是可编辑字段；不把折叠状态、错误或光标当改动。
- `RecentCategories.forKind(entries:List<Entry>, kind:EntryKind):List<String>`：只选指定类型，date降序、同日id升序，trim名称、去重后最多6个；不修改Entry。默认建议支出餐饮/交通/购物/住房/其他、收入工资/奖金/其他由UI继续提供。

- [x] 先写行为测试并取得实际RED，再实现GREEN。必须覆盖新增空分类、编辑空白分类/备注和稳定ID、未改动/单字段改动、同日ID稳定排序、跨类型隔离、trim去重、最多6个与输入不变。未定义API编译报错不当行为RED；必要时测试先写，再加最小可编译未实现体，使断言真正失败。
- [x] 本机命令：`& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest')`；保留RED/GREEN日志与XML，不删旧测试。
- [x] `git diff --check`后只提交本任务文件；报告写test-artifacts/sdd-v1-s1/task1-report.md。独立规格/质量审查通过后才进入Task2。

### Task 2: S1界面与状态集成

**Files:** 修改ui/EntryEditor.kt、ui/LedgerScreen.kt、ui/LedgerPages.kt、ui/Theme.kt、MainActivity.kt、app/build.gradle.kts、.github/workflows/android.yml、.github/scripts/verify-upgrade.sh、LedgerUpgradeTest.kt；新增ui/MonthPicker.kt、必要小型页面/状态文件；新增LedgerS1Test.kt，适配原UI测试路径。

**Consumes:** Task1的EntryEditorState/RecentCategories。**Produces:** 保持LedgerScreen原回调入口，保持测试tag editor-form、add-entry、nav-*、ledger-list、stats-list、settings-list、entry-*、entry-detail、amount-key-*；新增month-picker、discard-draft、note-toggle、recent-categories。

- [x] 在旧UI先添加真实行为RED用例：空分类保存拒绝、返回出现放弃确认且继续编辑保留输入/旋转保留、备注折叠与IME、月标题选择到1900-01及9999-12、深色系统切换、三个字号可达且记账按钮不覆盖列表。先仅提交测试并push当前私有draft PR分支，读取CI实际失败再改生产UI；旧19UI断言继续保留。
- [x] 作者2026-10-05反馈优先：成功提示自动消失已通过；页面内联金额键盘未通过。金额键盘初始隐藏，点金额后在底部独立面板弹出，能收起；Back先收金额面板，再次返回才执行dirty检查。备注/日期/分类获得焦点收起金额面板并使用系统IME；切回金额收IME、金额及备注草稿保留。面板与可滚动表单分配实际高度、不遮挡内容，窄屏/font2可到达全部键和收起入口。新真实RED用例覆盖这些弹出/收起/交接行为，原键标签保持。
- [x] 新记录无预选分类；保存空分类显示“请选择或填写分类”；类型切换保留自定义内容，建议网格按类型更新。最近分类按Task1合同显示，每型最多6个。备注初始折叠；编辑非空备注初始展开；点击“添加备注”/“收起备注”不会清空；旋转保留展开和全部输入。
- [x] 新增/编辑取消、Dialog返回、系统Back在IME关闭后进入同一放弃检查；未改动直接回到来源，改动显示“放弃未保存的修改？”、“继续编辑”与“放弃修改”。busy期间不能取消/重复提交。成功只由匹配operationId结束，无放弃提示。确认本身旋转后仍可继续编辑或放弃。
- [x] 对detail/edit/delete使用saveable原始小快照或稳定ID，移除UI中BackupCodec调用；已有新VM恢复、在途操作释放规则保持。不将全账本塞入Bundle。详情返回保持搜索/月份/滚动，编辑仍同ID。
- [x] 月标题变为至少48dp的选择按钮，选择器有年份输入及12月网格、边界校验与取消；确认才改变月份，搜索/类型保持。保留上月、下月、本月，1900–9999范围严格。
- [x] 跟随系统浅深主题，完整配色；保留深青主色与中文系统字体。图标有文字/语义，条目至少48dp；日期组头显示日期与当前可见条数，筛选时明确“统计为整月”。全局记一笔在编辑/详情/删除/导出恢复许可确认时隐藏。初屏真实viewport避开按钮，字体1.0/1.3/2.0均测可达。
- [x] 导出选择位置前告知明文、整本50000条/10MiB和可信位置；设置说明系统提供者可能云盘、建议可信第二位置。导出失败提示新文件可能不完整、保留旧备份，读取/恢复错误继续准确；不得泄露账本内容。为输出流失败添加有效RED/GREEN（若需改VM文件只改错误文案分支）。
- [x] 新包0.1.4/code6，设置版本从BuildConfig或共同常量显示一致；升级fixture/script期望code6，固定旧ff8479f/code3→code6。CI监听增加codex/**，保留feat/**和main。
- [x] 运行`& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')`；再push真实设备CI。适配旧测试中取消确认/新增分类/折叠备注路径，原行为断言、10秒等待、19用例均保留。修复按证据进行，不靠增等待时间。
- [x] task2-report.md记录准确提交、RED/GREEN、限制；独立规格/质量审查后方可技术交付。

### Task 3: 完整技术核验与交付

**Files:** docs/VALIDATION_V014.md、REVIEW_V014.md、PHONE_ACCEPTANCE_V014.md、HANDOFF.md（仅隔离工作区）、README.md；忽略output与test-artifacts。

- [x] ROOT实读本轮JVM/设备XML、lint、升级seed/verify原始runner、安装/启动/签名，不以BUILD成功代替UI与升级。读取实际截图，检查浅深、三字号、放弃确认、最近分类与月选择。
- [x] 本机APK用原debug证书，原applicationId/min26/target35、schema1/format1、权限/备份排除/许可全部核对。APK新名an-jizhang-0.1.4-debug.apk，独立SHA256SUMS-v0.1.4.txt；复制到两output，旧文件逐项hash保持。
- [x] 整分支与最终技术交付独立审查通过，更新私有draft PR #4标题描述与本任务附件。所有手机验收框保持未勾；禁止合main。
- [x] 文档小步commit/push，实核local/remote/PR HEAD一致、private、draft、工作区干净；主目录main和两原文档hash与dirty状态不变。
- [x] 给作者APK、校验/手机表和限制。下一步先导出备份、同签名正常覆盖安装验收。不能卸载/清库/强制降级；S2等待作者S1确认。
