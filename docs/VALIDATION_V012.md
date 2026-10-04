# 0.1.2 验证记录（技术验证通过，待手机验收）

日期：2026-10-05（Asia/Shanghai）。0.1.2本轮完整技术测试已通过，本机测试APK已按新文件名复制到两处output；最终交付独立审查及私有草稿PR同步正收口。作者已确认0.1.0和0.1.1全部通过；0.1.2手机验收尚待执行，不继承旧通过结果。[PHONE_ACCEPTANCE_V012](PHONE_ACCEPTANCE_V012.md)八项全未勾。

## 范围与代码来源

已验收0.1.1固定基线为`976817290302e712519297cc5b810ce019d2d383`，PR #2已MERGED。沿用隔离工作区及功能分支`feat/ledger-query-v0.1.2`。本轮生产实现提交`895cea973e5084a1d63b51d7b8ad6f59313c5edc`；升级fixture/脚本/CI同步提交为`8f1c4c902ff387ad1755b4d25600cff0ddcb5e8c`；三行test-only提交观测后最终受测代码HEAD为`0076b6b11de015a81f7c2f4e2337f9408ccd9a41`，其后本轮交付只改文档。

版本为0.1.2/code3：只筛选所选月的分类或备注，关键词trim后做忽略大小写的字面子串匹配，与全部/收入/支出类型AND组合；一键清除两个条件但月份不变。原输入和类型在顶层rememberSaveable，切页/切月/Activity重建保留。顶部与统计页仍使用完整月数据，导出仍是全账本。无新依赖/权限，不改生产ViewModel、DB、BackupCodec、EntryEditor、schema1或备份format1；固定来源与采用决定见[REUSE_V012](REUSE_V012.md)。

## 基线和纯逻辑RED/GREEN

`build-20261005-001147.log`在固定9768172基线上使用`--rerun-tasks`实际重跑36项JVM，0失败/0错误/0跳过；原始XML在`test-artifacts/sdd-v012/baseline-9768172/`。

新增EntryFilterTest共14项，先加入可编译的返回原输入stub，运行：

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', '--tests', 'com.xiaoliumiaomiao.anjizhang.EntryFilterTest'
```

实际RED为14项、12失败、2通过、0错误/0跳过，`BUILD FAILED in 12s`。例如字面`%`查询返回了未含该符号的多余记录，分类/备注匹配返回不该保留的交通记录；失败是AssertionError业务断言，不是编译或环境错误。原始日志/XML在`test-artifacts/sdd-v012/task1-red/`。

实施后同定向命令实际GREEN为14/0/0/0，随后运行全JVM：

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest'
```

实际执行testDebugUnitTest，7份原始XML合计50/0/0/0：BackupCodec12、EntryFilter14、LedgerDatabase5、LedgerViewModel11、MainActivity1、Money5、Summary2。定向与全量均成功12秒，证据在`task1-green/`、`task1-full/`。测试覆盖月份边界、空词/空白、trim及内部空格、分类OR备注、类型AND、Unicode/ASCII大小写边界、中文换行、字面特殊符号、原顺序/对象/输入不变及200字符备注/更长查询。

## 真实旧UI RED与夹具修复历史

阶段A只增加四项Query设备测试，生产UI仍0.1.1/code2；第一次独立审查规格Approved、质量Changes requested，发现新旧测试复制了整块IME关闭逻辑。ROOT核对后调整测试文件范围，抽取共用KeyboardTestSupport，保留真实Back、10000ms等待及finally恢复；原六项业务断言不变。修复后独立复审Approved，原报告未覆盖。

tests-only提交`6c8c530b42d35dc000cd0c5e4dd82b65b668d9bb`对应真实[CI 37217431685](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37217431685)：build job成功；下载实读7份JVM XML为50/0/0/0，lint0 Error/9 Warning。设备原始XML为10项、4失败、0错误/0跳过：四项Query均`java.lang.AssertionError: No node found ... 搜索分类或备注`，原六Smoke全部通过。失败来自旧UI确实未提供搜索控件，是有效UI RED。connected失败后升级链未执行，不能称该次升级已通过。原始run.json/log/XML及ui-red-cases.json在`test-artifacts/ci-v012-37217431685/`。

