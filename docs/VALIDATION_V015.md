# 0.1.5／V1.0 S2 验证记录

日期：2026-10-07（Asia/Shanghai）。7be候选运行、升级、视觉和本机原生材料已有实际批准；文档提交cfa0594已完成Git同步并获最终技术FullDelivery Approved C0/I0/M1（仅既有工具提示）。作者0.1.4验收与S2技术验证／手机验收分别记录。

## 冻结候选身份与当前门禁

冻结源码`7be948c40c96e7fe0d75448e284ff52ee8e45c03`／tree `5008b8fd219621f4cad7f2a95c598eb8a92eb3df`，0.1.5/code7。RV-M1局部纵排与三字号单行断言增量Source Spec／Quality独审Approved C0/I0/M0，接续Whole Source C0/I0/M2；candidateNative独审Approved C0/I0/M0。新 [CI37626498602 attempt1](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37626498602) 两job success：111 JVM／55 UI均0F0E0S，旧81／35逐名missing0，新增30／20；lint0E9W。seed／verify各1通过、code6→7同CI临时证书覆盖；新60张原PNG已由ROOT及独立审查者逐件看，Runtime／Upgrade／Visual Approved C0/I0/M0、RV-M1关闭。作者手机20项未勾。

| 项目 | 当前实际证据与余下门禁 |
|---|---|
| 已验收S1基线／S2分支 | `c476eac220ba3f062200365671a674e5decfd06d`／0.1.4/code6；`codex/ledger-statistics-v0.1.5` |
| 冻结候选SHA／tree | `7be948c40c96e7fe0d75448e284ff52ee8e45c03`／`5008b8fd219621f4cad7f2a95c598eb8a92eb3df` |
| 新CI build | CI37626498602 attempt1；111 JVM／0F0E0S，原81 missing0＋新30；lint0E9W；原件`ci-s2-37626498602-attempt1/android-build/` |
| device55 UI／seed／verify | 7be新run原XML 55／0F0E0S、旧35 missing0；seed／verify各唯一OK(1 test)、completion=-1、exit0；code6→7原安装／启动成功、schema1三UUID及字段／跨月统计保持；CI公开证书`705ddb35fc0ef0a48128b643e82e26071f6f05cd5ee40694bf928ed20e0928ab` |
| Source／candidateNative | `max-drill-layout-review.md`增量Approved C0/I0/M0，全分支C0/I0/M2；本机原生材料Approved C0/I0/M0 |
| 7be Runtime／Visual／Upgrade | `s2-final-7be-runtime-visual-review.md`独审三项Approved C0/I0/M0；59业务＋1升级原PNG逐件复核，三字号MAX负金额完整单行，RV-M1关闭 |
| 本机APK审计 | 9,834,364 bytes，SHA256 `072afe980ecdbeaae398c4630d49ecc7a6374ef465e013b05d31b998aeb43c46`；0.1.5/code7/min26/target35；旧本机公开debug证书同为`209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d` |
| 两output新APK／PNG | `delivery.json`：各1 APK＋1独立SHA256SUMS＋59业务原PNG；120个新副本hash逐项与来源一致 |
| 107原件／16合同 | `delivery.json`复制后核验：107原件及16数据／安全合同hash不变 |
| 文档后同步／FullDelivery | cfa0594已实核整分支diff-check exit0、本地／远端／PR HEAD一致、clean／private／OPEN／draft；`s2-final-delivery-review.md`实际技术FullDelivery Approved C0/I0/M1，Task3文档C0/I0/M0 |
| 作者手机验收 | 20项未勾，设备型号／Android版本未提供 |

## 范围与分析口径

0.1.5/code7在原schema1／format1账目上派生月度／年度统计，输入完整state.entries，明细关键词和收支筛选不影响统计。分类身份为类型＋Kotlin trim后名称，大小写敏感，钻取按完整期间／类型／精确名称匹配，不把“餐饮外卖”混入“餐饮”。

金额合计／结余／比较差额用exact Long加减，百分比和平均每笔用BigDecimal、HALF_UP。完整总览计入期间未来账目并提示；同期窗口单独标双方起止日期与金额，历史用完整上期，当前用双方相同经过天数，未来／1900边界说明无法比较。基期无记录不生成虚构增长百分比。年度12月一次遍历积累12桶，图形仅用已计算金额求柱高比例，同源文字保留准确金额。

