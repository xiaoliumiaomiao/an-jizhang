<!-- final-r2-current-start -->
# 当前独立审查：0.1.7 r2（2026-10-10）

**技术 FullDelivery 已独立批准；作者手机验收待完成。** 下方10/08内容完整保留为历史，不再作为当前结论。

- 核查日期：2026-10-10 16:16:12 +08:00（Asia/Shanghai）。实际开发目录：`C:\Users\13765\.codex\worktrees\ledger-v017-recovery\an-jizhang`；分支 `codex/ledger-v017-recovery`，基础 HEAD `0cfbb8ec79c882cefc68615152e2ddde73c5b2e5`，0.1.7/code9。实现未提交，暂存区为空；基础提交不等于此次受测工作区。
- 主目录：`D:\AIproject\an-jizhang`；仍为 main / `ff8479f75cd917bc49e5ab2173ad7e9906dc480c` / 0.1.2/code3 源码。只维护主目录交接并新增 output，不在旧源码上接续 .7。原 `ledger-v0-1-1` 残留目录完整保留。
- [受测源码清单](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/final-candidate-r2/validation-manifest.json) 90 文件 SHA-256：`8E4A62710D77B1F21EB630ECF63BADDADFF3C64086967E6C8E1DB6A470A27CC2`；当前源码／冻结源码一致。最终 app 9,671,333 bytes SHA-256：`008F75F2680EFDE3DAC0D2265BD623EBE52E0CD888AC630BCEAD9BEFF09730CA`；test 1,241,395 bytes SHA-256：`8F348EF5815082D772E299218492AAB9D7F36640721345A913A72763D4D6B5B0`。

审查覆盖完整0cf基底以来的.6和.7未提交分支、tracked及untracked产品／测试／CI。Source reviewer未实施此轮五文件修复；视觉分工两人各自实际view_image(original)，108完整PNG中30个v017与78个非v017均逐张实看，不能用来源hash替代看图。原130JVM、完整70方法及两升级逐名／原raw independently核验。

| 独立范围 | r2 结论与原件 |
|---|---|
| Source | Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/source-native-review/source-review-r2.md) |
| Native | Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/source-native-review/native-review-r2.md) |
| 五文件最小修复 | Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/source-native-review/regression-fix-task-review-r2.md) |
| Runtime／Upgrade | 各 Approved C0/I0/M0，[报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/delivery-review/runtime-upgrade-final-r2-v1.md) |
| Visual | Approved C0/I0/M2，23 Modern＋14配对＋完整108逐张分工原图＋真实IME/Back；[主报告](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/visual-review/report-r2.md)、[另78原图](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/full-ui-visual-review/full70-non-v017-visual-review-r2.md) |

[FullDelivery 独立原件](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/delivery-review/full-delivery-final-r2-v1.md)，批准范围为此次 r2 技术交付，C0 / I0 / M2；两项 Minor 是既有首屏密度与 2.0 字号年度 12 轴换行，均不阻断。

Source最终报告hash `39C7F73AB8DAC7447FD62361EE4817F42C2B7187106C37437F62A36B4ED518BB`；Native `31E676D92684318E1B8BF0EF9F00F9E9E83B831FA7C88617461E1DF112F45BBB`；Runtime/Upgrade `5C4A3E7A4EAA24377321B897095B4C1811C38DCACBA65C83E9F5A70442E67CF4`。各批准只绑定r2身份和其范围，不沿用首候选批准。

交付：[主目录最终 APK](D:/AIproject/an-jizhang/output/an-jizhang-0.1.7-debug.apk)、[SHA256SUMS](D:/AIproject/an-jizhang/output/SHA256SUMS-v0.1.7.txt)、[147原图及元数据](D:/AIproject/an-jizhang/output/ui-v0.1.7/)。活跃开发目录 output 有逐字节相同副本；产品 APK 为最终008F，测试 APK不交给手机安装。公开debug证书 SHA-256 `209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`，与旧.4/.6一致。[两处副本校验](C:/Users/13765/.codex/worktrees/ledger-v017-recovery/an-jizhang/test-artifacts/ui-v017/recovery-20261010/delivery-copy-verification-r2.json)；旧269原交付和恢复的134副本完整保留。

## 留存问题与明确未验

作者手机30项全部未勾。API26实机、完整TalkBack、真实进程kill及作者对新版审美的接受仍未验证；模拟器／旋转／semantics不替代这些验收。IME补图中的金额为空，只证明备注与Back视觉，不扩大为金额持久的视觉证据。

已知非阻断视觉 Minor 2 项：明细首屏内容较疏；系统2.0字号下年度最右“12”换为上下“1”“2”，文字未丢失，准确每月金额列表可用。lint唯一 UseKtx 为原 SQLite 事务建议；构建另有两处 quadraticBezierTo 弃用提示。未据此引入依赖或改事务。

SQLite schema1／JSON format1／CNY Long整数分／稳定ID／完整字段／事务合并与重复跳过／冲突整批拒绝／50,000条和10MiB限制保持；无INTERNET／遥测／云同步新增。系统备份和设备迁移全域排除、固定依赖与许可资产保持。app自身signature权限与test REORDER_TASKS分别核验，不称绝对零权限。

未提交、推送、合并或发布；未实施S3分类库／预算／schema2。下一步由作者在真实手机完成30项：先在现有应用导出可信备份，再正常覆盖安装本次同签名debug APK。安装异常保留原应用和账本，停止覆盖；不卸载、清库、强降级。完整原备份与新隔离开发目录是回退文件边界，模拟器测试不代替手机备份。

详细实际测试与失败闭环见 [当前验证](VALIDATION_V017.md)，真实作者验收见 [手机30项](PHONE_ACCEPTANCE_V017.md)。
<!-- final-r2-current-end -->

