<!-- final-r2-current-start -->
# 当前验证：0.1.7 r2（2026-10-10）

**技术 FullDelivery 已独立批准；作者手机验收待完成。** 以下当前段落覆盖下方10/08历史快照，旧正文逐字节保留。

- 核查日期：2026-10-10 16:16:12 +08:00（Asia/Shanghai）。实际开发目录：`C:\Users\13765\.codex\worktrees\ledger-v017-recovery\an-jizhang`；分支 `codex/ledger-v017-recovery`，基础 HEAD `0cfbb8ec79c882cefc68615152e2ddde73c5b2e5`，0.1.7/code9。实现未提交，暂存区为空；基础提交不等于此次受测工作区。
- 主目录：`D:\AIproject\an-jizhang`；仍为 main / `ff8479f75cd917bc49e5ab2173ad7e9906dc480c` / 0.1.2/code3 源码。只维护主目录交接并新增 output，不在旧源码上接续 .7。原 `ledger-v0-1-1` 残留目录完整保留。
- [受测源码清单](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/final-candidate-r2/validation-manifest.json) 90 文件 SHA-256：`8E4A62710D77B1F21EB630ECF63BADDADFF3C64086967E6C8E1DB6A470A27CC2`；当前源码／冻结源码一致。最终 app 9,671,333 bytes SHA-256：`008F75F2680EFDE3DAC0D2265BD623EBE52E0CD888AC630BCEAD9BEFF09730CA`；test 1,241,395 bytes SHA-256：`8F348EF5815082D772E299218492AAB9D7F36640721345A913A72763D4D6B5B0`。

| 本轮实际门禁 | 2026-10-10 结果 | 原始证据 |
|---|---|---|
| fresh 构建／JVM／lint | 15:43:12 起强制实际执行 82 tasks；130 JVM / 0F / 0E / 0S；lint 0E / 1W（原 UseKtx） | [构建日志](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/build-20261010-154312.log)、[冻结报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/final-candidate-r2/) |
| Modern 局部 | 15:43:59–15:45:47，5 / 0F / 0E / 0S，23 原 PNG | [状态及原件](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/modern-final-r2/state.json) |
| 同 fixture 前后 | .6/code8 与最终 .7/code9 各 1 实际 runner、各 7 原 PNG；同最终 test APK／26 笔 fixture／2025-05 和全年2025／720×1280／density320／font1／light | [before](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/comparison-final-before-r2/state.json)、[after](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/comparison-final-after-r2/state.json) |
| 完整常规 UI | 15:46:54–15:58:31，70 / 0F / 0E / 0S，70 唯一方法、108 原 PNG；旧64保留、新6全部执行 | [唯一 fresh XML 与 runner](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/full-ui-final-r2/state.json) |
| .6→.7 覆盖 | 15:58:31–15:58:36；真实同签名 -r，seed1／verify1，VERIFIED | [原件](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/upgrade-016-final-r2/state.json) |
| .4→.7 覆盖 | 15:58:37–15:58:43；独立真实同签名 -r，seed1／verify1，VERIFIED | [原件](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/upgrade-014-final-r2/state.json) |
| 系统 IME／Back 补充 | 两链结束后，最终已安装 APK 同 SHA；真实打开系统备注键盘、输入合成备注、返回收起键盘且编辑器／备注保留；随后取消并放弃，未保存 | [IME 原图](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/supplement-ui-note-typed/note-typed.png)、[Back 原图](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/supplement-ui-note-back/note-back.png)、[放弃草稿](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/supplement-ui-draft-discard/state.json) |

完整UI唯一XML SHA-256 `FFB9AA48CE7CBA0CAD621B5BD7B6499F6EC2AE75BBEC9DBF285675EAE72EBF28`。两个升级都核对旧3条全部UUID／类型／分／分类／日期／含引号与真实换行的备注、跨月精确金额、schema1、两次重开、备份规则和真实冷启动；seed后无reset／卸载／重播种／普通suite插入。

| 独立范围 | r2 结论与原件 |
|---|---|
| Source | Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/source-native-review/source-review-r2.md) |
| Native | Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/source-native-review/native-review-r2.md) |
| 五文件最小修复 | Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/source-native-review/regression-fix-task-review-r2.md) |
| Runtime／Upgrade | 各 Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/delivery-review/runtime-upgrade-final-r2-v1.md) |
| Visual | Approved C0/I0/M2，23 Modern＋14配对＋完整108逐张分工原图＋真实IME/Back；[主报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/visual-review/report-r2.md)、[另78原图](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/full-ui-visual-review/full70-non-v017-visual-review-r2.md) |

