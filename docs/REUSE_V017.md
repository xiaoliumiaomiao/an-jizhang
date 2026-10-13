# 0.1.7 复用调研与采用决定

查验日期：2026-10-08（UTC）。本轮目标 `0.1.7 / versionCode 9`：统计以真实图表为主，统一明细、统计、记一笔／编辑和设置的视觉。以下是实时来源核验及实施决定，不是已完成代码、安全认证或运行验证。

## 来源、维护、许可与成本

| 来源与固定方式 | 实时查验结果 | 采用决定、成本与安全边界 |
|---|---|---|
| [Cashew](https://github.com/jameskokoska/Cashew)，查验当日公开 main／README；不导入库或代码版本 | README 说明 Flutter、Drift 与 Firebase；[LICENSE](https://raw.githubusercontent.com/jameskokoska/Cashew/main/LICENSE) 为 GPL v3 或更高版本。README 指向 commits／changelog，仓库页面未显示归档；最新提交时间本次未取得，不能据此宣称持续维护 | 只参考图表主导、紧凑总览与分类信息层级。Flutter 技术栈及云服务不进入应用；不复制源码、截图、图标、品牌或文案，不产生新增运行依赖。 |
| [Ivy Wallet](https://github.com/Ivy-Apps/ivy-wallet)，归档 main；不导入代码版本 | GitHub 明示 2026-04-11 archived／read-only，描述为 no longer maintained；[LICENSE](https://raw.githubusercontent.com/Ivy-Apps/ivy-wallet/main/LICENSE) 为 GPL-3.0 | 仅作历史产品组织参考。停止维护和 GPL 采用边界不适合本轮私有交付，不复制其模块、代码或素材。 |
| Google Compose Samples：[JetLagged README](https://raw.githubusercontent.com/android/compose-samples/main/JetLagged/README.md)、[Reply README](https://raw.githubusercontent.com/android/compose-samples/main/Reply/README.md)，查验当日 main；不导入示例版本 | Google 官方示例；JetLagged 展示自定义布局／图形，Reply 展示导航、主题及适应尺寸；两个 README 与仓库 [LICENSE](https://raw.githubusercontent.com/android/compose-samples/main/LICENSE) 均注明 Apache-2.0。README 可读；最新提交时间本次未取得，不把示例最新环境视为本项目依赖合同 | 参考图形区域的层级、统一主题和尺寸适应原则。本轮原创实现，不搬示例代码、AGSL shader、动画、图片或 adaptive 新依赖。固定旧 Compose 上逐项实际编译和验证。 |
| [Vico](https://github.com/patrykandpatrick/vico)，候选固定 [v3.3.1](https://github.com/patrykandpatrick/vico/releases/tag/v3.3.1)／[版本 LICENSE](https://raw.githubusercontent.com/patrykandpatrick/vico/v3.3.1/LICENSE) | releases/latest 实际重定向 v3.3.1，页面列出后续提交及 Android 图表崩溃修复；Apache-2.0。[固定版本目录](https://raw.githubusercontent.com/patrykandpatrick/vico/v3.3.1/gradle/libs.versions.toml) 列 Kotlin 2.4.10、Compose 1.12.0、Material3 1.9.0、AGP 9.3.2 | 不采用。目录是其项目版本信息，不能简化为每个已发布 artifact 的最低要求，但与本项目固定链存在明显差异，未做 artifact 兼容试验。最低 SDK 条件相符也不能证明 Kotlin／Compose ABI 和工具链兼容；不为图表升级工具链或引入额外传递依赖。 |
| 已有 Compose Canvas／semantics；沿用 BOM 2025.04.01／Runtime 1.8.0／Material3 1.3.2 | 实时阅读 Android 官方 [绘图 API](https://developer.android.com/develop/ui/compose/graphics/draw/overview) 与 [semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)。文档是 API 参考，不是可直接导入的固定实现 | 采用现有 API 原创月每日收支趋势、全年 12 月双柱、同类型分类环图与横条。无图表库；自行承担零值／单点／极值／尺寸、配色和语义的验证成本。 |

本次公开 GitHub API 与部分 commits 页面读取失败，因此没有取得 Cashew／Google Samples 的最新提交 SHA。它们仅是查验日期明确的设计参考，不是固定并复制的实现资产；不虚构版本或维护日期。Vico 的候选版本已固定，实际采用的 AndroidX 依赖继续按仓库旧版本固定。

## 原创实现及保留合同

- 图表输入来自完整所选月／年的账目，不受主明细搜索或收支筛选影响。分类环图、横条、金额与钻取统一使用当前选定类型及精确修剪分类键。
- 金额聚合、总额、结余、排序和比较继续使用 Long 整数分与精确边界检查。仅最终 Canvas 坐标允许浮点投影，不由浮点反算金额，不把圆环或线条面积当业务数据。
- 无记录月／日保留零值和文字说明；空类型不绘制假比例。单点、单分类 100%、1 分钱、最大单笔、多笔大金额及负结余单独验证，不除零、不越界、不显示 NaN／Infinity。
- 图表同时提供期间、类型、日期或分类、准确金额、文字占比的 semantics 与可读文字清单，不能只依赖颜色、形状或图像。复用原 testTag；新增标记不覆盖旧业务标记。
- 统一浅深主题、系统中文 sans、间距和圆角；不下载远程字体或图库，不复制竞品资产。任何未来实际新增第三方文件须另记固定版本／SHA、来源、许可证、修改和哈希；完整许可证随 APK 保留。

固定栈保持：AGP 8.9.2、Kotlin 2.1.20、Gradle 8.11.1、Compose BOM 2025.04.01、Activity 1.10.1、Lifecycle 2.8.7、Coroutines 1.10.2；minSdk 26、compile／target 35。保留 SQLite schema 1、JSON format 1、应用私有目录、系统备份排除和现有权限；不新增联网、遥测、云同步、分类库或预算迁移。

## 当前结果与待验

接续资料 [0.1.6 验证](VALIDATION_V016.md) 记录 2026-10-08 的 118 JVM／64 UI、0.1.4→0.1.6 升级与技术独审成功，属于上一轮证据。0.1.7 的本地 JVM、build／lint、完整模拟器 UI／新图表边界与语义、0.1.6／8→0.1.7／9 覆盖升级、同 fixture 四页 before／after 原图、大字／深色／窄屏和独立审查均待执行，不能继承上一轮通过。

执行与验收合同见 [现代 UI 计划](superpowers/plans/2026-10-08-modern-ui-v017.md)。作者手机验收与手机 debug 留在本轮技术核验后的最终阶段，不预填通过；不提交、推送或发布。
