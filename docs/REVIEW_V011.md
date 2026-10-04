# 0.1.1 全分支最终独立只读审查

以下首份报告是6ed67e5的历史静态审查快照；首次Task2和全分支审查均漏掉raw/pretty协议问题，随后由真实CI暴露并按下方增量报告修复。原审批与未验证边界保留，不追溯改写成提前发现或完整CI通过。

审查范围：已验收基线 `57799ca1751a98e801c091f1cd6cc69f50cb4c77` → `6ed67e5f896813507515a886d3e7e6e96e187f89`。
唯一变更包：`test-artifacts/sdd-v011/final-review-package.diff`（13 文件，447 insertions / 20 deletions，含 log/stat/U10）。
已读 `agent.md`、HANDOFF、0.1.1 计划、两个任务 brief/report/review、APK 独立审计及原始证据。按 requesting-code-review/code-reviewer.md 执行。源码与 Git 未改动，未构建、安装、操作设备或重跑测试；唯一新文件是本报告。

## Spec Compliance

**实现规格门禁：Approved。** 当前代码符合本轮确认的三项范围；整个交付的真实 CI 与文档同步仍待主 agent 完成，不据静态审查预勾。

- 版本为 code 2/name 0.1.1，applicationId、minSdk26、target/compileSdk35 及依赖未变：`app/build.gradle.kts:6-15`。完整 diff 中没有生产数据库、备份解析器、manifest、许可或签名配置变更。为核对兼容性，定点读取未改 `LedgerDatabase.kt:15-28`（schema1、无破坏性迁移）、`BackupCodec.kt:12-15,26-28,35-42`（10MiB/50000、format/version/CNY）、`Entry.kt:4`（全字段 data class）和 `MonthlySummary.kt:5-25`（整数分统计）；升级夹具调用的生产接口均为既有接口。
- 容量文案使用“且”，同时说明条数与字节限制、备注容量、不支持分批导出，未将备份上限说成保存条数上限：`LedgerScreen.kt:112-119`。原 smoke 的唯一改动是新增两条真实滚动可见性断言：`LedgerSmokeTest.kt:49-51`；原六个 @Test、业务断言和 10000ms 等待完整保留。
- 六项 JSON 回归使用真实 decode，重复键包括 Unicode 解码后同名键；拒绝原因有明确断言。5/6 层 note 控制组区分字段类型与语法深度门禁；50000/50001 完整有效记录均先验证 UTF-8 小于字节上限，前者全量 Entry 等值、后者明确条数错误：`BackupCodecTest.kt:21-73`。现有解析器未改；实测 GREEN 是保护原逻辑的有效结果。
- 升级 fixture 的两方法无清库 Rule、assume、Ignore 或 Compose。seed 读取真实旧安装版本，只接受空库，保存后 close/reopen；verify 读取真实新版本，仅检查既有数据库并再次 reopen：`LedgerUpgradeTest.kt:19-41,45-64`。三条固定数据、全字段/条数、schema1、10月10000/1/9999分及9月0/1234/-1234分、分类汇总均用硬编码预期比较：`:62-77`。Entry 的 data class 等值使完整列表比较有效。
- 安装前 aapt 核对包名及真实 1/0.1.0→2/0.1.1，apksigner 验签并比较三份单一 SHA256 公开证书：`.github/scripts/verify-upgrade.sh:26-52`。脚本要求 CI、唯一在线 emulator、QEMU、ci-ledger AVD，所有实际设备命令显式 -s 并设 timeout：`:15-24,55-98`。设备发现命令本身无 -s 是合理的。
- 升级链只选择 seed/verify 精确方法，旧安装→种子→force-stop→install -r→验证→启动，中间无卸载/清库/-d/connected：`verify-upgrade.sh:87-98`。runner 同时检查恰一项 OK、唯一 completion=-1、无失败/崩溃；raw 和去CR输出分别留存：`:68-84`。set -euo pipefail、EXIT 原状态以及工作流 EXIT 显式 exit 保留失败，部分证据不能构成整体成功：`:2,7-13`、`android.yml:123-142`。
- CI 在同 job 固定完整 SHA checkout 基线、persist-credentials=false、核对 wrapper、先构建旧 APK，再跑原 smoke，最后升级；notClass 仅排除升级类：`.github/workflows/android.yml:54-65,113-142`。独立检查原 smoke 的清库 Rule 属于该类，未影响升级 fixture：`LedgerSmokeTest.kt:22-27`。always/14天产物包含升级证据，read权限和固定 action 保留：`android.yml:8-9,144-157`。35分钟预算明确用于额外基线构建，实际成本仍需 CI 记录。