统计／钻取详情按稳定UUID读当前账目；编辑取消留详情，保存／删除仅消费匹配operationId并回原来源，移出范围或删除后重新派生。保留原busy／pending同步守卫，独立保存少量期间、类型、来源与滚动状态，不把账本放Bundle。Dispatchers.Default请求绑定源列表身份、期间、today及重试token；取消显式重抛，渲染检查当前请求。页面访问／ON_RESUME刷新日期，未承诺常驻跨午夜后台计时。

不改DB、BackupCodec、VM操作协议、旧MonthlySummary、Money、LedgerRules、依赖、manifest或备份合同；不新增INTERNET、广告、遥测、云同步、预算、账户、分类库、储蓄率或AI评语。统计只分析已记录事实，无记录不代表没有消费，结余不等同账户余额。

## 真实RED与源码修复历史

| 阶段与证据 | 当时实际结果／证明范围 |
|---|---|
| Task1最初占位RED | 111 JVM／28F，部分NPE；不是全部合格行为RED，原件保留 |
| Task1中间RED | 111／29F，仍有空single异常；不冒称全为断言失败 |
| Task1最终断言RED | task1-red-final：111／29F／0E／0S，29失败均AssertionError；原81通过。一个零／负百分比分母用例占位时自然通过 |
| Task1实现GREEN | task1-green：111／0F／0E／0S，新增30，原81逐名missing=[]；受审提交d2e0198 |
| PhaseA测试初审 | C0/I1/M2：失败PNG命名camelCase不符合helper要求、排行观测和全幅viewport检查不足；c46159b定向修复，独审C0/I0/M0 |
| PhaseA真实设备RED | CI37603318838／c46159ba45bffbfcea015262e120b27551edaf72：55 UI／20F／0E／0S，原35全过。19首缺stats-kind-income，年度入口首缺stats-mode-year，升级未执行 |
| Task2初审d17 | Source Needs fixes C0/I2/M3：旧支出分类直接显示断言与长页不兼容；钻取helper抢在真实异步列表挂载前断言；日期／年度刻度和既有告警另记 |
| 958eb85修复及增审 | 保留新信息顺序；旧三处原断言前真实滚动；钻取等待列表，增实际日期与1/4/8/12刻度。仍发现旧滚动入口缺真实统计就绪等待，I1未关 |
| db03ab8 test-only修复 | 仅三测试文件14行：原10000ms等待stats-list恰1、无loading／reading、明确拒绝error，再真实滚动。Task2 Source Spec／Quality Approved C0/I0/M1 |
| 整分支源码独审 | c476→db03共25文件／2395行包，Source Spec／Quality Approved C0/I0/M2。M1既有工具告警；M2两新文档EOF空行，wholebranch diff-check实际exit2，最终Task3去掉并实核 |
| 两次替代CI | d17的37606907472、958的37607659592实际cancelled，原run.json保留，不能作为设备GREEN |
| db03真实设备失败 | CI37608177669 attempt1：111 JVM／0F0E0S、lint0E9W；55 UI／9F0E0S，原33pass2fail、新13pass7fail，升级未执行，44PNG非final；原件ci-s2-37608177669-attempt1保留 |
| test-only后续整合 | a137精确分类scope／b3实际reading与list屏障／f0d7f65e731e4dbd11f808056feeff2f3393bd3d整合；静态Source Approved C0/I0/M2，字体问题未解决，设备／升级未放行 |
| f0第二次实际设备失败 | CI37611542196 attempt1／exact f0：55UI3F0E0S，原35全过且missing=[]、新17过；111JVM全0／lint0E9W，升级未执行，53PNG含6font失败图，非final |
| ff1bdb修复前候选 | 测试helper＋CI device job45min，Source独审Approved C0/I0/M2；attempt1环境故障，attempt2运行／升级通过并有RV-M1，仅为历史 |
| 7be RV-M1源码修复 | 分类钻取复用纵排，三字号加真实单行断言；Source增量Approved C0/I0/M0、native候选Approved；新断言未在旧横排代码自动RED，ff1原图人工观察不能冒称自动RED |
| 7be新设备／升级 | CI37626498602 attempt1新build111全0／lint0E9W，新device55 UI全0，三字号单行断言实际必经并通过，seed／verify各1通过；独立原件审查见下文 |

