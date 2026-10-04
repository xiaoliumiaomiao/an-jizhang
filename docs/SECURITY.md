# 首版安全检查

检查日期：2026-10-04。此记录区分源代码、直接依赖、最终APK和作者设备结果。

## 资产与依赖

- 官方wrapper固定SHA并实际校验JAR及Gradle分发SHA256；MoneyBook仅适配MIT许可的精确金额解析片段，不采用其加密/覆盖恢复模块。完整许可保存在third_party与APK assets，来源见REUSE_AUDIT.md。
- 只读检查实际Gradle缓存中的AndroidX Compose/Activity/Lifecycle、Kotlin标准库与协程直接运行时依赖。对8个指定坐标/版本使用OSV querybatch查询，返回无已知公告；GitHub匿名REST查询遭限流，未把请求失败算作通过。
- Kotlin旧公告 [GHSA-2qp4-g3q3-f92w](https://github.com/advisories/GHSA-2qp4-g3q3-f92w)影响<=1.5.32；本项目2.1.20不在该公告影响范围内。AndroidX检索命中的Sentry回放组件未采用。
- 本检查不是全量传递依赖/操作系统无漏洞保证；新增或升级依赖需要重新审查。JUnit/Robolectric属于测试依赖，不随生产APK打包。

## 账本与恢复

- 金额整数分；持久化前验证记录；备份大小、格式版本、币种、字段类型、日期、记录条数及重复ID均检查。
- 同ID内容冲突整批拒绝，SQLite事务回滚，已经有批末冲突真实行为测试。
- 明文JSON便于作者自行保存及迁移；导出界面必须明确文件包含账目。没有宣传“已加密”或加入固定密钥。
- 数据在应用私有目录；源Manifest无联网/广泛存储权限，关闭自动备份。最终合并Manifest与APK资源将在VALIDATION.md记录，不能仅以源代码推断。

## 当前状态

核心及供应链独立审查无阻塞项。界面、文档选择器、最终APK权限、模拟器和作者实机验收继续按HANDOFF.md/VALIDATION.md记录实际进度。