## Strengths

- 兼容性检查同时覆盖安装身份、公开签名、SQLite版本、实际持久化文件、全部业务字段与跨月整数统计，未用内联 BuildConfig、重新 seed 或升级后的实际值作为预期。
- 脚本对 ADB 返回0但 JUnit失败、缺结果、崩溃、测试数/完成码错误有独立门禁；保留 CRLF 原件、规范结果和原退出码，有利于失败诊断。
- 主 app 的变化限于版本和容量说明，原六项设备测试未削弱，JSON边界回归能区分错误来源。

## 真实证据核对

1. 本次直接解析 `task1-green-baseline/TEST-*.xml` 与 `task1-final-jvm/TEST-*.xml`：两组各 36 tests，0 failures/errors/skipped；BackupCodecTest 为12项。读取 lint XML 为8项 GradleDependency + 1项 UseKtx Warning，无 Error。读取 `build-20261004-225413.log:77,106` 确认实际测试任务及构建成功；`build-20261004-230035.log:37,86,100` 显示 JVM UP-TO-DATE、新 fixture 编译及构建成功。没有将后者声称为新一轮 JVM 执行。
2. 已读完整合成 `gate-probe.sh`、`gate-probe-results.log:1-13` 和 Bash 语法日志：LF/CRLF通过；JUnit失败、缺结果、崩溃混成功、两测试、重复/错误completion均拒绝；安装失败23原码保留，证书/多设备/非QEMU拒绝。探针是 fake ADB/aapt/apksigner，对真实脚本控制流有意义，但不能证明真实设备升级。
3. 已读 `audit-signature-candidate.txt:1-10` 与 `audit-signature-v010.txt:1-10`：两份实际 APK 的 apksigner 原输出均 Verifies、v2=true、单签名，公开证书SHA256同为 `209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`。未读取密钥。
4. 本次独立重新计算冻结候选和 `output/an-jizhang-0.1.1-debug.apk` 的 SHA256，均为 `81A92BF36F1867CAB1AA2DF9B318B5D42E18E16DD65EEB864209F6BD0E8D7448`。两者完全相同，可沿用冻结候选审计。已读原权限输出只有 signature 自定义接收器权限、无联网/广泛存储权限；许可证三方逐文件摘要匹配见 `audit-licenses.json`。APK内备份规则等额外静态检查来自独立 `apk-audit.md:17-34`，本次没有重复全部 APK 工具检查。

## Issues

### Critical (Must Fix)

无。

### Important (Should Fix)

无新增实现问题。以下未完成交付门禁不是已验证通过的声明。

### Minor (Nice to Have)

无本次变更新引入且值得阻挡交付的问题。已有固定依赖/SDK XML兼容提示和9项lint Warning 已如实记录，不建议为消除提示扩大本轮范围。

## 未验证项与交付门禁

- 本次审查未读取真实 CI `37211853745` 的最终状态、XML、安装/证书/runner/launch 证据。主 agent 必须确认36 JVM、原6 smoke、seed1、verify1均成功且无跳过；实际 APK 旧目标进程类加载、升级连续性和35分钟预算不能由编译/探针/静态审查推定。
- 当前 `docs/VALIDATION_V011.md:21-29` 仍写升级待执行、审查进行中，`docs/HANDOFF.md:5-7` 表明实现中；README 仍以已交付0.1.0为主。这与审查时尚未核对真实CI一致，不视为虚报。最终交付前应基于真实证据同步 README、HANDOFF、VALIDATION 和计划勾选，清楚标注旧版历史数据，不留下误导性当前状态。
- `docs/PHONE_ACCEPTANCE_V011.md:3-14` 正确区分旧版作者已验收与新版待确认，先备份、同签名直接覆盖、拒绝卸载解决签名问题；新版项目均未预勾。真实作者手机升级验收保持待确认。
- 远端 private、远端HEAD/干净工作区、草稿PR与附着、旧APK最终保留路径由主 agent 完成。静态审查不证明这些交付动作。

## Assessment

**Spec gate: Approved（实现范围）。Quality gate: Approved。**

**Ready to merge? With pending validation — 完成真实 CI 证据核对与最终文档同步后可合并；当前不能凭本报告声称0.1.1全部交付完成。**

理由：指定全分支代码满足范围、兼容性和失败门禁要求，原始本机证据及公开签名与交付哈希一致，未发现 Critical/Important。真实设备升级结果与最终交付状态仍须独立补齐；本报告不会以静态批准代替实际执行。

## 首次真实CI失败后的最小修复增量审查

