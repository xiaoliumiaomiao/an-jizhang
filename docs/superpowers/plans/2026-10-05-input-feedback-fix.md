# 0.1.3 成功反馈与金额键盘修复计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 删除等成功反馈自动结束；金额用应用内数字键盘，备注保留系统输入法。

**Architecture:** 保留当前ViewModel、操作ID和Editor状态合同；成功消息增加独立ID与单调期限，root等待剩余时间后按ID清除。金额沿用String草稿和Money.parse，现有滚动表单内使用只读金额栏及数字按钮，不新增输入法、第二Dialog或依赖。

**Tech Stack:** 当前固定Kotlin/Compose1.8/Material3 1.3.2、Robolectric及Android API35 CI。

## 范围及保护

- 主目录有作者未提交的HANDOFF和规划文件，完全保留；实施只在已附工作区feat/ledger-ui-v0.1.3，PR4保持draft。
- 本轮仅两个修复；保留schema1、backup format1、整数分、min26、applicationId、原依赖/权限/签名。
- 成功提示约3秒，旋转不刷新期限；独立消息ID覆盖保存/删除/导出/恢复，同文案重复操作不能互相误清。只清成功消息，不清error及completed/failed/pending IDs，不新增VM定时Job。
- 金额数字0–9、小数点、退格、清空，尾部编辑；空串点转0.，第二点/第三位小数/超九位整数不追加。保留0暂态和原最终金额校验，不用浮点数。备注、分类、日期、搜索继续原系统IME；按金额或数字时收IME且清文本焦点。
- 数字区在原表单内滚动，触控至少48dp；金额及其他字段仍rememberSaveable，稳定ID、取消/失败/重建及busy提交保护保留。
- 原55 JVM/16UI逐名保留；旧金额输入动作改实际按键，其他字段后严格closeKeyboard保持，不扩大10秒等待或改提交Job观测。
- 构建串行；隔离CI合成数据，不在作者手机运行测试。旧APK/校验/截图保留，新输出an-jizhang-0.1.3-fix1-debug.apk，versionName0.1.3/code5。
- 先按官方固定源码核验安全复用，资料及采用决定记到REUSE_INPUT_FEEDBACK.md；不导入商业代码或第三方键盘库。

## 文件职责

| 文件 | 责任 |
|---|---|
| ui/LedgerViewModel.kt / LedgerScreen.kt | 消息ID、期限及按ID清除；root短时显示 |
| ui/AmountKeypadInput.kt / AmountKeypad.kt / EntryEditor.kt | 纯数字转换、内联按钮及只读金额、IME交接 |
| LedgerViewModelTest.kt / AmountKeypadInputTest.kt | 真实DB消息竞争/错误保留与金额输入边界RED/GREEN |
| LedgerInputFeedbackTest.kt / AmountKeypadTestSupport.kt | 真实删除消失、数字按键、备注IME及恢复；用户动作helper |
| 既有五仪器测试类 | 仅金额动作适配，全部业务断言/锁/原native等待保留 |
| app/build.gradle.kts / LedgerUpgradeTest.kt / verify-upgrade.sh | code5覆盖包身份；固定旧基线/严格runner规则保持 |
| 交付/验证/手机表 | 记录本轮实际结果及独立审查，旧证据仍保留 |

## Task 1：可复现回归与纯状态修复

- [ ] 实读官方API/现有消息路径，完成复用与诊断记录。
- [ ] 新消息及数字转换用例先运行有效RED，再最小实现，运行全部JVM确认原55保留。
- [ ] 新3仪器测试先在旧Editor/root运行真实RED（旧16保留）；记录原XML/日志，不把编译当设备结果。

## Task 2：实际界面修复

- [ ] root按期限和ID清成功提示，旧错误继续可见。
- [ ] 只读金额+内联数字键盘；从备注IME返回金额收IME；原字段、候选ID、最终校验与busy保护保留。
- [ ] 原16金额动作改真实按键，其他系统输入路径的严格等待与原断言保留。
- [ ] 串行完整构建、lint、AndroidTest编译并独立规格/质量审查，修阻塞后复审。

## Task 3：可安装交付

- [ ] frozen code真实CI全套、旧同证书覆盖、原始runner和图像核验；原55/16与新测试逐名读取实际XML。
- [ ] 新包version/code/权限/完整许可/公开签名审计，固定新文件双output复制和SHA；旧APK/校验前缀保留。
- [ ] 更新PR4及正式交付文档；最终独立完整审查、local/remote/PR同步，main与作者改动保留。
- [ ] 提供修复APK及本轮手机检查入口；未由作者验证项保持未勾，不合main。