PhaseA的20个失败全部停于首个新控件，不能推定每个编辑／删除／旋转／刻度等后续分支都已有RED。其failure watcher可在finally还原尺寸／字体／主题后采图，例如fontTwo失败图为1080×2340；诊断图不能当实际360dp／font2验收。Task2一次reload代码编译失败unresolved isCompleted也保留在报告，不能当行为RED。原失败正文、原图与拒绝审查不覆盖或改写。

## 测试名称与最终实读结果

旧81 JVM名称见baseline-jvm-names.json；旧35常规UI与2升级名称见task2-original-test-names.json。新增JVM30为StatisticsPeriodTest 4、StatisticsNumbersTest 7、PeriodAnalysisTest 19；覆盖年边界／闰年、精确分类、Top3、参与校验、未来账目、历史／当前／闰年窗口、零基数、溢出及整数分舍入。

新增常规UI20项已在7be新run原始XML逐名核对并全部通过：

- monthlyCategoriesAndMetricsUseFullLedgerDespiteDetailFilter
- annualTwelveMonthsAndMonthEntryUseIndependentYear
- yearBoundsRejectInvalidYearsWithoutNavigatingBeyondRange
- categoryDrillUsesExactTrimKindCaseAndCompletePeriod
- drillRotationAndBothBackPathsRetainOriginAndIndependentScroll
- drillEditCancelLeavesDetailAndOriginalFields
- editedDateMovesEntryOutAndReturnsToOriginalDrill
- editedKindMovesEntryOutAndReturnsToOriginalDrill
- editedCategoryMovesEntryOutWithoutChangingMonthTotal
- deleteLastDrillEntryReturnsToPersistentEmptyCategory
- topThreeCancelSaveAndDeleteReturnToStatisticsSource
- comparisonShowsExactHistoricalRangesAndDifferences
- comparisonDistinguishesMissingBasesFutureAndEarliestPeriod
- currentMonthComparesEqualDaysAndOverviewIncludesFutureRecords
- currentYearComparesEqualDaysAndOverviewIncludesFutureRecords
- rapidPeriodAndSnapshotChangesNeverLeaveStaleVisibleResults
- fontOneAt360DpCanReadAndOperateMonthlyAnnualAndDrill
- fontOnePointThreeAt360DpCanReadAndOperateMonthlyAnnualAndDrill
- fontTwoAt360DpCanReadAndOperateMonthlyAnnualAndDrill
- emptyTypesAndThemesShowHonestZeroStatesAndReadableNativeBars

原35不是源码逐字节未动，Smoke版本字面改成0.1.5。原81 JVM／35 UI方法名和业务断言保留，测试源码有最小兼容适配：三项Smoke／Flow在原10000ms预算内等真实读取／派生就绪并滚到“支出分类”；Query统计分类断言限定精确category tag与原字面文本，避免同文Top3匹配；Recovery在分类／备注输入之间执行既有真实IME关闭。没有删名、删业务断言或加长等待，不能称旧测试逐字节未动。ROOT及独立审查者已实读原XML并逐名确认旧81／35 missing0、新30／20全部执行且全0；源码存在或编译本身不算通过。

## 本机与新CI build实际记录

```powershell
& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')
```

7be本机`build-20261007-210539.log`实际37秒／82 tasks中19 executed／63 UP-TO-DATE，含生产、JVM、instrumentation编译与APK打包；独立读取14 XML为111全0、lint0Error／9Warning。`apk-audit/frozen-local-build.log`随后冻结精确HEAD/tree的assemble为7秒／37 UP-TO-DATE，非clean重新编译。本机原生audit已实读raw badging／manifest／权限／备份规则／新旧公开v2证书与随包三许可证：applicationId `com.xiaoliumiaomiao.anjizhang`、0.1.5/code7/min26/target35；仅既有signature receiver权限，无INTERNET；allowBackup=false，legacy五域与cloud／transfer各五域path="."全排除。旧新本机证书SHA256同为`209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`；没有读私钥。真实APK 9,834,364 bytes／SHA256 `072afe980ecdbeaae398c4630d49ecc7a6374ef465e013b05d31b998aeb43c46`，与audit/tree/log绑定。`max-drill-layout-review.md`独立candidateNative Approved仅涵盖此候选，不替代新CI升级或复制后保护。

