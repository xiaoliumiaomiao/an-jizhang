# 0.1.6 验证记录

记录日期：2026-10-08。候选版本 `0.1.6 / versionCode 8`，开发目录 `C:\Users\13765\.codex\worktrees\ledger-v0-1-1\an-jizhang`，分支 `codex/ledger-statistics-v0.1.5`，基础 HEAD `0cfbb8ec79c882cefc68615152e2ddde73c5b2e5`。本轮源码是未提交的工作区快照，不能以基础提交替代受测快照。

本轮实际技术验证已完成：118 JVM全0、lint0E9W、最终完整64 UI全0及同签名0.1.4→0.1.6覆盖升级seed／verify各1通过。19张新鲜原图已归档，两处42个新交付读回一致；独立Runtime／Upgrade及19图Visual已Approved，C0/I0/M0；本轮未提交技术FullDelivery独立Approved，C0/I0/M0，见[最终交付审查](../test-artifacts/ui-v016/final-full-delivery-review.md)。本记录不代表作者手机验收。

## 变更及数据边界

- 月、年统计增加收入／支出分类比例条；总额、占比和准确分类钻取继续使用完整所选期间与本收支类型的统计范围。比例保留文字说明，不只依靠颜色。
- 导出、恢复操作失败与统计读取失败分开处理，操作失败后仍可使用已有统计和分类钻取。保存已成功但后续实际重新读取失败时，不再误承诺旧账目未被替换；保留旧内存快照、读取错误状态与重试读取入口。
- 数据库 `schema 1`、备份 `format 1`、现有依赖与权限边界保持；不实施 S3 分类库、预算或迁移。版本候选由 0.1.4／code 6 覆盖至 0.1.6／code 8。

## 当前验证结果

| 项目 | 本轮实际结果 | 原件 |
|---|---|---|
| 候选构建 | 成功，31 秒；82 tasks，20 executed、62 up-to-date，非 clean 全重建 | [构建日志](../test-artifacts/ui-v016/candidate-build/build-20261008-115833.log) |
| JVM | 14 份 XML，118 tests／0 failures／0 errors／0 skipped；旧 111 项缺失 0，新增 7 项 | [汇总](../test-artifacts/ui-v016/candidate-build/jvm-summary.json) |
| Android lint | 0 errors／9 warnings；8 项 GradleDependency、1 项 UseKtx | 同次候选构建及 candidate-build 归档 |
| 原生包审计 | Native Approved，Critical 0／Important 0／Minor 0；产品 APK 身份、签名、权限、备份排除、许可证与依赖核验 | [审计报告](../test-artifacts/ui-v016/native-audit/report.md) |
| 源码审查 | 最终产品源码、三项 test-only 修正及最新两项就绪修正均 Approved，C0／I0／M0；绑定下述各自快照 | [源码报告](../test-artifacts/ui-v016/final-source-review.md)、[三项增量](../test-artifacts/ui-v016/incremental-test-source-review.md)、[最新就绪增量](../test-artifacts/ui-v016/readiness-test-source-review.md) |
| 第一套完整设备验证 | 64／2F／0E／0S，旧 55 项缺失 0；反馈时钟测试与 Smoke 旧版本断言失败 | [run 1](../test-artifacts/ui-v016/device-run1-failed/summary.json) |
| 三项 test-only 修正局部验证 | 3／0F／0E／0S；反馈消失、严格版本导航、月度比例条 | [局部 3 项](../test-artifacts/ui-v016/device-local-green/summary.json) |
| 第二套完整设备验证 | INCOMPLETE；最后观测 45／64、0F，宿主会话中断及设备 UiAutomation DeadObjectException／instrumentation crash，无最终 XML 或 Gradle 结论 | [run 2](../test-artifacts/ui-v016/device-run2-interrupted/summary.json) |
| 第三套完整设备验证 | 64／2F／0E／0S；分类字号配置重建与 LargeText 备注节点失败，分别诊断 | [run 3](../test-artifacts/ui-v016/device-run3-failed/summary.json) |
| 两项测试就绪修正局部验证 | 4／0F／0E／0S；79 秒，67 tasks，5 executed、62 up-to-date | [局部 4 项](../test-artifacts/ui-v016/device-readiness-local-green/summary.json) |
| 最终完整设备验证 | 64／0F／0E／0S，旧55项逐名missing0、新9项全部在；9m55，67 tasks，1 executed、66 up-to-date | [最终汇总](../test-artifacts/ui-v016/device-final-green/summary.json)、[完整XML与runner](../test-artifacts/ui-v016/device-final-green/results/)、[日志](../test-artifacts/ui-v016/device-final-green/build.log) |
| 覆盖升级 | 同签名0.1.4/code6→0.1.6/code8正常替换；seed1／verify1均唯一OK与completion=-1，schema1、三笔每字段和跨月汇总保留、启动ok | [升级汇总](../test-artifacts/ui-v016/upgrade-local/summary.json)、[原件](../test-artifacts/ui-v016/upgrade-local/) |
| 最终视觉／交付 | 19张本轮原图Visual Approved C0/I0/M0，42个新副本hash一致；FullDelivery独立Approved C0/I0/M0 | [原图清单](../test-artifacts/ui-v016/device-final-green/screens-manifest.json)、[交付读回](../test-artifacts/ui-v016/delivery.json) |

