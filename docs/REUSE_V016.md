# 0.1.6 复用与实现说明

记录日期：2026-10-08。范围为月／年分类比例条及备份操作错误与统计错误的隔离；版本 `0.1.6 / code 8`。沿用已有统计、分类钻取、状态管理与许可证资产，数据库 schema 1、备份 format 1 保持。

## 本轮复用

| 需求 | 使用已有能力 | 本轮边界 |
|---|---|---|
| 月／年收入、支出分类比例条 | 现有完整期间聚合、稳定分类排序与准确分类钻取；Compose 原生绘制与布局 | 按本期间、本类型总额显示比例；单分类 100%，空期间不制造比例条。沿用文字金额及占比，保留长名称、大金额与点击目标可读性。 |
| 比例条可访问性 | Compose semantics、既有文字与触摸目标 | 表达分类、金额和比例，信息不只由颜色传递；验证普通／大字号和明暗主题。 |
| 导出或恢复失败后的统计 | 既有 ViewModel、统计刷新与操作反馈 | 操作错误不再把有效统计变成读取失败；真实写入失败保留对应说明，后续刷新不虚构数据库未变。 |
| 测试与视觉证据 | 既有 JVM、Compose instrumentation、截图及升级 runner | 复用原 111 JVM、55 UI，新增 7 JVM／9 UI；保留旧业务断言及严格版本检查，不以局部通过代替完整运行。 |

静态分类占比使用轻量 Compose 绘制与语义，不引入图表库、网络服务或新产品素材。实现参考 Compose 官方 [绘图](https://developer.android.com/develop/ui/compose/graphics/draw/overview)、[语义](https://developer.android.com/develop/ui/compose/accessibility/semantics) 与 [进度组件](https://developer.android.com/develop/ui/compose/components/progress)；本产品展示的是分类占比，文字范围与金额继续由已有账目统计提供。

## 依赖、许可证与数据兼容

本轮依赖保持：AGP 8.9.2、Kotlin 2.1.20、Gradle 8.11.1、Compose BOM 2025.04.01／Runtime 1.8.0／Material3 1.3.2、Activity 1.10.1、Lifecycle 2.8.7、Coroutines 1.10.2。offline 设备运行缺失的固定 UTP 工具组件后来补齐缓存，属于现有构建工具，不是新增应用依赖。

产品 APK 内 Android Architecture Samples、MoneyBook 许可证及 NOTICE 三份资产与源码逐字节一致，见 [原生审计](../test-artifacts/ui-v016/native-audit/report.md)。产品保留既有自身 signature receiver 权限，无 Android 系统 uses-permission／联网权限；测试 APK 的 REORDER_TASKS 单独记录。系统备份／设备迁移全域排除保持，未启用系统账本备份。

S3 分类库、预算、schema 2／format 2 迁移不在本轮实施范围。完整迁移与保持当前功能的两条路径由父会话决定，分别考虑数据迁移风险和工期；本轮验证不会预设该决策。

## 验证状态

本轮候选构建成功，118 JVM全0、lint0E9W；原两套64/2F与一套INCOMPLETE保留，修复后局部3项／4项通过，最终完整64项全0、旧55逐名missing0、新9全部在。同签名0.1.4→0.1.6升级seed／verify各1通过，schema1与三笔每字段／两月汇总保持；19新鲜原图、两处42新副本及旧227文件核验已归档。独立Runtime／Upgrade和19图Visual已Approved、C0/I0/M0；本轮未提交技术FullDelivery独立Approved、C0/I0/M0。见[VALIDATION_V016](VALIDATION_V016.md)、[REVIEW_V016](REVIEW_V016.md)；[22项手机清单](PHONE_ACCEPTANCE_V016.md)全部未勾。
