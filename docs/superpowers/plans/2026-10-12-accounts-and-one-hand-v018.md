# 0.1.8：账户、统一记账入口与单手界面

> 执行方式：subagent-driven-development；按任务实施、独立审查、修复并复查。用户已授权实施及测试，未授权提交、推送、合并或发布。2026-10-12。

**目标**：落实作者本轮界面反馈，并让账户具有真实的离线收支关联、转账和手动余额维护能力。

**架构**：沿用 SQLiteOpenHelper、Compose、现有 ViewModel/Mutex 与 Long 整数分；schema1 无损升级 schema2。账目、账户、转账、余额调整及类别组成一次性快照；备份 format2 完整导出，继续接受 format1。转账和余额调整独立于收支记录。主题和惯用手仅存本机 SharedPreferences。

**技术栈**：现有 Android/Kotlin/Compose Material3 1.3.2、min26/target35、固定缓存与 scripts/build.ps1。不得新增依赖、网络权限、市场数据、云同步或预算。

## Global Constraints

- 实际工作区 C:\Users\13765\.codex\worktrees\ledger-v017-recovery\an-jizhang；分支 codex/ledger-accounts-v0.1.8；base HEAD 0cfbb8ec79c882cefc68615152e2ddde73c5b2e5。main 源码与作者暂存区不动。旧 .7 文件已完整封存，新增实现不得覆盖旧交付证据。
- 不提交、不暂存、不推送、不合并、不发布；正常测试只操作独占合成 ci-ledger 模拟器，不操作作者手机。一次只运行一项 Gradle/设备验证。禁止破坏性数据库重建。
- CNY 整数分，单笔收支/转账 1..Money.MAX_CENTS；稳定 UUID。所有恢复先完整严格解析与验证，事务合并，相同 ID 内容相同跳过，冲突整批拒绝。format1 50,000 账目/10MiB 原合同保留。
- 账户内部余额统一为净资产符号：收入加、支出减、转出减、转入加；负债欠款为负，正数为溢缴。汇总资产为所有正余额之和、负债为所有负余额的绝对值之和、净资产为总和，包含已归档账户。界面解释透支/溢缴。截止日期为今天，未来事件到期才进入当前余额。
- 手动余额为“截至今天”的校准：事务内重读当前余额，记录 target-current 的独立调整；补录、编辑、删除历史记录后余额重新计算。恢复使用原 delta，不重新校准。稳定 adjustment UUID 重试不得重复调整。
- 旧账保留原 ID/六字段，新增 accountId=null，显示“未分配账户”；不得虚构旧账户余额。类别名称仍为历史快照，类别重命名不改写旧账。记一笔仅选择/新增类别，设置页管理名称、归档、排序。
- 完整功能合同仍保留：统计/筛选/下钻上下文、草稿/旋转、IME/返回、重复操作防护、危险确认、明文备份说明、严格解析和错误可恢复。用户明确改变的日期输入、最近栏目、导航及设置呈现改用新合同测试，不保留隐藏旧控件迎合测试。
- 导航明细/统计/账户/设置，全局 FAB；日期整行直接开日历，隐藏日期输入模式；类别只显示 chips 和新增，无“最近使用”“快捷选择”标题。两个统计期间区域共用美术组件。
- 主题跟随系统/浅色/深色，惯用手右/左，持久保存。实际 Activity 与 Dialog 的背景/系统栏图标随主题同步。左手切换物理主要操作位置，不把中文、金额、日期或整个布局变成 RTL。FAB 下方避让使用实测高度。
- 0.1.8/code10；作者“其他功能审核通过”独立记录，不推断 API26、TalkBack、真实进程 kill 或本轮新功能已验收。每阶段维护实际主/开发 HANDOFF，并保留历史正文。

## 冻结接口与数据合同

domain/Accounts.kt 定义 AccountKind(ASSET,LIABILITY)、AccountSubtype(CASH,BANK,FUND,CREDIT_CARD,LOAN,OTHER)、AdjustmentReason(INITIAL,CALIBRATION)；Account(id,name,kind,subtype,archived=false,sortOrder=0)，AccountTransfer(id,fromAccountId,toAccountId,amountCents,date,note="")，AccountAdjustment(id,accountId,deltaCents,date,targetBalanceCents,note="",reason=CALIBRATION)，BalanceAdjustmentRequest(id,accountId,targetBalanceCents,date,note="",reason=CALIBRATION)。AccountKind 与 subtype 兼容性由 UI 推荐，不禁止 OTHER；有任何关联后 kind 不可改变。

domain/Categories.kt 定义 Category(id,kind,name,sortOrder=0,archived=false)，name范围1..20，与Entry.category兼容；Account.name为1..30。domain/LedgerSnapshot.kt 定义 LedgerSnapshot(entries=emptyList(),accounts=emptyList(),transfers=emptyList(),adjustments=emptyList(),categories=emptyList())。Entry 最末新增 accountId:String?=null。