新CI37626498602 attempt1两job success；ROOT与独立审查者分别读取新build的111 JVM／0F0E0S、原81 missing0、新30、lint0E9W，以及新device的55 UI／0F0E0S、原35 missing0、新20、seed／verify各1通过。历史Task1／db03／f0／ff1本机构建与失败留在下方历史节及其原报告；旧APK hash9643086a…属于ff1，不用于7be交付。

桌面1k／10k／50k合成基准见task1-benchmark.jsonl及environment原件；50k年度分析median3.36725ms、P95 5.9224ms，仅为Windows／JDK纯逻辑开销，未包含Android／SQLite／Compose，不声称手机500ms或帧率目标达成。既有SDK XML3／4兼容及lint8 GradleDependency＋1 UseKtx告警如实保留。

## 同签名升级的最终原始证据

基线固定c476／0.1.4/code6→新0.1.5/code7，同job临时debug证书。ROOT与独立审查者已实读三证书、版本badging、实际install／install-r、seed／verify原始`am instrument -w -r` runner的唯一OK(1 test)、completion=-1、exit、launch、schema1、三UUID、kind／amountCents／category／date／中文换行备注及跨月统计；下段记录结果。

最终`ci-s2-37626498602-attempt1/android-device/upgrade-artifacts/`的seed／verify原始runner各唯一OK(1 test)、completion=-1、exit0；三次安装与启动原件成功。基线0.1.4/code6、当前0.1.5/code7，CI包公开证书同为`705ddb35fc0ef0a48128b643e82e26071f6f05cd5ee40694bf928ed20e0928ab`；schema1三UUID的kind／amountCents／category／date／中英标点换行备注以及9月／10月统计在seed-reopened、upgrade-opened、upgrade-reopened逐字段一致。seed→verify中无清库、卸载、再connected或重seed。S1接受CI37599529944 attempt2保留build／rerun device且81／35／seed／verify全0，只证明旧code3→6，不能继承为S2 code6→7。

## 原始PNG、合成金额与视觉边界

7be新run的`android-device/device-ui-screens/`有59张原业务PNG（36 S2＋23旧回归），另有升级首页`android-device/device-screen.png`，合计60张新原图；ROOT逐件读图见`root-runtime-visual-reviewed.json`，独立逐件长度／SHA256及视觉结论见`s2-final-7be-runtime-visual-review.md`。三张360dp font1／1.3／2 MAX钻取负金额完整单行、无截断；新restored-scroll图显示2025-05-04..01的104..101，独立滚动anchor由通过测试证明，不沿用ff1历史图107..104。

独立样本2025-05收入100.01／支出16.42／结余83.59；2025年350.01／45.42／304.59。收入／支出分类、占比和Top3按实际fixture核。新run raw LedgerS2记录today=2026-10-07、timezone=Etc/UTC、future=true；当前窗口支出10.01对4.01、收入100.00对50.00，完整总览实际收入300.00／支出10.01／结余289.99。不能把总览与窗口金额混用。

旧statistics.png现滚到“支出分类”后采图，非首页总览；其旧fixture为实际当前月两笔餐饮12.34和交通56.78，总支出69.12／结余-69.12，收入0.00。不得套用2025-05的S2样本或把文件名当视口证明。

font1／1.3／2的720×1280／320dpi／360dp真实配置、字形／viewport及操作断言已在新run核对；主题场景另测light／dark，未提供font2-dark受控交叉，不合并不同图推定交叉通过。年度font图最后定位12刻度；独立审查按原图实际可见范围记录plot／图例／刻度，不将图外内容称为同时可见。s2-year-overview实际为chart视口；s2-drill-precise是月餐饮图，其后年度四日期由Runtime断言承担。loading／error、单类型空、编辑移出等没有全部专图，按实际运行和单图边界记录。

