# 首版架构

Kotlin 原生安卓应用，单 Activity，Jetpack Compose Material3 界面。采用小型应用的直接依赖注入，不引入 Hilt、网络客户端或图片加载器。

- `domain/`：Entry、Money、LedgerRules、MonthlySummary，纯 Kotlin 规则，可直接单元测试。
- `data/`：LedgerDatabase 基于 Android SQLiteOpenHelper，CRUD 和备份合并事务；BackupCodec 使用 Android org.json，严格验证输入。
- `ui/`：LedgerViewModel 以 StateFlow 管理状态，数据库/文件 I/O 使用后台协程；LedgerScreen 提供记账、统计、设置及编辑对话框。
- `MainActivity.kt`：Activity Result API 连接系统文档选择器；生命周期安全地处理备份结果。

数据库是唯一事实来源。金额 Long 整数分，日期 LocalDate（API26原生可用），UUID保证备份去重稳定性。DB v1 后续升级只可增加显式迁移，不允许删除重建。

只对金额解析片段和官方 Gradle Wrapper 做代码级复用；业务流程、存储和备份重新实现以符合已确认需求。保留第三方许可证于仓库及 APK assets，应用设置展示。

构建依赖固定版本，Gradle分发包配置SHA-256。JDK/SDK仅项目范围，local.properties和缓存不提交。正式签名与商店部署未授权，不创建发布密钥。
