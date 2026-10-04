# 开发与构建

当前仓库使用功能分支开发，账本、工具、缓存、APK及签名密钥不会提交Git。先读 agent.md、REQUIREMENTS.md、HANDOFF.md。

## 这台 Windows 的一条构建路径

本次会话下载的官方 JDK 与 Android SDK 保存在 `.tooling`；`.tooling/toolchain.json` 记录版本、路径、来源和校验值。脚本读取配置，仅设置当前进程变量，结束恢复原值，不改系统 PATH。

在项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

命令依次运行单元/SQLite测试、Android lint、debug APK编译。成功信号：`BUILD SUCCESSFUL`；APK位于 `app/build/outputs/apk/debug/app-debug.apk`，日志位于忽略的 `test-artifacts`。

如失败，先查看最后一个日志中的第一处 `FAILURE` / `failed`，按报错修复后重复同一条命令。不要删除数据库或真实备份来解决构建问题。

## 新机器

先安装官方 Android Studio 并在 SDK Manager 安装 Android SDK Platform 35、Build-Tools 35.0.0，然后在 Android Studio 打开项目。使用兼容的 JDK17或21；本次验证用的具体版本见 VALIDATION.md。IDE会用提交的 Gradle Wrapper 获取校验过的Gradle8.11.1。

如果需要命令行，传入机器现有工具路径，不复制本机绝对路径配置：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1 -JavaHomePath '你的JDK目录' -SdkRootPath '你的Android SDK目录'
```

官方安装说明：[Android Studio](https://developer.android.com/studio/install)。

## 测试与恢复边界

- JVM/domain：金额输入和统计；Robolectric：真实Android SQLite及JSON行为。两者不能替代手机验收。
- GitHub Actions：执行编译/lint/tests以及Android35模拟器测试；只有运行报告成功才能标为通过。
- 本地输出是debug测试APK。正式发布需要单独签名、版本策略与作者授权。
- 回退代码可保留当前分支，再切回此前提交；不要用 `git reset --hard` 清除未保存工作。
- 应用升级保留数据依赖签名及数据库迁移。卸载会删除账本，操作前先在应用中导出备份；备份是明文，选可信位置保管。

## 0.1.1 CI设备与升级测试

CI在仅含合成数据的临时模拟器运行6项smoke。它们会清理测试数据库；升级fixture的两个方法要求不同真实安装版本，因此由后面的专门链分别执行。CI的smoke命令为：

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.xiaoliumiaomiao.anjizhang.LedgerUpgradeTest --no-daemon
```

CI先在同job从固定57799ca基线构建旧APK，再完成上述smoke，最后用相同临时debug证书执行旧版seed、install-r新版、verify，旧→新链中保留数据库。verify-upgrade.sh限定CI=true、唯一emulator且AVD名ci-ledger，并严格检查两个runner结果；作者手机按PHONE_ACCEPTANCE_V011.md做正常安装与操作验收。APK哈希、公开证书、runner原始/规范化输出和截图存于android-device产物。

脚本单方法测试使用`am instrument -w -r`：`-w`等待完成，`-r`提供原始runner协议；默认展示格式可能只有`OK (1 test)`而没有completion code。脚本要求两者同时成功，防止ADB退出0掩盖测试失败，参见[官方参数说明](https://developer.android.com/studio/test/command-line)。