## 最终实物、同步和手机边界

新文件an-jizhang-0.1.5-debug.apk、SHA256SUMS-v0.1.5.txt和v0.1.5-screenshots已各复制至隔离与主目录output。`delivery.json`记录两处各1 APK／1校验／59原业务PNG、120新目标全部逐项hash一致；107原件（105旧output＋作者HANDOFF／未跟踪V1.0方案）及16既有数据安全合同复制后hash保持。不覆盖旧校验，不改作者主目录两文档与归档方案checkbox。

仅8份允许交付文档在最终受测SHA之后修改；REUSE_V015和新S2计划已去掉额外EOF空行，本地整分支`git diff c476eac..HEAD --check`实测exit0，该格式Minor已修。2026-10-07T13:58:17Z实际同步见`delivery-sync-before-final-review.json`：本地／远端／PR5均`cfa0594e1e1feccff037a07875ad72fa34062c38`，clean／private／OPEN／draft；远端main仍294479f，主目录main仍ff8479f且原两份作者文档状态保持。最高独审又独立查询GitHub与本地、重算120副本及107＋16保护hash，给Task3文档Spec／Quality Approved C0/I0/M0、技术FullDelivery Approved C0/I0/M1（仅既有工具提示），见`s2-final-delivery-review.md`。本次只写回已发生的批准并勾技术收尾项；后续docs-only提交的最终身份另由`delivery-sync-final.json`记录，不把文档提交冒称新的111／55设备受测SHA。

只具API35临时合成模拟器证据；API26完整S2实机、作者自然手机、真实进程kill、完整TalkBack和确定性Default乱序调度未执行。20项 [PHONE_ACCEPTANCE_V015](PHONE_ACCEPTANCE_V015.md)未勾。作者先导出0.1.4备份，再正常覆盖安装本机同签名包，安装异常保留应用／账本，不卸载、清库或强降级。S2作者验收前不合S2；S3与正式发布另行确认。

## db03实际9失败与后续test-only候选（历史）

实际受测db03／CI37608177669 attempt1，证据在ci-s2-37608177669-attempt1/{run.json,root-ui-summary.json,root-failures.json,android-build,android-device}。111 JVM全0、lint0E9W；55 UI／9F／0E／0S，原33通过2失败、新13通过7失败，升级未执行，44PNG并非最终成功交付图。该运行不能改写为GREEN。

| 真实失败 | 证据判断与最小后续候选 |
|---|---|
| Query combinesKeywordAndTypeClearsBothAndKeepsFullMonthStatistics | 原“午餐  ¥12.34”有两个同文节点（分类与Top3），不是期望金额不同；a137限定唯一category tag＋原文本，保持原业务断言，尚需新设备通过 |
| Recovery queuedSaveRestoredWithNewViewModelKeepsDraftAndCanRetrySameId | 分类输入后备注setField可见性失败；日志有IME show，但缺当时Rect／PNG，不能唯一归因产品遮挡。f0只补既有真实closeKeyboard于两个字段间，保留备注／保存／新VM恢复／ID和完整字段断言 |
| 三个font1／1.3／2 | 都先失败于本月结余83.59的严格TextLayoutResult overflow检查，尚未到最大金额／年图／钻取；不是后续viewport断言失败。失败图在finally恢复后为1080×2340 loading帧，不能证明360dp原配置字形clipping。f0只增加size／paragraph／constraints／line／font与viewport诊断及finally前原配置PNG，保留严格overflow与full viewport；问题未解决，不能先改字体或放宽断言 |
| annualTwelveMonthsAndMonthEntryUseIndependentYear与emptyTypesAndThemesShowHonestZeroStatesAndReadableNativeBars | 真实图表完整ContentDescription存在；assertContentDescriptionContains默认substring=false误要求精确短句。f0三调用改为完整原句assertContentDescriptionEquals，产品描述／后续刻度金额不改。三字号同调用尚未到达，不能写已复现该处 |
| topThreeCancelSaveAndDeleteReturnToStatisticsSource | 取消后第一次硬件Back仍留详情，Dialog input channel销毁与Back竞争有同次logcat证据，不能归因滚动状态已坏；f0以单个原10000ms要求editor-form消失、原生Activity焦点后仍发一次Back，保留anchor和位置断言；新屏障未设备验证 |
| monthlyCategoriesAndMetricsUseFullLedgerDespiteDetailFilter | 真实expected六分类／actual[]，失败原图在页尾。原快速swipe采样缺覆盖保证，但缺逐轮node／flags／offset，唯一根因未证明；f0以通用category-prefix找真实首分类、同帧placed／viewport／y顺序采样、至多半viewport无惯性ScrollBy重叠扫描，仍20轮／严格有序完整expected／reachedEnd，加失败逐轮诊断；不按expected逐项定位制造顺序 |