domain/AccountBalances.kt 提供 balances(snapshot,asOf:LocalDate):Map<String,Long>、totals(...):AccountTotals(assetCents,debtCents,netCents)、validateSnapshot(snapshot)；精确 BigInteger 中间计算，最终 Long 精确转换，禁止 Long.MIN_VALUE；所有日期前缀与汇总校验，先按日期合并事件避免转账中间状态误报。BalanceMoney.parse(raw):Long? 支持零/负号和整数分，超范围/小数超两位拒绝，原 Money.parse 不变。

LedgerDatabase 保留 listEntries/save/delete/merge(List<Entry>)；新增 snapshot/saveAccount/createAccount/archiveAccount/saveTransfer/deleteTransfer/adjustBalance/deleteAdjustment/saveCategory/reorderCategories/merge(DecodedBackup)。普通写操作也按候选完整快照验证后事务落库。调整返回 AccountAdjustment（重复相同请求返回旧事件）；同 UUID 不同请求拒绝。归档账户只允许原账目继续保有原链接，不允许新记录/新转账使用归档账户。

BackupCodec 保留 encode(List<Entry>)/decode(String):List<Entry> 的 format1 安全适配；旧 encode 拒绝非 null accountId 防止静默丢失。正式新增 encodeSnapshot(snapshot)/decodeBackup(raw):DecodedBackup(snapshot,sourceVersion)。format2 root 严格为 format/version/currency/entries/accounts/transfers/adjustments/categories，各记录字段精确对应上述模型全部字段。每种金融数组最多50,000，accounts最多1,000、categories最多100,000；format1仍10MiB（MAX_BYTES），format2为32MiB（MAX_SNAPSHOT_BYTES），decodeBackup先32MiB门禁，严格version解析后对v1再次10MiB限制，深度8，未知/重复键、非整数字段、重复 UUID、孤儿引用或非法余额拒绝。跨数组 ID 可相同，数组内部不可重复。v1 merge 相同旧六字段跳过并保留本地账户链接，v2 包括 accountId 完整相等才跳过；任一实体冲突整批回滚。类别缺省由默认列表及旧账 distinct(kind,trimmed name) 补全，不改写账目本身。

### 容量与引导目录补充（只读风险审查后锁定）

format2允许32MiB，类别上限100,000，使合法旧format1的50,000 distinct类别及10MiB账目不被新metadata阻断迁移/全量导出；须真实近10MiB、多字节类别边界验证。旧encode/decode/MAX_BYTES10MiB不改变。设置说明两个格式的实际限制。

ImportResult保留imported/skipped为原账目数，增加各实体的imported/skipped计数和catalogAdopted默认false供结果说明。仅format2且本地entries/accounts/transfers/adjustments全部为空、categories按ID全部字段精确等于canonical defaults、无extra时，允许在同一个merge事务采用incoming.categories整体引导目录（incoming空目录也原样，不在read时重补）。此自动默认目录不代表作者数据；只有内容变化才catalogAdopted=true并明确显示“已恢复类别设置”。类别-only任何rename/reorder/archive/custom、零账户或其他实体均不适用；其他同ID冲突完整拒绝。删除引导目录和所有插入同事务，晚冲突/写故障恢复原目录。v1派生目录只按(kind,trimmed name)补缺，不以旧派生ID覆盖或冲突作者已编辑目录。若canonical派生ID已被重命名占用，派生新类别使用新UUID；不触碰原作者行。

账户余额计算在Dispatchers.Default按(snapshot identity,today)缓存，不在每帧主线程计算50k记录；日期在ON_RESUME/页面重入及前台跨午夜更新。编辑器类别大目录用Lazy搜索，不将100k类别铺成FlowRow。

## Task 1: 账户领域模型、余额与校验

**文件**：修改 domain/Entry.kt、LedgerRules.kt；新增 Accounts.kt、Categories.kt、LedgerSnapshot.kt、AccountBalances.kt、BalanceMoney.kt 和对应 JVM AccountBalancesTest/BalanceMoneyTest。其他文件不改。