该RED保留当时旧版本/旧升级链，不能当作当前0.1.1→0.1.2最终GREEN。后续接入UI并同步新升级链后，才在当前代码HEAD统一取得完整CI证据。

## 本机UI接入、fixture与最后构建

Task2接入后执行：

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest'
```

`task-2-ui-build-20261005-005120.log`实际退出0，成功32秒，78项任务中30执行/48 UP-TO-DATE。testDebugUnitTest实际执行；原始七suite XML为50/0/0/0。lint XML为0 Error/9 Warning，其中8项固定依赖GradleDependency提示、1项UseKtx SQLite事务建议。主APK与设备测试APK均实际编译/打包成功。证据副本在`test-artifacts/sdd-v012/task-2-ui-evidence-20261005-005120/`，不能以本机编译替代真实设备GREEN。

Task3阶段1将升级链固定到已验收9768172的0.1.1/code2→0.1.2/code3，保留全部runner、证书、CI/单设备/QEMU/ci-ledger、timeout及中途不清库门禁。实际运行`assembleDebugAndroidTest`，`task-3-assemble-gradle-original.log`成功22秒，4执行/40 UP-TO-DATE，fixture Kotlin/dex/package重新执行。现有Git Bash的`bash -n .github/scripts/verify-upgrade.sh`退出0，见`task-3-bash-syntax.log`；语法通过不是实际升级通过。

ROOT在当前代码HEAD再执行上方完整构建，`final-local-build-20261005-010150.log`退出0，成功19秒，3执行/75 UP-TO-DATE。其中testDebugUnitTest明确为UP-TO-DATE，不声称重新执行50项JVM；前次实际通过结果保留。`final-apk-freeze.json`现场记录最终构建APK与候选审计完整SHA相同，生产来源仍895cea9，升级测试/CI来源为8f1c4c9。

环境诊断如实保留：SDK工具只理解XML v3而遇到v4；MainActivityTest原始XML有SQLite“cannot set library path, library already loaded”，9768172基线同文件亦存在且均1项通过；lint9项Warning没有隐藏。本轮不扩大工具安装、依赖升级或数据库改动来消除既有提示。

## 本机APK包装审计与冻结

精确审计的构建产物`app/build/outputs/apk/debug/app-debug.apk`已复制到本工作区与原项目`output/an-jizhang-0.1.2-debug.apk`。实际大小9408857 bytes，SHA-256：

```text
8c82dbc30e482232b537868174ffb1aa2c5ec1e9c878420ee9fb7857b381ae42
```

候选audit的8条原生aapt/apksigner命令均退出0，ROOT已实读原始输出；最后构建哈希相同，因此可复用针对该精确包的审计。包名`com.xiaoliumiaomiao.anjizhang`、0.1.2/code3、min26/target35、安记账及可启动MainActivity已核验。新包和本次现场重读的旧本机0.1.0/0.1.1包均v2签名有效，公开证书SHA-256相同：

```text
209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d
```

实际打包Manifest只请求内部signature的DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION，无INTERNET或广泛存储权限；allowBackup=false。full-backup-content、cloud-backup和device-transfer各自排除root/file/database/sharedpref/external全域，均path="."。ZIP内三完整许可与源码及third_party逐文件长度和SHA相同：

| 文件 | bytes | SHA-256 |
| --- | ---: | --- |
| NOTICE.txt | 1771 | `7212bb59327a5b7d2a80b6cf26397c83c3533021f2c382bc924aaf812c68c016` |
| MoneyBook-LICENSE | 1090 | `dd301e9fdf17457614273e0a316e39fee5b5074685c475c748ebdc1f614630c8` |
| Android-Architecture-Samples-LICENSE | 11562 | `460c644a8e66e4e0d21a30feb4c9afd603922ef1d07e5043160e567688daf17c` |

原始证据在`test-artifacts/sdd-v012/apk-candidate-audit-20261005-005120/`，报告为`apk-candidate-audit.md`。没有读出、复制或上传keystore。旧文件现场复核未变：primary0.1.0为9835648 bytes/SHA `6fb6bfebf726da15d4fa9756d3917253173e2e444710caaca62b7f677e748280`；primary及工作区0.1.1均9408857 bytes/SHA `81a92bf36f1867cab1aa2df9b318b5d42e18e16dd65eeb864209f6bd0e8d7448`。

## 最终完整CI与交付门禁

[CI 37220141121](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37220141121)对应代码HEAD`0076b6b11de015a81f7c2f4e2337f9408ccd9a41`，两job均success。ROOT下载并实读原始run.json/run.log、七JVM XML、设备XML、两runner原始/规范化输出、三公开证书、badging/install/launch及截图；原始资料在ignored `test-artifacts/ci-v012-37220141121/`，不是从绿色图标推断业务通过。

- JVM：七suite共50 tests / 0 failures / 0 errors / 0 skipped；lint 0 Error / 9 Warning（8 GradleDependency、1 UseKtx）。build实际成功4m1s。
- API35/Android15设备XML：10 / 0 / 0 / 0，四Query和原六Smoke全部通过；connected构建及设备测试实际成功4m10s。negative testcase日志17:32:00.772记录`failed save submitted ... jobs=1`，随后原错误/输入断言通过；它证明本次保存提交后业务路径通过，不反推旧失败根因。
- 固定9768172源码0.1.1/code2→0.1.2/code3，同job三包原始公开证书均`a8fe856cede437a842b028ec74930bb1f5f406fe67d95061fbbc01d1102596b0`。这份CI证书与本机证书不同，CI APK不用于覆盖作者应用。
- raw seed/verify各恰1个`OK (1 test)`、恰1个completion且为`INSTRUMENTATION_CODE: -1`，失败模式扫描为0；PackageManager真实读出旧2/新3，schema1、三记录全部字段/中文引号换行/两个月统计在seed reopen与upgrade open/reopen均保留。10月收入10000分、支出1分、结余9999分；9月支出1234分、结余-1234分。
- 原始三install均Success；设备唯一emulator-5554、QEMU=1、AVD=ci-ledger；脚本与工作流未在seed→verify间清库/卸载/reseed/connected。launch Status: ok，Upgrade validation exit: 0，Device validation exit: 0。
- ROOT实际查看device-screen.png：中文安记账首页、2026年10月、收入¥100.00/支出¥0.01/结余¥99.99、搜索框/三类型/清除、全月说明、显示2/本月2条、原中文换行备注、编辑删除与底部导航可见。此为隔离CI合成数据截图，不是作者手机验收。

- [x] 当前受测代码两job成功、50 JVM/10 UI全0及lint实际结果。
- [x] 固定0.1.1→0.1.2同证书真实替换、两runner、字段/统计/启动/截图原始证据。
- [x] 两处output新文件复制及新旧哈希、旧校验行原字节前缀核验。
- [ ] 私有draft PR、附件和本机/远端/PR HEAD同步（完成后填实际URL/证据）。
- [ ] 最终交付原始证据与文档独立review（源码Approved不代替delivery）。
- [ ] 作者独立执行0.1.2手机表并反馈；八项全未勾，不合main或正式发布。

## 两处APK交付与记录修正

实际输出：`C:/Users/13765/.codex/worktrees/ledger-v0-1-1/an-jizhang/output/an-jizhang-0.1.2-debug.apk`和`D:/AIproject/an-jizhang/output/an-jizhang-0.1.2-debug.apk`。两处均9408857 bytes，完整SHA与上方审计相同；三旧APK前后仍为原SHA，0.1.0/0.1.1未覆盖。File.Copy(overwrite=false)和两目录preflight拒异哈希覆盖；SHA256SUMS只追加唯一新行。

复制脚本经/root/v012_delivery_guard_review独立Spec/Quality Approved后执行，原script SHA为`75f94855a4f82379d8d37d89d38a7414e22f7b3125f7a332358ddb17cc225ac6`。首次报告的Get-Content行携带PowerShell Provider/PSDrive元数据，Depth9序列化膨胀至331634729 bytes；APK复制和校验已完成，记录问题未改变APK或旧文件。ROOT保留原ignored `apk-delivery-first-run-raw.json`，避免再全文输出；报告脚本仅一行改用File.ReadAllLines返回普通字符串，此修正没有重跑复制。

ROOT独立复核新旧APK，并重新计算两份当前SHA256SUMS的原始前缀：worktree前94 bytes的SHA为`44b23947f1744d34eb713fbc3c5a4f8e0f82068879f6caccbdd96746f20c9b3b`，primary前188 bytes的SHA为`a5d5075011d99a27b12b1deaa879d10cbaa80efd87024cb603bd0c2aa7d7e7f0`，均与交付前独立快照一致；新0.1.2行恰一次。紧凑`apk-delivery.json`只含基本类型、actual hash/path/bytes/原前缀相同boolean，并明确reportingFixExecuted=false、phoneAccepted=false；原大记录未删除、未入Git。
## 保留的限制与回退边界

整本备份最多50000条/10 MiB，当前没有分批导出，保存账目没有同等总量上限；明文备份保存到可信位置。本轮不承诺拼音/繁简转换、Unicode规范化或强制停止后的长期筛选偏好；普通长查询不做200字符硬截断，极端Bundle大小仍未获手机验收证明。

debug签名继续用于测试交付，不同机器/CI运行可能不同签名，CI下载不能替代本机作者覆盖安装包。作者先在0.1.1导出备份，再在技术交付后正常覆盖安装；安装报错保留原应用和账本，不卸载、清库或强制降级，修复以更高版本继续。正式签名、应用锁/加密、多账户和商店发布仍在本轮范围之外。0.1.0/0.1.1历史验证和已确认手机记录继续保留。

## 首次完整 CI 的实际失败记录

[CI 37218934264](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37218934264) 在代码提交 `8f1c4c902ff387ad1755b4d25600cff0ddcb5e8c` 已结束。build job success；主 agent 实读 50 JVM / 0 failures / 0 errors / 0 skipped，lint 0 Error / 9 Warning。device job failure，设备 XML 为 10 tests / 1 failure / 0 errors / 0 skipped；新增四项 LedgerQueryTest 全部通过，原六项 LedgerSmokeTest 中五项通过。

唯一失败为 `databaseSaveFailureAfterRecreationKeepsEditorInputs`，在 LedgerSmokeTest.kt:165 等待错误文字时发生 10000ms ComposeTimeoutException。当前只读调查保存点击、异步任务提交及重建顺序，原因尚未确认；不把局部 UI 通过写为完整验证通过。connected 失败后升级脚本没有执行，不能称覆盖升级成功。原始 run.json、run.log、device-logcat.txt、设备 XML 和 ui-failure-summary.json 保留在 ignored `test-artifacts/ci-v012-37218934264/`。

## 保存提交观测实验与独立增量审查

只读诊断实读失败 XML 与限定时间段原始日志：IME 于17:10:32.876显示、17:10:33.203重建；negative case没有提交前VM/jobs观测。日志缺少SQLite/error文字不能证明保存未dispatch，原失败根因未确定，没有据此改生产。

ROOT只授权单变量：在该negative方法增加只读model、持锁后Jobs快照、真实click后重建前复用awaitSubmitted，共三行；不加关闭IME、重试、扩时、skip、直接状态写或生产修改。原DROP/recreate/unlock、错误文字10000ms、金额与取消断言全部保留。现有helper在持锁下观察NEW active child，能确认真实提交前提并在缺少提交时输出state/jobs/语义树。

实际assembleDebugAndroidTest退出0、11秒、4执行/40 UP-TO-DATE，原始build-20261005-011900.log与副本哈希一致。独立/root/v012_final_review实读三行diff、失败证据和原始编译日志，Spec/Quality Approved、Critical0/Important0，Final delivery仍Pending；报告task-2-dispatch-observation-review.md。ROOT核验review tree822835c9c336043fead7447d93850e2e865babe2与提交0076b6b11de015a81f7c2f4e2337f9408ccd9a41的tree一致后推送。

这是增加诊断与时序前提的实验；后续即使GREEN，也不能单凭它反推原失败一定来自未dispatch。诊断、首次完整CI失败、原始日志及报告均保留。