## 受测文件身份

- 产品 APK：9,838,189 bytes，SHA-256 `9F3FC66DA998A9E0003588DABD48FCC14B4C27D7BDEE3D18362A18859FF3C76A`。上述后续修正只改测试，产品 APK 保持此值。
- 三项 test-only 修正时的测试 APK：1,342,929 bytes，SHA-256 `55D0BD2B2F871A4881141F3A4B0B0B8CA8EAD4B746EC0277741FB39BD77B3BDB`，补充原生核验为 PASS，见 [final-test 汇总](../test-artifacts/ui-v016/native-audit/final-test/summary.json)。该轮 83 路径 manifest SHA-256 `769DF6A5C45E0BAB5DB79058C64BCFE0744B36DF8231768C7EDE9AD4B5030CB9`。
- 最新两项测试就绪修正后的测试 APK：1,348,879 bytes，SHA-256 `C22A60E21AF968648173A4845BACDB20E448DF8B10C5633FA168B96419C80777`。当前 [source-manifest](../test-artifacts/ui-v016/candidate-build/source-manifest.json) 83 路径，SHA-256 `0F9CD83523481AA0A6DF77C7F23EE4D54EC05606626EDE9B9528CA2317EEBD2D`；局部 4 项 XML SHA-256 `013332736F670DD58F301AFF0F02C6E6CEDF3E10C8A63E727F995B062F9BF9A8`。此快照已取得 [独立 Source Spec／Quality Approved，C0／I0／M0](../test-artifacts/ui-v016/readiness-test-source-review.md) 及 [Test Native Increment Approved，C0／I0／M0](../test-artifacts/ui-v016/native-audit/final-test-readiness/report.md)，不依靠继承 769DF 快照的结论。