1. 先写可运行的行为测试；接口尚不存在时先加入最小类型签名，编译错误不算 RED。让未实现余额方法失败，保存实际 RED 输出。
2. 实现冻结接口。余额计算参考：
```kotlin
val amounts = snapshot.accounts.associate { it.id to BigInteger.ZERO }.toMutableMap()
// entries income + / expense -; transfer two legs; adjustments delta
// filter date <= asOf, then exact BigInteger reduction and longValueExact()
```
校验 UUID/Account.name1..30/Category.name1..20/note<=200/date1900..9999/重复ID/引用/类别kind-name唯一、sortOrder>=0；原Entry.category1..20保持。已归档记录是合法历史引用；存储层再检查新增行为限制。
3. 测试资产收支、负债消费/退款、借款与还款/资产转账、零/负余额、未来截止、已归档汇总、超过单笔 cap 的合法调整、精确抵消/溢出/Long.MIN_VALUE拒绝。余额调整原始 target 不是绝对覆盖锚点。
4. focused RED/GREEN 后跑全 JVM；只允许领域新增测试，不弱化既有测试。报告命令、原日志/原 XML、改动文件、自审和关注点。独立审查通过再进入 Task2。

## Task 2: schema2、类别存储与 format2 完整备份

**文件**：data/LedgerDatabase.kt、BackupCodec.kt；必要的新 data/LedgerSnapshotStorage.kt（仅清晰拆分 SQL 读写）、CategoryDefaults.kt；新增 AccountDatabaseTest/AccountBackupTest；现有 LedgerDatabaseTest/BackupCodecTest 仅增加业务测试，不删原断言。

1. 按冻结接口写真实 Robolectric SQLite schema1 创建/插入/关闭/升级/reopen 的 RED，确认 PRAGMA user_version 应2仍1；失败必须因缺功能。
2. onConfigure 开启 foreign key；onUpgrade 仅 1→2：创建四新表，ALTER entries ADD account_id nullable FK。不得重建旧 entries；账户 UPDATE/INSERT 不用 CONFLICT_REPLACE。snapshot 一次读事务一致；全部写入事务候选校验。
3. saveAccount保留零余额新增/编辑；新增createAccount(account,initialRequest:BalanceAdjustmentRequest?=null)复合事务，initialRequest须同account.id、reason INITIAL，先完整候选验证再一次写入账户与初始调整；同账户+同初始请求稳定UUID重复无写返回，同UUID异内容拒绝，插入两实体间故障完整回滚。手动调整按请求 ID 查重后重读余额并计算 delta；同请求重复即返回原事件，不按新余额二次算。删除/历史编辑会重新计算；转账一行实体且原子；归档保留账目与汇总。
4. categories 初始化默认餐饮/交通/购物/居家/娱乐/医疗/教育/其他支出与工资/奖金/理财/其他收入，加上旧账去重；默认 ID 由固定 namespace+kind/name 确定，跨安装一致。add 保存 kind/name 唯一，重命名不改历史 Entry.category，排序要求该kind全部当前非归档ID各一次。归档后编辑原账仍可保留原名称。
5. format2 严格解析先 complete snapshot 验证，再完整事务合并。保留 v1 适配并新增 v1/v2 区别测试、同 ID 本地关联保留测试、所有实体往返、晚冲突整批回滚、孤儿/非法数字/重复键/边界。候选 union 完整验证后先父实体、后子实体插入。
6. 资产/负债间四种账户kind组合的转账（含借入/还款及负债转负债）、负债溢缴、连续/重复校准、未来事件到期、历史修改/删除、归档引用、数量/金额边界和故障无半笔均用真实 DB 测试。focused then全 JVM；全结果交独立审查。

## Task 3: ViewModel 快照与账户/类别操作

**文件**：ui/LedgerViewModel.kt、EntryEditorState.kt、EditorSavedState.kt；新增 AccountUiState.kt（如需要）和 ViewModel/编辑草稿 JVM tests。

1. LedgerUiState 增加 accounts/transfers/adjustments/categories，完整快照由同一次 DB snapshot 读取得到；原 entries/忙碌/错误/反馈/operationId 合同保持。正式 export 改 encodeSnapshot，restore 改 decodeBackup+merge；保留旧文件完整错误分层和mutex/close行为，流读取MAX_SNAPSHOT_BYTES+1；v1独立10MiB、v2为32MiB。旧VM限流测试改验证新总读取上限及v1独立上限，保留限流/无写/不泄露数据断言。
2. 新操作 saveAccount/createAccount/archiveAccount/saveTransfer/deleteTransfer/adjustBalance/deleteAdjustment/saveCategory/reorderCategories 与既有 execute 管线一致，稳定请求 UUID、防并发、成功后一次刷新、刷新失败可重试；错误信息明确“未改动”不得误用在已写入但读回失败的情形。新增 accountId 到 draft/saveable dirty判定与转换末尾，旧 draft Saver 兼容。
3. 实际测试 account写成功/忙碌/失败/读回失败和调整重试幂等、v1导入保留关联/v2导出包含全实体、草稿账户旋转保存/取消/dirty。原23 VM和11draft测试语义保持。必要旧导出版本断言改为新严格schema业务断言并注明用户授权。
4. focused RED/GREEN then全 JVM，报告数据接口供 UI 使用，独立审查。

