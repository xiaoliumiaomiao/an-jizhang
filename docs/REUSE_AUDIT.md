# 首版复用审查

固定来源、许可证与修改列表见 [NOTICE](../third_party/NOTICE.txt)。完整许可证同时保存在 third_party 和 APK assets/licenses。

- 官方 [android/architecture-samples](https://github.com/android/architecture-samples/tree/ee66e1526b84c026615df032c705842b7d2a521f) 固定 SHA ee66e1526b84c026615df032c705842b7d2a521f：只采用 Gradle wrapper 四件套与 Apache-2.0 LICENSE。jar SHA256 已实际校验为 c5a643cf80162e665cc228f7b16f343fef868e47d3a4836f62e18b7e17ac018a。properties 指向 Gradle 8.11.1 官方分发并设置 checksum；未引入样例业务、权限或依赖。
- [AndersOnLin4/moneybook](https://github.com/AndersOnLin4/moneybook/tree/98c50341da262e839f68557e721eee38aca57067) 固定 SHA 98c50341da262e839f68557e721eee38aca57067：MIT，原始 Format.kt 经 raw.githubusercontent.com 读取，只适配 String.toCents 的 BigDecimal 精确换算。增加十进制格式、正数和上限校验。原项目 Double 格式化与加密/恢复模块均不采用；代码片段无额外依赖及权限。
- 本应用 AndroidX/Compose 与 Robolectric 通过固定 Maven 版本使用，测试依赖不进入生产 APK。未整体 fork 项目，未执行第三方安装脚本。

固定版本使采用内容可复查，维护更新仍须重新验证兼容性和许可证。Gradle/AGP/Kotlin/Compose 是否兼容，以实际构建及测试结果为准。

## 备选审查（只参考，不采用源码）

| 候选及固定提交 | 许可证 | 决定 |
|---|---|---|
| [BudgetBudy](https://github.com/devaldaki3/BudgetBudy/tree/928602bcee05a1f8ea4431452971f4367e048f9c) | MIT | 功能/结构参考，首版无需整体依赖 |
| [Xpense](https://github.com/saranzafar/expense-tracker-flutter-mobile-app/tree/9a59521519ba8925ffc618cb2b86ef36fa5c9468) | MIT | 云相关依赖多，不符合纯本地首版 |
| [SplitMate](https://github.com/Sagar-kesarkar/SplitMate/tree/fbe434c2266916b9c32885f0af6aa7c974c43947) | MIT | 权限及功能范围较重，不采用 |
| [Flow](https://github.com/flow-mn/flow/tree/faeab54a0eb04a85ed0751c8ae9e9cd031849fc2) | GPL | 仅界面设计参考，不复制代码 |

候选由主 agent 固定 SHA 审查；本实现实际只下载并采用 NOTICE 所列两项。