[FullDelivery 独立原件](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/delivery-review/full-delivery-final-r2-v1.md)，批准范围为此次 r2 技术交付，C0 / I0 / M2；两项 Minor 是既有首屏密度与 2.0 字号年度 12 轴换行，均不阻断。

交付：[主目录最终 APK](D:/AIproject/an-jizhang/output/an-jizhang-0.1.7-debug.apk)、[SHA256SUMS](D:/AIproject/an-jizhang/output/SHA256SUMS-v0.1.7.txt)、[147原图及元数据](D:/AIproject/an-jizhang/output/ui-v0.1.7/)。活跃开发目录 output 有逐字节相同副本；产品 APK 为最终008F，测试 APK不交给手机安装。公开debug证书 SHA-256 `209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`，与旧.4/.6一致。[两处副本校验](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/delivery-copy-verification-r2.json)；旧269原交付和恢复的134副本完整保留。

## 保留的失败与修复依据

本轮初始Modern真实5/5F；精确滚动后5/2F，期间展开状态修复后5/0F。真实native palette／暗色Dialog状态图标RED后修复，最终Modern原5方法全部GREEN；断言、48dp点击预算和原金额字段合同保留。

第一完整70真实8F/0E/0S已保留在 full-ui-final：5个2px浮点比例边界、2个可见下钻采样排序、1个旧S1背景期待。仅4份测试／helper修正，像素容差仍2px、精确排序／fixture不变； focused8实际GREEN。新增不滚动四日期raw几何断言实际RED（1日底1081>视口1072），随后唯一产品修复为月Canvas156→144dp，年图不变；最终r2完整70GREEN。[最小差异和原件](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/regression-fix/)。月日期裁切I1已由真实after原图和独审关闭。

15:41构建成功但lintReport UP-TO-DATE使证据新鲜度守卫拒绝，未冻结，不归为产品测试失败；15:43强制重跑取得全部新原件。历史10/08测试、首候选或首full失败不替代最终r2结论。

## 手机与未验证边界

作者手机30项全部未勾。API26实机、完整TalkBack、真实进程kill及作者对新版审美的接受仍未验证；模拟器／旋转／semantics不替代这些验收。IME补图中的金额为空，只证明备注与Back视觉，不扩大为金额持久的视觉证据。

已知非阻断视觉 Minor 2 项：明细首屏内容较疏；系统2.0字号下年度最右“12”换为上下“1”“2”，文字未丢失，准确每月金额列表可用。lint唯一 UseKtx 为原 SQLite 事务建议；构建另有两处 quadraticBezierTo 弃用提示。未据此引入依赖或改事务。

SQLite schema1／JSON format1／CNY Long整数分／稳定ID／完整字段／事务合并与重复跳过／冲突整批拒绝／50,000条和10MiB限制保持；无INTERNET／遥测／云同步新增。系统备份和设备迁移全域排除、固定依赖与许可资产保持。app自身signature权限与test REORDER_TASKS分别核验，不称绝对零权限。

未提交、推送、合并或发布；未实施S3分类库／预算／schema2。下一步由作者在真实手机完成30项：先在现有应用导出可信备份，再正常覆盖安装本次同签名debug APK。安装异常保留原应用和账本，停止覆盖；不卸载、清库、强降级。完整原备份与新隔离开发目录是回退文件边界，模拟器测试不代替手机备份。
<!-- final-r2-current-end -->

# 0.1.7 验证记录

记录日期：2026-10-08（UTC）。候选 `0.1.7 / versionCode 9`，开发目录 `C:\Users\13765\.codex\worktrees\ledger-v0-1-1\an-jizhang`，分支 `codex/ledger-statistics-v0.1.5`，基础 HEAD `0cfbb8ec79c882cefc68615152e2ddde73c5b2e5`。暂存区为空，本轮为未提交工作区；受测对象必须绑定源码 manifest 与 APK，不能以基础提交或旧 0.1.6 结论替代。