## Task 4: 本机主题/惯用手与共享期间组件

**文件**：新增 ui/UiPreferences.kt、data/UiPreferencesStore.kt、ui/WindowAppearance.kt、ui/PeriodNavigator.kt；修改 ui/Theme.kt；新增 UiPreferencesStoreTest。暂不改 MainActivity/LedgerScreen。

1. ThemeMode(SYSTEM,LIGHT,DARK)、Handedness(RIGHT,LEFT)、UiPreferences(theme=SYSTEM,hand=RIGHT)，SharedPreferences同步加载，StateFlow发布；写入使用IO上的commit并处理false，成功反馈在落盘后；setTheme/setHand在同一Mutex内基于最新state.copy以防连续设置互相覆盖；无效旧值安全默认。写入立即可读、重建 store 持久；不污染跟随系统判断。
2. LedgerTheme(preferences=UiPreferences(),content) 保持旧 trailing-lambda 调用，CompositionLocal 提供 resolvedDark/hand。WindowAppearance 用当前 Activity1.10.1 enableEdgeToEdge 与 SystemBarStyle.auto detectDarkMode，Compose DialogWindowProvider window同步背景、status/nav图标（全屏editor背景跟随palette，浮层保持透明window背景与原scrim/圆角）（API26以上适用）。实际窗口对比验证在Task7完成，不用纯资源测试冒充。
3. PeriodNavigator(month,onPrevious,onNext,onPick,onToday,modifier) 一处年月显著主按钮+48dp上一/下一 icon+“本月”chip；年度也共用内部PeriodNavigationControls。文字正常中文，font2自然换行不截断；保留原month/year测试tags并合理加语义。
4. store持久和主题解析先行为RED/GREEN；组件编译，无新依赖。独立审查后UI接线。

## Task 5: 统一记一笔编辑器与账户/设置页面

**文件**：ui/EntryEditor.kt（及新 EditorCategoryPicker.kt）、AmountKeypad.kt（仅按hand移动收起主动作，不改数字键/计算/输入合同）；新增 AccountsPage.kt、AccountDialogs.kt、SettingsPages.kt。此任务提供组件，尚不改 root导航/MainActivity/旧月份组件。

1. EntryEditor 扩展可选 accounts/categories/onAddCategory 回调，保留旧调用默认安全适配以能编译。日期整行主 Surface + 日历图标 + 中文日期 +“选择日期”，无 ISO 可编辑框；DatePicker(showModeToggle=false)沿用 UTC/year bounds。回退/IME/草稿/键盘高度48%/系统Insets及discard确认保持。账户选择支持未分配/活动账户，编辑既有已归档引用保留标签；类别默认按设置顺序前12个chips+新增，“全部类别”进入可搜索Lazy列表，选中可见，不出现最近/快捷标题，不允许在此重命名或排序。
2. 编辑器原Dialog保留可用的草稿及独立window合同，通过同主页面颜色/字体/圆角/工具栏建立一致感；全屏Dialog显示真实当前页上下文及清楚返回入口。主要保存/关闭按hand物理位置，数字键盘/日期不反向。
3. AccountsPage(snapshot, callbacks, contentPadding) 显示净资产/资产/负债与现金/银行/基金/信用卡/借款等自定名字；新增/编辑名字与类型、归档/显示归档、账户详情最近账目/转账/调整独立列表。金额显示负债欠款或溢缴、资产负值透支。创建可含初始余额（createAccount同事务写入，INITIAL调整稳定ID）；失败保留输入且能重试。转账选两不同活动账户、正金额、日历日期、备注，保存一次；手动校准今天的余额，负债欠款/溢缴模式清楚，允许零/负资产，解释历史重算；确认删除调整/转账防误触，保持收支不变。UI使用专用稳定请求id和pending反馈，禁止仅用下一帧busy防double click。
4. SettingsPages 根五行：外观与操作/收支类别/备份与恢复/本地隐私/关于。说明置子页；主题3选、hand2选；分类收入/支出、增加、重命名、归档与上移下移，稳定排序；类别名历史快照解释置子页。备份明文/v1 10MiB与v2 32MiB/容量及导入危险确认保留、原许可可访问。
5. 组件只接明确 callbacks，不直接自行开 DB；带48dp语义和testtags。focused编译/领域 tests；真正UI输入、位置、旋转测试由Task7在真实候选APK完成。独立审查。

## Task 6: 四 tab、FAB、主题接线与新规格 UI 回归

**文件**：MainActivity.kt、ui/LedgerScreen.kt、LedgerPages.kt、StatisticsPage.kt、StatisticsState.kt，app/build.gradle.kts、相关 androidTest helpers/tests及新 LedgerAccountsTest/LedgerPreferencesTest。

