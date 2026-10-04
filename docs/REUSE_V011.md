# 0.1.1 开发前复用审查

2026-10-04。本轮在实施前由另一subagent只读核对开发社区与官方资产；主agent复核官方文档。决定继续使用既有固定版本JUnit4、AndroidX Test及官方SDK adb/apksigner，无新增生产或测试依赖，不复制外部源码，不执行第三方安装脚本。

| 复用资产 | 核对来源 | 决定 |
|---|---|---|
| adb | [Android官方文档](https://developer.android.com/tools/adb)、[AOSP NOTICE](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/NOTICE) | 使用已有官方Platform Tools；覆盖安装用-r，指定隔离模拟器serial，不操作作者手机 |
| APK签名检查 | [apksigner](https://developer.android.com/tools/apksigner)、[签名文档](https://developer.android.com/studio/publish/app-signing) | 只读取APK公开证书摘要；CI同job临时debug签名，不上传本机私钥 |
| instrumentation方法筛选 | [官方命令行测试文档](https://developer.android.com/studio/test/command-line) | 沿用AndroidX runner1.6.2/ext junit1.2.1，分别运行升级seed和verify；[AndroidX Apache许可](https://github.com/android/android-test/blob/main/LICENSE) |
| JSON边界夹具 | [JSONTokener](https://developer.android.com/reference/org/json/JSONTokener)、[JUnit4.13.2断言API](https://junit.org/junit4/javadoc/4.13.2/org/junit/Assert.html) | 合成合法记录，自写断言保护既有严格门禁；JUnit4[许可为EPL-1.0](https://github.com/junit-team/junit4/blob/r4.13.2/LICENSE-junit.txt)，属于已有测试依赖，不进入生产APK |
| 版本规则 | [Android版本文档](https://developer.android.com/studio/publish/versioning) | 应用版本code1→2/name0.1.0→0.1.1，SQLite schema和备份version保持1 |

以上移动文档仅作为公共API参考，无新增复制源码，因此没有新上游源码SHA。原首版固定SHA的MoneyBook片段、官方Wrapper及完整许可证照旧保留，见REUSE_AUDIT.md。

升级链风险已识别：既有LedgerSmokeTest会清测试库，connected任务结束卸载主包；它们不能夹在旧→新链中。专门夹具只在空白临时模拟器写入虚构记录，覆盖安装后比较所有字段及统计，遇到未知旧数据直接失败。实际执行结果另记VALIDATION_V011.md，不由本调研推定通过。
