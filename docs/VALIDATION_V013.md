# 0.1.3 验证记录

状态：技术验证进行中，作者手机验收待执行。以下只记录已经产生的证据，不将旧版验收或编译成功当成本轮设备通过。

## 范围与基线

作者已确认 0.1.2 通过；已验收 main 为 `ff8479f75cd917bc49e5ab2173ad7e9906dc480c`。本轮在 `feat/ledger-ui-v0.1.3` 重构明细、统计、设置、详情及全屏编辑，不改变 schema1、备份 format1、整数分规则、applicationId、minSdk26、依赖或权限。

开发前的官方成熟产品比较与复用决定见 [REUSE_V013](REUSE_V013.md)，需求和状态契约见 [DESIGN_V013](DESIGN_V013.md)，作者检查见 [PHONE_ACCEPTANCE_V013](PHONE_ACCEPTANCE_V013.md)。所有设备测试使用隔离 CI AVD 与合成账目；不操作作者手机数据库。

## 已执行的本机验证

- 基线：`build-20261005-025531.log`，原 50 JVM 用例全通过，7 XML 已归档。
- 日期分组：4 个新用例实际 RED 为 3 失败，GREEN 为 4 通过；原 50 个名称保留，全套增加到 54。不为颜色值编写镜像测试。
- 失败 operation ID：新用例先实际失败，再通过；连续同文案失败仍产生独立 ID，旧成功不误消费，合法重试可完成。
- 新界面完整构建：`build-20261005-032114.log`，55 JVM、0 失败/错误/跳过，lint 0 错误/9 提示，应用和仪器测试 APK 编译成功。XML、原日志及 lint 在 ignored `test-artifacts/sdd-v013/final-local-after-palette/`。
- 两个新 VM 恢复用例：`build-20261005-032924.log` 编译成功；`StateRestorationTester` 只模拟保存的 Compose UI 状态恢复到新 VM，没有实际杀 Android 进程。

原 50 JVM、原 6 Smoke 和 4 Query 的断言、同 VM 与 10 秒等待均保留。旧 UI 只调整新页面访问路径，没有用 `assertExists` 替换可见性断言。新增恢复测试对真实排队任务、同 ID 保存及删除 sentinel 的数据库结果作断言。

## 已执行的 CI RED

[旧界面运行 37227250211](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37227250211)，代码 `0e0896f51173b9ff937519a63c3af0b2d76e723c`：build success，JVM 54 通过；connected 共 13，原 10 全通过，新增 Flow 3 在旧版缺少 `ledger-list`/`nav-settings` 等入口处失败，0 错误/跳过。升级链因 connected 失败未执行。原始日志/XML 和失败详情保存在 ignored `test-artifacts/ci-v013-red-37227250211/`。

最终源码审查发现 F1：保存 UI 的 pending IDs 恢复到新 VM 后，没有后台任务能产生其完成结果，可能永久禁用操作。两个恢复 fixture 经独立准备审查后随新 UI 提交到有意回归检查点 `fe2974467826e65b178fb6fab24d0a0c4755ce9d`；[运行 37228768041](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37228768041) 待完成及原始结果读取。此检查点不是交付版本。

## 待完成门禁

owner 最小修复、最新完整本机构建和独立增量审查；真实新 UI、恢复及窄屏 CI；8 张原生合成截图；固定 0.1.2/code3 → 0.1.3/code4 的同证书替换、seed/verify 原始 runner、安装与启动；最终本机 APK 签名/权限/许可审计、新文件复制和旧文件哈希保留；私有 draft PR、最终三方 HEAD 同步及作者手机验收。尚不声称技术交付或作者验收完成。