1. root明细/统计/账户/设置；删除记一笔假tab，放共享ExtendedFloatingActionButton(testTag add-entry)。默认End、左手Start；各主/子页都可用，其他modal/保存时禁用或隐藏，返回保持来源及统计下钻上下文。实测onSizeChanged高度+spacing预留列表尾部，测试实际scroll-to-last内容与FAB bounds不重叠。
2. MainActivity持有偏好store并collect，LedgerTheme同步actualActivity；子Dialog采用同CompositionLocal与真实windowappearance。两个原periodareas换共用组件，旧统计全月/全年/导航/还原状态不改业务。settings新根/子页接线、账户全callbacks与pending反馈接线。
3. versionName0.1.8/versionCode10。日期旧 performTextReplacement 全部改共用 calendar helper，真实点日期且保留闰日/边界/草稿/编辑移动月份断言。最近6测试被用户新合同明确替代，记录移除原因，不保留隐藏输入；不削弱金额/statistics/IME/back/rotation assertions。备份文案测试进入子页、保持风险确认。必要 fixture seed 由测试helper显式saveCategory注册缺少的kind/trimmed名称，或以真实format1合并派生目录；普通DB.save只保存账目，不隐式重建/重播已由作者改名的类别。保持Entry原category字符串，不回退自由文本用户入口。
4. 新 UI测试全主tabFAB、类目录新增/设置排序/无最近、日期首次日历、账户收支/借还/初始与校准0/转账不计收支/归档、左右位置保存/重建持久、manual主题与actualwindow包括Dialog。focused真实 RED/GREEN 先，再完整编译/JVM/lint，独审。

## Task 7: 冻结候选、完整设备与升级验证、交付

**文件**：新增 test-artifacts/ui-v018 固定守卫/manifest/原生runner/原图/原XML、androidTest 新增 LedgerRawUpgradeSeedTest（独立原始schema1种子）与 LedgerSchema2UpgradeTest（新版验证），旧LedgerUpgradeTest仅作为历史替换合同保留并排除常规运行、.github/scripts/verify-upgrade.sh与现有workflow版本门禁（同步0.1.8/code10，保留已有远端.4链；.7没有固定提交，使用本机封存.7/.6/.4 APK各做覆盖链，不虚构远端.7 ref；未实际远端执行CI不得称CI通过）；docs/VALIDATION_V018.md、REVIEW_V018.md、PHONE_ACCEPTANCE_V018.md；两处HANDOFF新前缀、新output/.8APK与SHA256SUMS（不覆盖.7）。

1. freeze所有受测源码与toolchain/测试方法清单，新强制offline JVM/lint/assembleDebug/assembleDebugAndroidTest，确认原报告时间、0失败与无新增lint阻塞；列明已存在SDKXML/deprecated/UseKtx警告。source guard停止写源码直到所有候选门禁完成。
2. 独占合成API35 ci-ledger：真实原生APK与test APK hash/证书检查、单runner唯一方法完整回归；左右/light/dark/font1/1.3/2及noteIME/Back真实PNG；逐张真实原图视觉审查。未跑项明确列出，不声称phone/TalkBack/API26通过。
3. 实际.7→.8同证书adb install -r，seed阶段不能引用新Entry构造/DB API：直接 schema1 SQLite synthetic固定ID六字段fixture回读，并识别.7/code9。verify阶段冷启动新版原ID六字段/accountIdnull/schema2、分月统计/账户建立/关联/转账/校准/format1恢复+format2全量往返。各链setup可在独占合成模拟器创建干净旧版本夹具；seed之后禁止卸载、清数据、降级或重播fixture冒充升级；必要旧.6/.4→.8也独立检查，不以.7链代替旧证据。
4. 若失败保留原始RED，最小修复+covering tests+独立复审，再新冻结并重跑受影响门禁；不就地修改冻结输出/失败证据。独立 Source/Native/RuntimeUpgrade/Visual 和整个交付审查通过后复制两处新交付，以所有实际hash交叉验证。
5. HANDOFF写实际date、main/dev、branch/HEAD、version、dirty/index状态、受测源码manifest/APK、每种测试日期/结果/原件、已完成/待验收，原正文byte-preserve且写前sha并发检查。作者旧“其他功能通过”反馈单独引用；本轮手机验收只列新改动及数据升级，不要求重批旧不相关项。

## 当前进度

2026-10-12 05:53:10 +08:00：Task1–5 Approved；Task6 root已接线，r2强制全量283JVM/0F0E0S/24freshXML、lint0E4UseKtxW；118源候选BF57C332冻结。实际API35 focused16/11F0E0S（Accounts10/8F、Prefs3/2F、Recovery3/1F），完整FAILED原件及source/path/APK/method guards匹配保留，控件/窗口定位与日历断言待最小诊断复验，非GREEN。设备/Gradle已STOP交还solewriter；基础导航/相反系统Activity主题/左右手大字体tail/queuedDelete/unknownRESTORE通过。Task6正式代码门禁及Task7全UI/原图/三旧-r链/.8debugAPK交付未完成。main作者/index未动，HANDOFF双方当前核查前缀已更新，未暂存/提交/推送/合并/发布。