Spec: Approved；Quality: Approved（增量 Base 6ed67e5 → Head 1971199，唯一 tracked 修复为 verify-upgrade.sh:74 的 `-w` → `-w -r`）。
根因确认：真实 seed 文本有旧版/持久化字段及 `OK (1 test)`，但无 completion，error 为 missing completion；属于输出协议不匹配，不是账目断言失败（ci-37211853745/android-device/upgrade-artifacts/seed-instrumentation.txt:3-11；error.txt:1）。
官方依据：Android 官方文档确认 `-r` 输出 raw，补充该参数与现有严格协议门禁一致（https://developer.android.com/studio/test/command-line#am-instrument-flags）。
修复覆盖：seed/verify 共用 run_method，两个调用均取得 `-r`；OK1、唯一 completion=-1、失败/崩溃拒绝条件原样保留（verify-upgrade.sh:79-84,89,93）；diff 之外的签名/设备/原退出码/超时约束没有改动。
有意义 RED/14 GREEN：mock 按实际 `-r` 参数选择 raw/真实 pretty 文本，两成功案例分别断言精确 seed/verify；新增有 `-r` 却缺 completion 的负例仍退出1且禁止新版安装/verify，安装失败原退出23保留（task-2-probes/gate-probe.sh:43-51,73-101；raw-format-red.log；raw-format-green-14.log:1-14；对应 exit.txt 为1/0）。
历史纠正：保留 task-2-review.md 首次审批；首次审查漏掉“调用未请求 raw 而 mock 始终提供 raw”的 Important 协议风险，真实 CI 暴露后已由本次最小修复及参数敏感探针补齐，不能将首次审批表述为真实升级成功。
当前严重项：Critical 无；Important 无未解决项；Minor 无新增项；首次审查列出的遗留工具/lint 噪声不受本次修复影响。
Cannot verify：36 JVM/6 UI 为主 agent 已实读结果，本轮直接实读 seed 通过与脚本随后失败；新包替换、verify 及完整真实修复 CI 仍待执行，14 mock 不能证明数据库升级通过。
检查边界：唯一 diff 只读一遍，实读指定报告/探针/真实失败输出及官方参数文档；未重跑测试或 Git 命令，仅新增本审查报告，未改源码、Git 状态或主 agent 文档。


## 最终真实CI与交付文档独立核对

# 0.1.1 最终交付证据与文档独立核对

代码/测试提交：`19711990718adb5bee717db64e8aa76d2ccfbb09`。真实运行：`37213110853`。沿用此前全分支及1971199修复增量审查，本次直接核对原始CI产物、当前交付文档及本地APK，不仅接受主agent摘要。只新增本报告，未修改源码/Git/交付文档，未重新构建、运行测试或设备操作。

## Gates

- **Spec: Approved。** 原范围、六项JSON边界回归、原六项smoke、精确旧/新版本升级与完整数据保留均有实现及真实证据。
- **Quality: Approved。** 1971199只补runner原始输出参数，严格失败门禁保留；真实seed和verify完成码补齐，首次协议缺陷已闭环。
- **Delivery documents: Approved。** 前次预检的状态矛盾、0.1.0历史混淆及修复行号问题已改；当前技术成功、作者手机未验收、最终版本控制收尾分开记录。
- **Critical 0 / Important 0 / Minor 0 新问题。** 既有9项lint提示继续如实保留。

## 独立读取的真实证据

证据根：`test-artifacts/ci-37213110853/`。