# 0.1.7 独立审查记录

日期：2026-10-08（UTC）；目标 `0.1.7 / code 9`，开发分支 `codex/ledger-statistics-v0.1.5`，基础 HEAD `0cfbb8ec79c882cefc68615152e2ddde73c5b2e5`。对象为未提交工作区及各次冻结 manifest／APK；暂存区为空，未提交、推送或发布。

本轮最终 Source、Native、Runtime／Upgrade、Visual 和 FullDelivery 均待完成。本文件记录已发生的首候选结果及问题，不沿用 0.1.6 Approved，也不以作者尚未查看的新视觉作为审美接受。

## 已发生结果及待闭环项

| 范围 | 当前实际状态 | 原件与边界 |
|---|---|---|
| 实施前独立审查 | 已读取规则、旧源码与 0.1.6 证据并提出保持合同和验证要求 | [initial-audit.md](../test-artifacts/ui-v017/initial-audit.md)；只读规划审查，不是本轮运行或最终批准 |
| 行为测试 | 12 项 domain 实际 RED：12 个 AssertionError／0E／0S；随后首候选 130 JVM 全 0 | [RED](../test-artifacts/ui-v017/chart-red/summary.json)、[首构建](../test-artifacts/ui-v017/candidate-build/state.json) |
| 首候选本地构建 | 成功；15 XML／130 JVM 全 0，lint 0E／10W，两包成功 | [build.log](../test-artifacts/ui-v017/candidate-build/build.log)、[lint.xml](../test-artifacts/ui-v017/candidate-build/lint.xml)；仍是呈现修复前候选 |
| 独立源码首次发现 | I1：收入环图前两分类同色；M1：月日期刻度等距分布偏离真实坐标。owner 已写修复，最终复审待归档 | [chart-report.md](../test-artifacts/ui-v017/chart-report.md)记录修复范围；不据实施者自检填写 Source Approved |
| 首候选 Native 合同检查 | 精确两包合同检查通过、22 工具命令 exit 0；最终候选未批准 | [initial-candidate-status.md](../test-artifacts/ui-v017/native-audit/initial-candidate-status.md)；首 raw／summary 保留，最终另存独立目录 |
| Runtime／Upgrade | 待执行／待独立结论 | 目标 70 UI 与 .6／8→.7／9、.4／6→.7／9 两条 seed／verify 各 1；不借旧版本 runner |
| Visual／作者审美 | before／after 七图配对与字号／深色／窄屏原图及最终独立结论待完成；作者手机未验 | 同 fixture 26 笔、同配置和同测试 APK；源码或截图脚本不证明实际视觉达标 |
| FullDelivery | 待完成 | 最终快照、原件、APK／哈希、两处副本与旧 269 文件保护均须核验；目前没有交付批准 |

首候选 source-manifest SHA-256 `C9E24FAF224103B22006AEECDB84413871E890DC8D035178AAA5FC94CD8F9A2C`；产品 `F20A788A69EF235DDF963393A9AB210B4EC6A8DE064774EEB62DE5979D66116F`（9,671,109 bytes）；测试 `128534B7F4AF8CB5644D7505FE0F4190F28B321FA47B20339B2F871856A4F111`（1,362,423 bytes）。这些值仅绑定首候选，不能自动扩大至修复后的最终源码或包。最终 `test-artifacts/ui-v017/candidate-build-final/` 为计划路径，结果／新哈希待实际归档。

## 最终审查必须确认

- 图表来自完整所选期间并与准确文字同源；Long 分、排序、同期范围和精确类别下钻保持。环图“其余（N类）”只汇总前 5 外数据，无真实类别键或点击；真实“其他”可准确下钻。视觉标签调整不能弱化金额、零值、Long 或旧测试断言。
- I1 配色与 M1 坐标修复由独立 reviewer 复核，最终 lint 验证 ModifierParameter 已消除；不能以改完代码或首构建无错误代替复审。当前 10 个 warning 含本轮 1 项 ModifierParameter，与旧 9 项分开记录。
- 旧 118 JVM／64 UI 逐名保留，实际新 12 JVM／6 UI 都执行；最终 70 UI 的 fresh XML／runner 与源码、两包相符，0F／0E／0S，失败、中断和局部修正分别保留。
- 两条正常覆盖升级仅在受守卫的合成 AVD 执行；seed 后没有清库、卸载、降级、reset、再播种或会清数据的 suite；验证旧 ID、全部字段、schema 1、跨月金额、备份与冷启动。
- 四页 before／after 同 26 fixture、2025-05／2025、同金额和 360dp／720×1280／density320／font1／light；各 7 原图逐张独立审查，额外大字／深色／窄屏、MAX／零空／单点／负结余和 IME／Back 真实取证。原 PNG 不修图，配对元数据与哈希有据。
- Native 对最终 APK 新核验公开签名、package／版本／SDK、精确权限、全域系统备份排除、完整许可证、固定依赖和哈希。应用既有自身 signature 权限与测试 REORDER_TASKS 分别记录，不写绝对零权限；不读私钥或真实账本。
- 最终 Source／Native／Runtime／Upgrade／Visual 报告先归档，再完成独立 FullDelivery。旧 output 与作者正文保护重新核对，真实最终测试结果写入 [验证](VALIDATION_V017.md) 后才形成交付结论。

[30 项手机清单](PHONE_ACCEPTANCE_V017.md)全部未勾。作者对现代视觉、图表理解与手机实际使用的接受度仍待最后确认；技术门禁和旧版作者反馈不能替代这一结果。S3／schema 2／format 2 及正式发布不在本轮范围。