### 已提交结果与 UI 重读恢复（Task3 预审补充）

保留原 completedOperationId/failedOperationId/readError 合同：落库后读回失败仍保留旧五类快照与旧 completed，发布本次failed并标真正readError。同时新增按operationId关联、可观察且可消费的committed结果（可含原AccountAdjustment/ImportResult）；DB返回后留存，即使刷新失败亦不丢失，无初始调整返回null也明确已提交。成功load不得无声抹掉仍待消费的结果。拒绝/事务失败和已提交后读取失败用不同文案，不能把后者说成未改动/未导入；不回显异常原始敏感内容。

账户/初始余额/校准草稿保存稳定业务UUID与完整请求date，VM每次尝试operationId可新建；同请求跨日或归档重试原样传DB，不用旧state预判挡掉DB幂等查询、不重算delta。首次跨午夜且尚未提交的旧日期请求应拒绝并保输入；重新发起新请求才用新业务身份。UI在已提交但重读失败时提供“重试读取”，待新完整快照与原结果匹配后完成窗口状态；重读前避免把同UUID编辑成新内容导致冲突或再创建新账户，不凭下一帧busy防double click。

两套Saver及最终Entry转换均保留末尾accountId，旧six-field恢复默认null，raw输入/原六字段位置不变。Task3只写brief授权VM/state/tests；EntryEditor最终转换与账户dialog/root的结果消费由Task5/6接线。最小实现建议与原23VM/11draft逐项合同在 research/vm-contract-preflight.md；该报告只读预审不代表实现已批准。
### Task2 实际回归后的测试夹具边界例外

首full202/2F已保留。两旧LedgerViewModelTest在故意DROP entries后直接调用LedgerDatabase.onCreate重建；schema2另外四表仍在，整库onCreate会报accounts已存在。Task2可最小调整这两处fixture为只raw重建entries（schema2末尾accountId/FK保持），精简共用helper可用；原方法名与所有业务断言原样、VM生产代码不动。禁止修改production onCreate为容错/隐式重播seed来适配夹具。该测试文件纳入Task2实际差异、最终source manifest和报告；原202/2F与focusedGREEN字节保留，EOF空白与fixture修正后全JVM再实际验证。此例外由既有回归暴露的具体原因授权，不扩大其他测试范围。

### 偏好落盘与实际窗口补充（Task4只读预检）

读取 research/preferences-preflight.md 的最小路线与现有官方缓存源码证据。生产使用 applicationContext 的进程唯一 UiPreferencesStore（Activity重建复用同一实例/Mutex）；初始加载仅一次，各键未知值/错误类型分别回默认。两个setter在同Mutex内基于最后确认state.copy，以同Editor写两个键。commit=false可能已改SharedPreferences内存：不重新读取其candidate当成功、不publish、不显示成功，明确失败可重试；下一次双键写入只用最后确认state，dirty时相等值也不能短路。可做同锁内一次旧值补偿，补偿失败只记录未确认状态，不声称磁盘完全回滚，不增加无限重试或通用配置框架。真实成功值重新构造测试store读取，生产不允许旋转时读失败memory生成新store。

同锁内用NonCancellable+IO覆盖短小commit及结果state publish；等待Mutex期间取消无写，commit中取消仍让磁盘与state完成一致，恢复原dispatcher可能抛CancellationException，调用方不将其转换为“保存失败”。用可控制barrier/latch测试：先污染prefs内存再false、随后setHand不携失败theme、dirty相等值重试、两个setter不覆盖、锁等待取消及commit中取消。不要以重建同进程prefs读回独自证明磁盘成功。

WindowAppearance按解析后的手动/system主题更新每个实际Window。Activity enableEdgeToEdge只接受ComponentActivity；Dialog helper必须在各Dialog内容内部找最近DialogWindowProvider，不能更新Activity假装更新Dialog。浮层保持透明window背景、默认decorFits/dim/scrim/圆角；全屏editor用palette背景且保留既有Insets。API26导航栏不能只改深色图标却留旧暗底，需适合当前mode底色；API29+ contrast属性才调用。手动DARK/系统LIGHT与手动LIGHT/系统DARK另测，不能用资源night等于用户选择替代真实Window。前后期箭头不镜像，hand只移动物理主要操作；原picker/统计state仍由调用方持有，共用组件不私存期间、不捕获跨午夜旧today。Task4的JVM/编译不代替Task6/7原生UI证据。
### 历史类别、同步窗口 owner 与余额草稿补充（Task5只读预检）