b3 awaitAnalysis要求真实唯一list且无loading／reading，不只靠Compose idle，明确error仍失败；受控reading/error场景不调用它，因此未等待掉原必须观察的中间态。a137／b3／f0全部test-only，生产最近仍958。whole-source-review曾因新增matcher／ranking缺陷重新打开Needs fixes，f0实际独立增审恢复Source Spec／Quality Approved C0/I0/M2；它只关闭源码项，不关闭实际Runtime失败。既有工具告警和两文档EOF Minor仍保留。

f0本机build-20261007-190106.log为14s／4 executed／44 UP-TO-DATE，实际instrumentation编译与测试APK打包，生产Kotlin UP-TO-DATE；不是新增111 JVM／lint或设备结果。当时f0已经由ROOT push进入新CI；其后实际结果与ff1环境运行见下文，不把当时HEAD填为最终受测SHA。Runtime／Visual／Upgrade／APK副本／FullDelivery仍未通过；新证据可能显露先前未到达分支失败。原诊断报告task2-runtime-test-diagnosis.md／task2-runtime-diagnosis.md和旧失败保持。

## f0三字号余额误判的实际绑定与ff1bdb候选（历史）

已完成CI37611542196 attempt1／exact f0d7f65e731e4dbd11f808056feeff2f3393bd3d。root-ui-summary.json实际55 tests／3F／0E／0S；root-ui-name-retention.json原35 missing=[]且全通过，新增17通过。111 JVM全0、lint0E9W，升级未执行；53PNG包含3张原配置failure-before-reset及3张finally恢复后font失败图，不当最终成功图集。db03的Query／Recovery／chart／rank／Top3等后续修复在此次真正通过，旧失败原件仍保留；不能据其他52方法通过把三个font早停后的流程判为执行。

font-layout-api-investigation.md从固定官方Compose1.8.0源码／实际runtime javap机制出发，再独立实读XML和三张恢复前原PNG，完成实际数值绑定：

| fontScale | 实际node／result px | 语义MultiParagraph px | 唯一单行left/top/right/bottom | 原始overflow |
|---|---|---|---|---|
|1.0|412×81|656×81|0/0/412/81|width=true／height=false|
|1.3|422×82|656×82|0/0/422/82|width=true／height=false|
|2.0|543×107|656×107|0/0/543/107|width=true／height=false|

三项parent constraints均minWidth0／maxWidth656／minHeight0／maxHeightInfinity，Clip／wrap／maxLines Int.MAX_VALUE，Ltr、无placeholder；lineCount1、无超max／ellipsis。node与实际行尺寸相等，只有语义MultiParagraph保留父max656宽，与String Text快路径slowCreateTextLayoutResultOrNull用parentmax新建MultiParagraph却配旧自然layoutSize的公式吻合。三张恢复前图实际余额完整；此次首余额失败可归为语义API宽度重建误判，没有证据据此改生产字形或字号。

f0原语义与geometry为不同idle调用，未输出完整lineStart/end／所有字符bbox／bidi run，不能称新的严格谓词已被f0全部观测。ff1bdb0074a8091a5e18cd71d54eba698ab9a7a8只改共享StatisticsTestSupport与CI设备job：保留普通raw无overflow；同runOnIdle读取唯一unmerged Text与实际节点／配置／viewport，窄范围自然宽单行签名才核精确text与density、width-only／parentmax、min0、Clip／wrap／IntMax、Ltr／left-start、无style／placeholder等未知因素、全文lineStart0/endlength、每字符bbox和行边界均在实际节点内，仅0.01px浮点epsilon与正常ceil不足1px。任何未知、多行、高度或缺字继续失败，原fullyVisible与48dp不变，不做通用widthOverflow忽略、生产hook、字体缩小或金额字面豁免。