当前只有首候选本地构建和图表 domain 测试已有成功原件。首候选源码审查发现收入环图前两分类同色（I1）及月刻度位置失真（M1），owner 已写修复；最终重建及复审待完成。所有本轮模拟器、覆盖升级、before／after 原图、最终独审与交付尚待执行或核验，不授予技术 Full Delivery，也不代表作者接受新版审美。

## 本轮范围与不变合同

实现方向为原创 Compose Canvas 的月每日收支趋势、年 12 月双柱、同类型分类环图及完整横条列表，统计图表前置；明细、统计、记一笔／编辑、设置使用统一浅深 token。环图前 5 分类之外仅作“其余（N类）”视觉汇总，不具分类键或下钻动作；完整列表保持逐类准确下钻，用户真实“其他”仍是普通类别。

Long 整数分、稳定 UUID／全部 Entry 字段、准确日期／分类范围、operation ID、反馈 expiry、失败草稿、IME／Back、明细查询与统计分离、操作错误与读取错误分离均保留。SQLite schema 1、JSON format 1、固定依赖、离线及既有权限／系统备份排除边界不变；不实施 S3 分类库、预算或迁移。详情见 [计划](superpowers/plans/2026-10-08-modern-ui-v017.md) 与 [复用记录](REUSE_V017.md)。

## 已执行：首候选构建，不是最终修复版本

| 项目 | 原始结果 | 证据 |
|---|---|---|
| 构建时间 | 2026-10-08 06:37:06.9439865→06:37:45.2103628 UTC，exit 0；Gradle 报 37 秒，82 tasks，24 executed／58 up-to-date，非 clean 全重建 | [state.json](../test-artifacts/ui-v017/candidate-build/state.json)、[build.log](../test-artifacts/ui-v017/candidate-build/build.log) |
| JVM | 15 份 XML，130 tests／0 failures／0 errors／0 skipped；旧 118 项逐名缺失 0，新增 ChartSeries 12 项均在 | [unit-results](../test-artifacts/ui-v017/candidate-build/unit-results/) |
| Android lint | 0 errors／10 warnings：GradleDependency 8、UseKtx 1、ModifierParameter 1 | [lint.xml](../test-artifacts/ui-v017/candidate-build/lint.xml) |
| 产品／测试 APK | 两包成功；以下哈希只绑定首候选 | 同目录冻结 APK 和 state |
| 首候选 Native 合同检查 | 此精确两包检查通过，22 个官方 native tool 命令 exit 0；不含图表颜色／刻度，未授予最终交付批准 | [首候选检查状态](../test-artifacts/ui-v017/native-audit/initial-candidate-status.md) |

首候选身份：

- [源码 manifest](../test-artifacts/ui-v017/candidate-build/source-manifest.json) SHA-256：`C9E24FAF224103B22006AEECDB84413871E890DC8D035178AAA5FC94CD8F9A2C`。
- 产品 APK：9,671,109 bytes；SHA-256 `F20A788A69EF235DDF963393A9AB210B4EC6A8DE064774EEB62DE5979D66116F`。
- 测试 APK：1,362,423 bytes；SHA-256 `128534B7F4AF8CB5644D7505FE0F4190F28B321FA47B20339B2F871856A4F111`。

这批源码与 APK 在呈现修复前冻结，不能标记为最终包。待归档路径 `test-artifacts/ui-v017/candidate-build-final/` 仅表示后续计划，当前不提供不存在的下载或结果链接。最终构建需重新绑定源码、两包、JVM 与 lint；本轮 ModifierParameter 修正不得未经最终 lint 就写“警告已消失”。

## 行为 RED、GREEN 与审查修正

[chart-red](../test-artifacts/ui-v017/chart-red/summary.json) 为 2026-10-08 06:18:51.3800236 UTC 实际执行的 12 tests／12 failures／0 errors／0 skipped；12 个失败均为 `java.lang.AssertionError`，针对实现前可编译的 ChartSeries／图表骨架，不是编译或 fixture 异常。原 XML、日志与旧骨架均保留。同一 12 项在首候选 130 JVM 中通过；实际受测身份以上述 manifest 为准，不把 RED 摘要的基础 HEAD 当完整受测源码。

12 项覆盖每日／全年桶、月份与闰日／年界、完整期间、空数据、输入不变／输出快照、非法记录、金额及笔数溢出、MAX／负结余、有限绘制比例、前 5＋精确其余、空／单分类／真实“其他”等合同。后续只将视觉汇总标签“其他”改为“其余”以免混淆真实分类，金额、零值、Long 和下钻边界断言未放宽；最终测试仍须重跑。