读取 research/components-preflight.md；它只读研究，最终状态/result/Saver API以Task3完成报告为准。EntryEditor最终Entry构造明确携带第七accountId，两套Saver之外不能再次丢关联。编辑旧账未显式重选类别则原Entry.category字符串原样保留（含合法原空白），不以目录改名反查替换、不blanket trim；显式选目录才赋该Category.name。新增用户输入提交前trim。旧分类被归档/改名或不在当前活动目录仍明确可见并可保留；空作者目录不在UI/load中重播默认。设置排序提交该kind全部活动IDs，不能只提交搜索可见子集。

根同步持有单一modal owner/session（可直接root持有dialog spec，避免两份visible状态）；打开任何editor/account/category/危险确认时先同步acquire，再显示。门禁分为openBlocked（busy/pending/externalModal/已有owner）与submitBlocked(owner)（busy/pending/externalModal/非本owner）；当前editor的保存不被自己的editing禁用。任何submit callback调用前同步发送latch，不能等下一帧busy。日期/分类/放弃确认是父窗口内部子窗口，阻止父submit但关闭时只恢复父owner，不能放开globalFAB。

账户新建含INITIAL保存两个稳定UUID，同createAccount事务；校准/转账保存业务UUID和完整date/payload，与尝试operationId分离。saveTransfer/saveAccount是upsert，稳定UUID本身不防已保存内容覆写；匹配committed且readError时冻结整份输入及所有模式/日期/新增入口，只允许重试读取，完整快照与本窗口结果核对后关闭并consume本receipt。未提交失败才解锁保输入；其他窗口旧结果不误关本窗口。无INITIAL返回null也明确已提交。重建时不把未知旧结果当未提交，不清业务UUID/frozenpayload；Activity旋转与真实kill验证分别记录。

账户详情账目返回保有detailOrigin=account及accountDetailId；global新记账成功/取消保留底下账户详情/设置子页/统计drill来源。左右手移动保存/关闭/键盘收起及主要操作物理位置，数字键顺序与中文/日期不RTL。负债初始/校准输入采用欠款/溢缴模式+非负magnitude（欠款负、溢缴正、零均0），拒绝负号反转；资产可输入零或负余额并显示透支。余额解析BalanceMoney范围与原Money单笔收支范围分开，不能复用正收支错误文案；说明校准截至今天、不计收支，历史补录/编辑/删除后重算。DatePicker显式showModeToggle=false、UTC双向转换、yearRange1900..9999，取消不改date，未选date确认禁用。
### Task5 最小纯输入转换测试补充

Root另外授权新增 app/src/test/java/com/xiaoliumiaomiao/anjizhang/AccountInputTest.kt，覆盖实际UI使用的最小纯转换：负债非负magnitude到欠款/溢缴符号、零/Long范围/非法负号，以及原始类别和第七accountId保存（若对应helper存在）。纯函数保留在既已授权UI文件，不改Task3领域/Saver数据合同，不建通用表单框架。必要的可编译stub行为RED/focusedGREEN/full JVM保留真实证据；这些纯测试不替代Task6/7真实日历、左右按钮、草稿旋转或实际Window验证。
### Task6 最小同步会话门禁测试补充

Root另外授权新增 app/src/test/java/com/xiaoliumiaomiao/anjizhang/UiOperationSessionTest.kt（仅在真实Root使用最小pure会话/门禁helper时），helper保留于已授权LedgerScreen.kt，不引入通用导航/工作流框架。行为RED/GREEN验证同一帧double open/submit同步门禁、父owner与child关闭/自身保存、attempt匹配与committed-readError冻结/ready消费、未知重建状态保留UUID/payload，避免Compose测试两次performClick间自动idle掩盖重复调用。真正UI仍须Task6/7原生断言。新seed/schema2两类由Task7写，Task6只适配常规helper/test及实际focusedRED/GREEN。新版实际升级执行路径为ui-v018/verify-local-upgrade-v018.ps1，消费合同research/upgrade-driver-preparation.md；工具Parser/preflight并非运行通过。
### Task5/6 显式重新发起跨日余额请求