旧 0.1.4、新产品及已审测试 APK 的公开签名证书 SHA-256 均为 `209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`。产品仅使用既有自身 signature 权限 `com.xiaoliumiaomiao.anjizhang.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，没有 Android 系统 uses-permission 或联网权限；已审测试 APK 使用 `android.permission.REORDER_TASKS`。不能表述为“绝对零权限”。`allowBackup=false`，旧／新 APK 的 legacy、云备份与设备迁移排除规则一致，root、file、database、sharedpref、external 均全域排除。三份产品许可证资产逐字节与源码一致；无新增依赖。

## 失败、修正与证据边界

新增设备测试和旧行为 APK 在实现前准备，但实际设备 RED 在产品实现后才执行：分类比例条与操作错误两原 runner 各 1／1F，保留 [device-red](../test-artifacts/ui-v016/device-red/) 及两张失败图。不能写成“设备 RED 后才实现”。早期 JVM 23／7F 含 3 项 SDK 28 fixture ClassCastException；修正 fixture 后为 23／5F 的 AssertionError。nullable String 编译失败、fixture 问题与产品行为 RED 分别保留在 Task 1／2 原件中。

首次 offline connected 运行因未缓存的固定 UTP 工具组件失败，没有执行 UI，见 [build-20261008-120319.log](../test-artifacts/build-20261008-120319.log)。补齐现有工具组件后才取得完整设备结果，未新增产品依赖。

反馈测试原单项复现 1／1F。该次观测显示虚拟时钟 2,672 ms 对应真实时间 5,260 ms，重建耗时 183 ms；失败不是“提示永久显示”的证据。修正只对齐测试时钟，保留真实 5,000 ms 上限、原 10,000 ms 等待预算、同一 ViewModel／消息 ID／expiry、数据库与导航断言，并断言不得早于原 expiry 消失，见 [诊断](../test-artifacts/ui-v016/feedback-failure-diagnosis.md)。另两项修正保留 Smoke 的严格 0.1.6 断言，以及分类标题定位和两条实际比例条可见断言。

第三套失败分别处理：分类配置 setup 中约 1.34 秒的自动 Activity 重建与显式 recreate 存在竞争，时间线和源码支持该推断；修正等待同一 RESUMED Activity 在两个真实帧保持配置、焦点与附着状态，保留原等待预算和 finally 恢复。LargeText 原第 47 行找不到备注节点；修正使用原键盘收起 helper、等待备注按钮可用并执行一次点击，等待原唯一备注字段，保留原金额、分类、200 字备注、保存、详情与恢复断言。两者均为 test-only，未固定 sleep、整场景重试或放宽业务断言，见 [配置就绪修正](../test-artifacts/ui-v016/category-config-readiness-fix.md)、[备注就绪修正](../test-artifacts/ui-v016/large-text-readiness-report.md)。局部 4 项通过仍不证明最终整套通过，也不把两次失败合并为同一原因。

## 本轮实际升级、截图和交付

实际0.1.4/code6旧包SHA 5F71A8BF1A9C1E617B6A87DD6852EE45C2E6CDF18B084DBF070381C34C656AF6；新产品9F3F与最终测试C22A包在唯一隔离API35 ci-ledger／emulator-5564执行。脚本及执行前hash护栏明确旧版install、测试install -t、seed、force-stop、新版install -r、verify、冷启动；seed与verify之间没有卸载、清库、降级、connected task或重新播种。升级原件记录实际包版本、3笔UUID／收支类型／整数分金额／分类／日期／含中文引号换行备注，重新开库两次均等于预期；10月结余9999分、9月-1234分。这里只验证合成账本，不声称访问或测试作者真实账本。05:13:15UTC汇总SHA 2E8679690245C2DB19FDB83D102A5C579FE7DB38F982CBE89DF2AB24833271F5；安装stdout仅报告Success，命令链由冻结脚本BC10A0AA…与实际执行记录交叉支持，不称为内核级命令审计。

最终完整UI受测时间05:00:30–05:10:26UTC，XML SHA 5588D909B5839A59296D6E6C8329D8FABD16F22FE2648B6FEAF9B3FCFEBF43A3，summary SHA 9DBF1204F84F04CDA1E8A20A2F0FB853C33556DF7D465074AB74FE54F1FF38AA；gradle日志原名build-20261008-130030.log，归档为device-final-green/build.log。118 JVM与该完整设备运行的产品源码／APK相同，后续变更仅测试；逐项83文件来源仍匹配0F9CD835…快照。

19张v016原PNG逐件hash且设备mtime均晚于正确完整UI起点1791435630，最早1791435653。清单旧起点受JSON自动日期转换丢失UTC偏移影响，误为-8h；原错误6B3E清单保留为pre-timezone-correction-screens-manifest.json，改从原始ISO UTC字符串解析后重新核全部19张，正确清单SHA AFB63D26B30B6570AFE6D1B0E5D20828A02700372F79081795C340ABFDE9E46B，原图字节不变。三指定字体场景各month/year/drill共9图：360dp、1-light／1.3-light／2-dark；其他10图继承本次模拟器360dp／font1.3／light状态，不能沿用首套411dp或未知字号的假设。

两处output已交付an-jizhang-0.1.6-debug.apk、SHA256SUMS-v0.1.6.txt和ui-v0.1.6/下19张原PNG，共42个新文件逐件读回一致；旧227个输出及两AGENTS原字节保持，HEAD和空暂存区未变。主目录源码仍0.1.2/code3，没有同步开发代码；新APK是独立候选产物。交付清单[delivery.json](../test-artifacts/ui-v016/delivery.json)写于批准前，仅记录副本／保护；现与独立[FullDelivery Approved C0/I0/M0](../test-artifacts/ui-v016/final-full-delivery-review.md)共同支持本轮未提交技术交付。[Runtime／Upgrade独立报告](../test-artifacts/ui-v016/final-runtime-upgrade-review.md)和[最终19图Visual](../test-artifacts/ui-v016/final-visual-review.md)均Approved、C0/I0/M0。四份文档、两处HANDOFF、42新副本和227旧输出经独立FullDelivery Approved、C0/I0/M0；本次仅写回实际批准状态，没有改动受测产品／测试源码或APK。

## 未验证项和下一步

本轮技术交付已通过；最终阶段执行[22项手机最终验收](PHONE_ACCEPTANCE_V016.md)及手机debug；全部未勾，设备／Android／原安装版本待记录。API35合成模拟器不证明API26实机、真实进程kill或完整TalkBack；本轮没有执行手机操作。S3分类库／月预算／schema2/format2由父会话决定，不在本轮实施；完整S3需独立迁移／旧备份兼容规格，保持现有功能则先完成整体验收。未提交、推送、合并或发布，不预设后续选择。
## 基线与历史，分别记录

2026-10-08 改前基线为 `0cfbb8e / 0.1.5 code 7` 加原交接文档改动：111 JVM／0F／0E／0S，lint 0E／9W，82 tasks 全 executed，构建成功，见 [基线日志](../test-artifacts/build-20261008-113154.log) 与 [基线原件](../test-artifacts/ui-v016/baseline-0cfbb8e/summary.json)；未重跑基线设备测试。

2026-10-07 的 111 JVM／55 UI、升级 seed／verify 各 1 项通过、CI `37626498602` attempt 1，以及作者 0.1.4 验收，均为历史结果。它们不替代 2026-10-08 的 118 JVM、64 UI 或 0.1.4→0.1.6 验证。

临时验证AVD已依据原启动父PID32704／子PID13540、实际headless路径及唯一ci-ledger身份停止，processExited=true；[回执](../test-artifacts/ui-v016/emulator-shutdown.json)。原件与临时SDK／AVD文件保留，无清理阻塞。
