# 0.1.8 复用审查（2026-10-12）

采用现有固定依赖：Android SQLiteOpenHelper/SQLiteDatabase、Compose Material3 1.3.2 DatePicker/Scaffold/FabPosition、Activity1.10.1 SystemBarStyle、SharedPreferences。本机缓存AAR/官方源码确认所需API存在，不更新依赖。沿用已打包的许可/署名资产，不整仓复制、不执行第三方安装脚本。

来源与决定：

- https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper ：采用版本事务升级，保留旧entries表；无破坏性重建。
- https://www.sqlite.org/lang_altertable.html ：新增nullable account_id，旧六字段不改写。
- https://www.sqlite.org/lang_conflict.html ：父账户更新不用会删除旧行的REPLACE。
- https://developer.android.com/develop/ui/compose/components/datepickers ：复用日历弹窗，隐藏输入模式入口；整行点击日历。
- https://developer.android.com/reference/androidx/activity/SystemBarStyle ：以已缓存1.10.1源码为准同步手动主题与原生系统栏。
- https://developer.android.com/license ：参考官方API文档；不复制第三方业务实现。AndroidX既有Apache-2.0许可继续保留。

账户业务以本项目整数分领域模型实现。不采用银行SDK、基金行情、网络权限、第三方ORM、遥测或云服务，避免增加离线账本权限与升级依赖风险。审计报告见 test-artifacts/ui-v018/research/ui-audit.md、baseline-audit.md；这些是研究，不是运行验收证明。