ff1bdb build-20261007-193548.log与193634.log均13s、4executed／44UP-TO-DATE，instrumentation编译／打包真实执行，生产KotlinUP-TO-DATE；未重跑生产JVM／lint，不称设备通过。device job总预算35→45min只是为f0约27min且早停后的完整字体流程及baseline第二构建／升级预留，不是实际job timeout根因修复；业务10000ms、两个12min步骤、buildjob25min和失败语义均保持。

ff1bdb源码独审已Approved C0/I0/M2；当时尚未取得设备通过。当时三个font的MAX／年度／刻度／钻取后续尚未运行；attempt2结果见下节。APK副本／FullDelivery继续待ROOT。当时ff1bdb不是本轮最终SHA；最终APK审计／copy必须绑定最终真正受测身份，不能沿用候选metadata直接声明完整交付。

## ff1bdb attempt1 环境故障与同SHA设备重跑（历史）

[CI37615633124 attempt1](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37615633124) 的HEAD固定`ff1bdb0074a8091a5e18cd71d54eba698ab9a7a8`。build job `112773156746`成功，ROOT实读111 JVM／0F／0E／0S、lint0E9W；device-smoke job `112773156330`失败，设备XML实际55 tests／42 failures／0 errors／0 skipped、13通过。原始`ci-s2-37615633124-attempt1/{run.json,root-ui-summary.json,root-failures.json,failed-job.log,android-device/device-logcat.txt}`和独立`ci-ff1-environment-diagnosis.md`保留。`nativeSuccessPngs=0`，`android-device/device-ui-screens/pull.log`显示截图目录不存在；升级seed／verify未执行，不能拿到成功图或code6→7结论。

独立诊断以`device-logcat.txt:18479`的11:42:55.232窗口焦点等待、`:23508`的11:43:00.244 Pixel Launcher ANR及`:24133`的11:43:01.837系统ANR窗口注册Back为起点；首个应用测试至`:71583`的11:48:57.989才开始，相差约6分钟。ROOT实看`android-device/device-screen.png`，仍有“Pixel Launcher isn't responding”系统模态。29项首要失败停于`KeyboardTestSupport.kt:18`等待IME**出现**，10项停于`captureLedgerScreen`有焦点frame门禁，另2项为IME显示、1项为原生Back；三个新字体用例均停在`openStats→month→closeKeyboard`，未到`readable`及新增`LedgerTextGeometry`。这些与系统模态占用焦点的机制一致；并未取得ANR trace，不能唯一认定Launcher主线程为何停顿，也不能从此运行判定应用字体helper通过或失败。设备job结束于12:06:51Z，不是45分钟job timeout。

ROOT在**相同SHA**仅重跑失败device-smoke job，attempt2于2026-10-07T12:21:18Z启动；attempt1的成功build job沿用，没有再跑。这段记录重跑启动时的状态；随后attempt2全部实际通过并获独立审查，见下节。重跑GREEN也不反推attempt1故障的更深根因；作者手机20项依旧未勾。

当时ff1候选`apk-audit-ff1bdb0/audit.json`绑定同一ff1bdb HEAD/tree；`candidate-native-review.md`独审Approved C0/I0/M0，实读包身份0.1.5/code7/min26/target35、唯一既有signature权限与无INTERNET、备份全域排除、旧本机公开debug证书相同及三完整许可。APK仍9,834,075 bytes／SHA256 `9643086ab64d8c3e91393e201f92bba9022fdaae1a150e745c7fc96d701918c8`，候选assemble为6秒／37 UP-TO-DATE，非clean重编译。候选审查同时只读确认107原件与16合同hash无差异；该段为ff1修复前候选历史，不代表7be设备通过、两output复制后复核或FullDelivery；7be候选审计见页首及末节。

## ff1 attempt2 完整技术通过与RV-M1历史限制