首审 I1／M1 的修复限于 StatisticsCharts：第二分类改为可区分的独立浅深颜色；月标签与 Canvas 共用真实日坐标，按实际标签宽度处理边缘；modifier 调至第一个 optional 参数。原回调、semantics／testTag、准确金额与 12 项业务断言保持，见 [chart-report](../test-artifacts/ui-v017/chart-report.md)。最终独立 Source 复核与设备像素／截图待完成。编辑页自检证据见 [editor-report](../test-artifacts/ui-v017/editor-report.md)，自检不等于运行通过或独审批准。

## 设备、升级及视觉：待执行的明确合同

| 门禁 | 必须得到的本轮证据 | 当前状态 |
|---|---|---|
| 完整模拟器 UI | 70 项：旧 64＋Comparison 1＋Modern 5；原／新用例逐名在，0F／0E／0S，原始 runner、唯一 fresh XML、日志和设备身份 | 待执行／待最终原件 |
| 0.1.6／code 8→0.1.7／code 9 | 同公开证书正常覆盖；seed 1、verify 1，schema 1／全部字段／跨月金额和冷启动保留 | 待执行 |
| 0.1.4／code 6→0.1.7／code 9 | 独立旧版链，同签名 `install -r`；seed 1、verify 1，不借用上一轮结果 | 待执行 |
| 同 fixture before／after | 0.1.6 与最终 0.1.7 使用同一最终测试 APK；各一个 Comparison runner／各 7 张原始 PNG，四页与月年统计视口配对 | 待执行 |
| 现代图表与编辑边界 | 新 Modern 5 项及旧回归；font 1／1.3／2、深色／窄屏、MAX／长分类／200 字备注、零空／单点／负结余、真实 IME／Back、精确类别及比例语义 | 待执行 |
| 独立最终审查 | Source、Native、Runtime／Upgrade、Visual 与 FullDelivery，分别绑定最终实物和原件 | 待完成 |

比较 fixture 为 26 笔合成账目，期间 2025-05／全年 2025；五月收入 ¥21,950.00（2,195,000 分）、支出 ¥5,402.00（540,200 分）、结余 ¥16,548.00（1,654,800 分）。配置固定 360dp／720×1280、density 320、font 1.0、light；七幅取景为明细、月总览、月分类、年总览、年分类、编辑金额盘、设置。必须保存 fixture SHA、准确期间／金额、配置与原 PNG 哈希，不能把测试源码里的期望值写成已取得截图证据。

脚本准备已归档为 [PREPARED_NOT_EXECUTED](../test-artifacts/ui-v017/script-prep-report.md)。设备只允许经 QEMU、ci-ledger／API35、独占锁和唯一设备守卫确认的 `emulator-5564`，不接触手机。空合成库准备仅可发生在升级 seed 之前；seed 开始后不得 uninstall／clear／reset／重新播种或插入会清 fixture 的完整 UI suite。before 不通过 connected 偷装 .7；旧已装高版本不得为取 before 强降级。局部通过或脚本 AST 检查不替代完整 runner。

## 交付保护、手机及历史

原两处 output 共 269 文件及作者文档冻结见 [baseline-protection.json](../test-artifacts/ui-v017/baseline-protection.json)。两处 HANDOFF 新 v017 顶部下的原完整 .6／历史正文按字节保留，见 [handoff-state.json](../test-artifacts/ui-v017/handoff-state.json)；这是开始时保护证据，最终仍需重算并核对。当前没有本轮最终 APK／原图复制或 FullDelivery 批准结果，不提交、推送、合并或发布。

手机与作者审美验收见 [30 项清单](PHONE_ACCEPTANCE_V017.md)，全部待确认，安排在本轮 UI、升级及技术核验后的最终阶段。API35 合成结果即使后续通过，也不自动证明作者手机、API26 实机、完整 TalkBack 或真实进程 kill。

2026-10-08 的 0.1.6／118 JVM／64 UI、0.1.4→0.1.6 升级与技术 Approved 为上一轮 [历史依据](VALIDATION_V016.md)；10 月 7 日的 111／55 和作者 0.1.4 验收更早。均不替代本轮 130 JVM、70 UI、两条 .7 升级链或新审美接受。
