# 0.1.4 / V1.0 S1 复用审查

日期：2026-10-05（Asia/Shanghai）。本轮只补齐S1；不安装竞品、不复制竞品品牌/截图/源码、不下载新图标或字体。

| 来源 | 固定版本与许可 | 本轮采用边界 |
|---|---|---|
| [Compose状态保存官方说明](https://developer.android.com/develop/ui/compose/state-saving) | 继续Compose BOM 2025.04.01、Lifecycle 2.8.7；AndroidX Apache-2.0 | 原始草稿字段与必要ID使用rememberSaveable/Saver；只阅读API说明，不复制文章或示例。SavedState不保存完整账本。 |
| [Material3官方主题说明](https://developer.android.com/develop/ui/compose/designsystems/material3) | 已有Material3及BOM；AndroidX Apache-2.0 | 使用现有lightColorScheme/darkColorScheme与系统浅深模式判断；自行定义配色，不增加动态远程主题。 |
| [官方日期选择器说明](https://developer.android.com/develop/ui/compose/components/datepickers) | 已有Material3；AndroidX Apache-2.0 | 保留现有日期选择公共API；年月选择器自行组合既有组件。官方最新示例不代表固定版本兼容，须实际编译。 |
| 现有原创Canvas导航图标、金额键盘 | 仓库既有源码，原创 | 保留三导航语义图形、内置金额键盘与备注系统IME；不引入图标库、自定义表达式计算器。 |
| 既有Gradle Wrapper、MoneyBook金额片段 | 已有固定SHA与完整许可见REUSE_AUDIT.md及third_party/NOTICE.txt | 不扩大复制范围，继续整数分金额与原输入校验；完整既有许可保持随APK包装。 |

上述三个官方页面已在本轮实际打开。使用库API与复制文档/示例是不同许可行为；本轮自行实现，没有新增复制的第三方文件，因此没有新增第三方文件哈希或许可。

维护与安全决定：不升级AGP/Kotlin/BOM/Lifecycle/Coroutines，不增加依赖、联网或广泛存储权限；原账目、数据库迁移合同与外部备份格式不变。新增界面API必须在当前固定版本实际编译，并经过原搜索、草稿、操作ID、备份和升级回归。本轮 API 均已在既有固定版本编译，并由 VALIDATION_V014 记录实际测试；来源官方不替代真实运行和作者验收。

深色系统栏问题另实读 [Android 15 target35行为说明](https://developer.android.com/about/versions/15/behavior-changes-15) 和固定 [activity1.10.1官方源码](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.10.1/activity-1.10.1-sources.jar)。仅核验透明系统栏及AUTO图标策略，没有复制第三方实现或升级依赖；新增windowBackground日／夜颜色均按本项目已有配色自行定义。API26的native窗口JVM回归为Robolectric模型，实际设备仍仅API35临时模拟器。两种证据与自然手机验收分别记录。