[CI37615633124 attempt2](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37615633124) 在**同一SHA ff1bdb0074a8091a5e18cd71d54eba698ab9a7a8**完成，run success。`ci-s2-37615633124-attempt2/`保留新device原件；build沿用attempt1，`retained-build-provenance.json`的40文件长度/hash逐项相同，非第二次JVM／lint执行。独立`s2-final-runtime-visual-review.md`从14份JVM XML读111／0F／0E／0S、原81逐名missing0，lint0E9W；设备XML读55／0F／0E／0S、原35逐名missing0，新20（4 Display＋16 Statistics）实际均执行。logcat的TestRunner为57 started／finished／0 failed，含55常规及seed／verify各1。ROOT和独立审查者的CI解析各有原件，不以run badge代替XML。

三字号方法全部完成余额、年度1／4／8／12刻度、MAX金额、分类钻取与返回、12月点击和配置恢复。logcat六条`LedgerTextGeometry`恰为三字号各余额与MAX金额；均记录360dp、density2、width overflow为真且height为假、parent宽656，node与行右／底一致、字符box覆盖全文且在真实节点内。普通年度刻度/多行长分类未走窄fallback。独立实际打开59张业务PNG（36 S2＋23旧）及升级首页1张，共60张并逐项SHA256盘点；视觉结论的单图边界见原独审，不能把滚出视口的部分说成同图可见。

升级为固定c476／0.1.4/code6→ff1／0.1.5/code7，同job三份CI临时公开证书SHA256均`94dbc85ab3187f3bb2cf7a5b980f061a263a051fbb20df2812e065c0c1353a3f`。原install、seed、`install -r`、verify、launch链实读；seed／verify各唯一`OK (1 test)`、completion=-1、exit0。schema1、三UUID、kind／amountCents／category／date／中文引号与换行备注、9月及10月逐字段／跨月统计相同，启动首页与原始runner各有明确证明范围。本机旧新公开debug证书为另一条`209fe...`，不能把CI临时签名当作者手机覆盖签名。

独审对**ff1精确候选**给出Runtime Approved、Upgrade Approved、Visual Approved with Minor RV-M1，C0/I0/M1；FullDelivery Pending。RV-M1来自`s2-font-1-0-drill.png`与`s2-font-1-3-drill.png`：360dp MAX负金额在横排记录里小数尾部分别折至下一行，负号、小数点和所有数字未丢失或遮挡；font2纵排为单行。此为阅读效率限制，不能声称三字号MAX钻取均单行。ROOT决定本轮以局部纵排和三字号断言修正，故ff1的完整运行／视觉结论是**修复前历史证据**；新候选必须重新绑定源码、设备/升级、原图和native审计，未复制两output，也未取得FullDelivery或作者手机通过。

## 7be冻结候选、新CI与RV-M1关闭

`max-drill-layout-review.md`对ff1→7be四文件+23/-3增量给Source Spec／Quality Approved C0/I0/M0，接续全分支C0/I0/M2。统计分类钻取是唯一显式纵排EntryRow调用，金额沿用原Money.format／正负号与28sp样式，普通明细保持原布局；稳定ID、点击48dp、来源返回和滚动容器不变。三字号既有Display方法在MAX字面检查后增加`drillAmountSingleLine`，直接绑定指定UUID的真实unmerged金额Text，要求唯一LayoutResult、全文lineStart/end、恰一行、无ellipsis／超max／raw overflow，原readable／viewport断言与后续年度流程仍在。**此新断言从未在旧横排生产布局上自动执行RED**；ff1原图是人工发现RV-M1的证据，静态批准不等于新设备GREEN。

7be候选原生审计与当前冻结APK值见上表。本轮本机四任务build和新CI37626498602 attempt1新build分别111 JVM全0／lint0E9W；新device55 UI全0，旧35保留，code6→7 raw升级seed／verify各1全0。新三字号MAX单行断言在成功用例中无条件必经，59业务原PNG＋升级首页的独立逐张审阅给Runtime／Upgrade／Visual Approved C0/I0/M0，RV-M1关闭。新断言未在旧横排实现上实际自动跑RED；ff1旧图只作为人工视觉缺陷证据。两output新包／校验／原图已复制读回；文档cfa0594完成实际Git同步并获技术FullDelivery Approved C0/I0/M1。作者手机20项继续Pending。