1. `run.json`显示headSha完整匹配1971199，run completed/success，build与device-smoke两个job均success；从15:28:07Z到15:39:09Z，设备job约11分钟，未触及35分钟预算。
2. 直接解析 `android-build/test-results/testDebugUnitTest/TEST-*.xml`，6份suite和实际testcase数量一致：BackupCodec12、Database5、ViewModel11、MainActivity1、Money5、Summary2，合计36，failure/error/skipped均0。直接解析 `android-build/reports/lint-results-debug.xml`，8项GradleDependency、1项UseKtx Warning，无Error。
3. 直接读取 `android-device/app/build/outputs/androidTest-results/connected/debug/TEST-emulator-5554 - 15-_app-.xml`，该目录仅一份XML，6个原LedgerSmokeTest方法全部通过，failure/error/skipped均0；包括emptyLedgerAndNavigation、输入重建、保存/删除运行中重建、失败保留输入、自定义分类和0.01金额行为。页面新增断言位于已审代码，未更改或跳过。
4. 直接读取 `android-device/upgrade-artifacts/seed-instrumentation-raw.txt`及normalized文件：class和test明确为seedOldVersionLedger，numtests=1；真实安装versionName0.1.0/code1、debuggable=true；schema1，seed后重新打开的3条完整Entry与硬编码预期一致；恰一个OK (1 test)和最终INSTRUMENTATION_CODE: -1。
5. 直接读取verify的raw及normalized文件：仅verifyUpgradedLedger，numtests=1；真实安装0.1.1/code2；upgrade-opened和upgrade-reopened都为schema1且同3条Entry。包含工资收入10000分、10月2日自定义分类1分支出、9月30日交通1234分支出；中文、引号、真实换行备注保留。10月10000/1/9999分及分类自定义分类=1，9月0/1234/-1234分及交通=1234均一致；恰一个OK (1 test)和最终-1，无failure/crash/skipped状态。重新定点读取 `LedgerUpgradeTest.kt:37-41,58-77`，verify仍只读与重新打开，未重新播种。
6. 已读baseline/current/test三份certificate文件，均仅同一公开SHA256 signer `41d7b6082546c6ab0cde5bc90543e6e281ece527320ede1ee50e9166d51f3505`；certificate-comparison与之一致。三份badging首行分别为app1/0.1.0、app2/0.1.1、独立test包。CI设备证据为唯一emulator-5554、qemu=1、ci-ledger。
7. baseline/test安装、current-install raw及normalized均Success；脚本`:92`确为install -r，新版runner实际运行在此之后。launch raw及normalized为MainActivity、COLD、Status: ok。result.txt为Upgrade validation exit: 0，summary.txt为真实1→2保留成功；`run.log:774,777,813,816,824-826`确认两阶段OK/-1以及最终Upgrade和Device validation实际退出0，不是仅打印的shell脚本模板。
8. 最后读取jvm/ui/upgrade-summary.json，其36/6/1/1、0失败/错误/跳过、run/head/cert结论与上述独立原始读数一致。本次未重复查看截图；`VALIDATION_V011.md:53`明确为主agent查看，与本报告的独立字段/启动证据不混同。

## 本地交付与文档

本次独立Get-FileHash：

| 文件 | SHA256 |
|---|---|
| `D:\AIproject\an-jizhang\output\an-jizhang-0.1.0-debug.apk` | `6FB6BFEBF726DA15D4FA9756D3917253173E2E444710CAACA62B7F677E748280` |
| `D:\AIproject\an-jizhang\output\an-jizhang-0.1.1-debug.apk` | `81A92BF36F1867CAB1AA2DF9B318B5D42E18E16DD65EEB864209F6BD0E8D7448` |
| 当前隔离worktree `output/an-jizhang-0.1.1-debug.apk` | `81A92BF36F1867CAB1AA2DF9B318B5D42E18E16DD65EEB864209F6BD0E8D7448` |

原项目SHA256SUMS保留新旧两行且一致；隔离worktree新包校验行一致。原包存在、内容哈希未变。沿用先前公开证书审计，本机旧/新包为209fe650...109d，与CI的41d7b608...3505明确不同，文档未混淆。没有读取密钥或重复验签工具。

- `README.md:14-20`、`PHONE_ACCEPTANCE_V011.md:3-14`明确先在旧版导出备份，再以本机0.1.1直接覆盖、确认数据保留；旧包保留，签名不符保留原版，不使用卸载解决。作者手机六项仍全部未勾。
- `HANDOFF.md:5-7`当前0.1.1技术成功与首次失败/修复/新成功运行一致；`:9,19`标0.1.0历史，旧验收与JSON回归过时句已纠正。
- `VALIDATION_V011.md:23,41,47-55`保留首次失败、1971199最小修复、参数敏感RED/14 GREEN和最终真实CI完整链；`:29,51`分别给出本机与CI公开证书，生产APK来源69acd94与后续测试/CI变更分明。本轮文档没有把mock、首次seed或0.1.0作者验收当成完整0.1.1升级。
- `DEVELOPMENT.md:41-50`、`REUSE_V011.md:9,15`及计划示例使用-w -r，smoke排除升级fixture、临时设备和连续升级约束一致。
- `REVIEW_V011.md`顶部明确6ed为历史快照并承认Task2及全分支原审查遗漏；增量节行号74、79-84、89/93已改。建议交付时追加或链接本次最终证据结论，使读者从历史审查直接到达已闭环状态；无需改写历史报告。
- 计划Task2 Step1-5勾选与证据一致；Step6未勾合理，最终提交/push、private、远端HEAD和干净工作区的收尾仍由主agent随后核验。本次不预先断言那些动作完成，也不要求提前勾选。

本次已消除代码、设备验证和交付文档的审查阻塞；最终远端收尾及作者手机验收保持各自明确边界。