账户新增 INITIAL 与手动 CALIBRATION 的首次旧日期请求被拒绝后，仅在 root 的本窗口 failedAttempt 匹配、没有匹配 committed receipt、无 busy/readError/unknownFrozen，且完整原请求日期不同于当前今天时，才提供“重新发起”按钮。same-date 普通输入/事务失败原样可重试，不要求重新发起。AccountEditorDialog/BalanceEditorDialog 以 canRestartRequest=false、onRequestRestart=Unit callback 为 trailing 输入，由 root 控制资格并同步 live gate 后操作；父 modal/session 保留，raw 名称/金额/备注和负债欠款/溢缴模式不变，显式更新业务 UUID（新账户及 INITIAL 两个ID；校准一个ID）和点击当下 fresh today，清旧 attempt/error 后形成新尝试。不得使用过时捕获的 today，不得自动换业务ID重试；已提交或未知结果永远禁止该入口。此控制并非 Task5 原生已验证，Task6 接线及 Task7 运行覆盖。
### Task6 读取重试的独立同步门禁

onRetryRead故意不使用包含frozen/pending的普通submit门禁，否则唯一恢复入口会被自身冻结拒绝。root专用read入口必须同步latch后调用VM.load，确认本session或无modal全局页面可读、latest VM busy/readSending均false；允许本owner frozen，保留业务ID/fullpayload/pending attempt，拒绝同帧重复读取。latch在真实读取完成或失败之后释放，不能靠延时或看旧busy=false推测。现VM.load为Unit异步，若实现中缺少可无竞态观察的完成信号，先向root报告具体接口需要，root可窄授权awaitable read completion +对应行为测试；不得静默扩展领域/写事务合同。组件帧级busy只负责呈现，root callback内同步门禁才是防重复保证。旋转/unknown结果读取与普通页面读错恢复均覆盖；读失败保留输入和重试入口。
### Task6 窄授权读取完成接口与测试范围

2026-10-12 root确认具体Unit异步load无法无竞态释放专用read latch，授权LedgerViewModel.kt仅新增 loadAwaitable(): kotlinx.coroutines.Job，private work返回实际Job，旧load():Unit明确保留，export等既有public Unit不随推断意外改为Job；所有mutate String、commit/receipt、Mutex/DB.close、领域/事务/备份合同保持。Root onRetryRead先同步latch，再等待精确Job完成/失败后仅匹配本读token/owner释放；取消正确finally和传播，不当保存失败，无delay/旧busy猜测。允许在既有AccountViewModelTest.kt或LedgerViewModelTest.kt新增真实completion/failure/cancellation覆盖，原251+9方法不削弱，实际可编译行为RED→focusedGREEN/完整最终验证且独立冻结VMdelta及新测试字节/原日志/XML。公开新增签名及最终选择testfile写入Task6报告，正式Task6差异审查覆盖此窄crosscut；没有扩大写事务/领域范围。
### Task6 2026-10-12 04:52 窄接线与工具门禁更新

root新增窄授权CategoryDrillPage.kt接收实测contentPadding/FAB尾部避让，不改查询/筛选/下钻；MonthPicker.kt实际Dialog内部SyncDialogWindowAppearance(FLOATING)及所需controlled gate，不改日期选择合同。VM允许 exportAwaitable(open):Job 与旧export(open):Unit显式兼容，沿用原导出内容/容量/写流/反馈，精确Job completion按本owner/token解除SAF latch。对应AccountViewModelTest真实completion/failure/cancel TDD，原methods保留；均在实际nativeRED后实施，记录差异与复审。

工具正式r3 FULL读取Approved C0I0M0，research/validation-tools-review-r3.md SHA5AF3521CE2B40D273E9C6667DA56B5FC83E9DF3E35B5006BAFC1FF49374C2BC3；原r1三I和r2I1/M1全关闭。commonBB251E4.../runner717E8A0.../upgrade0C95586...固定，helper准备批准不等于native已过。Task6 native-red-r1 frozen117source，manifest4B1FE8F9436E90129DEBA61C53C24FB83A0822094D181E0AB4F8C0463EA08E3F，app1D5DFD5C.../testFF1EDCA2...，实际forceassemble66tasks/37s，仅机械版本与三新testsource。root即将顺序运行Accounts2及Preferences2两独立phase，未接root前实际行为RED待观察，产品writer此阶段停止写/Gradle。
### Task6 校准金额的匹配缓存初值窄接线

Root授权AccountDialogs.kt仅BalanceEditorDialog新增 trailing currentBalanceCents:Long?=null。首次rememberSaveable initializer从matching(snapshotRevision,today) cache金额生成无分组/货币符号/科学计数的原始十进制输入，禁止Double；LIAB负=欠款非负magnitude，正=溢缴，零默认欠款，ASSET保留原符号。Restored/unknown/pending/已有编辑raw、模式和业务UUID/date均不被cache或recomposition覆写。Root只在该账户匹配缓存ready后acquire/open校准，避免0/stale闪；null仅旧临时调用空输入适配，最终root传真实值。其他Task5组件/parse/Saver/领域/存储不动；必要纯initializer行为可补既有AccountInputTest，实际校准prefill/sign/零/历史重算/旋转仍需native。最终API、scope delta、测试结果归Task6 report及独审。